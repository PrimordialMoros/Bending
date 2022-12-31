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

package me.moros.bending.platform.block;

import java.util.Iterator;
import java.util.function.Supplier;
import java.util.stream.Stream;

import me.moros.bending.model.functional.Suppliers;
import me.moros.bending.model.registry.Container;
import me.moros.bending.model.registry.Registry;
import me.moros.bending.util.KeyUtil;
import net.kyori.adventure.key.Key;

record TagImpl(Key key, Supplier<Container<BlockType>> container) implements BlockTag {
  TagImpl(Container<BlockType> container) {
    this(container.key(), Suppliers.cached(container));
  }

  static final Registry<Key, BlockTag> REGISTRY = Registry.vanilla("tags");
  static final Registry<Key, Container<BlockType>> DATA_REGISTRY = Registry.vanilla("tags");

  static BlockTag get(String key) {
    var k = KeyUtil.vanilla(key);
    BlockTag instance = new TagImpl(k, Suppliers.lazy(() -> DATA_REGISTRY.get(k)));
    REGISTRY.register(instance);
    return instance;
  }

  Container<BlockType> fromVanilla() {
    return container.get();
  }

  @Override
  public boolean containsValue(BlockType type) {
    return fromVanilla().containsValue(type);
  }

  @Override
  public int size() {
    return fromVanilla().size();
  }

  @Override
  public Stream<BlockType> stream() {
    return fromVanilla().stream();
  }

  @Override
  public Iterator<BlockType> iterator() {
    return fromVanilla().iterator();
  }
}
