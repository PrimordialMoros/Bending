/*
 * Copyright 2020-2025 Moros
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

import java.util.Set;

import me.moros.bending.api.ability.preset.Preset;
import me.moros.bending.api.ability.preset.PresetRegisterResult;
import me.moros.bending.api.event.PresetRegisterEvent;
import org.jspecify.annotations.Nullable;

/**
 * Represents a user that has a set of {@link Preset Presets}.
 */
public interface PresetUser {
  /**
   * Get a copy of this user's presets.
   * @return a copy of this user's presets
   */
  Set<Preset> presets();

  /**
   * Get the current number of presets for this user.
   * @return the number of registered presets
   */
  int presetSize();

  /**
   * Get this user's preset by its name.
   * @param name the preset name to find
   * @return the specified preset if found, null otherwise
   */
  @Nullable Preset presetByName(String name);

  /**
   * Attempt to register the specified preset to the user. Calls a {@link PresetRegisterEvent}.
   * @param preset the preset to add
   * @return the result
   */
  PresetRegisterResult addPreset(Preset preset);

  /**
   * Attempt to remove the specified preset from the user.
   * @param preset the preset to remove
   * @return true if the preset was removed successfully, false otherwise
   */
  boolean removePreset(Preset preset);
}
