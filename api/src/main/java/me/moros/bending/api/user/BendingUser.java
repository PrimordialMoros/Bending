/*
 * Copyright 2020-2026 Moros
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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.ability.preset.Preset;
import me.moros.bending.api.ability.preset.PresetRegisterResult;
import me.moros.bending.api.config.attribute.AttributeHolder;
import me.moros.bending.api.event.ElementChangeEvent.ElementAction;
import me.moros.bending.api.game.Game;
import me.moros.bending.api.gui.Board;
import me.moros.bending.api.platform.entity.DelegateLivingEntity;
import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.bending.api.temporal.Cooldown;
import me.moros.bending.api.user.profile.BenderProfile;
import me.moros.bending.api.util.Tasker;
import me.moros.bending.api.util.collect.ElementSet;
import me.moros.bending.api.util.data.DataContainer;
import me.moros.bending.api.util.functional.BendingConditions;
import net.kyori.adventure.util.TriState;
import org.jspecify.annotations.Nullable;

/**
 * Base {@link User} implementation for all living entities.
 */
sealed class BendingUser implements User, DelegateLivingEntity permits BendingPlayer {
  private final Game game;
  private final LivingEntity entity;
  private final DataContainer container;
  private final Map<String, TriState> virtualPermissions;
  private final AttributeHolder attributeMap;
  private final BiPredicate<User, AbilityDescription> condition;

  private final ElementSet elements;
  private final SlotContainer slots;
  private final Set<Preset> presets;

  private boolean canBend = true;
  private int index = 1;

  protected BendingUser(Game game, LivingEntity entity) {
    this.game = game;
    this.entity = entity;
    this.container = DataContainer.blocking(500, TimeUnit.MILLISECONDS);
    this.virtualPermissions = new ConcurrentHashMap<>();
    this.attributeMap = AttributeHolder.createEmpty();
    this.condition = BendingConditions.all();
    this.elements = ElementSet.mutable();
    this.slots = new SlotContainer();
    this.presets = ConcurrentHashMap.newKeySet(6);
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
    return ElementSet.copyOf(elements);
  }

  protected boolean hasElements() {
    return !elements.isEmpty();
  }

  @Override
  public boolean hasElement(Element element) {
    return elements.contains(element);
  }

  @Override
  public boolean hasElements(Collection<Element> elements) {
    return this.elements.containsAll(elements);
  }

  @Override
  public boolean addElement(Element element) {
    if (!hasElement(element) && game().eventBus().postElementChangeEvent(this, element, ElementAction.ADD)) {
      boolean empty = elements.isEmpty();
      elements.add(element);
      validatePassives();
      if (empty) {
        board();
      }
      return true;
    }
    return false;
  }

  @Override
  public boolean removeElement(Element element) {
    if (hasElement(element) && game().eventBus().postElementChangeEvent(this, element, ElementAction.REMOVE)) {
      elements.remove(element);
      slots.validate(desc -> hasElements(desc.elements()));
      validateAbilities();
      board().updateAll();
      return true;
    }
    return false;
  }

  @Override
  public boolean chooseElement(Element element) {
    if (game().eventBus().postElementChangeEvent(this, element, ElementAction.CHOOSE)) {
      elements.set(ElementSet.of(element));
      Preset elementPreset = presetByName(element.name().toLowerCase(Locale.ROOT));
      if (elementPreset != null) {
        slots.fromPreset(elementPreset, this::validate);
      } else {
        slots.validate(desc -> hasElements(desc.elements()));
        validateAbilities();
      }
      board().updateAll();
      return true;
    }
    return false;
  }

  private void validateAbilities() {
    game.abilityManager(worldKey()).destroyUserInstances(this, a -> !hasElements(a.description().elements()));
    validatePassives();
  }

  private void validatePassives() {
    game.abilityManager(worldKey()).createPassives(this);
  }

  @Override
  public Preset slots() {
    return slots.toPreset();
  }

  @Override
  public boolean bindPreset(Preset preset) {
    if (game().eventBus().postMultiBindChangeEvent(this, preset)) {
      Preset oldBinds = slots();
      slots.fromPreset(preset, this::validate);
      board().updateAll();
      return !oldBinds.matchesBinds(slots.getArray());
    }
    return false;
  }

  @Override
  public @Nullable AbilityDescription boundAbility(int slot) {
    if (slot < 1 || slot > 9) {
      return null;
    }
    return slots.get(slot - 1);
  }

  @Override
  public void bindAbility(int slot, @Nullable AbilityDescription desc) {
    if (slot < 1 || slot > 9 || (desc != null && !desc.canBind())) {
      return;
    }
    if (game().eventBus().postSingleBindChangeEvent(this, slot, desc)) {
      slots.set(slot - 1, desc);
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
  public boolean onCooldown(AbilityDescription desc) {
    return Cooldown.MANAGER.isTemp(Cooldown.of(this, desc));
  }

  @Override
  public boolean addCooldown(AbilityDescription desc, long duration) {
    if (duration > 0 && game().eventBus().postCooldownAddEvent(this, desc, duration)) {
      Cooldown.of(this, desc, () -> onRemoveCooldown(desc), duration);
      updateBoard(desc, true);
      return true;
    }
    return false;
  }

  private void onRemoveCooldown(AbilityDescription desc) {
    if (valid()) { // Ensure user is valid before processing
      updateBoard(desc, false);
      game().eventBus().postCooldownRemoveEvent(this, desc);
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
    board(); // Update board visibility
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
  public AttributeHolder attributeModifiers() {
    return attributeMap;
  }

  private void updateBoard(@Nullable AbilityDescription desc, boolean cooldown) {
    if (desc != null && !desc.canBind()) {
      board().updateMisc(desc, cooldown);
    } else {
      board().updateAll();
    }
  }

  private boolean validate(AbilityDescription desc) {
    return desc.canBind() && hasElements(desc.elements()) && hasPermission(desc);
  }

  // Presets
  @Override
  public Set<Preset> presets() {
    return Set.copyOf(presets);
  }

  @Override
  public int presetSize() {
    return presets.size();
  }

  @Override
  public @Nullable Preset presetByName(String name) {
    return presets.stream().filter(p -> p.name().equalsIgnoreCase(name)).findAny().orElse(null);
  }

  @Override
  public PresetRegisterResult addPreset(Preset preset) {
    String n = preset.name();
    if (presets.contains(preset) || presets.stream().map(Preset::name).anyMatch(n::equalsIgnoreCase)) {
      return PresetRegisterResult.EXISTS;
    }
    if (n.isEmpty() || preset.isEmpty() || !game().eventBus().postPresetRegisterEvent(this, preset)) {
      return PresetRegisterResult.CANCELLED;
    }
    presets.add(preset);
    return PresetRegisterResult.SUCCESS;
  }

  @Override
  public boolean removePreset(Preset preset) {
    if (presets.remove(preset)) {
      game().eventBus().postPresetUnregisterEvent(this, preset);
      return true;
    }
    return false;
  }

  @Override
  public BenderProfile toProfile() {
    return BenderProfile.of(uuid(), !store().has(Board.HIDDEN), elements(), slots(), presets);
  }

  @Override
  public boolean fromProfile(BenderProfile profile) {
    if (!uuid().equals(profile.uuid()) || !valid()) {
      return false;
    }
    if (elements.set(profile.elements())) {
      validateAbilities();
    }
    slots.fromPreset(profile.slots(), this::validate);
    presets.clear();
    presets.addAll(profile.presets().values());
    if (profile.board()) {
      Tasker.sync().submit(() -> board().updateAll());
    } else {
      store().add(Board.HIDDEN, true);
    }
    return true;
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
}
