/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.model.user;

import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import me.moros.bending.Bending;
import me.moros.bending.events.ElementChangeEvent;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.predicate.general.CompositeBendingConditional;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.qual.IntRange;

public interface User extends BukkitUser {
  @NonNull ElementHolder elementHolder();

  default boolean hasElement(@NonNull Element element) {
    return elementHolder().hasElement(element);
  }

  default boolean addElement(@NonNull Element element) {
    if (elementHolder().addElement(element)) {
      Bending.eventBus().postElementChangeEvent(this, ElementChangeEvent.Result.ADD);
      return true;
    }
    return false;
  }

  default boolean removeElement(@NonNull Element element) {
    if (elementHolder().removeElement(element)) {
      validateSlots();
      Bending.eventBus().postElementChangeEvent(this, ElementChangeEvent.Result.REMOVE);
      return true;
    }
    return false;
  }

  default @NonNull Set<@NonNull Element> elements() {
    return elementHolder().elements();
  }

  default boolean element(@NonNull Element element) {
    elementHolder().clear();
    elementHolder().addElement(element);
    validateSlots();
    Bending.game().abilityManager(world()).clearPassives(this);
    Bending.game().abilityManager(world()).createPassives(this);
    Bending.eventBus().postElementChangeEvent(this, ElementChangeEvent.Result.CHOOSE);
    return true;
  }

  boolean onCooldown(@NonNull AbilityDescription desc);

  void addCooldown(@NonNull AbilityDescription desc, long duration);

  /**
   * Like setSlotAbility but won't call any events
   */
  void slotAbilityInternal(@IntRange(from = 1, to = 9) int slot, @Nullable AbilityDescription desc);

  /**
   * This is to be used when setting individual slots.
   * If you want to bind or change multiple slots then use dummy presets
   */
  void slotAbility(@IntRange(from = 1, to = 9) int slot, @Nullable AbilityDescription desc);

  Optional<AbilityDescription> slotAbility(@IntRange(from = 1, to = 9) int slot);

  Optional<AbilityDescription> selectedAbility();


  /**
   * @return the ability's name or an empty string if no ability is bound to the currently selected slot
   */
  default @NonNull String selectedAbilityName() {
    return selectedAbility().map(AbilityDescription::name).orElse("");
  }

  default void clearSlot(@IntRange(from = 1, to = 9) int slot) {
    slotAbility(slot, null);
  }

  @NonNull CompositeBendingConditional bendingConditional();

  default boolean canBend(@NonNull AbilityDescription desc) {
    return bendingConditional().test(this, desc);
  }

  /**
   * Checks bound abilities and clears any invalid ability slots.
   * A slot is considered invalid if the user doesn't have the ability's element or doesn't have its permission.
   */
  default void validateSlots() {
    IntStream.rangeClosed(1, 9).forEach(i -> slotAbility(i).ifPresent(desc -> {
      if (!hasElement(desc.element()) || !hasPermission(desc) || !desc.canBind()) {
        slotAbilityInternal(i, null);
      }
    }));
  }

  default boolean hasPermission(@NonNull String permission) {
    return true;
  }

  default boolean hasPermission(@NonNull AbilityDescription desc) {
    return hasPermission(desc.permission());
  }

  default boolean canBuild(@NonNull Block block) {
    return Bending.game().protectionSystem().canBuild(this, block);
  }
}
