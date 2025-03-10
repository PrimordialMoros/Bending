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

package me.moros.bending.sponge.platform;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.damage.DamageCause;
import me.moros.bending.api.platform.entity.DelegateEntity;
import me.moros.bending.api.platform.entity.DelegateLivingEntity;
import me.moros.bending.api.platform.entity.DelegatePlayer;
import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.platform.potion.Potion;
import me.moros.bending.api.platform.potion.PotionEffect;
import me.moros.bending.api.platform.potion.PotionEffectTag;
import me.moros.bending.api.platform.world.World;
import me.moros.bending.api.util.data.DataKey;
import me.moros.bending.sponge.platform.block.SpongeBlockState;
import me.moros.bending.sponge.platform.entity.SpongeEntity;
import me.moros.bending.sponge.platform.entity.SpongeLivingEntity;
import me.moros.bending.sponge.platform.entity.SpongePlayer;
import me.moros.bending.sponge.platform.item.SpongeItem;
import me.moros.bending.sponge.platform.world.SpongeWorld;
import net.kyori.adventure.key.Key;
import net.minecraft.world.effect.MobEffect;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.cause.entity.damage.DamageTypes;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.server.ServerWorld;

public final class PlatformAdapter {
  private PlatformAdapter() {
  }

  public static org.spongepowered.api.effect.potion.PotionEffectType toSpongePotion(PotionEffect effect) {
    return RegistryTypes.POTION_EFFECT_TYPE.get().value(rsk(effect.key()));
  }

  public static org.spongepowered.api.effect.potion.PotionEffect toSpongePotion(Potion p) {
    return org.spongepowered.api.effect.potion.PotionEffect.builder().potionType(toSpongePotion(p.effect()))
      .duration(Ticks.of(p.duration())).amplifier(p.amplifier()).ambient(p.ambient())
      .showParticles(p.particles()).showIcon(p.icon()).build();
  }

  public static Potion fromSpongePotion(org.spongepowered.api.effect.potion.PotionEffect p) {
    var effect = PotionEffect.registry().getOrThrow(fromRsk(p.type().key(RegistryTypes.POTION_EFFECT_TYPE)));
    return Potion.builder(effect).duration((int) p.duration().ticks()).amplifier(p.amplifier())
      .ambient(p.isAmbient()).particles(p.showsParticles()).icon(p.showsIcon()).build();
  }

  public static PotionEffectTag potionCategory(org.spongepowered.api.effect.potion.PotionEffectType type) {
    return switch (((MobEffect) type).getCategory()) {
      case BENEFICIAL -> PotionEffectTag.BENEFICIAL;
      case NEUTRAL -> PotionEffectTag.NEUTRAL;
      case HARMFUL -> PotionEffectTag.HARMFUL;
    };
  }

  public static Item fromSpongeItem(ItemType type) {
    return Item.registry().getOrThrow(fromRsk(type.key(RegistryTypes.ITEM_TYPE)));
  }

  public static BlockType fromSpongeBlock(org.spongepowered.api.block.BlockType type) {
    return BlockType.registry().getOrThrow(fromRsk(type.key(RegistryTypes.BLOCK_TYPE)));
  }

  public static ItemType toSpongeItemType(Item item) {
    return RegistryTypes.ITEM_TYPE.get().value(rsk(item.key()));
  }

  public static ItemStack toSpongeItem(Item item) {
    return ItemStack.of(toSpongeItemType(item));
  }

  public static ItemStack toSpongeItem(ItemSnapshot item) {
    return ((SpongeItem) item).handle().copy();
  }

  public static ItemStackSnapshot toSpongeItemSnapshot(ItemSnapshot item) {
    return ((SpongeItem) item).handle().asImmutable();
  }

  public static ItemSnapshot fromSpongeItem(ItemStack itemStack) {
    return new SpongeItem(itemStack);
  }

  // Sponge data keys dont have proper equals and hashcode checks so we need to cache and use the same key instances
  private static final Map<DataKey<?>, org.spongepowered.api.data.Key<?>> keyCache = new ConcurrentHashMap<>();

  @SuppressWarnings("unchecked")
  public static <T> org.spongepowered.api.data.Key<Value<T>> dataKey(DataKey<T> key) {
    return (org.spongepowered.api.data.Key<Value<T>>) keyCache.computeIfAbsent(key, k -> org.spongepowered.api.data.Key.from(rsk(k), k.type()));
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

  public static me.moros.bending.api.platform.entity.Entity fromSpongeEntity(Entity entity) {
    if (entity instanceof ServerPlayer player) {
      return fromSpongeEntity(player);
    } else if (entity instanceof Living living) {
      return PlatformAdapter.fromSpongeEntity(living);
    } else {
      return new SpongeEntity(entity);
    }
  }

  public static LivingEntity fromSpongeEntity(Living entity) {
    if (entity instanceof ServerPlayer player) {
      return fromSpongeEntity(player);
    } else {
      return new SpongeLivingEntity(entity);
    }
  }

  public static Player fromSpongeEntity(ServerPlayer entity) {
    return new SpongePlayer(entity);
  }

  public static Entity toSpongeEntity(me.moros.bending.api.platform.entity.Entity entity) {
    if (entity instanceof DelegateEntity delegate) {
      return toSpongeEntity(delegate.entity());
    }
    return ((SpongeEntity) entity).handle();
  }

  public static Living toSpongeEntity(LivingEntity entity) {
    if (entity instanceof DelegateLivingEntity delegate) {
      return toSpongeEntity(delegate.entity());
    }
    return ((SpongeLivingEntity) entity).handle();
  }

  public static ServerPlayer toSpongeEntity(Player player) {
    if (player instanceof DelegatePlayer delegate) {
      return toSpongeEntity(delegate.entity());
    }
    return ((SpongePlayer) player).handle();
  }

  public static DamageCause fromSpongeCause(org.spongepowered.api.event.cause.entity.damage.source.DamageSource source) {
    if (source.isExplosive()) {
      return DamageCause.EXPLOSION;
    } else if (source.isFire() && !(source instanceof AbilityDamageSource)) {
      return DamageCause.FIRE;
    }
    var type = source.type();
    if (type == DamageTypes.FALL.get()) {
      return DamageCause.FALL;
    } else if (type == DamageTypes.FLY_INTO_WALL.get()) {
      return DamageCause.KINETIC;
    } else if (type == DamageTypes.IN_WALL.get()) {
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
