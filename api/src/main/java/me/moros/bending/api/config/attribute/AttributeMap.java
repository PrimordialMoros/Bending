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

package me.moros.bending.api.config.attribute;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

import net.kyori.adventure.key.Key;

record AttributeMap(Map<Key, AttributeModifier> modifierMap) implements AttributeHolder {
  AttributeMap() {
    this(new ConcurrentHashMap<>());
  }

  @Override
  public boolean add(ModifyPolicy policy, Attribute attribute, Modifier modifier) {
    Key key = extractKey(policy, attribute);
    modifierMap.merge(key, modifier.asAttributeModifier(policy, attribute), this::mergeEntries);
    return true;
  }

  private AttributeModifier mergeEntries(AttributeModifier first, AttributeModifier second) {
    return first.modifier().merge(second.modifier()).asAttributeModifier(first.policy(), first.attribute());
  }

  @Override
  public boolean remove(Predicate<AttributeModifier> predicate) {
    return modifierMap.entrySet().removeIf(e -> predicate.test(e.getValue()));
  }

  @Override
  public void clear() {
    modifierMap.clear();
  }

  @Override
  public Stream<AttributeModifier> stream() {
    return modifierMap.values().stream();
  }

  private static Key extractKey(ModifyPolicy policy, Attribute attribute) {
    String combinedValue = policy.key().value() + '/' + attribute.value().toLowerCase(Locale.ROOT);
    return Key.key(policy.key().namespace(), combinedValue);
  }
}
