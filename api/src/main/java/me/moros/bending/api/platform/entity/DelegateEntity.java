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

package me.moros.bending.api.platform.entity;

import java.util.Optional;
import java.util.function.UnaryOperator;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.data.DataKey;
import me.moros.bending.api.util.data.DataKeyed;
import net.kyori.adventure.audience.Audience;
import org.jspecify.annotations.Nullable;

/**
 * Represents a platform entity.
 */
public interface DelegateEntity extends Entity {
  Entity entity();

  @Override
  default boolean valid() {
    return entity().valid();
  }

  @Override
  default boolean isOnGround() {
    return entity().isOnGround();
  }

  @Override
  default void remove() {
    entity().remove();
  }

  @Override
  default boolean isProjectile() {
    return entity().isProjectile();
  }

  @Override
  default <V> @Nullable V property(DataKeyed<V> dataKeyed) {
    return entity().property(dataKeyed);
  }

  @Override
  default <V> boolean setProperty(DataKeyed<V> dataKeyed, V value) {
    return entity().setProperty(dataKeyed, value);
  }

  @Override
  default <V> boolean editProperty(DataKeyed<V> dataKeyed, UnaryOperator<V> operator) {
    return entity().editProperty(dataKeyed, operator);
  }

  @Override
  default <T> Optional<T> get(DataKey<T> key) {
    return entity().get(key);
  }

  @Override
  default <T> void add(DataKey<T> key, T value) {
    entity().add(key, value);
  }

  @Override
  default <T> void remove(DataKey<T> key) {
    entity().remove(key);
  }

  @Override
  default boolean damage(double damage) {
    return entity().damage(damage);
  }

  @Override
  default boolean damage(double damage, Entity source) {
    return entity().damage(damage, source);
  }

  @Override
  default boolean damage(double damage, User source, AbilityDescription desc) {
    return entity().damage(damage, source, desc);
  }

  @Override
  default Audience audience() {
    return entity();
  }
}
