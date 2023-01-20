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
import org.spongepowered.api.Sponge;
import org.spongepowered.api.registry.DefaultedRegistryType;
import org.spongepowered.api.registry.DefaultedRegistryValue;
import org.spongepowered.api.registry.RegistryType;
import org.spongepowered.api.registry.RegistryTypes;

final class SpongeRegistryInitializer implements RegistryInitializer {
  @Override
  public void initBlockTypeRegistry(Registry<Key, BlockType> registry) {
    new BlockInitializer().init();
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
}
