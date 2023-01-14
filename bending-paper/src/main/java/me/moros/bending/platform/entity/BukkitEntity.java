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

package me.moros.bending.platform.entity;

import java.util.UUID;
import java.util.function.Supplier;

import me.moros.bending.adapter.NativeAdapter;
import me.moros.bending.model.functional.Suppliers;
import me.moros.bending.platform.BukkitDataHolder;
import me.moros.bending.platform.PlatformAdapter;
import me.moros.bending.platform.world.BukkitWorld;
import me.moros.bending.platform.world.World;
import me.moros.math.FastMath;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Projectile;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.NonNull;

public class BukkitEntity extends BukkitDataHolder implements Entity {
  private final org.bukkit.entity.Entity handle;

  private BukkitEntity(Supplier<org.bukkit.entity.Entity> handle) {
    super(handle, handle);
    this.handle = handle.get();
  }

  public BukkitEntity(org.bukkit.entity.Entity handle) {
    this(Suppliers.cached(handle));
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
    return PlatformAdapter.ENTITY_TYPE_INDEX.valueOrThrow(handle().getType());
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
    return FastMath.round(handle().getLocation().getYaw());
  }

  @Override
  public int pitch() {
    return FastMath.round(handle().getLocation().getPitch());
  }

  @Override
  public Vector3d location() {
    return Vector3d.from(handle().getLocation());
  }

  @Override
  public Vector3d direction() {
    Location loc = handle().getLocation();
    double xRadians = Math.toRadians(loc.getYaw());
    double yRadians = Math.toRadians(loc.getPitch());
    double a = Math.cos(yRadians);
    return Vector3d.of(-a * Math.sin(xRadians), -Math.sin(yRadians), a * Math.cos(xRadians));
  }

  @Override
  public Vector3d velocity() {
    return Vector3d.from(handle().getVelocity());
  }

  @Override
  public void velocity(Vector3d velocity) {
    handle().setVelocity(velocity.clampVelocity().to(Vector.class));
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
  public boolean inWater(boolean fullySubmerged) {
    return handle().isInWaterOrBubbleColumn() && (!fullySubmerged || NativeAdapter.instance().eyeInWater(this));
  }

  @Override
  public boolean inLava(boolean fullySubmerged) {
    return handle().isInLava() && (!fullySubmerged || NativeAdapter.instance().eyeInLava(this));
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
    return handle().teleport(position.to(Location.class, handle().getWorld()));
  }

  @Override
  public @NonNull UUID uuid() {
    return handle().getUniqueId();
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
}
