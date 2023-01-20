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

package me.moros.bending.platform.item;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import me.moros.bending.model.functional.Suppliers;
import me.moros.bending.model.registry.Container;
import me.moros.bending.util.KeyUtil;
import net.kyori.adventure.key.Key;

record TagImpl(Key key, Supplier<Container<Item>> supplier) implements ItemTag {
  private static final Container<Item> EMPTY = Container.create(KeyUtil.simple("empty"), Set.of());

  static ItemTag get(String key) {
    return reference(KeyUtil.vanilla(key));
  }

  static ItemTag reference(Key key) {
    return new TagImpl(key, Suppliers.lazy(() -> Item.registry().getTag(key)));
  }

  static ItemTag fromContainer(Container<Item> container) {
    return new TagImpl(container.key(), Suppliers.cached(container));
  }

  Container<Item> container() {
    var container = supplier().get();
    return container == null ? EMPTY : container;
  }

  @Override
  public boolean containsValue(Item type) {
    return container().containsValue(type);
  }

  @Override
  public int size() {
    return container().size();
  }

  @Override
  public Stream<Item> stream() {
    return container().stream();
  }

  @Override
  public Iterator<Item> iterator() {
    return container().iterator();
  }
}
