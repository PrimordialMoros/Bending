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

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.data.DataKey;
import me.moros.bending.model.raytrace.ContextBuilder;
import me.moros.bending.model.user.User;
import me.moros.bending.platform.item.Inventory;
import me.moros.bending.platform.potion.Potion;
import me.moros.bending.platform.potion.PotionEffect;
import me.moros.bending.platform.property.BooleanProperty;
import me.moros.bending.platform.world.World;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a platform entity.
 */
public interface DelegateEntity extends LivingEntity {
  LivingEntity entity();

  @Override
  default EntityType type() {
    return entity().type();
  }

  default int id() {
    return entity().id();
  }

  @Override
  default Component name() {
    return entity().name();
  }

  @Override
  default @NonNull UUID uuid() {
    return entity().uuid();
  }

  @Override
  default World world() {
    return entity().world();
  }

  @Override
  default boolean ai() {
    return entity().ai();
  }

  @Override
  default void ai(boolean value) {
    entity().ai(value);
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
  default double eyeHeight() {
    return entity().eyeHeight();
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
  default Ray ray() {
    return entity().ray();
  }

  @Override
  default Ray ray(double range) {
    return entity().ray(range);
  }

  @Override
  default boolean inWater(boolean fullySubmerged) {
    return entity().inWater(fullySubmerged);
  }

  @Override
  default boolean inLava(boolean fullySubmerged) {
    return entity().inLava(fullySubmerged);
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
  default boolean sneaking() {
    return entity().sneaking();
  }

  @Override
  default void sneaking(boolean sneaking) {
    entity().sneaking(sneaking);
  }

  @Override
  default @Nullable Inventory inventory() {
    return entity().inventory();
  }

  @Override
  default boolean isOnGround() {
    return entity().isOnGround();
  }

  @Override
  default ContextBuilder rayTrace(double range) {
    return entity().rayTrace(range);
  }

  @Override
  default ContextBuilder rayTrace(Vector3d origin, Vector3d dir) {
    return entity().rayTrace(origin, dir);
  }

  @Override
  default Vector3d mainHandSide() {
    return entity().mainHandSide();
  }

  @Override
  default Vector3d handSide(boolean right) {
    return entity().handSide(right);
  }

  @Override
  default Vector3d rightSide() {
    return entity().rightSide();
  }

  @Override
  default Vector3d leftSide() {
    return entity().leftSide();
  }

  @Override
  default boolean addPotion(Potion potion) {
    return entity().addPotion(potion);
  }

  @Override
  default boolean hasPotion(PotionEffect effect) {
    return entity().hasPotion(effect);
  }

  @Override
  default @Nullable Potion potion(PotionEffect effect) {
    return entity().potion(effect);
  }

  @Override
  default void removePotion(PotionEffect effect) {
    entity().removePotion(effect);
  }

  @Override
  default Collection<Potion> activePotions() {
    return entity().activePotions();
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
  default int airCapacity() {
    return entity().airCapacity();
  }

  @Override
  default int remainingAir() {
    return entity().remainingAir();
  }

  @Override
  default void remainingAir(int amount) {
    entity().remainingAir(amount);
  }

  @Override
  default Entity shootArrow(Position origin, Vector3d direction, double power) {
    return entity().shootArrow(origin, direction, power);
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
  default @NonNull Audience audience() {
    return entity();
  }
}
