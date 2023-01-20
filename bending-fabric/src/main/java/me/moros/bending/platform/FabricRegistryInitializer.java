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

import java.util.Objects;
import java.util.function.Function;

import me.moros.bending.model.registry.Registry;
import me.moros.bending.model.registry.Tag;
import me.moros.bending.model.registry.TagBuilder;
import me.moros.bending.platform.block.BlockInitializer;
import me.moros.bending.platform.block.BlockTag;
import me.moros.bending.platform.block.BlockType;
import me.moros.bending.platform.entity.EntityType;
import me.moros.bending.platform.item.Item;
import me.moros.bending.platform.item.ItemTag;
import me.moros.bending.platform.particle.Particle;
import me.moros.bending.platform.potion.PotionEffect;
import me.moros.bending.platform.sound.Sound;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;

final class FabricRegistryInitializer implements RegistryInitializer {
  @Override
  public void initBlockTypeRegistry(Registry<Key, BlockType> registry) {
    new BlockInitializer().init();
  }

  @Override
  public void initBlockTypeTagRegistry(Registry<Key, BlockType> registry, Function<Key, TagBuilder<BlockType, BlockTag>> builder) {
    initTag(registry, BlockTag::builder, BuiltInRegistries.BLOCK);
  }

  @Override
  public void initEntityTypeRegistry(Registry<Key, EntityType> registry) {
    init(registry, BuiltInRegistries.ENTITY_TYPE);
  }

  @Override
  public void initItemRegistry(Registry<Key, Item> registry) {
    init(registry, BuiltInRegistries.ITEM);
  }

  @Override
  public void initItemTagRegistry(Registry<Key, Item> registry, Function<Key, TagBuilder<Item, ItemTag>> builder) {
    initTag(registry, builder, BuiltInRegistries.ITEM);
  }

  @Override
  public void initParticleRegistry(Registry<Key, Particle> registry) {
    init(registry, BuiltInRegistries.PARTICLE_TYPE);
  }

  @Override
  public void initPotionEffectRegistry(Registry<Key, PotionEffect> registry) {
    init(registry, BuiltInRegistries.MOB_EFFECT);
  }

  @Override
  public void initSoundRegistry(Registry<Key, Sound> registry) {
    init(registry, BuiltInRegistries.SOUND_EVENT);
  }

  private void init(Registry<Key, ?> registry, net.minecraft.core.Registry<?> vanillaRegistry) {
    for (var key : vanillaRegistry.keySet()) {
      registry.get(key);
    }
  }

  private <V extends Keyed, T extends Tag<V>, S> void initTag(
    Registry<Key, V> registry, Function<Key, TagBuilder<V, T>> builder, net.minecraft.core.Registry<S> defaultedRegistry
  ) {
    var tags = defaultedRegistry.getTags().toList();
    for (var entry : tags) {
      var data = entry.getSecond().stream().map(Holder::value).map(defaultedRegistry::getKey).map(registry::get)
        .filter(Objects::nonNull).toList();
      if (!data.isEmpty()) {
        var tagKey = entry.getFirst().location();
        registry.getTagOrCreate(tagKey, k -> builder.apply(k).add(data).build());
      }
    }
  }
}
