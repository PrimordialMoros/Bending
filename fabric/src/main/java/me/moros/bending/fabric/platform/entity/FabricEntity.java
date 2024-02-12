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

package me.moros.bending.fabric.platform.entity;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.EntityType;
import me.moros.bending.api.platform.property.BooleanProperty;
import me.moros.bending.api.platform.world.World;
import me.moros.bending.api.util.data.DataKey;
import me.moros.bending.api.util.functional.Suppliers;
import me.moros.bending.fabric.mixin.accessor.EntityAccess;
import me.moros.bending.fabric.platform.PlatformAdapter;
import me.moros.bending.fabric.platform.world.FabricWorld;
import me.moros.math.FastMath;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.Projectile;
import org.checkerframework.checker.nullness.qual.NonNull;

public class FabricEntity implements Entity {
  private net.minecraft.world.entity.Entity handle;
  private final Supplier<Map<BooleanProperty, Boolean>> properties;

  public FabricEntity(net.minecraft.world.entity.Entity handle) {
    this.handle = handle;
    this.properties = Suppliers.lazy(IdentityHashMap::new);
  }

  public void setHandle(net.minecraft.world.entity.Entity handle) {
    if (uuid().equals(handle.getUUID())) {
      this.handle = handle;
    }
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
    return new FabricWorld((ServerLevel) handle().level());
  }

  @Override
  public EntityType type() {
    return EntityType.registry().getOrThrow(BuiltInRegistries.ENTITY_TYPE.getKey(handle().getType()));
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
    var pos = handle().position();
    return Vector3d.of(pos.x(), pos.y(), pos.z());
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
    var vel = handle().getDeltaMovement();
    return Vector3d.of(vel.x(), vel.y(), vel.z());
  }

  @Override
  public void velocity(Vector3d velocity) {
    var clamped = velocity.clampVelocity();
    handle().setDeltaMovement(clamped.x(), clamped.y(), clamped.z());
    handle().hurtMarked = true;
  }

  @Override
  public boolean valid() {
    return handle().isAlive();
  }

  @Override
  public boolean dead() {
    return !handle().isAlive();
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
  public boolean isOnGround() {
    return handle().onGround();
  }

  @Override
  public boolean inWater() {
    return handle().isInWaterOrBubble();
  }

  @Override
  public boolean inLava() {
    return handle().isInLava();
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
    var type = PlatformAdapter.dataTypeIfExists(key);
    if (type == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(handle().getAttached(type));
  }

  @Override
  public <T> void add(DataKey<T> key, T value) {
    handle().setAttached(PlatformAdapter.dataType(key), value);
  }

  @Override
  public <T> void remove(DataKey<T> key) {
    var type = PlatformAdapter.dataTypeIfExists(key);
    if (type != null) {
      handle().removeAttached(type);
    }
  }

  @Override
  public @NonNull UUID uuid() {
    return handle().getUUID();
  }

  @Override
  public TriState checkProperty(BooleanProperty property) {
    var vanillaProperty = PropertyMapper.PROPERTIES.get(property);
    if (vanillaProperty == null) {
      return TriState.byBoolean(properties.get().get(property));
    }
    return vanillaProperty.get(handle());
  }

  @Override
  public void setProperty(BooleanProperty property, boolean value) {
    var vanillaProperty = PropertyMapper.PROPERTIES.get(property);
    if (vanillaProperty != null) {
      vanillaProperty.set(handle(), value);
    } else {
      properties.get().put(property, value);
    }
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
      return uuid().equals(other.uuid());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return uuid().hashCode();
  }
}
