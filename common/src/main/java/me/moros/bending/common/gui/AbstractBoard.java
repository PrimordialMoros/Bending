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

package me.moros.bending.common.gui;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.gui.Board;
import me.moros.bending.api.locale.Message;
import me.moros.bending.api.user.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import static net.kyori.adventure.text.format.TextDecoration.STRIKETHROUGH;

public abstract class AbstractBoard<Team> implements Board {
  protected static final Component SEP = Component.text(" -------------- ");
  protected static final Component INACTIVE = Component.text("> ", NamedTextColor.DARK_GRAY);
  protected static final Component ACTIVE = Component.text("> ");

  protected final User user;
  protected final Map<AbilityDescription, Indexed<Team>> misc;

  private int selectedSlot;

  protected AbstractBoard(User user) {
    this.user = user;
    this.misc = new ConcurrentHashMap<>(); // Used for combos and misc abilities
    selectedSlot = user.currentSlot();
  }

  protected void init() {
    updateAll();
    for (int slot = 1; slot <= 9; slot++) { // init slot prefixes
      setPrefix(getOrCreateTeam(slot), slot == selectedSlot ? ACTIVE : INACTIVE);
    }
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public void updateAll() {
    var snapshot = user.slots().abilities();
    for (int slot = 1; slot <= 9; slot++) {
      AbilityDescription desc = snapshot.get(slot - 1);
      Component suffix;
      if (desc == null) {
        suffix = Message.BENDING_BOARD_EMPTY_SLOT.build(slot);
      } else {
        suffix = !user.onCooldown(desc) ? desc.displayName() : desc.displayName().decorate(STRIKETHROUGH);
      }
      setSuffix(getOrCreateTeam(slot), suffix);
    }
  }

  @Override
  public void activeSlot(int oldSlot, int newSlot) {
    if (validSlot(oldSlot) && validSlot(newSlot)) {
      if (selectedSlot != oldSlot) {
        oldSlot = selectedSlot; // Fixes bug when slot is set using setHeldItemSlot
      }
      selectedSlot = newSlot;
      setPrefix(getOrCreateTeam(oldSlot), INACTIVE);
      setPrefix(getOrCreateTeam(newSlot), ACTIVE);
    }
  }

  @Override
  public void updateMisc(AbilityDescription desc, boolean show) {
    if (show && misc.isEmpty()) {
      setPrefix(getOrCreateTeam(10), SEP);
    }
    Team team = misc.computeIfAbsent(desc, d -> createTeam(11, pickAvailableSlot())).team();
    if (show) {
      setPrefix(team, INACTIVE.append(desc.displayName().decorate(STRIKETHROUGH)));
    } else {
      removeTeam(team);
      misc.remove(desc);
      if (misc.isEmpty()) {
        removeTeam(getOrCreateTeam(10));
      }
    }
  }

  protected abstract void setPrefix(Team team, Component content);

  protected abstract void setSuffix(Team team, Component content);

  protected abstract Team getOrCreateTeam(int slot);

  protected abstract Indexed<Team> createTeam(int slot, int textSlot);

  protected abstract void removeTeam(Team team);

  protected int pickAvailableSlot() {
    int idx = 11;
    for (var indexedTeam : misc.values()) {
      idx = Math.max(indexedTeam.textSlot() + 1, idx);
    }
    return idx;
  }

  private boolean validSlot(int slot) {
    return 1 <= slot && slot <= 9;
  }

  protected record Indexed<T>(T team, int textSlot) {
    public static <T> Indexed<T> create(T team, int textSlot) {
      return new Indexed<>(team, textSlot);
    }
  }

  private static final String[] CHAT_CODES;

  static {
    CHAT_CODES = new String[16];
    for (int i = 0; i < CHAT_CODES.length; i++) {
      CHAT_CODES[i] = "§%s§r".formatted(Integer.toHexString(i));
    }
  }

  protected static String generateInvisibleLegacyString(int slot) {
    String hidden = CHAT_CODES[slot % CHAT_CODES.length];
    return slot <= CHAT_CODES.length ? hidden : hidden + generateInvisibleLegacyString(slot - CHAT_CODES.length);
  }
}
