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

package me.moros.bending.paper.platform.entity;

import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.EntityType;
import me.moros.bending.api.platform.property.Property;
import me.moros.bending.api.platform.world.World;
import me.moros.bending.api.util.data.DataKey;
import me.moros.bending.api.util.data.DataKeyed;
import me.moros.bending.paper.platform.BukkitDataHolder;
import me.moros.bending.paper.platform.world.BukkitWorld;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Location;
import org.bukkit.entity.Projectile;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BukkitEntity implements Entity {
  private final org.bukkit.entity.Entity handle;

  public BukkitEntity(org.bukkit.entity.Entity handle) {
    this.handle = handle;
  }

  public org.bukkit.entity.Entity handle() {
    return handle;
  }

  @Override
  public int id() {
    return handle().getEntityId();
  }

  @Override
  public UUID uuid() {
    return handle().getUniqueId();
  }

  @Override
  public EntityType type() {
    return EntityType.registry().getOrThrow(handle().getType().key());
  }

  @Override
  public World world() {
    return new BukkitWorld(handle().getWorld());
  }

  @Override
  public Vector3d location() {
    return Vector3d.of(handle().getX(), handle().getY(), handle().getZ());
  }

  @Override
  public Vector3d direction() {
    double xRadians = Math.toRadians(handle().getYaw());
    double yRadians = Math.toRadians(handle().getPitch());
    double a = Math.cos(yRadians);
    return Vector3d.of(-a * Math.sin(xRadians), -Math.sin(yRadians), a * Math.cos(xRadians));
  }

  @Override
  public Vector3d velocity() {
    var vel = handle().getVelocity();
    return Vector3d.of(vel.getX(), vel.getY(), vel.getZ());
  }

  @Override
  public void velocity(Vector3d velocity) {
    var vel = velocity.clampVelocity();
    handle().setVelocity(new Vector(vel.x(), vel.y(), vel.z()));
  }

  @Override
  public boolean valid() {
    return handle().isValid();
  }

  @Override
  public boolean dead() {
    return handle().isDead();
  }

  @Override
  public boolean isOnGround() {
    return handle().isOnGround();
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
  public boolean teleport(Position position) {
    var w = handle().getWorld();
    return handle().teleport(new Location(w, position.x(), position.y(), position.z()));
  }

  @Override
  public <V> @Nullable V property(DataKeyed<V> dataKeyed) {
    return BukkitEntityProperties.PROPERTIES.getValue(dataKeyed, handle());
  }

  @Override
  public <V> boolean setProperty(DataKeyed<V> dataKeyed, V value) {
    if (dataKeyed instanceof Property<V> property && property.isValidValue(value)) {
      return BukkitEntityProperties.PROPERTIES.setValue(dataKeyed, handle(), value);
    }
    return false;
  }

  @Override
  public <V> boolean editProperty(DataKeyed<V> dataKeyed, UnaryOperator<V> operator) {
    return BukkitEntityProperties.PROPERTIES.editValue(dataKeyed, handle(), operator);
  }

  @Override
  public <T> Optional<T> get(DataKey<T> key) {
    return new BukkitDataHolder(handle()).get(key);
  }

  @Override
  public <T> void add(DataKey<T> key, T value) {
    new BukkitDataHolder(handle()).add(key, value);
  }

  @Override
  public <T> void remove(DataKey<T> key) {
    new BukkitDataHolder(handle()).remove(key);
  }

  @Override
  public Audience audience() {
    return handle();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof BukkitEntity other) {
      return handle().equals(other.handle());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return handle.hashCode();
  }
}
