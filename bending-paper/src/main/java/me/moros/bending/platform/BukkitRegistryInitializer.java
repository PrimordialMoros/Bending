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

import java.util.function.Function;
import java.util.function.Predicate;

import me.moros.bending.model.registry.Registry;
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
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;

final class BukkitRegistryInitializer implements RegistryInitializer {
  @Override
  public void initBlockTypeRegistry(Registry<Key, BlockType> registry) {
    new BlockInitializer().init();
  }

  @Override
  public void initBlockTypeTagRegistry(Registry<Key, BlockType> registry, Function<Key, TagBuilder<BlockType, BlockTag>> builder) {
    initTag(registry, builder, Bukkit.getTags(Tag.REGISTRY_BLOCKS, Material.class), Material::isBlock);
  }

  @Override
  public void initEntityTypeRegistry(Registry<Key, EntityType> registry) {
    init(registry, org.bukkit.Registry.ENTITY_TYPE);
  }

  @Override
  public void initItemRegistry(Registry<Key, Item> registry) {
    init(registry, org.bukkit.Registry.MATERIAL);
  }

  @Override
  public void initItemTagRegistry(Registry<Key, Item> registry, Function<Key, TagBuilder<Item, ItemTag>> builder) {
    initTag(registry, builder, Bukkit.getTags(Tag.REGISTRY_ITEMS, Material.class), Material::isItem);
  }

  @Override
  public void initParticleRegistry(Registry<Key, Particle> registry) {
    // No registry in bukkit
  }

  @Override
  public void initPotionEffectRegistry(Registry<Key, PotionEffect> registry) {
    init(registry, org.bukkit.Registry.POTION_EFFECT_TYPE);
  }

  @Override
  public void initSoundRegistry(Registry<Key, Sound> registry) {
    init(registry, org.bukkit.Registry.SOUNDS);
  }

  private <T extends org.bukkit.Keyed> void init(Registry<Key, ?> registry, Iterable<T> bukkitRegistry) {
    for (var keyed : bukkitRegistry) {
      registry.get(PlatformAdapter.fromNsk(keyed.getKey()));
    }
  }

  private <V extends Keyed, T extends me.moros.bending.model.registry.Tag<V>, S extends org.bukkit.Keyed> void initTag(
    Registry<Key, V> registry, Function<Key, TagBuilder<V, T>> builder, Iterable<Tag<S>> tags, Predicate<S> materialPredicate
  ) {
    for (var bukkitTag : tags) {
      var data = bukkitTag.getValues().stream().filter(materialPredicate)
        .map(mat -> registry.get(PlatformAdapter.fromNsk(mat.getKey()))).toList();
      if (!data.isEmpty()) {
        var tagKey = PlatformAdapter.fromNsk(bukkitTag.getKey());
        registry.getTagOrCreate(tagKey, k -> builder.apply(k).add(data).build());
      }
    }
  }
}
