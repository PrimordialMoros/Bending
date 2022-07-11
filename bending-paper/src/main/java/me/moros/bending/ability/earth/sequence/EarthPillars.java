/*
 * Copyright 2020-2022 Moros
 *
 * This file is part of Bending.
 *
 * Bending is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bending is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bending. If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.ability.earth.sequence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import me.moros.bending.ability.common.Pillar;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.registry.Registries;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.EarthMaterials;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class EarthPillars extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private static AbilityDescription pillarsDesc;

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Collection<Pillar> pillars = new ArrayList<>();
  private final Collection<Entity> affectedEntities = new HashSet<>();
  private Predicate<Block> predicate;

  private double factor;

  public EarthPillars(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    loadConfig();

    factor = 1;
    if (method == Activation.FALL) {
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
    CollisionUtil.handle(user, collider, this::createPillar, true);

    if (!pillars.isEmpty()) {
      user.addCooldown(description(), userConfig.cooldown);
      removalPolicy = Policies.builder().build();
      return true;
    }
    return false;
  }

  @Override
  public void loadConfig() {
    userConfig = ConfigManager.calculate(this, config);
  }

  @Override
  public UpdateResult update() {
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
        ParticleUtil.of(Particle.BLOCK_DUST, EntityUtil.entityCenter(entity))
          .count(8).offset(1, 0.1, 1).data(base.getBlockData()).spawn(user.world());
        int length = FastMath.floor(3 * factor);
        Pillar.builder(user, base, EarthPillar::new).predicate(predicate).build(length).ifPresent(pillars::add);
      }
      return true;
    }
    return false;
  }

  public static void onFall(User user) {
    if (user.selectedAbilityName().equals("Catapult")) {
      if (pillarsDesc == null) {
        pillarsDesc = Objects.requireNonNull(Registries.ABILITIES.fromString("EarthPillars"));
      }
      user.game().activationController().activateAbility(user, Activation.FALL, pillarsDesc);
    }
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  private final class EarthPillar extends Pillar {
    private EarthPillar(Builder builder) {
      super(builder);
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      if (entity.equals(user.entity())) {
        return false;
      }
      if (!affectedEntities.contains(entity)) {
        affectedEntities.add(entity);
        DamageUtil.damageEntity(entity, user, userConfig.damage * factor, description());
      }
      EntityUtil.applyVelocity(EarthPillars.this, entity, Vector3d.PLUS_J.multiply(userConfig.knockup * factor));
      return true;
    }
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 6000;
    @Modifiable(Attribute.RADIUS)
    private double radius = 10;
    @Modifiable(Attribute.DAMAGE)
    private double damage = 2;
    @Modifiable(Attribute.STRENGTH)
    private double knockup = 0.8;
    private double maxScaleFactor = 1.5;
    private double fallThreshold = 12;
    private double maxFallThreshold = 60;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "earth", "sequences", "earthpillars");
    }
  }
}

