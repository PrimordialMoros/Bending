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

package me.moros.bending.api.platform.block;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import me.moros.bending.api.registry.Container;
import me.moros.bending.api.util.KeyUtil;
import me.moros.bending.api.util.functional.Suppliers;
import net.kyori.adventure.key.Key;

record TagImpl(Key key, Supplier<Container<BlockType>> supplier) implements BlockTag {
  private static final Container<BlockType> EMPTY = Container.create(KeyUtil.simple("empty"), Set.of());

  static BlockTag get(String key) {
    return reference(KeyUtil.vanilla(key));
  }

  static BlockTag reference(Key key) {
    return new TagImpl(key, Suppliers.lazy(() -> BlockType.registry().getTag(key)));
  }

  static BlockTag fromContainer(Container<BlockType> container) {
    return new TagImpl(container.key(), Suppliers.cached(container));
  }

  Container<BlockType> container() {
    var container = supplier().get();
    return container == null ? EMPTY : container;
  }

  @Override
  public boolean containsValue(BlockType type) {
    return container().containsValue(type);
  }

  @Override
  public int size() {
    return container().size();
  }

  @Override
  public Stream<BlockType> stream() {
    return container().stream();
  }

  @Override
  public Iterator<BlockType> iterator() {
    return container().iterator();
  }
}
