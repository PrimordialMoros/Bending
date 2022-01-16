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

package me.moros.bending.ability.air.sequence;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import me.moros.bending.Bending;
import me.moros.bending.ability.air.AirScooter;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ExpiringSet;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Disk;
import me.moros.bending.model.collision.geometry.OBB;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.user.User;
import me.moros.bending.registry.Registries;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.VectorUtil;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.collision.CollisionUtil;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class AirWheel extends AbilityInstance {
  private static final AABB BOUNDS = new AABB(new Vector3d(-0.4, -2, -2), new Vector3d(0.4, 2, 2));
  private static final Config config = new Config();
  private static AbilityDescription scooterDesc;

  private User user;
  private Config userConfig;

  private final ExpiringSet<Entity> affectedEntities = new ExpiringSet<>(500);

  private AirScooter scooter;
  private Collider collider;
  private Vector3d center;

  private long nextRenderTime;

  public AirWheel(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    if (Bending.game().abilityManager(user.world()).hasAbility(user, AirWheel.class)) {
      return false;
    }
    if (scooterDesc == null) {
      scooterDesc = Objects.requireNonNull(Registries.ABILITIES.ability("AirScooter"));
    }
    scooter = new AirScooter(scooterDesc);
    if (user.onCooldown(scooterDesc) || !scooter.activate(user, Activation.ATTACK)) {
      return false;
    }
    scooter.canRender = false;

    this.user = user;
    loadConfig();

    center = user.location().add(new Vector3d(0, 0.8, 0));
    nextRenderTime = 0;
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = Bending.configManager().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    long time = System.currentTimeMillis();
    center = user.location().add(new Vector3d(0, 0.8, 0)).add(user.direction().setY(0).multiply(1.2));
    collider = new Disk(new OBB(BOUNDS, Vector3d.PLUS_J, Math.toRadians(user.yaw())), new Sphere(center, 2));

    if (time >= nextRenderTime) {
      render();
      nextRenderTime = time + 100;
    }

    Block base = center.subtract(new Vector3d(0, 1.6, 0)).toBlock(user.world());
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
    DamageUtil.damageEntity(entity, user, userConfig.damage, description());
    return true;
  }

  @Override
  public void onDestroy() {
    scooter.onDestroy();
    user.addCooldown(description(), userConfig.cooldown);
  }

  private void render() {
    Vector3d rotateAxis = Vector3d.PLUS_J.cross(user.direction().setY(0));
    VectorUtil.circle(user.direction().multiply(1.6), rotateAxis, 40).forEach(v ->
      ParticleUtil.air(center.add(v)).spawn(user.world())
    );
  }

  public @NonNull Vector3d center() {
    return center;
  }

  @Override
  public @NonNull Collection<@NonNull Collider> colliders() {
    return List.of(collider);
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.DAMAGE)
    public double damage;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "air", "sequences", "airwheel");

      cooldown = abilityNode.node("cooldown").getLong(8000);
      damage = abilityNode.node("damage").getDouble(1.0);
    }
  }
}

