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

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import me.moros.bending.platform.property.BooleanProperty;
import me.moros.bending.platform.property.EntityProperty;
import net.kyori.adventure.util.TriState;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import static java.util.Map.entry;

final class PropertyMapper {
  static final Map<BooleanProperty, BukkitProperty<? extends LivingEntity>> PROPERTIES;

  static {
    PROPERTIES = Map.ofEntries(
      entry(EntityProperty.SNEAKING, boolProp(Player.class, Player::isSneaking, Player::setSneaking)),
      entry(EntityProperty.SPRINTING, boolProp(Player.class, Player::isSprinting, Player::setSprinting)),
      entry(EntityProperty.ALLOW_FLIGHT, boolProp(Player.class, Player::getAllowFlight, Player::setAllowFlight)),
      entry(EntityProperty.FLYING, boolProp(Player.class, Player::isFlying, Player::setFlying)),
      entry(EntityProperty.GLIDING, boolProp(LivingEntity.class, LivingEntity::isGliding, LivingEntity::setGliding)),
      entry(EntityProperty.CHARGED, boolProp(Creeper.class, Creeper::isPowered, Creeper::setPowered))
    );
  }

  static <E extends LivingEntity> BukkitProperty<E> boolProp(Class<E> type, Predicate<E> getter, BiConsumer<E, Boolean> setter) {
    return new BukkitProperty<>(type, getter, setter);
  }

  record BukkitProperty<E extends LivingEntity>(Class<E> type, Predicate<E> getter, BiConsumer<E, Boolean> setter) {
    TriState get(LivingEntity data) {
      if (type.isInstance(data)) {
        return TriState.byBoolean(getter.test(type.cast(data)));
      }
      return TriState.NOT_SET;
    }

    void set(LivingEntity entity, boolean value) {
      if (type.isInstance(entity)) {
        setter.accept(type.cast(entity), value);
      }
    }
  }
}
