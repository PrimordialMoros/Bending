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

package me.moros.bending.api.platform.item;

import me.moros.bending.api.registry.Tag;
import me.moros.bending.api.registry.TagBuilder;
import me.moros.bending.api.util.KeyUtil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;

public sealed interface ItemTag extends Keyed, Tags, Tag<Item> permits TagImpl {
  default boolean isTagged(Key key) {
    Item type = Item.registry().get(key);
    return type != null && isTagged(type);
  }

  default boolean isTagged(ItemSnapshot item) {
    return isTagged(item.type());
  }

  static TagBuilder<Item, ItemTag> builder(String namespace) {
    return builder(KeyUtil.simple(namespace));
  }

  static TagBuilder<Item, ItemTag> builder(Key key) {
    return new TagBuilder<>(key, Item.registry(), TagImpl::fromContainer);
  }

  static ItemTag reference(Key key) {
    return TagImpl.reference(key);
  }
}
