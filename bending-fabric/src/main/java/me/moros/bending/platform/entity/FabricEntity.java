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

import java.util.Optional;
import java.util.UUID;

import me.moros.bending.fabric.mixin.EntityAccess;
import me.moros.bending.model.data.DataKey;
import me.moros.bending.platform.FabricMetadata;
import me.moros.bending.platform.PlatformAdapter;
import me.moros.bending.platform.world.FabricWorld;
import me.moros.bending.platform.world.World;
import me.moros.math.FastMath;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.projectile.Projectile;
import org.checkerframework.checker.nullness.qual.NonNull;

public class FabricEntity implements Entity {
  private final net.minecraft.world.entity.Entity handle;

  public FabricEntity(net.minecraft.world.entity.Entity handle) {
    this.handle = handle;
  }

  public net.minecraft.world.entity.Entity handle() {
    return handle;
  }

  @Override
  public int id() {
    return handle().getId();
  }

  @Override
  public Component name() {
    return handle().getDisplayName().asComponent();
  }

  @Override
  public World world() {
    return new FabricWorld((ServerLevel) handle().getLevel());
  }

  @Override
  public EntityType type() {
    return PlatformAdapter.ENTITY_TYPE_INDEX.valueOrThrow(handle().getType());
  }

  @Override
  public double width() {
    return handle().getBbWidth();
  }

  @Override
  public double height() {
    return handle().getBbHeight();
  }

  @Override
  public int yaw() {
    return FastMath.round(handle().getYRot());
  }

  @Override
  public int pitch() {
    return FastMath.round(handle().getXRot());
  }

  @Override
  public Vector3d location() {
    return Vector3d.from(handle().position());
  }

  @Override
  public Vector3d direction() {
    double xRadians = Math.toRadians(handle().getYRot());
    double yRadians = Math.toRadians(handle().getXRot());
    double a = Math.cos(yRadians);
    return Vector3d.of(-a * Math.sin(xRadians), -Math.sin(yRadians), a * Math.cos(xRadians));
  }

  @Override
  public Vector3d velocity() {
    return Vector3d.from(handle().getDeltaMovement());
  }

  @Override
  public void velocity(Vector3d velocity) {
    var clamped = velocity.clampVelocity();
    handle().setDeltaMovement(clamped.x(), clamped.y(), clamped.z());
  }

  @Override
  public boolean valid() {
    return handle().isAlive(); // TODO improve
  }

  @Override
  public boolean dead() {
    return handle().isAlive();
  }

  @Override
  public int maxFreezeTicks() {
    return handle().getTicksRequiredToFreeze();
  }

  @Override
  public int freezeTicks() {
    return handle().getTicksFrozen();
  }

  @Override
  public void freezeTicks(int ticks) {
    handle().setTicksFrozen(ticks);
  }

  @Override
  public int maxFireTicks() {
    return ((EntityAccess) handle()).bending$maxFireTicks();
  }

  @Override
  public int fireTicks() {
    return handle().getRemainingFireTicks();
  }

  @Override
  public void fireTicks(int ticks) {
    handle().setRemainingFireTicks(ticks);
  }

  @Override
  public boolean inWater(boolean fullySubmerged) {
    return handle().isInWaterOrBubble() && (!fullySubmerged || handle().isEyeInFluid(FluidTags.WATER));
  }

  @Override
  public boolean inLava(boolean fullySubmerged) {
    return handle().isInLava() && (!fullySubmerged || handle().isEyeInFluid(FluidTags.LAVA));
  }

  @Override
  public boolean visible() {
    return !handle().isInvisible();
  }

  @Override
  public double fallDistance() {
    return handle().fallDistance;
  }

  @Override
  public void fallDistance(double distance) {
    handle().fallDistance = (float) distance;
  }

  @Override
  public void remove() {
    handle().discard();
  }

  @Override
  public boolean isProjectile() {
    return handle() instanceof Projectile;
  }

  @Override
  public boolean gravity() {
    return !handle().isNoGravity();
  }

  @Override
  public void gravity(boolean value) {
    handle().setNoGravity(!value);
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
    handle().teleportTo(position.x(), position.y(), position.z());
    return true;
  }

  @Override
  public <T> Optional<T> get(DataKey<T> key) {
    return FabricMetadata.INSTANCE.metadata(handle()).get(key);
  }

  @Override
  public <T> void add(DataKey<T> key, T value) {
    FabricMetadata.INSTANCE.metadata(handle()).add(key, value);
  }

  @Override
  public <T> void remove(DataKey<T> key) {
    FabricMetadata.INSTANCE.metadata(handle()).remove(key);
  }

  @Override
  public @NonNull UUID uuid() {
    return handle().getUUID();
  }

  @Override
  public @NonNull Audience audience() {
    return Audience.empty();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof FabricEntity other) {
      return handle().equals(other.handle());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return handle.hashCode();
  }
}
