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

package me.moros.bending.api.platform.entity;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import me.moros.bending.api.registry.Container;
import me.moros.bending.api.util.KeyUtil;
import me.moros.bending.api.util.functional.Suppliers;
import net.kyori.adventure.key.Key;

record TagImpl(Key key, Supplier<Container<EntityType>> supplier) implements EntityTypeTag {
  private static final Container<EntityType> EMPTY = Container.create(KeyUtil.simple("empty"), Set.of());

  static EntityTypeTag get(String key) {
    return reference(KeyUtil.vanilla(key));
  }

  static EntityTypeTag reference(Key key) {
    return new TagImpl(key, Suppliers.lazy(() -> EntityType.registry().getTag(key)));
  }

  static EntityTypeTag fromContainer(Container<EntityType> container) {
    return new TagImpl(container.key(), Suppliers.cached(container));
  }

  Container<EntityType> container() {
    var container = supplier().get();
    return container == null ? EMPTY : container;
  }

  @Override
  public boolean containsValue(EntityType type) {
    return container().containsValue(type);
  }

  @Override
  public int size() {
    return container().size();
  }

  @Override
  public Stream<EntityType> stream() {
    return container().stream();
  }

  @Override
  public Iterator<EntityType> iterator() {
    return container().iterator();
  }
}
