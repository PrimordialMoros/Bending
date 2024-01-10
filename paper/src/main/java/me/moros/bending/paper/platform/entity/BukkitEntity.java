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

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.EntityType;
import me.moros.bending.api.platform.property.BooleanProperty;
import me.moros.bending.api.platform.world.World;
import me.moros.bending.api.util.data.DataHolder;
import me.moros.bending.api.util.data.DataKey;
import me.moros.bending.api.util.functional.Suppliers;
import me.moros.bending.paper.platform.BukkitDataHolder;
import me.moros.bending.paper.platform.world.BukkitWorld;
import me.moros.math.FastMath;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Projectile;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.NonNull;

public class BukkitEntity implements Entity {
  private final org.bukkit.entity.Entity handle;
  private final Supplier<DataHolder> holder;
  private final Supplier<Map<BooleanProperty, Boolean>> properties;

  public BukkitEntity(org.bukkit.entity.Entity handle) {
    this.handle = handle;
    this.holder = Suppliers.lazy(() -> BukkitDataHolder.combined(handle()));
    this.properties = Suppliers.lazy(IdentityHashMap::new);
  }

  public org.bukkit.entity.Entity handle() {
    return handle;
  }

  @Override
  public int id() {
    return handle().getEntityId();
  }

  @Override
  public Component name() {
    return handle().name();
  }

  @Override
  public World world() {
    return new BukkitWorld(handle().getWorld());
  }

  @Override
  public EntityType type() {
    return EntityType.registry().getOrThrow(handle().getType().key());
  }

  @Override
  public double width() {
    return handle().getWidth();
  }

  @Override
  public double height() {
    return handle().getHeight();
  }

  @Override
  public int yaw() {
    return FastMath.round(handle().getYaw());
  }

  @Override
  public int pitch() {
    return FastMath.round(handle().getPitch());
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
  public int maxFreezeTicks() {
    return handle().getMaxFreezeTicks();
  }

  @Override
  public int freezeTicks() {
    return handle().getFreezeTicks();
  }

  @Override
  public void freezeTicks(int ticks) {
    handle().setFreezeTicks(ticks);
  }

  @Override
  public int maxFireTicks() {
    return handle().getMaxFireTicks();
  }

  @Override
  public int fireTicks() {
    return handle().getFireTicks();
  }

  @Override
  public void fireTicks(int ticks) {
    handle().setFireTicks(ticks);
  }

  @Override
  public boolean isOnGround() {
    return handle().isOnGround();
  }

  @Override
  public boolean inWater() {
    return handle().isInWaterOrBubbleColumn();
  }

  @Override
  public boolean inLava() {
    return handle().isInLava();
  }

  @Override
  public boolean visible() {
    return !(handle() instanceof ArmorStand stand) || stand.isVisible();
  }

  @Override
  public double fallDistance() {
    return handle().getFallDistance();
  }

  @Override
  public void fallDistance(double distance) {
    handle().setFallDistance((float) distance);
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
    return handle().hasGravity();
  }

  @Override
  public void gravity(boolean value) {
    handle().setGravity(value);
  }

  @Override
  public boolean invulnerable() {
    return handle().isInvulnerable();
  }

  @Override
  public void invulnerable(boolean value) {
    handle().setInvulnerable(value);
  }

  @Override
  public boolean teleport(Position position) {
    var w = handle().getWorld();
    return handle().teleport(new Location(w, position.x(), position.y(), position.z()));
  }

  @Override
  public @NonNull UUID uuid() {
    return handle().getUniqueId();
  }

  @Override
  public TriState checkProperty(BooleanProperty property) {
    var bukkitProperty = PropertyMapper.PROPERTIES.get(property);
    if (bukkitProperty == null) {
      return TriState.byBoolean(properties.get().get(property));
    }
    return bukkitProperty.get(handle());
  }

  @Override
  public void setProperty(BooleanProperty property, boolean value) {
    var bukkitProperty = PropertyMapper.PROPERTIES.get(property);
    if (bukkitProperty != null) {
      bukkitProperty.set(handle(), value);
    } else {
      properties.get().put(property, value);
    }
  }

  @Override
  public @NonNull Audience audience() {
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

  @Override
  public <T> Optional<T> get(DataKey<T> key) {
    return holder.get().get(key);
  }

  @Override
  public <T> void add(DataKey<T> key, T value) {
    holder.get().add(key, value);
  }

  @Override
  public <T> void remove(DataKey<T> key) {
    holder.get().remove(key);
  }
}
