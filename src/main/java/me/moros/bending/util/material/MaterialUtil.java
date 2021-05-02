/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.util.material;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.destroystokyo.paper.MaterialSetTag;
import com.destroystokyo.paper.MaterialTags;
import me.moros.bending.Bending;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.inventory.InventoryHolder;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class MaterialUtil {
  public static final Map<Material, Material> COOKABLE = new HashMap<>();
  public static final Map<Material, Material> ORES = new HashMap<>();
  public static final MaterialSetTag BREAKABLE_PLANTS;
  public static final MaterialSetTag TRANSPARENT;
  public static final MaterialSetTag CONTAINERS;
  public static final MaterialSetTag UNBREAKABLES;
  public static final MaterialSetTag METAL_ARMOR;

  static {
    COOKABLE.put(Material.PORKCHOP, Material.COOKED_PORKCHOP);
    COOKABLE.put(Material.BEEF, Material.COOKED_BEEF);
    COOKABLE.put(Material.CHICKEN, Material.COOKED_CHICKEN);
    COOKABLE.put(Material.COD, Material.COOKED_COD);
    COOKABLE.put(Material.SALMON, Material.COOKED_SALMON);
    COOKABLE.put(Material.POTATO, Material.BAKED_POTATO);
    COOKABLE.put(Material.MUTTON, Material.COOKED_MUTTON);
    COOKABLE.put(Material.RABBIT, Material.COOKED_RABBIT);
    COOKABLE.put(Material.WET_SPONGE, Material.SPONGE);
    COOKABLE.put(Material.KELP, Material.DRIED_KELP);
    COOKABLE.put(Material.STICK, Material.TORCH);

    ORES.put(Material.COAL_ORE, Material.COAL);
    ORES.put(Material.LAPIS_ORE, Material.LAPIS_LAZULI);
    ORES.put(Material.REDSTONE_ORE, Material.REDSTONE);
    ORES.put(Material.DIAMOND_ORE, Material.DIAMOND);
    ORES.put(Material.EMERALD_ORE, Material.EMERALD);
    ORES.put(Material.NETHER_QUARTZ_ORE, Material.QUARTZ);
    ORES.put(Material.IRON_ORE, Material.IRON_INGOT);
    ORES.put(Material.GOLD_ORE, Material.GOLD_INGOT);
    ORES.put(Material.NETHER_GOLD_ORE, Material.GOLD_NUGGET);

    NamespacedKey key = Bending.dataLayer().NSK_MATERIAL;
    BREAKABLE_PLANTS = new MaterialSetTag(key)
      .add(Tag.SAPLINGS.getValues())
      .add(Tag.FLOWERS.getValues())
      .add(Tag.SMALL_FLOWERS.getValues())
      .add(Tag.TALL_FLOWERS.getValues())
      .add(Tag.CROPS.getValues())
      .add(MaterialTags.MUSHROOMS.getValues())
      .add(Material.GRASS, Material.TALL_GRASS, Material.LARGE_FERN,
        Material.VINE, Material.FERN, Material.SUGAR_CANE, Material.DEAD_BUSH).ensureSize("Breakble plants", 38);

    TRANSPARENT = new MaterialSetTag(key)
      .add(BREAKABLE_PLANTS.getValues())
      .add(Tag.SIGNS.getValues())
      .add(Tag.FIRE.getValues())
      .add(Tag.CARPETS.getValues())
      .add(Tag.BUTTONS.getValues())
      .add(Material.AIR, Material.CAVE_AIR, Material.VOID_AIR, Material.COBWEB, Material.SNOW)
      .endsWith("TORCH").ensureSize("Transparent", 93);

    CONTAINERS = new MaterialSetTag(key).add(
      Material.CHEST, Material.TRAPPED_CHEST, Material.ENDER_CHEST, Material.BARREL,
      Material.SHULKER_BOX, Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER,
      Material.DISPENSER, Material.DROPPER, Material.ENCHANTING_TABLE, Material.BREWING_STAND,
      Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL, Material.BEACON,
      Material.GRINDSTONE, Material.CARTOGRAPHY_TABLE, Material.LOOM, Material.SMITHING_TABLE, Material.JUKEBOX
    ).ensureSize("Containers", 21);

    UNBREAKABLES = new MaterialSetTag(key).add(
      Material.BARRIER, Material.BEDROCK, Material.OBSIDIAN, Material.CRYING_OBSIDIAN,
      Material.NETHER_PORTAL, Material.END_PORTAL, Material.END_PORTAL_FRAME, Material.END_GATEWAY
    ).ensureSize("Unbreakables", 8);

    METAL_ARMOR = new MaterialSetTag(key).add(
      Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS,
      Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS,
      Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS
    ).ensureSize("Metal Armor", 12);
  }

  public static boolean isAir(@NonNull Block block) {
    return block.getType().isAir();
  }

  public static boolean isTransparent(@NonNull Block block) {
    return TRANSPARENT.isTagged(block);
  }

  public static boolean isTransparentOrWater(@NonNull Block block) {
    return block.getType() == Material.WATER || TRANSPARENT.isTagged(block);
  }

  public static boolean isContainer(@NonNull Block block) {
    return CONTAINERS.isTagged(block.getType()) || (block.getState() instanceof InventoryHolder);
  }

  public static boolean isUnbreakable(@NonNull Block block) {
    return UNBREAKABLES.isTagged(block.getType()) || isContainer(block) || (block.getState() instanceof CreatureSpawner);
  }

  public static boolean isIgnitable(@NonNull Block block) {
    if ((block.getType().isFlammable() || block.getType().isBurnable()) && isTransparent(block)) {
      return true;
    }
    return isAir(block) && block.getRelative(BlockFace.DOWN).getType().isSolid();
  }

  public static boolean isFire(@NonNull Block block) {
    return MaterialSetTag.FIRE.isTagged(block.getType());
  }

  public static boolean isLava(@NonNull Block block) {
    return block.getType() == Material.LAVA;
  }

  public static boolean isWater(@NonNull Block block) {
    return block.getType() == Material.WATER || isWaterLogged(block.getBlockData());
  }

  public static boolean isWaterLogged(@NonNull BlockData data) {
    return data instanceof Waterlogged && ((Waterlogged) data).isWaterlogged();
  }

  public static boolean isSnow(@NonNull Block block) {
    return block.getType() == Material.SNOW;
  }

  // Finds a suitable solid block type to replace a falling-type block with.
  public static @NonNull BlockData getSolidType(@NonNull BlockData data) {
    return getSolidType(data, data);
  }

  public static @NonNull BlockData getSolidType(@NonNull BlockData data, BlockData def) {
    if (MaterialTags.CONCRETE_POWDER.isTagged(data)) {
      Material material = Material.getMaterial(data.getMaterial().name().replace("_POWDER", ""));
      return material == null ? def : material.createBlockData();
    }
    switch (data.getMaterial()) {
      case SAND:
        return Material.SANDSTONE.createBlockData();
      case RED_SAND:
        return Material.RED_SANDSTONE.createBlockData();
      case GRAVEL:
        return Material.STONE.createBlockData();
      default:
        return def;
    }
  }

  public static @NonNull BlockData getFocusedType(@NonNull BlockData data) {
    return data.getMaterial() == Material.STONE ? Material.COBBLESTONE.createBlockData() : getSolidType(data, Material.STONE.createBlockData());
  }

  // Finds a suitable soft block type to replace a solid block
  public static @NonNull BlockData getSoftType(@NonNull BlockData data) {
    if (data.getMaterial() == Material.SAND || MaterialTags.SANDSTONES.isTagged(data)) {
      return Material.SAND.createBlockData();
    } else if (data.getMaterial() == Material.RED_SAND || MaterialTags.RED_SANDSTONES.isTagged(data)) {
      return Material.RED_SAND.createBlockData();
    } else if (MaterialTags.STAINED_TERRACOTTA.isTagged(data)) {
      return Material.CLAY.createBlockData();
    } else if (MaterialTags.CONCRETES.isTagged(data)) {
      Material material = Material.getMaterial(data.getMaterial().name() + "_POWDER");
      return Objects.requireNonNullElse(material, Material.GRAVEL).createBlockData();
    }
    switch (data.getMaterial()) {
      case STONE:
      case GRANITE:
      case POLISHED_GRANITE:
      case DIORITE:
      case POLISHED_DIORITE:
      case ANDESITE:
      case POLISHED_ANDESITE:
      case GRAVEL:
        return Material.GRAVEL.createBlockData();
      case DIRT:
      case MYCELIUM:
      case GRASS_BLOCK:
      case GRASS_PATH:
      case PODZOL:
      case COARSE_DIRT:
        return Material.COARSE_DIRT.createBlockData();
    }
    return Material.SAND.createBlockData();
  }

  public static boolean isSourceBlock(@NonNull Block block) {
    BlockData blockData = block.getBlockData();
    return blockData instanceof Levelled && ((Levelled) blockData).getLevel() == 0;
  }

  public static @NonNull BlockData lavaData(int level) {
    return Material.LAVA.createBlockData(d -> ((Levelled) d).setLevel((level < 0 || level > ((Levelled) d).getMaximumLevel()) ? 0 : level));
  }

  public static @NonNull BlockData waterData(int level) {
    return Material.WATER.createBlockData(d -> ((Levelled) d).setLevel((level < 0 || level > ((Levelled) d).getMaximumLevel()) ? 0 : level));
  }
}
