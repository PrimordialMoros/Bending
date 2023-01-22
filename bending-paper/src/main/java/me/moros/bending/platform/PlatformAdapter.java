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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import me.moros.bending.model.data.DataKey;
import me.moros.bending.platform.block.Block;
import me.moros.bending.platform.block.BlockState;
import me.moros.bending.platform.block.BlockType;
import me.moros.bending.platform.block.BukkitBlockState;
import me.moros.bending.platform.damage.DamageCause;
import me.moros.bending.platform.entity.BukkitEntity;
import me.moros.bending.platform.entity.BukkitLivingEntity;
import me.moros.bending.platform.entity.BukkitPlayer;
import me.moros.bending.platform.item.BukkitItem;
import me.moros.bending.platform.item.Item;
import me.moros.bending.platform.item.ItemSnapshot;
import me.moros.bending.platform.potion.Potion;
import me.moros.bending.platform.potion.PotionEffect;
import me.moros.bending.platform.world.BukkitWorld;
import me.moros.bending.platform.world.World;
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
      entry(BooleanPDT.INSTANCE), entry(PersistentDataType.STRING),
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
    return Objects.requireNonNull(Registry.POTION_EFFECT_TYPE.get(PlatformAdapter.nsk(effect.key())));
  }

  public static org.bukkit.potion.PotionEffect toBukkitPotion(Potion p) {
    return new org.bukkit.potion.PotionEffect(toBukkitPotion(p.effect()), p.duration(), p.amplifier(), p.ambient(), p.particles(), p.icon());
  }

  public static Potion fromBukkitPotion(org.bukkit.potion.PotionEffect p) {
    var effect = PotionEffect.registry().getOrThrow(fromNsk(p.getType().getKey()));
    return Potion.builder(effect).duration(p.getDuration()).amplifier(p.getAmplifier())
      .ambient(p.isAmbient()).particles(p.hasParticles()).icon(p.hasIcon()).build();
  }

  public static Item fromBukkitItem(Material material) {
    var item = Item.registry().get(fromNsk(material.getKey()));
    if (item == null || !material.isItem()) {
      throw new IllegalStateException(material.name() + " is not a valid item!");
    }
    return item;
  }

  public static BlockType fromBukkitBlock(Material material) {
    var blockType = BlockType.registry().get(fromNsk(material.getKey()));
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
    var stack = ((BukkitItem) item).handle().clone();
    stack.setAmount(item.amount());
    return stack;
  }

  public static ItemSnapshot fromBukkitItem(@Nullable ItemStack itemStack) {
    return itemStack == null ? ItemSnapshot.AIR.get() : new BukkitItem(itemStack);
  }

  public static NamespacedKey nsk(Key key) {
    return new NamespacedKey(key.namespace(), key.value());
  }

  // To ensure consistent equals/hashcode we need to duplicate keys
  public static Key fromNsk(NamespacedKey key) {
    return Key.key(key.namespace(), key.value());
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

  public static me.moros.bending.platform.entity.Entity fromBukkitEntity(Entity entity) {
    if (entity instanceof Player player) {
      return fromBukkitEntity(player);
    } else if (entity instanceof LivingEntity living) {
      return fromBukkitEntity(living);
    } else {
      return new BukkitEntity(entity);
    }
  }

  public static me.moros.bending.platform.entity.LivingEntity fromBukkitEntity(LivingEntity entity) {
    if (entity instanceof Player player) {
      return fromBukkitEntity(player);
    } else {
      return new BukkitLivingEntity(entity);
    }
  }

  public static me.moros.bending.platform.entity.player.Player fromBukkitEntity(Player entity) {
    return new BukkitPlayer(entity);
  }

  public static DamageCause fromBukkitCause(EntityDamageEvent.DamageCause cause) {
    return switch (cause) {
      case FIRE, FIRE_TICK -> DamageCause.FIRE;
      case FALL -> DamageCause.FALL;
      case FLY_INTO_WALL -> DamageCause.KINETIC;
      case SUFFOCATION -> DamageCause.SUFFOCATION;
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
