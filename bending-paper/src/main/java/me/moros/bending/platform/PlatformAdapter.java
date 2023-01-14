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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import me.moros.bending.Bending;
import me.moros.bending.model.data.DataKey;
import me.moros.bending.platform.block.Block;
import me.moros.bending.platform.block.BlockState;
import me.moros.bending.platform.block.BlockTag;
import me.moros.bending.platform.block.BlockType;
import me.moros.bending.platform.block.BukkitBlockState;
import me.moros.bending.platform.damage.DamageCause;
import me.moros.bending.platform.entity.BukkitEntity;
import me.moros.bending.platform.entity.BukkitLivingEntity;
import me.moros.bending.platform.entity.BukkitPlayer;
import me.moros.bending.platform.entity.EntityType;
import me.moros.bending.platform.item.BukkitItem;
import me.moros.bending.platform.item.Item;
import me.moros.bending.platform.item.ItemSnapshot;
import me.moros.bending.platform.item.ItemTag;
import me.moros.bending.platform.particle.Particle;
import me.moros.bending.platform.potion.Potion;
import me.moros.bending.platform.potion.PotionEffect;
import me.moros.bending.platform.sound.Sound;
import me.moros.bending.platform.world.BukkitWorld;
import me.moros.bending.platform.world.World;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.kyori.adventure.util.Index;
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
import org.checkerframework.checker.nullness.qual.Nullable;

public final class PlatformAdapter {
  private PlatformAdapter() {
  }

  public static final Index<org.bukkit.entity.EntityType, EntityType> ENTITY_TYPE_INDEX;
  public static final Index<org.bukkit.potion.PotionEffectType, PotionEffect> POTION_EFFECT_INDEX;
  public static final Index<Item, Material> ITEM_MATERIAL_INDEX;
  public static final Index<BlockType, Material> BLOCK_MATERIAL_INDEX;
  public static final Map<Class<?>, PersistentDataType<?, ?>> PERSISTENT_DATA_TYPE_MAP;

  static {
    var dummy = List.of(BlockType.AIR, BlockTag.DIRT, EntityType.PLAYER, Item.AIR, ItemTag.DIRT,
      Particle.FLAME, PotionEffect.INSTANT_HEALTH, Sound.BLOCK_FIRE_AMBIENT);
    Bending.plugin().logger().debug("Init: " + dummy.size()); // Required for proper class loading and initialization

    ENTITY_TYPE_INDEX = Index.create(t -> Registry.ENTITY_TYPE.get(nsk(t.key())), EntityType.registry().stream().toList());
    POTION_EFFECT_INDEX = Index.create(t -> Registry.POTION_EFFECT_TYPE.get(nsk(t.key())), PotionEffect.registry().stream().toList());
    Map<Material, Item> itemMap = new EnumMap<>(Material.class);
    Map<Material, BlockType> blockTypeMap = new EnumMap<>(Material.class);
    var itemRegistry = Item.registry();
    var blockRegistry = BlockType.registry();
    for (Material m : Registry.MATERIAL) {
      if (!m.isLegacy()) {
        if (m.isItem()) {
          tryAdd(m, itemRegistry, itemMap);
        }
        if (m.isBlock()) {
          tryAdd(m, blockRegistry, blockTypeMap);
        }
      }
    }
    ITEM_MATERIAL_INDEX = Index.create(Material.class, itemMap::get, itemMap.keySet().toArray(Material[]::new));
    BLOCK_MATERIAL_INDEX = Index.create(Material.class, blockTypeMap::get, blockTypeMap.keySet().toArray(Material[]::new));
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

  private static <T extends Keyed> void tryAdd(Material m, me.moros.bending.model.registry.Registry<Key, T> registry, Map<Material, T> map) {
    T value = registry.get(fromNsk(m.getKey()));
    if (value != null) {
      map.put(m, value);
    }
  }

  public static org.bukkit.potion.@Nullable PotionEffect toBukkitPotion(Potion p) {
    var bukkitType = POTION_EFFECT_INDEX.key(p.effect());
    return bukkitType == null ? null : new org.bukkit.potion.PotionEffect(bukkitType, p.duration(), p.amplifier(), p.ambient(), p.particles(), p.icon());
  }

  public static @Nullable Potion fromBukkitPotion(org.bukkit.potion.@Nullable PotionEffect p) {
    if (p == null) {
      return null;
    }
    var effect = POTION_EFFECT_INDEX.value(p.getType());
    return effect == null ? null : Potion.builder(effect).duration(p.getDuration()).amplifier(p.getAmplifier())
      .ambient(p.isAmbient()).particles(p.hasParticles()).icon(p.hasIcon()).build();
  }

  public static ItemStack toBukkitItem(ItemSnapshot item) {
    ItemStack stack = new ItemStack(ITEM_MATERIAL_INDEX.valueOrThrow(item.type()), item.amount());
    stack.setItemMeta(((BukkitItem) item).handle());
    return stack;
  }

  public static ItemSnapshot fromBukkitItem(@Nullable ItemStack itemStack, ItemSnapshot def) {
    return itemStack == null ? def : new BukkitItem(itemStack);
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
