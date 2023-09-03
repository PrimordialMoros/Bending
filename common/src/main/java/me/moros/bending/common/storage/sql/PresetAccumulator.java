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

package me.moros.bending.common.storage.sql;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.preset.Preset;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jdbi.v3.core.result.RowReducer;
import org.jdbi.v3.core.result.RowView;

public record PresetAccumulator(Function<UUID, @Nullable AbilityDescription> index)
  implements RowReducer<Map<String, AbilityDescription[]>, Preset> {

  @Override
  public Map<String, AbilityDescription[]> container() {
    return new HashMap<>();
  }

  @Override
  public void accumulate(Map<String, AbilityDescription[]> container, RowView rowView) {
    String name = rowView.getColumn("preset_name", String.class);
    int slot = rowView.getColumn("slot", Integer.class);
    AbilityDescription desc = index().apply(rowView.getColumn("ability_id", UUID.class));
    container.computeIfAbsent(name, n -> new AbilityDescription[9])[slot - 1] = desc;
  }

  @Override
  public Stream<Preset> stream(Map<String, AbilityDescription[]> container) {
    return container.entrySet().stream().map(e -> Preset.create(e.getKey(), e.getValue()));
  }
}
