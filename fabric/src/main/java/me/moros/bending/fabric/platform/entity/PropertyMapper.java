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

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import me.moros.bending.api.platform.property.BooleanProperty;
import me.moros.bending.api.platform.property.EntityProperty;
import me.moros.bending.fabric.mixin.accessor.CreeperAccess;
import me.moros.bending.fabric.mixin.accessor.EntityAccess;
import net.kyori.adventure.util.TriState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;

import static java.util.Map.entry;

final class PropertyMapper {
  static final Map<BooleanProperty, FabricProperty<? extends net.minecraft.world.entity.Entity>> PROPERTIES;

  static {
    PROPERTIES = Map.ofEntries(
      entry(EntityProperty.SNEAKING, boolProp(Entity.class, Entity::isShiftKeyDown, Entity::setShiftKeyDown)),
      entry(EntityProperty.SPRINTING, boolProp(Entity.class, Entity::isSprinting, Entity::setSprinting)),
      entry(EntityProperty.ALLOW_FLIGHT, boolProp(ServerPlayer.class, p -> p.getAbilities().mayfly, PlayerUtil::setAllowFlight)),
      entry(EntityProperty.FLYING, boolProp(ServerPlayer.class, e -> e.getAbilities().flying, PlayerUtil::setFlying)),
      entry(EntityProperty.GLIDING, boolProp(LivingEntity.class, LivingEntity::isFallFlying, (e, v) -> ((EntityAccess) e).bending$setSharedFlag(7, v))),
      entry(EntityProperty.CHARGED, boolProp(Creeper.class, Creeper::isPowered, (e, v) -> e.getEntityData().set(CreeperAccess.getDataIsPowered(), v)))
    );
  }

  static <E extends Entity> FabricProperty<E> boolProp(Class<E> type, Predicate<E> getter, BiConsumer<E, Boolean> setter) {
    return new FabricProperty<>(type, getter, setter);
  }

  record FabricProperty<E extends Entity>(Class<E> type, Predicate<E> getter, BiConsumer<E, Boolean> setter) {
    TriState get(Entity data) {
      if (type.isInstance(data)) {
        return TriState.byBoolean(getter.test(type.cast(data)));
      }
      return TriState.NOT_SET;
    }

    void set(Entity entity, boolean value) {
      if (type.isInstance(entity)) {
        setter.accept(type.cast(entity), value);
      }
    }
  }
}
