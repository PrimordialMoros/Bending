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

import java.util.Optional;
import java.util.function.Supplier;

import me.moros.bending.model.functional.Suppliers;
import me.moros.bending.model.registry.Registry;
import me.moros.bending.platform.item.Item;
import me.moros.bending.platform.sound.SoundGroup;
import me.moros.bending.util.KeyUtil;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;

record BlockTypeImpl(Key key, Supplier<BlockProperties> properties, Supplier<BlockState> state,
                     Supplier<Item> item) implements BlockType {
  static final Registry<Key, BlockType> REGISTRY = Registry.vanilla("blocks");
  static final Registry<Key, BlockProperties> PROPERTY_REGISTRY = Registry.vanilla("block.properties");
  static final Registry<Key, BlockState> STATE_REGISTRY = Registry.vanilla("block.state");
  static final Registry<Key, Item> ITEM_REGISTRY = Registry.vanilla("block.item");

  static BlockType get(String key) {
    return getOrCreate(KeyUtil.vanilla(key));
  }

  static BlockType getOrCreate(Key key) {
    var instance = REGISTRY.get(key);
    if (instance == null) {
      instance = new BlockTypeImpl(key,
        Suppliers.lazy(() -> PROPERTY_REGISTRY.get(key)),
        Suppliers.lazy(() -> STATE_REGISTRY.get(key)),
        Suppliers.lazy(() -> ITEM_REGISTRY.get(key))
      );
      REGISTRY.register(instance);
    }
    return instance;
  }

  BlockProperties fromVanilla() {
    return properties.get();
  }

  @Override
  public boolean isAir() {
    return fromVanilla().isAir();
  }

  @Override
  public boolean isSolid() {
    return fromVanilla().isSolid();
  }

  @Override
  public boolean isLiquid() {
    return fromVanilla().isLiquid();
  }

  @Override
  public boolean isFlammable() {
    return fromVanilla().isFlammable();
  }

  @Override
  public boolean hasGravity() {
    return fromVanilla().hasGravity();
  }

  @Override
  public boolean isCollidable() {
    return fromVanilla().isCollidable();
  }

  @Override
  public double hardness() {
    return fromVanilla().hardness();
  }

  @Override
  public @NonNull String translationKey() {
    return fromVanilla().translationKey();
  }

  @Override
  public SoundGroup soundGroup() {
    return fromVanilla().soundGroup();
  }

  @Override
  public BlockState defaultState() {
    return state.get();
  }

  @Override
  public Optional<Item> asItem() {
    return Optional.ofNullable(item.get());
  }
}
