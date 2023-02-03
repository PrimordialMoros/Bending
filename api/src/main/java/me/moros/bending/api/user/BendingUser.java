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

package me.moros.bending.api.user;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.ability.preset.Preset;
import me.moros.bending.api.board.Board;
import me.moros.bending.api.config.attribute.AttributeModifier;
import me.moros.bending.api.event.ElementChangeEvent.ElementAction;
import me.moros.bending.api.event.EventBus;
import me.moros.bending.api.functional.BendingConditions;
import me.moros.bending.api.game.Game;
import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.temporal.Cooldown;
import me.moros.bending.api.user.profile.BenderData;
import me.moros.bending.api.util.data.DataContainer;
import net.kyori.adventure.util.TriState;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Base {@link User} implementation for all living entities.
 */
public sealed class BendingUser implements User permits BendingPlayer {
  private final LivingEntity entity;
  private final Game game;
  private final DataContainer container;
  private final Set<Element> elements;
  private final Map<String, TriState> virtualPermissions;
  private final Collection<AttributeModifier> attributes;
  private final AbilityDescription[] slots;
  private final BiPredicate<User, AbilityDescription> condition;

  private boolean canBend = true;
  private int index = 1;

  protected BendingUser(Game game, LivingEntity entity, BenderData data) {
    this.entity = entity;
    this.game = game;
    container = DataContainer.blocking(500, TimeUnit.MILLISECONDS);
    virtualPermissions = new ConcurrentHashMap<>();
    attributes = ConcurrentHashMap.newKeySet();
    slots = new AbilityDescription[9];
    int size = Math.min(data.slots().size(), 9);
    for (int i = 0; i < size; i++) {
      slots[i] = data.slots().get(i);
    }
    elements = EnumSet.noneOf(Element.class);
    elements.addAll(data.elements());
    condition = BendingConditions.all();
    validateSlots();
  }

  @Override
  public LivingEntity entity() {
    return entity;
  }

  @Override
  public Game game() {
    return game;
  }

  @Override
  public DataContainer store() {
    return container;
  }

  @Override
  public Set<Element> elements() {
    return EnumSet.copyOf(elements);
  }

  @Override
  public boolean hasElement(Element element) {
    return elements.contains(element);
  }

  @Override
  public boolean addElement(Element element) {
    if (!hasElement(element) && EventBus.INSTANCE.postElementChangeEvent(this, element, ElementAction.ADD)) {
      elements.add(element);
      validatePassives();
      return true;
    }
    return false;
  }

  @Override
  public boolean removeElement(Element element) {
    if (hasElement(element) && EventBus.INSTANCE.postElementChangeEvent(this, element, ElementAction.REMOVE)) {
      elements.remove(element);
      validateAbilities();
      validateSlots();
      board().updateAll();
      return true;
    }
    return false;
  }

  @Override
  public boolean chooseElement(Element element) {
    if (EventBus.INSTANCE.postElementChangeEvent(this, element, ElementAction.CHOOSE)) {
      elements.clear();
      elements.add(element);
      validateAbilities();
      validateSlots();
      board().updateAll();
      return true;
    }
    return false;
  }

  private void validateAbilities() {
    game.abilityManager(worldKey()).destroyUserInstances(this, a -> !hasElement(a.description().element()));
    validatePassives();
  }

  private void validatePassives() {
    game.abilityManager(worldKey()).createPassives(this);
  }

  @Override
  public Preset createPresetFromSlots(String name) {
    return new Preset(0, name, slots);
  }

  @Override
  public boolean bindPreset(Preset preset) {
    if (EventBus.INSTANCE.postMultiBindChangeEvent(this, preset)) {
      Preset oldBinds = createPresetFromSlots("");
      preset.copyTo(slots);
      validateSlots();
      board().updateAll();
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
    if (EventBus.INSTANCE.postSingleBindChangeEvent(this, slot, desc)) {
      slots[slot - 1] = desc;
      board().updateAll();
    }
  }

  @Override
  public int currentSlot() {
    return index + 1;
  }

  @Override
  public void currentSlot(int slot) {
    if (slot >= 1 && slot <= 9) {
      index = slot - 1;
    }
  }

  @Override
  public @Nullable AbilityDescription selectedAbility() {
    return slots[index];
  }

  @Override
  public boolean onCooldown(AbilityDescription desc) {
    return Cooldown.MANAGER.isTemp(Cooldown.of(this, desc));
  }

  @Override
  public boolean addCooldown(AbilityDescription desc, long duration) {
    if (duration > 0 && EventBus.INSTANCE.postCooldownAddEvent(this, desc, duration)) {
      Cooldown.of(this, desc, () -> removeCooldown(desc), duration);
      updateBoard(desc, true);
      return true;
    }
    return false;
  }

  private void removeCooldown(AbilityDescription desc) {
    if (valid()) { // Ensure user is valid before processing
      updateBoard(desc, false);
      EventBus.INSTANCE.postCooldownRemoveEvent(this, desc);
    }
  }

  @Override
  public boolean canBend(AbilityDescription desc) {
    return condition.test(this, desc);
  }

  @Override
  public boolean canBend() {
    return canBend;
  }

  @Override
  public boolean toggleBending() {
    canBend ^= true;
    if (!canBend) {
      game.abilityManager(worldKey()).destroyUserInstances(this, a -> !a.description().isActivatedBy(Activation.PASSIVE));
    }
    return canBend;
  }

  @Override
  public Board board() {
    return Board.dummy();
  }

  @Override
  public boolean hasPermission(String permission) {
    return virtualPermissions.get(permission) != TriState.FALSE;
  }

  @Override
  public TriState setPermission(String permission, TriState state) {
    TriState prev;
    if (state == TriState.NOT_SET) {
      prev = virtualPermissions.remove(permission);
    } else {
      prev = virtualPermissions.put(permission, state);
    }
    return prev == null ? TriState.NOT_SET : prev;
  }

  @Override
  public boolean addAttribute(AttributeModifier modifier) {
    return attributes.add(modifier);
  }

  @Override
  public void clearAttributes() {
    attributes.clear();
  }

  @Override
  public Stream<AttributeModifier> attributes() {
    return attributes.stream();
  }

  private void updateBoard(@Nullable AbilityDescription desc, boolean cooldown) {
    if (desc != null && !desc.canBind()) {
      board().updateMisc(desc, cooldown);
    } else {
      board().updateAll();
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
    if (this == obj) {
      return true;
    }
    if (obj instanceof BendingUser other) {
      return entity().equals(other.entity());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return entity().hashCode();
  }

  public static Optional<User> createUser(Game game, LivingEntity entity, BenderData data) {
    Objects.requireNonNull(game);
    if (Registries.BENDERS.containsKey(entity.uuid()) || entity instanceof Player) {
      return Optional.empty();
    }
    return Optional.of(new BendingUser(game, entity, data));
  }
}
