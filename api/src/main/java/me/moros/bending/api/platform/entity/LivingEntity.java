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

package me.moros.bending.api.platform.entity;

import java.util.Collection;

import me.moros.bending.api.ability.Ability;
import me.moros.bending.api.collision.geometry.Ray;
import me.moros.bending.api.collision.raytrace.Context;
import me.moros.bending.api.collision.raytrace.ContextBuilder;
import me.moros.bending.api.event.EventBus;
import me.moros.bending.api.event.VelocityEvent;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.item.Inventory;
import me.moros.bending.api.platform.potion.Potion;
import me.moros.bending.api.platform.potion.PotionEffect;
import me.moros.bending.api.platform.property.BooleanProperty;
import me.moros.bending.api.platform.property.EntityProperty;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import net.kyori.adventure.util.TriState;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface LivingEntity extends Entity {
  boolean ai();

  void ai(boolean value);

  double eyeHeight();

  default Block eyeBlock() {
    return world().blockAt(eyeLocation());
  }

  default Vector3d eyeLocation() {
    return location().add(0, eyeHeight(), 0);
  }

  @Override
  default boolean applyVelocity(Ability ability, Vector3d velocity) {
    VelocityEvent event = EventBus.INSTANCE.postVelocityEvent(ability.user(), this, ability.description(), velocity);
    if (!event.cancelled()) {
      velocity(velocity);
      return true;
    }
    return false;
  }

  default Ray ray() {
    return new Ray(eyeLocation(), direction());
  }

  default Ray ray(double range) {
    return new Ray(eyeLocation(), direction().multiply(range));
  }

  default boolean sneaking() {
    return checkProperty(EntityProperty.SNEAKING) == TriState.TRUE;
  }

  default void sneaking(boolean sneaking) {
    setProperty(EntityProperty.SNEAKING, sneaking);
  }

  @Nullable Inventory inventory();

  /**
   * Prepare a composite ray trace matching the user's view and filtering the specified class type for entities.
   * @see ContextBuilder
   */
  default ContextBuilder rayTrace(double range) {
    return Context.builder(eyeLocation(), direction()).range(range).filterForUser(this);
  }

  default ContextBuilder rayTrace(Vector3d origin, Vector3d dir) {
    return Context.builder(origin, dir).filterForUser(this);
  }

  /**
   * Get the user's main hand side.
   * <p>Note: The returned value includes an offset and is ideal for showing charging particles.
   * @return a vector which represents the user's main hand location
   * @see #rightSide()
   * @see #leftSide()
   */
  default Vector3d mainHandSide() {
    Vector3d dir = direction().multiply(0.4);
    return switch (isRightHanded()) {
      case TRUE -> handSide(true);
      case FALSE -> handSide(false);
      case NOT_SET -> eyeLocation().add(dir);
    };
  }

  default TriState isRightHanded() {
    return TriState.NOT_SET;
  }

  /**
   * Gets the user's specified hand position.
   * @param right whether to get the right hand
   * @return a vector which represents the user's specified hand location
   */
  default Vector3d handSide(boolean right) {
    double y = sneaking() ? 1.2 : 1.575;
    Vector3d offset = direction().multiply(0.4).add(0, y, 0);
    return right ? rightSide().add(offset) : leftSide().add(offset);
  }

  /**
   * Gets the user's right side.
   * @return a vector which represents the user's right side
   */
  default Vector3d rightSide() {
    double angle = Math.toRadians(yaw());
    return location().subtract(Vector3d.of(Math.cos(angle), 0, Math.sin(angle)).normalize().multiply(0.3));
  }

  /**
   * Gets the user's left side.
   * @return a vector which represents the user's left side
   */
  default Vector3d leftSide() {
    double angle = Math.toRadians(yaw());
    return location().add(Vector3d.of(Math.cos(angle), 0, Math.sin(angle)).normalize().multiply(0.3));
  }

  boolean addPotion(Potion potion);

  default boolean addPotions(Iterable<Potion> potions) {
    boolean result = true;
    for (Potion potion : potions) {
      result &= addPotion(potion);
    }
    return result;
  }

  boolean hasPotion(PotionEffect effect);

  @Nullable Potion potion(PotionEffect effect);

  void removePotion(PotionEffect effect);

  Collection<Potion> activePotions();

  @Override
  default boolean isProjectile() {
    return false;
  }

  int airCapacity();

  int remainingAir();

  void remainingAir(int amount);

  Entity shootArrow(Position origin, Vector3d direction, double power);

  TriState checkProperty(BooleanProperty property);

  default void setProperty(BooleanProperty property, TriState value) {
    if (value != TriState.NOT_SET) {
      setProperty(property, value == TriState.TRUE);
    }
  }

  void setProperty(BooleanProperty property, boolean value);
}
