/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.ability.earth.sequences;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Predicate;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.Pillar;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.EarthMaterials;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.util.NumberConversions;

public class EarthPillars extends AbilityInstance {
  private static final Config config = new Config();

  private static AbilityDescription pillarsDesc;

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Collection<Pillar> pillars = new ArrayList<>();
  private final Collection<Entity> affectedEntities = new HashSet<>();
  private Predicate<Block> predicate;

  private double factor;

  public EarthPillars(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
    this.user = user;
    recalculateConfig();

    factor = 1;
    if (method == ActivationMethod.FALL) {
      double dist = user.entity().getFallDistance();
      if (dist < userConfig.fallThreshold || user.sneaking()) {
        return false;
      }
      if (dist >= userConfig.maxFallThreshold) {
        factor = userConfig.maxScaleFactor;
      } else {
        double fd = userConfig.fallThreshold;
        double deltaFactor = (userConfig.maxScaleFactor - factor) * (dist - fd) / (userConfig.maxFallThreshold - fd);
        factor += deltaFactor;
      }
    }

    predicate = b -> EarthMaterials.isEarthNotLava(user, b);
    Collider collider = new Sphere(user.location(), userConfig.radius * factor);
    CollisionUtil.handleEntityCollisions(user, collider, this::createPillar, true);

    if (!pillars.isEmpty()) {
      user.addCooldown(description(), userConfig.cooldown);
      removalPolicy = Policies.builder().build();
      return true;
    }
    return false;
  }

  @Override
  public void recalculateConfig() {
    userConfig = Bending.game().attributeSystem().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    pillars.removeIf(pillar -> pillar.update() == UpdateResult.REMOVE);
    return pillars.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
  }

  private boolean createPillar(Entity entity) {
    Block base = entity.getLocation().getBlock().getRelative(BlockFace.DOWN);
    boolean unique = pillars.stream()
      .noneMatch(p -> p.origin().getX() == base.getX() && p.origin().getZ() == base.getZ());
    if (predicate.test(base)) {
      if (unique) {
        ParticleUtil.create(Particle.BLOCK_DUST, entity.getLocation())
          .count(8).offset(1, 0.1, 1).data(base.getBlockData()).spawn();
        int length = NumberConversions.floor(3 * factor);
        Pillar.builder(user, base, EarthPillar::new).predicate(predicate).build(length).ifPresent(pillars::add);
      }
      return true;
    }
    return false;
  }

  public static void onFall(User user) {
    if (user.selectedAbilityName().equals("Catapult")) {
      if (pillarsDesc == null) {
        pillarsDesc = Bending.game().abilityRegistry()
          .abilityDescription("EarthPillars").orElseThrow(RuntimeException::new);
      }
      Bending.game().activationController().activateAbility(user, ActivationMethod.FALL, pillarsDesc);
    }
  }

  @Override
  public @NonNull User user() {
    return user;
  }

  private class EarthPillar extends Pillar {
    protected EarthPillar(@NonNull PillarBuilder builder) {
      super(builder);
    }

    @Override
    public boolean onEntityHit(@NonNull Entity entity) {
      if (entity.equals(user.entity())) {
        return false;
      }
      if (!affectedEntities.contains(entity)) {
        affectedEntities.add(entity);
        DamageUtil.damageEntity(entity, user, userConfig.damage * factor, description());
      }
      entity.setVelocity(Vector3.PLUS_J.scalarMultiply(userConfig.knockup * factor).clampVelocity());
      return true;
    }
  }

  private static class Config extends Configurable {
    @Attribute(Attribute.COOLDOWN)
    public long cooldown;
    @Attribute(Attribute.RADIUS)
    public double radius;
    @Attribute(Attribute.DAMAGE)
    private double damage;
    @Attribute(Attribute.STRENGTH)
    private double knockup;

    public double maxScaleFactor;
    public double fallThreshold;
    public double maxFallThreshold;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "earth", "sequences", "earthpillars");

      cooldown = abilityNode.node("cooldown").getLong(6000);
      radius = abilityNode.node("radius").getDouble(10.0);
      damage = abilityNode.node("damage").getDouble(2.0);
      knockup = abilityNode.node("knock-up").getDouble(0.8);

      maxScaleFactor = abilityNode.node("max-scale-factor").getDouble(1.5);
      fallThreshold = abilityNode.node("fall-threshold").getDouble(12.0);
      maxFallThreshold = abilityNode.node("max-fall-threshold").getDouble(60.0);
    }
  }
}

