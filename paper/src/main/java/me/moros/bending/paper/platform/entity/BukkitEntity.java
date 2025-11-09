/*
 * Copyright 2020-2025 Moros
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
import java.util.function.UnaryOperator;

import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.util.data.DataKey;
import me.moros.bending.api.util.data.DataKeyed;
import me.moros.bending.paper.platform.BukkitDataHolder;
import net.kyori.adventure.audience.Audience;
import org.bukkit.entity.Projectile;
import org.jspecify.annotations.Nullable;

public class BukkitEntity implements Entity {
  private final org.bukkit.entity.Entity handle;

  public BukkitEntity(org.bukkit.entity.Entity handle) {
    this.handle = handle;
  }

  public org.bukkit.entity.Entity handle() {
    return handle;
  }

  @Override
  public boolean valid() {
    return handle().isValid();
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
  public <V> @Nullable V property(DataKeyed<V> dataKeyed) {
    return BukkitEntityProperties.PROPERTIES.getValue(dataKeyed, handle());
  }

  @Override
  public <V> boolean setProperty(DataKeyed<V> dataKeyed, V value) {
    return BukkitEntityProperties.PROPERTIES.setValue(dataKeyed, handle(), value);
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
