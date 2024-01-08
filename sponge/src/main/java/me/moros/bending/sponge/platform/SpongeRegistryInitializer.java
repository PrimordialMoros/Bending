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

package me.moros.bending.sponge.platform;

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
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.MatterTypes;
import org.spongepowered.api.registry.DefaultedRegistryType;
import org.spongepowered.api.registry.DefaultedRegistryValue;
import org.spongepowered.api.registry.RegistryType;
import org.spongepowered.api.registry.RegistryTypes;

final class SpongeRegistryInitializer implements RegistryInitializer {
  @Override
  public void initBlockTypeRegistry(Registry<Key, BlockType> registry, Registry<Key, BlockProperties> propertyRegistry,
                                    Registry<Key, BlockState> stateRegistry, Registry<Key, Item> itemRegistry) {
    var blockTypes = Sponge.game().registry(RegistryTypes.BLOCK_TYPE).stream().toList();
    for (var mat : blockTypes) {
      var key = PlatformAdapter.fromRsk(mat.key(RegistryTypes.BLOCK_TYPE));
      var type = registry.getOrThrow(key);
      var data = mat.defaultState();
      stateRegistry.register(PlatformAdapter.fromSpongeData(data));
      propertyRegistry.register(mapProperties(type, data));
      mat.item().map(PlatformAdapter::fromSpongeItem).ifPresent(itemRegistry::register);
    }
  }

  @Override
  public void initBlockTypeTagRegistry(Registry<Key, BlockType> registry, Function<Key, TagBuilder<BlockType, BlockTag>> builder) {
    initTag(registry, builder, RegistryTypes.BLOCK_TYPE);
  }

  @Override
  public void initEntityTypeRegistry(Registry<Key, EntityType> registry) {
    init(registry, RegistryTypes.ENTITY_TYPE);
  }

  @Override
  public void initItemRegistry(Registry<Key, Item> registry) {
    init(Item.registry(), RegistryTypes.ITEM_TYPE);
  }

  @Override
  public void initItemTagRegistry(Registry<Key, Item> registry, Function<Key, TagBuilder<Item, ItemTag>> builder) {
    initTag(registry, builder, RegistryTypes.ITEM_TYPE);
  }

  @Override
  public void initParticleRegistry(Registry<Key, Particle> registry) {
    init(registry, RegistryTypes.PARTICLE_TYPE);
  }

  @Override
  public void initPotionEffectRegistry(Registry<Key, PotionEffect> registry) {
    init(registry, RegistryTypes.POTION_EFFECT_TYPE);
  }

  @Override
  public void initPotionEffectTagRegistry(Registry<Key, PotionEffect> registry, Function<Key, TagBuilder<PotionEffect, PotionEffectTag>> builder) {
    var spongeRegistry = Sponge.game().registry(RegistryTypes.POTION_EFFECT_TYPE);
    var map = spongeRegistry.stream().collect(Collectors.groupingBy(PlatformAdapter::potionCategory));
    for (var entry : map.entrySet()) {
      var data = entry.getValue().stream().map(p -> registry.get(spongeRegistry.valueKey(p))).toList();
      registry.getTagOrCreate(entry.getKey().key(), k -> builder.apply(k).add(data).build());
    }
  }

  @Override
  public void initSoundRegistry(Registry<Key, Sound> registry) {
    init(registry, RegistryTypes.SOUND_TYPE);
  }

  private void init(Registry<Key, ?> registry, RegistryType<?> registryType) {
    Sponge.game().registry(registryType).streamEntries()
      .forEach(keyed -> registry.get(PlatformAdapter.fromRsk(keyed.key())));
  }

  private <V extends Keyed, T extends Tag<V>, S extends DefaultedRegistryValue> void initTag(
    Registry<Key, V> registry, Function<Key, TagBuilder<V, T>> builder, DefaultedRegistryType<S> registryType
  ) {
    var spongeRegistry = Sponge.game().registry(registryType);
    var list = spongeRegistry.tags().toList();
    for (var tag : list) {
      var data = spongeRegistry.taggedValues(tag).stream()
        .map(keyed -> registry.get(PlatformAdapter.fromRsk(keyed.key(registryType)))).filter(Objects::nonNull).toList();
      if (!data.isEmpty()) {
        var tagKey = PlatformAdapter.fromRsk(tag.key());
        registry.getTagOrCreate(tagKey, k -> builder.apply(k).add(data).build());
      }
    }
  }

  private BlockProperties mapProperties(BlockType type, org.spongepowered.api.block.BlockState data) {
    var nms = (net.minecraft.world.level.block.state.BlockState) data;
    // TODO check deprecated
    return BlockProperties.builder(type, nms.getBlock().getDescriptionId())
      .isAir(nms.isAir())
      .isSolid(nms.isSolid())
      .isLiquid(data.getOrElse(Keys.MATTER_TYPE, null) == MatterTypes.LIQUID.get())
      .isFlammable(data.getOrElse(Keys.IS_FLAMMABLE, false))
      .hasGravity(data.getOrElse(Keys.IS_GRAVITY_AFFECTED, false))
      .isCollidable(nms.blocksMotion())
      .hardness(nms.getBlock().defaultDestroyTime())
      .soundGroup(mapSoundGroup(nms.getSoundType())).build();
  }

  private SoundGroup mapSoundGroup(SoundType group) {
    return new SoundGroup(mapSound(group.getBreakSound()),
      mapSound(group.getStepSound()),
      mapSound(group.getPlaceSound()),
      mapSound(group.getHitSound()),
      mapSound(group.getFallSound())
    );
  }

  private Sound mapSound(SoundEvent sound) {
    Object key = sound.getLocation();
    return Sound.registry().getOrThrow(PlatformAdapter.fromRsk((ResourceKey) key)); // Defaulted registry
  }
}
