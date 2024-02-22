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

package me.moros.bending.sponge.platform.entity;

import java.util.Optional;
import java.util.UUID;

import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.EntityType;
import me.moros.bending.api.platform.world.World;
import me.moros.bending.api.util.data.DataKey;
import me.moros.bending.sponge.platform.PlatformAdapter;
import me.moros.bending.sponge.platform.world.SpongeWorld;
import me.moros.math.FastMath;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.registry.RegistryTypes;

public class SpongeEntity implements Entity {
  private final org.spongepowered.api.entity.Entity handle;

  public SpongeEntity(org.spongepowered.api.entity.Entity handle) {
    this.handle = handle;
  }

  public org.spongepowered.api.entity.Entity handle() {
    return handle;
  }

  protected net.minecraft.world.entity.Entity nmsEntity() {
    return (net.minecraft.world.entity.Entity) handle();
  }

  @Override
  public int id() {
    return nmsEntity().getId();
  }

  @Override
  public Component name() {
    return handle().displayName().get();
  }

  @Override
  public World world() {
    return new SpongeWorld(handle().serverLocation().world());
  }

  @Override
  public EntityType type() {
    return EntityType.registry().getOrThrow(PlatformAdapter.fromRsk(handle().type().key(RegistryTypes.ENTITY_TYPE)));
  }

  @Override
  public double width() {
    return nmsEntity().getBbWidth();
  }

  @Override
  public double height() {
    return nmsEntity().getBbHeight();
  }

  @Override
  public int yaw() {
    return FastMath.round(handle().rotation().y());
  }

  @Override
  public int pitch() {
    return FastMath.round(handle().rotation().x());
  }

  @Override
  public Vector3d location() {
    var loc = handle().serverLocation();
    return Vector3d.of(loc.x(), loc.y(), loc.z());
  }

  @Override
  public Vector3d direction() {
    var dir = handle().direction();
    return Vector3d.of(dir.x(), dir.y(), dir.z());
  }

  @Override
  public Vector3d velocity() {
    var vel = handle().velocity().get();
    return Vector3d.of(vel.x(), vel.y(), vel.z());
  }

  @Override
  public void velocity(Vector3d velocity) {
    var vel = velocity.clampVelocity();
    handle().offer(Keys.VELOCITY, org.spongepowered.math.vector.Vector3d.from(vel.x(), vel.y(), vel.z()));
  }

  @Override
  public boolean valid() {
    return handle().isLoaded();
  }

  @Override
  public boolean dead() {
    return !nmsEntity().isAlive();
  }

  @Override
  public int maxFreezeTicks() {
    return nmsEntity().getTicksRequiredToFreeze();
  }

  @Override
  public int freezeTicks() {
    return nmsEntity().getTicksFrozen();
  }

  @Override
  public void freezeTicks(int ticks) {
    nmsEntity().setTicksFrozen(ticks);
  }

  @Override
  public int maxFireTicks() {
    return (int) handle().fireImmuneTicks().get().ticks();
  }

  @Override
  public int fireTicks() {
    return nmsEntity().getRemainingFireTicks();
  }

  @Override
  public void fireTicks(int ticks) {
    nmsEntity().setRemainingFireTicks(ticks);
  }

  @Override
  public boolean isOnGround() {
    return handle().onGround().get();
  }

  @Override
  public boolean inWater() {
    return nmsEntity().isInWaterOrBubble();
  }

  @Override
  public boolean inLava() {
    return nmsEntity().isInLava();
  }

  @Override
  public boolean visible() {
    return !nmsEntity().isInvisible();
  }

  @Override
  public double fallDistance() {
    return handle().getDouble(Keys.FALL_DISTANCE).orElse(0);
  }

  @Override
  public void fallDistance(double distance) {
    handle().offer(Keys.FALL_DISTANCE, distance);
  }

  @Override
  public void remove() {
    handle().remove();
  }

  @Override
  public boolean isProjectile() {
    return handle() instanceof Projectile;
  }

  @Override
  public boolean gravity() {
    return !nmsEntity().isNoGravity();
  }

  @Override
  public void gravity(boolean value) {
    nmsEntity().setNoGravity(!value);
  }

  @Override
  public boolean invulnerable() {
    return nmsEntity().isInvulnerable();
  }

  @Override
  public void invulnerable(boolean value) {
    nmsEntity().setInvulnerable(value);
  }

  @Override
  public boolean teleport(Position position) {
    var vec = org.spongepowered.math.vector.Vector3d.from(position.x(), position.y(), position.z());
    return handle().setPosition(vec);
  }

  @Override
  public <T> Optional<T> get(DataKey<T> key) {
    return handle().get(PlatformAdapter.dataKey(key));
  }

  @Override
  public <T> void add(DataKey<T> key, T value) {
    handle().offer(PlatformAdapter.dataKey(key), value);
  }

  @Override
  public <T> void remove(DataKey<T> key) {
    handle().remove(PlatformAdapter.dataKey(key));
  }

  @Override
  public UUID uuid() {
    return handle().uniqueId();
  }

  @Override
  public Audience audience() {
    return Audience.empty();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof SpongeEntity other) {
      return handle().equals(other.handle());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return handle.hashCode();
  }
}
