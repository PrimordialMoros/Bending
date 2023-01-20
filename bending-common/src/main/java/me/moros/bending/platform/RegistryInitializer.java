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

package me.moros.bending.platform;

import java.util.List;
import java.util.function.Function;

import me.moros.bending.model.registry.Registry;
import me.moros.bending.model.registry.TagBuilder;
import me.moros.bending.platform.block.BlockTag;
import me.moros.bending.platform.block.BlockType;
import me.moros.bending.platform.entity.EntityType;
import me.moros.bending.platform.entity.player.GameMode;
import me.moros.bending.platform.item.Item;
import me.moros.bending.platform.item.ItemTag;
import me.moros.bending.platform.particle.Particle;
import me.moros.bending.platform.potion.PotionEffect;
import me.moros.bending.platform.sound.Sound;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
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
    initPotionEffectRegistry(PotionEffect.registry());
    initSoundRegistry(Sound.registry());

    // Init items and tags
    initItemRegistry(Item.registry());
    initItemTagRegistry(Item.registry(), ItemTag::builder);

    // Init block types and tags
    initBlockTypeRegistry(BlockType.registry()); // Requires items, sounds for various properties
    initBlockTypeTagRegistry(BlockType.registry(), BlockTag::builder);

    MaterialUtil.init();
    EarthMaterials.init();
    WaterMaterials.init();
  }

  void initBlockTypeRegistry(Registry<Key, BlockType> registry);

  void initBlockTypeTagRegistry(Registry<Key, BlockType> registry, Function<Key, TagBuilder<BlockType, BlockTag>> builder);

  void initEntityTypeRegistry(Registry<Key, EntityType> registry);

  void initItemRegistry(Registry<Key, Item> registry);

  void initItemTagRegistry(Registry<Key, Item> registry, Function<Key, TagBuilder<Item, ItemTag>> builder);

  void initParticleRegistry(Registry<Key, Particle> registry);

  void initPotionEffectRegistry(Registry<Key, PotionEffect> registry);

  void initSoundRegistry(Registry<Key, Sound> registry);
}
