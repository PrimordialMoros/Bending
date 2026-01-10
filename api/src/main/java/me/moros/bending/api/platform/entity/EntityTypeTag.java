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

package me.moros.bending.api.platform.entity;

import me.moros.bending.api.registry.Tag;
import me.moros.bending.api.registry.TagBuilder;
import me.moros.bending.api.util.KeyUtil;
import net.kyori.adventure.key.Key;

public sealed interface EntityTypeTag extends Tags, Tag<EntityType> permits TagImpl {
  default boolean isTagged(Key key) {
    EntityType type = EntityType.registry().get(key);
    return type != null && isTagged(type);
  }

  static TagBuilder<EntityType, EntityTypeTag> builder(String namespace) {
    return builder(KeyUtil.simple(namespace));
  }

  static TagBuilder<EntityType, EntityTypeTag> builder(Key key) {
    return new TagBuilder<>(key, EntityType.registry(), TagImpl::fromContainer);
  }

  static EntityTypeTag reference(Key key) {
    return TagImpl.reference(key);
  }
}
