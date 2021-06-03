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

package me.moros.bending.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import me.moros.bending.model.attribute.AttributeModifier;
import me.moros.bending.model.user.User;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class AttributeRegistry implements Registry<AttributeModifier> {
  private final Map<UUID, Collection<AttributeModifier>> modifierMap;

  AttributeRegistry() {
    modifierMap = new HashMap<>();
  }

  public void add(@NonNull User user, @NonNull AttributeModifier modifier) {
    modifierMap.computeIfAbsent(user.entity().getUniqueId(), u -> new ArrayList<>()).add(modifier);
  }

  public void invalidate(@NonNull UUID uuid) {
    modifierMap.remove(uuid);
  }

  public boolean contains(@NonNull UUID uuid) {
    return modifierMap.containsKey(uuid);
  }

  public @NonNull Collection<@NonNull AttributeModifier> get(@NonNull UUID uuid) {
    return modifierMap.getOrDefault(uuid, new ArrayList<>());
  }

  @Override
  public @NonNull Iterator<AttributeModifier> iterator() {
    return modifierMap.values().stream().flatMap(Collection::stream).collect(Collectors.toList()).iterator();
  }
}
