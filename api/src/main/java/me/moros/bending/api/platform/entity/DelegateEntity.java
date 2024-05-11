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

package me.moros.bending.api.platform.entity;

import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.platform.world.World;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.data.DataKey;
import me.moros.bending.api.util.data.DataKeyed;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import net.kyori.adventure.audience.Audience;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a platform entity.
 */
public interface DelegateEntity extends Entity {
  Entity entity();

  @Override
  default int id() {
    return entity().id();
  }

  @Override
  default UUID uuid() {
    return entity().uuid();
  }

  @Override
  default World world() {
    return entity().world();
  }

  @Override
  default EntityType type() {
    return entity().type();
  }

  @Override
  default Vector3d location() {
    return entity().location();
  }

  @Override
  default Vector3d direction() {
    return entity().direction();
  }

  @Override
  default Vector3d velocity() {
    return entity().velocity();
  }

  @Override
  default void velocity(Vector3d velocity) {
    entity().velocity(velocity);
  }

  @Override
  default boolean valid() {
    return entity().valid();
  }

  @Override
  default boolean dead() {
    return entity().dead();
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
  default boolean teleport(Position position) {
    return entity().teleport(position);
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
  default double health() {
    return entity().health();
  }

  @Override
  default double maxHealth() {
    return entity().maxHealth();
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
