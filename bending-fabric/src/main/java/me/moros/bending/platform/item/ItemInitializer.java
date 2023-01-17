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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import me.moros.bending.model.registry.Container;
import me.moros.bending.platform.AbstractInitializer;
import me.moros.bending.platform.PlatformAdapter;
import net.kyori.adventure.key.Key;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet.Named;
import net.minecraft.core.Registry;
import org.slf4j.Logger;

public final class ItemInitializer extends AbstractInitializer {
  public ItemInitializer(Path path, Logger logger) {
    super(path, logger);
  }

  @Override
  public void init() {
    var map = collect();
    Collection<Key> missing = new ArrayList<>();
    for (ItemTag tag : ItemTag.registry()) {
      Key key = tag.key();
      var data = map.get(key);
      if (data != null && !data.isEmpty()) {
        TagImpl.DATA_REGISTRY.register(Container.create(key, data));
      } else {
        missing.add(key);
      }
    }
    checkMissing("itemtags.log", "Missing item tags: %d", missing);
  }

  private Map<Key, Set<Item>> collect() {
    return Registry.ITEM.getTags()
      .collect(Collectors.toMap(p -> p.getFirst().location(), p -> toSet(p.getSecond())));
  }

  private Set<Item> toSet(Named<net.minecraft.world.item.Item> holder) {
    return holder.stream().map(Holder::value).map(PlatformAdapter.ITEM_MATERIAL_INDEX::value)
      .filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
  }
}
