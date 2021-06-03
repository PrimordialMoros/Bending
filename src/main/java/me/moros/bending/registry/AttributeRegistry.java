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

import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;
import java.util.stream.Stream;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import me.moros.bending.model.attribute.AttributeModifier;
import me.moros.bending.model.user.User;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class AttributeRegistry implements Registry<AttributeModifier> {
  private final Multimap<UUID, AttributeModifier> modifierMap;

  @SuppressWarnings("UnstableApiUsage")
  AttributeRegistry() {
    modifierMap = MultimapBuilder.hashKeys().arrayListValues().build();
  }

  public void add(@NonNull User user, @NonNull AttributeModifier modifier) {
    modifierMap.put(user.entity().getUniqueId(), modifier);
  }

  public void invalidate(@NonNull User user) {
    modifierMap.removeAll(user.entity().getUniqueId());
  }

  public boolean contains(@NonNull User user) {
    return modifierMap.containsKey(user.entity().getUniqueId());
  }

  public @NonNull Stream<@NonNull AttributeModifier> attributes(@NonNull User user) {
    return modifierMap.get(user.entity().getUniqueId()).stream();
  }

  @Override
  public @NonNull Iterator<AttributeModifier> iterator() {
    return Collections.unmodifiableCollection(modifierMap.values()).iterator();
  }
}
