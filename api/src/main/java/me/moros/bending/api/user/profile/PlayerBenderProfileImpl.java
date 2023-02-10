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

package me.moros.bending.api.user.profile;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.ability.preset.Preset;
import org.checkerframework.checker.nullness.qual.Nullable;

record PlayerBenderProfileImpl(int id, UUID uuid, boolean board,
                               BenderProfile benderProfile) implements PlayerBenderProfile {
  @Override
  public List<@Nullable AbilityDescription> slots() {
    return benderProfile().slots();
  }

  @Override
  public Set<Element> elements() {
    return benderProfile().elements();
  }

  @Override
  public Set<Preset> presets() {
    return benderProfile().presets();
  }
}
