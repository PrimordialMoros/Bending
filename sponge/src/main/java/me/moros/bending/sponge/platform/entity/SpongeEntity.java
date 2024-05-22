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
import java.util.function.UnaryOperator;

import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.util.data.DataKey;
import me.moros.bending.api.util.data.DataKeyed;
import me.moros.bending.sponge.platform.PlatformAdapter;
import net.kyori.adventure.audience.Audience;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.entity.projectile.Projectile;

public class SpongeEntity implements Entity {
  private final org.spongepowered.api.entity.Entity handle;

  public SpongeEntity(org.spongepowered.api.entity.Entity handle) {
    this.handle = handle;
  }

  public org.spongepowered.api.entity.Entity handle() {
    return handle;
  }

  @Override
  public boolean valid() {
    return handle().isLoaded();
  }

  @Override
  public boolean isOnGround() {
    return handle().onGround().get();
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
  public <V> @Nullable V property(DataKeyed<V> dataKeyed) {
    return SpongeEntityProperties.PROPERTIES.getValue(dataKeyed, handle());
  }

  @Override
  public <V> boolean setProperty(DataKeyed<V> dataKeyed, V value) {
    return SpongeEntityProperties.PROPERTIES.setValue(dataKeyed, handle(), value);
  }

  @Override
  public <V> boolean editProperty(DataKeyed<V> dataKeyed, UnaryOperator<V> operator) {
    return SpongeEntityProperties.PROPERTIES.editValue(dataKeyed, handle(), operator);
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
