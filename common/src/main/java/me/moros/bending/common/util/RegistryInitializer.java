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

package me.moros.bending.common.util;

import java.util.List;
import java.util.function.Function;

import me.moros.bending.api.platform.block.BlockProperties;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.block.BlockTag;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.entity.EntityType;
import me.moros.bending.api.platform.entity.player.GameMode;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.item.ItemTag;
import me.moros.bending.api.platform.particle.Particle;
import me.moros.bending.api.platform.potion.PotionEffect;
import me.moros.bending.api.platform.potion.PotionEffectTag;
import me.moros.bending.api.platform.sound.Sound;
import me.moros.bending.api.registry.Registry;
import me.moros.bending.api.registry.TagBuilder;
import me.moros.bending.api.util.material.EarthMaterials;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.bending.api.util.material.WaterMaterials;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;

public interface RegistryInitializer extends Initializer {
  @Override
  default void init() {
    // Init defaults
    var dummy = List.of(BlockType.AIR, BlockTag.DIRT, EntityType.PLAYER, GameMode.SURVIVAL, Item.AIR, ItemTag.DIRT,
      Particle.FLAME, PotionEffect.INSTANT_HEALTH, Sound.BLOCK_FIRE_AMBIENT);
    dummy.forEach(Keyed::key);

    // Init the simple registries
    initEntityTypeRegistry(EntityType.registry());
    initParticleRegistry(Particle.registry());
    initSoundRegistry(Sound.registry());

    // Init potions and tags
    initPotionEffectRegistry(PotionEffect.registry());
    initPotionEffectTagRegistry(PotionEffect.registry(), PotionEffectTag::builder);

    // Init items and tags
    initItemRegistry(Item.registry());
    initItemTagRegistry(Item.registry(), ItemTag::builder);

    // Init block types and tags
    var clazz = ReflectionUtil.getClassOrThrow("me.moros.bending.api.platform.block.BlockTypeImpl");
    Registry<Key, BlockProperties> propertyRegistry = ReflectionUtil.getStaticFieldOrThrow(clazz, "PROPERTY_REGISTRY");
    Registry<Key, BlockState> stateRegistry = ReflectionUtil.getStaticFieldOrThrow(clazz, "STATE_REGISTRY");
    Registry<Key, Item> itemRegistry = ReflectionUtil.getStaticFieldOrThrow(clazz, "ITEM_REGISTRY");
    initBlockTypeRegistry(BlockType.registry(), propertyRegistry, stateRegistry, itemRegistry);
    initBlockTypeTagRegistry(BlockType.registry(), BlockTag::builder);

    MaterialUtil.init();
    EarthMaterials.init();
    WaterMaterials.init();
  }

  void initBlockTypeRegistry(Registry<Key, BlockType> registry, Registry<Key, BlockProperties> propertyRegistry,
                             Registry<Key, BlockState> stateRegistry, Registry<Key, Item> itemRegistry);

  void initBlockTypeTagRegistry(Registry<Key, BlockType> registry, Function<Key, TagBuilder<BlockType, BlockTag>> builder);

  void initEntityTypeRegistry(Registry<Key, EntityType> registry);

  void initItemRegistry(Registry<Key, Item> registry);

  void initItemTagRegistry(Registry<Key, Item> registry, Function<Key, TagBuilder<Item, ItemTag>> builder);

  void initParticleRegistry(Registry<Key, Particle> registry);

  void initPotionEffectRegistry(Registry<Key, PotionEffect> registry);

  void initPotionEffectTagRegistry(Registry<Key, PotionEffect> registry, Function<Key, TagBuilder<PotionEffect, PotionEffectTag>> builder);

  void initSoundRegistry(Registry<Key, Sound> registry);
}
