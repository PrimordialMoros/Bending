/*
 * Copyright 2020-2023 Moros
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

package me.moros.bending.common.ability.air.sequence;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.collision.CollisionUtil;
import me.moros.bending.api.collision.geometry.AABB;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.collision.geometry.Disk;
import me.moros.bending.api.collision.geometry.OBB;
import me.moros.bending.api.collision.geometry.Sphere;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.particle.ParticleBuilder;
import me.moros.bending.api.platform.world.WorldUtil;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.ExpiringSet;
import me.moros.bending.common.ability.air.AirScooter;
import me.moros.bending.common.config.ConfigManager;
import me.moros.math.Vector3d;
import me.moros.math.VectorUtil;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class AirWheel extends AbilityInstance {
  private static final AABB BOUNDS = new AABB(Vector3d.of(-0.4, -2, -2), Vector3d.of(0.4, 2, 2));
  private static final Config config = ConfigManager.load(Config::new);
  private static AbilityDescription scooterDesc;

  private User user;
  private Config userConfig;

  private final ExpiringSet<Entity> affectedEntities = new ExpiringSet<>(500);

  private AirScooter scooter;
  private Collider collider;
  private Vector3d center;

  private long nextRenderTime;

  public AirWheel(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (user.game().abilityManager(user.worldKey()).hasAbility(user, AirWheel.class)) {
      return false;
    }
    if (scooterDesc == null) {
      scooterDesc = Objects.requireNonNull(Registries.ABILITIES.fromString("AirScooter"));
    }
    scooter = new AirScooter(scooterDesc, false);
    if (user.onCooldown(scooterDesc) || !scooter.activate(user, Activation.ATTACK)) {
      return false;
    }

    this.user = user;
    loadConfig();

    center = user.location().add(0, 0.8, 0);
    nextRenderTime = 0;
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = user.game().configProcessor().calculate(this, config);
  }

  @Override
  public UpdateResult update() {
    long time = System.currentTimeMillis();
    center = user.location().add(0, 0.8, 0).add(user.direction().withY(0).multiply(1.2));
    collider = new Disk(new OBB(BOUNDS, Vector3d.PLUS_J, Math.toRadians(user.yaw())), new Sphere(center, 2));

    if (time >= nextRenderTime) {
      render();
      nextRenderTime = time + 100;
    }

    Block base = user.world().blockAt(center.subtract(0, 1.6, 0));
    WorldUtil.tryCoolLava(user, base);
    WorldUtil.tryExtinguishFire(user, base);

    CollisionUtil.handle(user, collider, this::onEntityHit);
    return scooter.update();
  }

  private boolean onEntityHit(Entity entity) {
    if (affectedEntities.contains(entity)) {
      return false;
    }
    affectedEntities.add(entity);
    entity.damage(userConfig.damage, user, description());
    return true;
  }

  @Override
  public void onDestroy() {
    scooter.onDestroy();
    user.addCooldown(description(), userConfig.cooldown);
  }

  private void render() {
    Vector3d rotateAxis = Vector3d.PLUS_J.cross(user.direction().withY(0));
    VectorUtil.circle(user.direction().multiply(1.8), rotateAxis, 40).forEach(v ->
      ParticleBuilder.air(center.add(v)).spawn(user.world())
    );
  }

  public Vector3d center() {
    return center;
  }

  @Override
  public Collection<Collider> colliders() {
    return List.of(collider);
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 8000;
    @Modifiable(Attribute.DAMAGE)
    private double damage = 1;

    @Override
    public List<String> path() {
      return List.of("abilities", "air", "sequences", "airwheel");
    }
  }
}

