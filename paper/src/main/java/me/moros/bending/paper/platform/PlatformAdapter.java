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

package me.moros.bending.paper.platform;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.damage.DamageCause;
import me.moros.bending.api.platform.entity.DelegateEntity;
import me.moros.bending.api.platform.entity.DelegateLivingEntity;
import me.moros.bending.api.platform.entity.DelegatePlayer;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.platform.potion.Potion;
import me.moros.bending.api.platform.potion.PotionEffect;
import me.moros.bending.api.platform.potion.PotionEffectTag;
import me.moros.bending.api.platform.world.World;
import me.moros.bending.api.util.data.DataKey;
import me.moros.bending.paper.platform.block.BukkitBlockState;
import me.moros.bending.paper.platform.entity.BukkitEntity;
import me.moros.bending.paper.platform.entity.BukkitLivingEntity;
import me.moros.bending.paper.platform.entity.BukkitPlayer;
import me.moros.bending.paper.platform.item.BukkitItem;
import me.moros.bending.paper.platform.world.BukkitWorld;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class PlatformAdapter {
  private PlatformAdapter() {
  }

  public static final Map<Class<?>, PersistentDataType<?, ?>> PERSISTENT_DATA_TYPE_MAP;

  static {
    PERSISTENT_DATA_TYPE_MAP = Map.ofEntries(
      entry(PersistentDataType.SHORT), entry(PersistentDataType.FLOAT), entry(PersistentDataType.DOUBLE),
      entry(PersistentDataType.BOOLEAN), entry(PersistentDataType.STRING),
      entry(PersistentDataType.BYTE), entry(PersistentDataType.BYTE_ARRAY),
      entry(PersistentDataType.INTEGER), entry(PersistentDataType.INTEGER_ARRAY),
      entry(PersistentDataType.LONG), entry(PersistentDataType.LONG_ARRAY),
      entry(PersistentDataType.TAG_CONTAINER), entry(PersistentDataType.TAG_CONTAINER_ARRAY)
    );
  }

  private static Entry<Class<?>, PersistentDataType<?, ?>> entry(PersistentDataType<?, ?> type) {
    return Map.entry(type.getComplexType(), type);
  }

  public static PotionEffectType toBukkitPotion(PotionEffect effect) {
    return Objects.requireNonNull(Registry.POTION_EFFECT_TYPE.get(nsk(effect.key())));
  }

  public static org.bukkit.potion.PotionEffect toBukkitPotion(Potion p) {
    return new org.bukkit.potion.PotionEffect(toBukkitPotion(p.effect()), p.duration(), p.amplifier(), p.ambient(), p.particles(), p.icon());
  }

  public static Potion fromBukkitPotion(org.bukkit.potion.PotionEffect p) {
    var effect = PotionEffect.registry().getOrThrow(p.getType().key());
    return Potion.builder(effect).duration(p.getDuration()).amplifier(p.getAmplifier())
      .ambient(p.isAmbient()).particles(p.hasParticles()).icon(p.hasIcon()).build();
  }

  public static PotionEffectTag potionCategory(PotionEffectType type) {
    return switch (type.getEffectCategory()) {
      case BENEFICIAL -> PotionEffectTag.BENEFICIAL;
      case NEUTRAL -> PotionEffectTag.NEUTRAL;
      case HARMFUL -> PotionEffectTag.HARMFUL;
    };
  }

  public static Item fromBukkitItem(Material material) {
    var item = Item.registry().get(material.key());
    if (item == null || !material.isItem()) {
      throw new IllegalStateException(material.name() + " is not a valid item!");
    }
    return item;
  }

  public static BlockType fromBukkitBlock(Material material) {
    var blockType = BlockType.registry().get(material.key());
    if (blockType == null || !material.isBlock()) {
      throw new IllegalStateException(material.name() + " is not a valid block type!");
    }
    return blockType;
  }

  public static Material toBukkitItemMaterial(Item item) {
    var mat = Registry.MATERIAL.get(nsk(item.key()));
    if (mat == null || !mat.isItem()) {
      throw new IllegalStateException(item.key() + " is not a valid item!");
    }
    return mat;
  }

  public static ItemStack toBukkitItem(Item item) {
    return new ItemStack(toBukkitItemMaterial(item));
  }

  public static ItemStack toBukkitItem(ItemSnapshot item) {
    var stack = ((BukkitItem) item).copy();
    stack.setAmount(item.amount());
    return stack;
  }

  public static ItemSnapshot fromBukkitItem(@Nullable ItemStack itemStack) {
    return itemStack == null ? ItemSnapshot.AIR.get() : new BukkitItem(itemStack);
  }

  public static NamespacedKey nsk(Key key) {
    return key instanceof NamespacedKey nsk ? nsk : new NamespacedKey(key.namespace(), key.value());
  }

  @SuppressWarnings("unchecked")
  public static <T> @Nullable PersistentDataType<?, T> dataType(DataKey<T> key) {
    return (PersistentDataType<?, T>) PERSISTENT_DATA_TYPE_MAP.get(key.type());
  }

  public static World fromBukkitWorld(org.bukkit.World world) {
    return new BukkitWorld(world);
  }

  public static org.bukkit.World toBukkitWorld(World world) {
    return ((BukkitWorld) world).handle();
  }

  public static Block fromBukkitBlock(org.bukkit.block.Block block) {
    return fromBukkitWorld(block.getWorld()).blockAt(block.getX(), block.getY(), block.getZ());
  }

  public static me.moros.bending.api.platform.entity.Entity fromBukkitEntity(Entity entity) {
    if (entity instanceof Player player) {
      return fromBukkitEntity(player);
    } else if (entity instanceof LivingEntity living) {
      return fromBukkitEntity(living);
    } else {
      return new BukkitEntity(entity);
    }
  }

  public static me.moros.bending.api.platform.entity.LivingEntity fromBukkitEntity(LivingEntity entity) {
    if (entity instanceof Player player) {
      return fromBukkitEntity(player);
    } else {
      return new BukkitLivingEntity(entity);
    }
  }

  public static me.moros.bending.api.platform.entity.player.Player fromBukkitEntity(Player entity) {
    return new BukkitPlayer(entity);
  }

  public static Entity toBukkitEntity(me.moros.bending.api.platform.entity.Entity entity) {
    if (entity instanceof DelegateEntity delegate) {
      return toBukkitEntity(delegate.entity());
    }
    return ((BukkitEntity) entity).handle();
  }

  public static LivingEntity toBukkitEntity(me.moros.bending.api.platform.entity.LivingEntity entity) {
    if (entity instanceof DelegateLivingEntity delegate) {
      return toBukkitEntity(delegate.entity());
    }
    return ((BukkitLivingEntity) entity).handle();
  }

  public static Player toBukkitEntity(me.moros.bending.api.platform.entity.player.Player player) {
    if (player instanceof DelegatePlayer delegate) {
      return toBukkitEntity(delegate.entity());
    }
    return ((BukkitPlayer) player).handle();
  }

  public static DamageCause fromBukkitCause(EntityDamageEvent.DamageCause cause) {
    return switch (cause) {
      case FIRE, FIRE_TICK -> DamageCause.FIRE;
      case FALL -> DamageCause.FALL;
      case FLY_INTO_WALL -> DamageCause.KINETIC;
      case SUFFOCATION -> DamageCause.SUFFOCATION;
      case ENTITY_EXPLOSION, BLOCK_EXPLOSION -> DamageCause.EXPLOSION;
      default -> DamageCause.CUSTOM;
    };
  }

  public static BlockState fromBukkitData(BlockData data) {
    return new BukkitBlockState(data);
  }

  public static BlockData toBukkitData(BlockState state) {
    return ((BukkitBlockState) state).handle();
  }
}
