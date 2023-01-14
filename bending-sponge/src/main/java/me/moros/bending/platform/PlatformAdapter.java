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

import me.moros.bending.model.data.DataKey;
import me.moros.bending.platform.block.Block;
import me.moros.bending.platform.block.BlockState;
import me.moros.bending.platform.block.BlockTag;
import me.moros.bending.platform.block.BlockType;
import me.moros.bending.platform.block.SpongeBlockState;
import me.moros.bending.platform.damage.DamageCause;
import me.moros.bending.platform.entity.EntityType;
import me.moros.bending.platform.entity.SpongeEntity;
import me.moros.bending.platform.entity.SpongeLivingEntity;
import me.moros.bending.platform.entity.SpongePlayer;
import me.moros.bending.platform.item.Item;
import me.moros.bending.platform.item.ItemSnapshot;
import me.moros.bending.platform.item.ItemTag;
import me.moros.bending.platform.item.SpongeItem;
import me.moros.bending.platform.particle.Particle;
import me.moros.bending.platform.potion.Potion;
import me.moros.bending.platform.potion.PotionEffect;
import me.moros.bending.platform.sound.Sound;
import me.moros.bending.platform.world.SpongeWorld;
import me.moros.bending.platform.world.World;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.kyori.adventure.util.Index;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.effect.potion.PotionEffectType;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.cause.entity.damage.DamageType;
import org.spongepowered.api.event.cause.entity.damage.DamageTypes;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.registry.RegistryType;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.server.ServerWorld;

public final class PlatformAdapter {
  private PlatformAdapter() {
  }

  public static final Index<org.spongepowered.api.entity.EntityType<?>, EntityType> ENTITY_TYPE_INDEX;
  public static final Index<PotionEffectType, PotionEffect> POTION_EFFECT_INDEX;
  public static final Index<ItemType, Item> ITEM_MATERIAL_INDEX;
  public static final Index<org.spongepowered.api.block.BlockType, BlockType> BLOCK_MATERIAL_INDEX;

  static {
    var dummy = List.of(BlockType.AIR, BlockTag.DIRT, EntityType.PLAYER, Item.AIR, ItemTag.DIRT,
      Particle.FLAME, PotionEffect.INSTANT_HEALTH, Sound.BLOCK_FIRE_AMBIENT);
    dummy.forEach(Keyed::key); // Required for proper class loading and initialization

    ENTITY_TYPE_INDEX = Index.create(t -> fromSpongeRegistry(RegistryTypes.ENTITY_TYPE, t), EntityType.registry().stream().toList());
    POTION_EFFECT_INDEX = Index.create(t -> fromSpongeRegistry(RegistryTypes.POTION_EFFECT_TYPE, t), PotionEffect.registry().stream().toList());
    ITEM_MATERIAL_INDEX = Index.create(t -> fromSpongeRegistry(RegistryTypes.ITEM_TYPE, t), Item.registry().stream().toList());
    BLOCK_MATERIAL_INDEX = Index.create(t -> fromSpongeRegistry(RegistryTypes.BLOCK_TYPE, t), BlockType.registry().stream().toList());
  }

  private static <T> T fromSpongeRegistry(RegistryType<T> type, Keyed key) {
    return Sponge.game().registry(type).value(rsk(key.key()));
  }

  public static org.spongepowered.api.effect.potion.@Nullable PotionEffect toSpongePotion(Potion p) {
    var spongeType = POTION_EFFECT_INDEX.key(p.effect());
    if (spongeType == null) {
      return null;
    }
    return org.spongepowered.api.effect.potion.PotionEffect.builder().potionType(spongeType)
      .duration(Ticks.of(p.duration())).amplifier(p.amplifier()).ambient(p.ambient())
      .showParticles(p.particles()).showIcon(p.icon()).build();
  }

  public static @Nullable Potion fromSpongePotion(org.spongepowered.api.effect.potion.@Nullable PotionEffect p) {
    if (p == null) {
      return null;
    }
    var effect = POTION_EFFECT_INDEX.value(p.type());
    return effect == null ? null : Potion.builder(effect).duration((int) p.duration().ticks()).amplifier(p.amplifier())
      .ambient(p.isAmbient()).particles(p.showsParticles()).icon(p.showsIcon()).build();
  }

  public static ItemStack toSpongeItem(ItemSnapshot item) {
    return ((SpongeItem) item).handle().copy();
  }

  public static ItemStackSnapshot toSpongeItemSnapshot(ItemSnapshot item) {
    return ((SpongeItem) item).handle().createSnapshot();
  }

  public static ItemSnapshot fromSpongeItem(ItemStack itemStack) {
    return new SpongeItem(itemStack);
  }

  public static <T> org.spongepowered.api.data.Key<Value<T>> dataKey(DataKey<T> key) {
    return org.spongepowered.api.data.Key.from(rsk(key), key.type());
  }

  public static ResourceKey rsk(Key key) {
    return ResourceKey.of(key.namespace(), key.value());
  }

  // To ensure consistent equals/hashcode we need to duplicate keys
  public static Key fromRsk(ResourceKey key) {
    return Key.key(key.namespace(), key.value());
  }

  public static World fromSpongeWorld(ServerWorld world) {
    return new SpongeWorld(world);
  }

  public static ServerWorld toSpongeWorld(World world) {
    return ((SpongeWorld) world).handle();
  }

  public static Block fromSpongeBlock(LocatableBlock block) {
    var loc = block.serverLocation();
    return fromSpongeWorld(loc.world()).blockAt(loc.blockX(), loc.blockY(), loc.blockZ());
  }

  public static me.moros.bending.platform.entity.Entity fromSpongeEntity(Entity entity) {
    if (entity instanceof ServerPlayer player) {
      return fromSpongeEntity(player);
    } else if (entity instanceof Living living) {
      return PlatformAdapter.fromSpongeEntity(living);
    } else {
      return new SpongeEntity(entity);
    }
  }

  public static me.moros.bending.platform.entity.LivingEntity fromSpongeEntity(Living entity) {
    if (entity instanceof ServerPlayer player) {
      return fromSpongeEntity(player);
    } else {
      return new SpongeLivingEntity(entity);
    }
  }

  public static me.moros.bending.platform.entity.player.Player fromSpongeEntity(ServerPlayer entity) {
    return new SpongePlayer(entity);
  }

  public static DamageCause fromSpongeCause(DamageType type) {
    if (type == DamageTypes.FIRE.get()) {
      return DamageCause.FIRE;
    } else if (type == DamageTypes.FALL.get()) {
      return DamageCause.FALL;
    } else if (type == DamageTypes.CONTACT.get()) { // TODO Incorrect mapping
      return DamageCause.KINETIC;
    } else if (type == DamageTypes.SUFFOCATE.get()) {
      return DamageCause.SUFFOCATION;
    } else {
      return DamageCause.CUSTOM;
    }
  }

  public static BlockState fromSpongeData(org.spongepowered.api.block.BlockState data) {
    return new SpongeBlockState(data);
  }

  public static org.spongepowered.api.block.BlockState toSpongeData(BlockState state) {
    return ((SpongeBlockState) state).handle();
  }
}
