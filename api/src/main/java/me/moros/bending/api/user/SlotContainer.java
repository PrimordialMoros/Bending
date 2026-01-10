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

import java.util.function.Predicate;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.preset.Preset;
import org.jspecify.annotations.Nullable;

// CopyOnWrite slots
final class SlotContainer {
  private transient volatile AbilityDescription[] slots;
  private transient volatile Preset presetRepresentation;
  private transient final Object lock;

  SlotContainer() {
    this.lock = new Object();
    this.slots = new AbilityDescription[9];
    this.presetRepresentation = Preset.empty();
  }

  AbilityDescription[] getArray() {
    return slots;
  }

  private void setArray(AbilityDescription[] array) {
    slots = array;
    presetRepresentation = Preset.from(getArray());
  }

  @Nullable AbilityDescription get(int idx) {
    return getArray()[idx];
  }

  void set(int idx, @Nullable AbilityDescription desc) {
    synchronized (lock) {
      AbilityDescription[] arr = getArray();
      if (arr[idx] != desc) {
        arr = arr.clone();
        arr[idx] = desc;
      }
      setArray(arr);
    }
  }

  Preset toPreset() {
    return presetRepresentation;
  }

  void fromPreset(Preset preset, Predicate<AbilityDescription> predicate) {
    AbilityDescription[] copy = new AbilityDescription[9];
    preset.copyTo(copy);
    for (int idx = 0; idx < copy.length; idx++) {
      AbilityDescription target = copy[idx];
      copy[idx] = (target != null && predicate.test(target)) ? target : null;
    }
    synchronized (lock) {
      setArray(copy);
    }
  }

  void validate(Predicate<AbilityDescription> predicate) {
    AbilityDescription[] arr = getArray();
    for (int idx = 0; idx < arr.length; idx++) {
      AbilityDescription desc = arr[idx];
      if (desc != null && !predicate.test(desc)) {
        arr[idx] = null;
      }
    }
    synchronized (lock) {
      setArray(arr);
    }
  }
}
