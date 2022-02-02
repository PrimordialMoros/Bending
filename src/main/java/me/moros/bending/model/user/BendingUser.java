/*
 * Copyright 2020-2022 Moros
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

package me.moros.bending.model.user;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import me.moros.bending.Bending;
import me.moros.bending.event.BindChangeEvent.BindType;
import me.moros.bending.event.ElementChangeEvent.ElementAction;
import me.moros.bending.game.temporal.Cooldown;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.predicate.general.BendingConditions;
import me.moros.bending.model.predicate.general.CompositeBendingConditional;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.user.profile.BenderData;
import me.moros.bending.registry.Registries;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BendingUser implements User {
  private final LivingEntity entity;
  private final DataHolder container;
  private final Set<Element> elements;
  private final AbilityDescription[] slots;
  private final CompositeBendingConditional bendingConditional;

  private final boolean isPlayer;

  protected BendingUser(@NonNull LivingEntity entity, @NonNull BenderData data) {
    this.entity = entity;
    this.isPlayer = entity instanceof Player;
    container = new DataContainer();
    slots = new AbilityDescription[9];
    int size = Math.min(data.slots().size(), 9);
    for (int i = 0; i < size; i++) {
      slots[i] = data.slots().get(i);
    }
    elements = EnumSet.noneOf(Element.class);
    elements.addAll(data.elements());
    bendingConditional = BendingConditions.builder().build();
    validateSlots();
  }

  @Override
  public @NonNull DataHolder store() {
    return container;
  }

  @Override
  public @NonNull LivingEntity entity() {
    return entity;
  }

  @Override
  public @NonNull Set<@NonNull Element> elements() {
    return EnumSet.copyOf(elements);
  }

  @Override
  public boolean hasElement(@NonNull Element element) {
    return elements.contains(element);
  }

  @Override
  public boolean addElement(@NonNull Element element) {
    if (!hasElement(element) && Bending.eventBus().postElementChangeEvent(this, ElementAction.ADD)) {
      elements.add(element);
      return true;
    }
    return false;
  }

  @Override
  public boolean removeElement(@NonNull Element element) {
    if (hasElement(element) && Bending.eventBus().postElementChangeEvent(this, ElementAction.REMOVE)) {
      elements.remove(element);
      validateSlots();
      updateBoard();
      return true;
    }
    return false;
  }

  @Override
  public boolean chooseElement(@NonNull Element element) {
    if (Bending.eventBus().postElementChangeEvent(this, ElementAction.CHOOSE)) {
      elements.clear();
      elements.add(element);
      validateSlots();
      updateBoard();
      Bending.game().abilityManager(world()).createPassives(this);
      return true;
    }
    return false;
  }

  @Override
  public @NonNull Preset createPresetFromSlots(@NonNull String name) {
    return new Preset(0, name, slots);
  }

  @Override
  public boolean bindPreset(@NonNull Preset preset) {
    if (Bending.eventBus().postBindChangeEvent(this, BindType.MULTIPLE)) {
      Preset oldBinds = createPresetFromSlots("");
      preset.copyTo(slots);
      validateSlots();
      updateBoard();
      Preset newBinds = createPresetFromSlots("");
      return oldBinds.compare(newBinds) > 0;
    }
    return false;
  }

  @Override
  public @Nullable AbilityDescription boundAbility(int slot) {
    if (slot < 1 || slot > 9) {
      return null;
    }
    return slots[slot - 1];
  }

  @Override
  public void bindAbility(int slot, @Nullable AbilityDescription desc) {
    if (slot < 1 || slot > 9) {
      return;
    }
    if (Bending.eventBus().postBindChangeEvent(this, BindType.SINGLE)) {
      slots[slot - 1] = desc;
      updateBoard();
    }
  }

  @Override
  public @Nullable AbilityDescription selectedAbility() {
    return null; // Non-player bending users don't have anything selected.
  }

  @Override
  public boolean onCooldown(@NonNull AbilityDescription desc) {
    return Cooldown.MANAGER.isTemp(Cooldown.of(this, desc));
  }

  @Override
  public boolean addCooldown(@NonNull AbilityDescription desc, long duration) {
    if (duration > 0 && Bending.eventBus().postCooldownAddEvent(this, desc, duration)) {
      Cooldown.of(this, desc, () -> removeCooldown(desc), duration);
      updateBoard(desc, true);
      return true;
    }
    return false;
  }

  private void removeCooldown(AbilityDescription desc) {
    if (valid()) { // Ensure user is valid before processing
      updateBoard(desc, false);
      Bending.eventBus().postCooldownRemoveEvent(this, desc);
    }
  }

  @Override
  public @NonNull CompositeBendingConditional bendingConditional() {
    return bendingConditional;
  }

  private void updateBoard(AbilityDescription desc, boolean cooldown) {
    if (isPlayer) {
      Bending.game().boardManager().updateBoardSlot((BendingPlayer) this, desc, cooldown);
    }
  }

  private void updateBoard() {
    if (isPlayer) {
      Bending.game().boardManager().updateBoard((BendingPlayer) this);
    }
  }

  /**
   * Checks bound abilities and clears any invalid ability slots.
   */
  private void validateSlots() {
    for (int i = 0; i < 9; i++) {
      AbilityDescription desc = slots[i];
      if (desc != null && (!hasElement(desc.element()) || !hasPermission(desc) || !desc.canBind())) {
        slots[i] = null;
      }
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof BendingUser other) {
      return entity().equals(other.entity());
    }
    return entity().equals(obj);
  }

  @Override
  public int hashCode() {
    return entity.hashCode();
  }

  public static Optional<BendingUser> createUser(@NonNull LivingEntity entity, @NonNull BenderData data) {
    if (Registries.BENDERS.contains(entity.getUniqueId()) || entity instanceof Player) {
      return Optional.empty();
    }
    return Optional.of(new BendingUser(entity, data));
  }
}
