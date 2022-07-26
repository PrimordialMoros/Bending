/*
 * Copyright 2020-2022 Moros
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

package me.moros.bending.util.material;

import java.util.Map;
import java.util.Objects;

import com.destroystokyo.paper.MaterialSetTag;
import com.destroystokyo.paper.MaterialTags;
import me.moros.bending.util.metadata.Metadata;
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

import static java.util.Map.entry;

/**
 * Group and categorize all various common materials.
 * Also provides utility methods to convert materials and block data to similar types.
 */
public final class MaterialUtil {
  public static final Map<Material, Material> COOKABLE;
  public static final Map<Material, Material> ORES;
  public static final MaterialSetTag BREAKABLE_PLANTS;
  public static final MaterialSetTag WATER_PLANTS;
  public static final MaterialSetTag TRANSPARENT;
  public static final MaterialSetTag CONTAINERS;
  public static final MaterialSetTag UNBREAKABLES;
  public static final MaterialSetTag METAL_ARMOR;

  static {
    COOKABLE = Map.ofEntries(
      entry(Material.PORKCHOP, Material.COOKED_PORKCHOP),
      entry(Material.BEEF, Material.COOKED_BEEF),
      entry(Material.CHICKEN, Material.COOKED_CHICKEN),
      entry(Material.COD, Material.COOKED_COD),
      entry(Material.SALMON, Material.COOKED_SALMON),
      entry(Material.POTATO, Material.BAKED_POTATO),
      entry(Material.MUTTON, Material.COOKED_MUTTON),
      entry(Material.RABBIT, Material.COOKED_RABBIT),
      entry(Material.WET_SPONGE, Material.SPONGE),
      entry(Material.KELP, Material.DRIED_KELP),
      entry(Material.STICK, Material.TORCH)
    );

    ORES = Map.ofEntries(
      entry(Material.COPPER_ORE, Material.COAL),
      entry(Material.DEEPSLATE_COPPER_ORE, Material.COAL),
      entry(Material.COAL_ORE, Material.COAL),
      entry(Material.DEEPSLATE_COAL_ORE, Material.COAL),
      entry(Material.LAPIS_ORE, Material.LAPIS_LAZULI),
      entry(Material.DEEPSLATE_LAPIS_ORE, Material.LAPIS_LAZULI),
      entry(Material.REDSTONE_ORE, Material.REDSTONE),
      entry(Material.DEEPSLATE_REDSTONE_ORE, Material.REDSTONE),
      entry(Material.DIAMOND_ORE, Material.DIAMOND),
      entry(Material.DEEPSLATE_DIAMOND_ORE, Material.DIAMOND),
      entry(Material.EMERALD_ORE, Material.EMERALD),
      entry(Material.DEEPSLATE_EMERALD_ORE, Material.EMERALD),
      entry(Material.NETHER_QUARTZ_ORE, Material.QUARTZ),
      entry(Material.IRON_ORE, Material.IRON_INGOT),
      entry(Material.DEEPSLATE_IRON_ORE, Material.IRON_INGOT),
      entry(Material.GOLD_ORE, Material.GOLD_INGOT),
      entry(Material.DEEPSLATE_GOLD_ORE, Material.GOLD_INGOT),
      entry(Material.NETHER_GOLD_ORE, Material.GOLD_NUGGET)
    );

    NamespacedKey key = Metadata.NSK_MATERIAL;
    WATER_PLANTS = new MaterialSetTag(key)
      .add(Material.SEAGRASS, Material.TALL_SEAGRASS, Material.KELP, Material.KELP_PLANT).lock();

    BREAKABLE_PLANTS = new MaterialSetTag(key)
      .add(WATER_PLANTS.getValues())
      .add(Tag.SAPLINGS.getValues())
      .add(Tag.FLOWERS.getValues())
      .add(Tag.SMALL_FLOWERS.getValues())
      .add(Tag.TALL_FLOWERS.getValues())
      .add(Tag.CROPS.getValues())
      .add(Tag.CAVE_VINES.getValues())
      .add(MaterialTags.MUSHROOMS.getValues())
      .add(MaterialTags.CORAL.getValues())
      .add(MaterialTags.CORAL_FANS.getValues())
      .add(Material.GRASS, Material.TALL_GRASS, Material.LARGE_FERN, Material.GLOW_LICHEN,
        Material.WEEPING_VINES, Material.WEEPING_VINES_PLANT, Material.TWISTING_VINES, Material.TWISTING_VINES_PLANT,
        Material.VINE, Material.FERN, Material.SUGAR_CANE, Material.DEAD_BUSH).lock();

    TRANSPARENT = new MaterialSetTag(key)
      .add(BREAKABLE_PLANTS.getValues())
      .add(Tag.SIGNS.getValues())
      .add(Tag.FIRE.getValues())
      .add(Tag.CARPETS.getValues())
      .add(Tag.BUTTONS.getValues())
      .add(Material.LIGHT, Material.AIR, Material.CAVE_AIR, Material.VOID_AIR, Material.COBWEB, Material.SNOW)
      .endsWith("TORCH").lock();

    CONTAINERS = new MaterialSetTag(key).add(
      Material.CHEST, Material.TRAPPED_CHEST, Material.ENDER_CHEST, Material.BARREL,
      Material.SHULKER_BOX, Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER,
      Material.DISPENSER, Material.DROPPER, Material.ENCHANTING_TABLE, Material.BREWING_STAND,
      Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL, Material.BEACON,
      Material.GRINDSTONE, Material.CARTOGRAPHY_TABLE, Material.LOOM, Material.SMITHING_TABLE, Material.JUKEBOX
    ).lock();

    UNBREAKABLES = new MaterialSetTag(key).add(
      Material.BARRIER, Material.BEDROCK, Material.OBSIDIAN, Material.CRYING_OBSIDIAN,
      Material.NETHER_PORTAL, Material.END_PORTAL, Material.END_PORTAL_FRAME, Material.END_GATEWAY
    ).lock();

    METAL_ARMOR = new MaterialSetTag(key).add(
      Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS,
      Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS,
      Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,
      Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS
    ).lock();
  }

  private MaterialUtil() {
  }

  public static boolean isAir(Block block) {
    return block.getType().isAir();
  }

  public static boolean isTransparent(Block block) {
    return TRANSPARENT.isTagged(block);
  }

  public static boolean isTransparentOrWater(Block block) {
    return TRANSPARENT.isTagged(block) || isWater(block);
  }

  public static boolean isContainer(Block block) {
    return CONTAINERS.isTagged(block.getType()) || (block.getState() instanceof InventoryHolder);
  }

  public static boolean isUnbreakable(Block block) {
    return UNBREAKABLES.isTagged(block.getType()) || isContainer(block) || (block.getState() instanceof CreatureSpawner);
  }

  public static boolean isIgnitable(Block block) {
    if ((block.getType().isFlammable() || block.getType().isBurnable()) && isTransparent(block)) {
      return true;
    }
    return isAir(block) && block.getRelative(BlockFace.DOWN).getType().isSolid();
  }

  public static boolean isFire(Block block) {
    return MaterialSetTag.FIRE.isTagged(block.getType());
  }

  public static boolean isCampfire(Block block) {
    return block.getType() == Material.CAMPFIRE || block.getType() == Material.SOUL_CAMPFIRE;
  }

  public static boolean isLava(Block block) {
    return block.getType() == Material.LAVA;
  }

  public static boolean isWaterPlant(Block block) {
    return WATER_PLANTS.isTagged(block.getType());
  }

  public static boolean isWater(Block block) {
    Material type = block.getType();
    if (type == Material.WATER || type == Material.BUBBLE_COLUMN) {
      return true;
    }
    return isWaterLogged(block) || isWaterPlant(block);
  }

  public static boolean isWaterLogged(Block block) {
    return block.getBlockData() instanceof Waterlogged waterlogged && waterlogged.isWaterlogged();
  }

  public static boolean isMeltable(Block block) {
    return WaterMaterials.isSnowBendable(block) || WaterMaterials.isIceBendable(block);
  }

  // Finds a suitable solid block type to replace a falling-type block with.
  public static BlockData solidType(BlockData data) {
    return solidType(data, data);
  }

  public static BlockData solidType(BlockData data, BlockData def) {
    if (MaterialTags.CONCRETE_POWDER.isTagged(data)) {
      Material material = Material.getMaterial(data.getMaterial().name().replace("_POWDER", ""));
      return material == null ? def : material.createBlockData();
    }
    return switch (data.getMaterial()) {
      case SAND -> Material.SANDSTONE.createBlockData();
      case RED_SAND -> Material.RED_SANDSTONE.createBlockData();
      case GRAVEL -> Material.STONE.createBlockData();
      default -> def;
    };
  }

  public static BlockData focusedType(BlockData data) {
    return data.getMaterial() == Material.STONE ? Material.COBBLESTONE.createBlockData() : solidType(data, Material.STONE.createBlockData());
  }

  // Finds a suitable soft block type to replace a solid block
  public static BlockData softType(BlockData data) {
    if (data.getMaterial() == Material.SAND || MaterialTags.SANDSTONES.isTagged(data)) {
      return Material.SAND.createBlockData();
    } else if (data.getMaterial() == Material.RED_SAND || MaterialTags.RED_SANDSTONES.isTagged(data)) {
      return Material.RED_SAND.createBlockData();
    } else if (MaterialTags.STAINED_TERRACOTTA.isTagged(data)) {
      return Material.CLAY.createBlockData();
    } else if (MaterialTags.CONCRETES.isTagged(data)) {
      Material material = Material.getMaterial(data.getMaterial().name() + "_POWDER");
      return Objects.requireNonNullElse(material, Material.GRAVEL).createBlockData();
    } else if (ORES.containsKey(data.getMaterial())) {
      return Material.GRAVEL.createBlockData();
    }
    return switch (data.getMaterial()) {
      case STONE, GRANITE, POLISHED_GRANITE, DIORITE, POLISHED_DIORITE, ANDESITE, POLISHED_ANDESITE,
        GRAVEL, DEEPSLATE, CALCITE, TUFF, SMOOTH_BASALT -> Material.GRAVEL.createBlockData();
      case DIRT, MYCELIUM, GRASS_BLOCK, DIRT_PATH, PODZOL, COARSE_DIRT, ROOTED_DIRT ->
        Material.COARSE_DIRT.createBlockData();
      default -> Material.SAND.createBlockData();
    };
  }

  public static boolean isSourceBlock(Block block) {
    BlockData blockData = block.getBlockData();
    return blockData instanceof Levelled levelled && levelled.getLevel() == 0;
  }

  public static BlockData lavaData(int level) {
    return Material.LAVA.createBlockData(d -> ((Levelled) d).setLevel((level < 0 || level > ((Levelled) d).getMaximumLevel()) ? 0 : level));
  }

  public static BlockData waterData(int level) {
    return Material.WATER.createBlockData(d -> ((Levelled) d).setLevel((level < 0 || level > ((Levelled) d).getMaximumLevel()) ? 0 : level));
  }
}
