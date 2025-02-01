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

package me.moros.bending.fabric.platform;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import me.moros.bending.api.registry.Tag;
import me.moros.bending.api.registry.TagBuilder;
import me.moros.bending.common.util.RegistryInitializer;
import me.moros.bending.fabric.mixin.accessor.BlockBehaviourAccess;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SoundType;

final class FabricRegistryInitializer implements RegistryInitializer {
  @Override
  public void initBlockTypeRegistry(Registry<Key, BlockType> registry, Registry<Key, BlockProperties> propertyRegistry,
                                    Registry<Key, BlockState> stateRegistry, Registry<Key, Item> itemRegistry) {
    for (var mat : BuiltInRegistries.BLOCK) {
      var key = BuiltInRegistries.BLOCK.getKey(mat);
      var type = registry.getOrThrow(key);
      var data = mat.defaultBlockState();
      stateRegistry.register(PlatformAdapter.fromFabricData(data));
      propertyRegistry.register(mapProperties(type, data));
      if (mat.asItem() instanceof BlockItem item) {
        itemRegistry.register(PlatformAdapter.fromFabricItem(item));
      }
    }
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
  public void initPotionEffectTagRegistry(Registry<Key, PotionEffect> registry, Function<Key, TagBuilder<PotionEffect, PotionEffectTag>> builder) {
    var vanillaRegistry = BuiltInRegistries.MOB_EFFECT;
    var map = vanillaRegistry.stream().collect(Collectors.groupingBy(PlatformAdapter::potionCategory));
    for (var entry : map.entrySet()) {
      var data = entry.getValue().stream().map(p -> registry.get(vanillaRegistry.getKey(p))).toList();
      registry.getTagOrCreate(entry.getKey().key(), k -> builder.apply(k).add(data).build());
    }
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
      var data = entry.stream().map(Holder::value).map(defaultedRegistry::getKey).map(registry::get)
        .filter(Objects::nonNull).toList();
      if (!data.isEmpty()) {
        var tagKey = entry.key().location();
        registry.getTagOrCreate(tagKey, k -> builder.apply(k).add(data).build());
      }
    }
  }

  private BlockProperties mapProperties(BlockType type, net.minecraft.world.level.block.state.BlockState data) {
    var block = data.getBlock();
    return BlockProperties.builder(type, block.getDescriptionId())
      .isAir(data.isAir())
      .isSolid(data.isSolid())
      .isLiquid(data.liquid())
      .isFlammable(FlammableBlockRegistry.getDefaultInstance().get(block).getBurnChance() > 0)
      .hasGravity(data.getBlock() instanceof FallingBlock)
      .isCollidable(((BlockBehaviourAccess) block).bending$hasCollision())
      .hardness(block.defaultDestroyTime())
      .soundGroup(mapSoundGroup(data.getSoundType())).build();
  }

  private SoundGroup mapSoundGroup(SoundType type) {
    return new SoundGroup(mapSound(type.getBreakSound()),
      mapSound(type.getStepSound()),
      mapSound(type.getPlaceSound()),
      mapSound(type.getHitSound()),
      mapSound(type.getFallSound())
    );
  }

  private Sound mapSound(SoundEvent sound) {
    return Sound.registry().getOrThrow(sound.location());
  }
}
