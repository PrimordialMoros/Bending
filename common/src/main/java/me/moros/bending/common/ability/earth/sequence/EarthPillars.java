/*
 * Copyright 2020-2024 Moros
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

package me.moros.bending.common.ability.earth.sequence;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.MultiUpdatable;
import me.moros.bending.api.ability.common.Pillar;
import me.moros.bending.api.collision.CollisionUtil;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.collision.geometry.Sphere;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.Direction;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.material.EarthMaterials;
import me.moros.bending.common.config.ConfigManager;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class EarthPillars extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private static AbilityDescription pillarsDesc;

  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final MultiUpdatable<Pillar> pillars = MultiUpdatable.empty();
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
      double dist = user.fallDistance();
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
    Collider collider = Sphere.of(user.location(), userConfig.radius * factor);
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
    userConfig = user.game().configProcessor().calculate(this, config);
  }

  @Override
  public UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    return pillars.update();
  }

  private boolean createPillar(Entity entity) {
    Block base = entity.block().offset(Direction.DOWN);
    boolean unique = pillars.stream()
      .noneMatch(p -> p.origin().blockX() == base.blockX() && p.origin().blockZ() == base.blockZ());
    if (predicate.test(base)) {
      if (unique) {
        base.type().asParticle(entity.center()).count(8).offset(1, 0.1, 1).spawn(user.world());
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

  private final class EarthPillar extends Pillar {
    private EarthPillar(Builder<EarthPillar> builder) {
      super(builder);
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      if (entity.uuid().equals(user.uuid())) {
        return false;
      }
      if (!affectedEntities.contains(entity)) {
        affectedEntities.add(entity);
        entity.damage(userConfig.damage * factor, user, description());
      }
      entity.applyVelocity(EarthPillars.this, Vector3d.PLUS_J.multiply(userConfig.knockup * factor));
      return true;
    }
  }

  @ConfigSerializable
  private static final class Config implements Configurable {
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
    public List<String> path() {
      return List.of("abilities", "earth", "sequences", "earthpillars");
    }
  }
}

