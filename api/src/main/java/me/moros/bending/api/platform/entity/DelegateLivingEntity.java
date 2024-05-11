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

import java.util.Collection;

import me.moros.bending.api.collision.geometry.Ray;
import me.moros.bending.api.collision.raytrace.ContextBuilder;
import me.moros.bending.api.platform.item.Inventory;
import me.moros.bending.api.platform.potion.Potion;
import me.moros.bending.api.platform.potion.PotionEffect;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a platform living entity.
 */
public interface DelegateLivingEntity extends DelegateEntity, LivingEntity {
  @Override
  LivingEntity entity();

  @Override
  default double eyeHeight() {
    return entity().eyeHeight();
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
  default @Nullable Inventory inventory() {
    return entity().inventory();
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
  default Entity shootArrow(Position origin, Vector3d direction, double power) {
    return entity().shootArrow(origin, direction, power);
  }

  @Override
  default boolean isProjectile() {
    return entity().isProjectile();
  }
}
