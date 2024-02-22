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

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.platform.property.BooleanProperty;
import me.moros.bending.api.platform.world.World;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.data.DataKey;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;

/**
 * Represents a platform entity.
 */
public interface DelegateEntity extends Entity {
  Entity entity();

  @Override
  default EntityType type() {
    return entity().type();
  }

  @Override
  default int id() {
    return entity().id();
  }

  @Override
  default Component name() {
    return entity().name();
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
  default double width() {
    return entity().width();
  }

  @Override
  default double height() {
    return entity().height();
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
  default int yaw() {
    return entity().yaw();
  }

  @Override
  default int pitch() {
    return entity().pitch();
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
  default boolean inWater() {
    return entity().inWater();
  }

  @Override
  default boolean underWater() {
    return entity().underWater();
  }

  @Override
  default boolean inLava() {
    return entity().inLava();
  }

  @Override
  default boolean underLava() {
    return entity().underLava();
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
  default int maxFreezeTicks() {
    return entity().maxFreezeTicks();
  }

  @Override
  default int freezeTicks() {
    return entity().freezeTicks();
  }

  @Override
  default void freezeTicks(int ticks) {
    entity().freezeTicks(ticks);
  }

  @Override
  default int maxFireTicks() {
    return entity().maxFireTicks();
  }

  @Override
  default int fireTicks() {
    return entity().fireTicks();
  }

  @Override
  default void fireTicks(int ticks) {
    entity().fireTicks(ticks);
  }

  @Override
  default double fallDistance() {
    return entity().fallDistance();
  }

  @Override
  default void fallDistance(double fallDistance) {
    entity().fallDistance(fallDistance);
  }

  @Override
  default boolean isOnGround() {
    return entity().isOnGround();
  }

  @Override
  default boolean visible() {
    return entity().visible();
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
  default void remove() {
    entity().remove();
  }

  @Override
  default boolean isProjectile() {
    return entity().isProjectile();
  }

  @Override
  default boolean gravity() {
    return entity().gravity();
  }

  @Override
  default void gravity(boolean value) {
    entity().gravity(value);
  }

  @Override
  default boolean invulnerable() {
    return entity().invulnerable();
  }

  @Override
  default void invulnerable(boolean value) {
    entity().invulnerable(value);
  }

  @Override
  default boolean teleport(Position position) {
    return entity().teleport(position);
  }

  @Override
  default TriState checkProperty(BooleanProperty property) {
    return entity().checkProperty(property);
  }

  @Override
  default void setProperty(BooleanProperty property, boolean value) {
    entity().setProperty(property, value);
  }

  @Override
  default Audience audience() {
    return entity();
  }
}
