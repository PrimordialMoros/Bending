/*
 * Copyright 2020-2024 Moros
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

import java.util.Optional;

import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.sound.SoundGroup;
import me.moros.bending.api.registry.DefaultedRegistry;
import me.moros.bending.api.registry.Registry;
import me.moros.bending.api.util.KeyUtil;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;

record BlockTypeImpl(Key key) implements BlockType {
  static final DefaultedRegistry<Key, BlockType> REGISTRY = Registry.vanillaDefaulted("blocks", BlockTypeImpl::new);
  static final Registry<Key, BlockProperties> PROPERTY_REGISTRY = Registry.vanilla("block.properties");
  static final Registry<Key, BlockState> STATE_REGISTRY = Registry.vanilla("block.state");
  static final Registry<Key, Item> ITEM_REGISTRY = Registry.vanilla("block.item");

  static BlockType get(String key) {
    return REGISTRY.get(KeyUtil.vanilla(key));
  }

  BlockProperties fromVanilla() {
    return PROPERTY_REGISTRY.getOrThrow(key());
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
    return STATE_REGISTRY.getOrThrow(key());
  }

  @Override
  public Optional<Item> asItem() {
    return Optional.ofNullable(ITEM_REGISTRY.get(key()));
  }
}
