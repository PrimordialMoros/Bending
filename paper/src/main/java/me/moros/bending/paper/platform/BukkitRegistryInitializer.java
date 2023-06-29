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

package me.moros.bending.paper.platform;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import me.moros.bending.api.platform.block.BlockProperties;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.block.BlockTag;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.entity.EntityType;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.item.ItemTag;
import me.moros.bending.api.platform.particle.Particle;
import me.moros.bending.api.platform.potion.PotionEffect;
import me.moros.bending.api.platform.potion.PotionEffectTag;
import me.moros.bending.api.platform.sound.Sound;
import me.moros.bending.api.platform.sound.SoundGroup;
import me.moros.bending.api.registry.Registry;
import me.moros.bending.api.registry.TagBuilder;
import me.moros.bending.common.util.RegistryInitializer;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.data.BlockData;

public final class BukkitRegistryInitializer implements RegistryInitializer {
  @Override
  public void initBlockTypeRegistry(Registry<Key, BlockType> registry, Registry<Key, BlockProperties> propertyRegistry,
                                    Registry<Key, BlockState> stateRegistry, Registry<Key, Item> itemRegistry) {
    for (Material mat : org.bukkit.Registry.MATERIAL) {
      if (mat.isBlock()) {
        var key = mat.key();
        var type = registry.getOrThrow(key);
        var data = mat.createBlockData();
        stateRegistry.register(PlatformAdapter.fromBukkitData(data));
        propertyRegistry.register(mapProperties(type, data));
        var item = Item.registry().get(key);
        if (item != null) {
          itemRegistry.register(item);
        }
      }
    }
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
  public void initPotionEffectTagRegistry(Registry<Key, PotionEffect> registry, Function<Key, TagBuilder<PotionEffect, PotionEffectTag>> builder) {
    var map = StreamSupport.stream(org.bukkit.Registry.POTION_EFFECT_TYPE.spliterator(), false)
      .collect(Collectors.groupingBy(PlatformAdapter::potionCategory));
    for (var entry : map.entrySet()) {
      var data = entry.getValue().stream().map(p -> registry.get(p.key())).toList();
      registry.getTagOrCreate(entry.getKey().key(), k -> builder.apply(k).add(data).build());
    }
  }

  @Override
  public void initSoundRegistry(Registry<Key, Sound> registry) {
    init(registry, org.bukkit.Registry.SOUNDS);
  }

  private <T extends org.bukkit.Keyed> void init(Registry<Key, ?> registry, Iterable<T> bukkitRegistry) {
    for (var keyed : bukkitRegistry) {
      registry.get(keyed.key());
    }
  }

  private <V extends Keyed, T extends me.moros.bending.api.registry.Tag<V>, S extends org.bukkit.Keyed> void initTag(
    Registry<Key, V> registry, Function<Key, TagBuilder<V, T>> builder, Iterable<Tag<S>> tags, Predicate<S> materialPredicate
  ) {
    for (var bukkitTag : tags) {
      var data = bukkitTag.getValues().stream().filter(materialPredicate)
        .map(mat -> registry.get(mat.key())).toList();
      if (!data.isEmpty()) {
        registry.getTagOrCreate(bukkitTag.key(), k -> builder.apply(k).add(data).build());
      }
    }
  }

  private BlockProperties mapProperties(BlockType type, BlockData data) {
    var mat = data.getMaterial();
    return BlockProperties.builder(type, mat.translationKey())
      .isAir(mat.isAir())
      .isSolid(mat.isSolid())
      .isLiquid(type == BlockType.WATER || type == BlockType.LAVA || type == BlockType.BUBBLE_COLUMN)
      .isFlammable(mat.isFlammable())
      .hasGravity(mat.hasGravity())
      .isCollidable(mat.isCollidable())
      .hardness(mat.getHardness())
      .soundGroup(mapSoundGroup(data.getSoundGroup())).build();
  }

  private SoundGroup mapSoundGroup(org.bukkit.SoundGroup group) {
    return new SoundGroup(mapSound(group.getBreakSound()),
      mapSound(group.getStepSound()),
      mapSound(group.getPlaceSound()),
      mapSound(group.getHitSound()),
      mapSound(group.getFallSound())
    );
  }

  private Sound mapSound(org.bukkit.Sound sound) {
    return Sound.registry().getOrThrow(sound.key());
  }
}
