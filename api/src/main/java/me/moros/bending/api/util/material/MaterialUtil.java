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

package me.moros.bending.api.util.material;

import me.moros.bending.api.platform.Direction;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.block.BlockStateProperties;
import me.moros.bending.api.platform.block.BlockTag;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.item.ItemTag;
import me.moros.bending.api.util.KeyUtil;
import net.kyori.adventure.key.Key;

/**
 * Group and categorize all various common materials.
 * Also provides utility methods to convert materials and block data to similar types.
 */
public final class MaterialUtil {
  private MaterialUtil() {
  }

  public static final BlockTag WATER_PLANTS = BlockTag.reference(KeyUtil.simple("water_plants"));
  public static final BlockTag BREAKABLE_PLANTS = BlockTag.reference(KeyUtil.simple("breakable_plants"));
  public static final BlockTag TRANSPARENT = BlockTag.reference(KeyUtil.simple("transparent"));
  public static final BlockTag LOCKABLE_CONTAINERS = BlockTag.reference(KeyUtil.simple("lockable_containers"));
  public static final BlockTag CONTAINERS = BlockTag.reference(KeyUtil.simple("containers"));
  public static final BlockTag UNBREAKABLES = BlockTag.reference(KeyUtil.simple("unbreakable"));
  public static final ItemTag METAL_ARMOR = ItemTag.reference(KeyUtil.simple("metal_armor"));

  private static final BlockTag STAINED_TERRACOTTA;
  private static final BlockTag SANDSTONES;
  private static final BlockTag RED_SANDSTONES;

  static {
    STAINED_TERRACOTTA = BlockTag.builder("stained_terracotta")
      .endsWith("terracotta")
      .not(BlockType.TERRACOTTA)
      .notEndsWith("glazed_terracotta")
      .build();

    SANDSTONES = BlockTag.builder("sandstone")
      .add(BlockType.SANDSTONE, BlockType.CHISELED_SANDSTONE, BlockType.CUT_SANDSTONE, BlockType.SMOOTH_SANDSTONE)
      .build();

    RED_SANDSTONES = BlockTag.builder("red_sandstone").endsWith("red_sandstone").build();
  }

  public static void init() {
    BlockTag.builder(WATER_PLANTS.key())
      .add(BlockTag.reference(KeyUtil.simple("extra_water_plants")))
      .add(BlockType.SEAGRASS, BlockType.TALL_SEAGRASS, BlockType.KELP, BlockType.KELP_PLANT)
      .buildAndRegister();
    BlockTag.builder(BREAKABLE_PLANTS.key())
      .add(BlockTag.reference(KeyUtil.simple("extra_breakable_plants")))
      .add(WATER_PLANTS)
      .add(BlockTag.SAPLINGS)
      .add(BlockTag.FLOWERS)
      .add(BlockTag.CROPS)
      .add(BlockTag.CAVE_VINES)
      .add(BlockTag.CORALS)
      .add(BlockType.BROWN_MUSHROOM, BlockType.RED_MUSHROOM)
      .add(BlockTag.CORALS)
      .add(BlockTag.CORAL_BLOCKS)
      .add(BlockType.SHORT_GRASS, BlockType.TALL_GRASS, BlockType.LARGE_FERN, BlockType.GLOW_LICHEN,
        BlockType.WEEPING_VINES, BlockType.WEEPING_VINES_PLANT, BlockType.TWISTING_VINES, BlockType.TWISTING_VINES_PLANT,
        BlockType.VINE, BlockType.FERN, BlockType.SUGAR_CANE, BlockType.DEAD_BUSH,
        BlockType.SMALL_DRIPLEAF, BlockType.BIG_DRIPLEAF, BlockType.BIG_DRIPLEAF_STEM)
      .buildAndRegister();
    BlockTag.builder(TRANSPARENT.key())
      .add(BlockTag.reference(KeyUtil.simple("extra_transparent")))
      .add(BREAKABLE_PLANTS)
      .add(BlockTag.SIGNS)
      .add(BlockTag.FIRE)
      .add(BlockTag.WOOL_CARPETS)
      .add(BlockTag.BUTTONS)
      .add(BlockType.LIGHT, BlockType.AIR, BlockType.CAVE_AIR, BlockType.VOID_AIR, BlockType.COBWEB, BlockType.SNOW)
      .endsWith("torch")
      .buildAndRegister();
    BlockTag.builder(LOCKABLE_CONTAINERS.key())
      .add(BlockTag.reference(KeyUtil.simple("extra_lockable_containers")))
      .add(BlockType.CHEST, BlockType.TRAPPED_CHEST, BlockType.BARREL, BlockType.SHULKER_BOX,
        BlockType.FURNACE, BlockType.BLAST_FURNACE, BlockType.SMOKER, BlockType.BEACON,
        BlockType.DISPENSER, BlockType.DROPPER, BlockType.HOPPER, BlockType.BREWING_STAND)
      .buildAndRegister();
    BlockTag.builder(CONTAINERS.key())
      .add(BlockTag.reference(KeyUtil.simple("extra_containers")))
      .add(LOCKABLE_CONTAINERS)
      .add(BlockType.ENDER_CHEST, BlockType.ENCHANTING_TABLE, BlockType.ANVIL, BlockType.CHIPPED_ANVIL, BlockType.DAMAGED_ANVIL,
        BlockType.GRINDSTONE, BlockType.CARTOGRAPHY_TABLE, BlockType.LOOM, BlockType.SMITHING_TABLE, BlockType.JUKEBOX)
      .buildAndRegister();
    BlockTag.builder(UNBREAKABLES.key())
      .add(BlockTag.reference(KeyUtil.simple("extra_unbreakables")))
      .add(BlockType.BARRIER, BlockType.BEDROCK, BlockType.OBSIDIAN, BlockType.CRYING_OBSIDIAN,
        BlockType.NETHER_PORTAL, BlockType.END_PORTAL, BlockType.END_PORTAL_FRAME, BlockType.END_GATEWAY)
      .buildAndRegister();

    ItemTag.builder(METAL_ARMOR.key())
      .add(ItemTag.reference(KeyUtil.simple("extra_metal_armor")))
      .add(Item.IRON_HELMET, Item.IRON_CHESTPLATE, Item.IRON_LEGGINGS, Item.IRON_BOOTS,
        Item.GOLDEN_HELMET, Item.GOLDEN_CHESTPLATE, Item.GOLDEN_LEGGINGS, Item.GOLDEN_BOOTS,
        Item.CHAINMAIL_HELMET, Item.CHAINMAIL_CHESTPLATE, Item.CHAINMAIL_LEGGINGS, Item.CHAINMAIL_BOOTS,
        Item.NETHERITE_HELMET, Item.NETHERITE_CHESTPLATE, Item.NETHERITE_LEGGINGS, Item.NETHERITE_BOOTS)
      .buildAndRegister();
  }

  public static boolean isAir(Block block) {
    return block.type().isAir();
  }

  public static boolean isTransparent(Block block) {
    return TRANSPARENT.isTagged(block);
  }

  public static boolean isTransparentOrWater(Block block) {
    return TRANSPARENT.isTagged(block) || isWater(block);
  }

  public static boolean isUnbreakable(Block block) {
    return UNBREAKABLES.isTagged(block) || CONTAINERS.isTagged(block) || block.world().isBlockEntity(block);
  }

  public static boolean isIgnitable(Block block) {
    if (block.type().isFlammable() && isTransparent(block)) {
      return true;
    }
    return isAir(block) && block.offset(Direction.DOWN).type().isSolid();
  }

  public static boolean isFire(BlockType type) {
    return BlockTag.FIRE.isTagged(type);
  }

  public static boolean isFire(Block block) {
    return BlockTag.FIRE.isTagged(block);
  }

  public static boolean isCampfire(Block block) {
    return BlockTag.CAMPFIRES.isTagged(block);
  }

  public static boolean isLava(Block block) {
    return block.type() == BlockType.LAVA;
  }

  public static boolean isWaterPlant(Block block) {
    return WATER_PLANTS.isTagged(block);
  }

  public static boolean isWater(Block block) {
    BlockType type = block.type();
    if (type == BlockType.WATER || type == BlockType.BUBBLE_COLUMN) {
      return true;
    }
    return isWaterLogged(block) || isWaterPlant(block);
  }

  public static boolean isWaterLogged(Block block) {
    var property = block.state().property(BlockStateProperties.WATERLOGGED);
    return Boolean.TRUE.equals(property);
  }

  public static boolean isMeltable(Block block) {
    return WaterMaterials.isSnowBendable(block) || WaterMaterials.isIceBendable(block);
  }

  // Finds a suitable solid block type to replace a falling-type block with.
  public static BlockType solidType(BlockType type) {
    return solidType(type, type);
  }

  public static BlockType solidType(BlockType type, BlockType def) {
    if (type.name().endsWith("_concrete_powder")) {
      BlockType result = BlockType.registry().fromString(type.name().replace("_powder", ""));
      return result == null ? def : result;
    }
    if (type == BlockType.SAND) {
      return BlockType.SANDSTONE;
    } else if (type == BlockType.RED_SAND) {
      return BlockType.RED_SANDSTONE;
    } else if (type == BlockType.GRAVEL) {
      return BlockType.STONE;
    } else {
      return def;
    }
  }

  public static BlockType focusedType(BlockType type) {
    return type == BlockType.STONE ? BlockType.COBBLESTONE : solidType(type, BlockType.STONE);
  }

  private static final BlockTag TO_GRAVEL = BlockTag.builder("soft-to-gravel")
    .add(BlockType.STONE, BlockType.GRANITE, BlockType.POLISHED_GRANITE, BlockType.DIORITE, BlockType.POLISHED_DIORITE,
      BlockType.ANDESITE, BlockType.POLISHED_ANDESITE, BlockType.GRAVEL, BlockType.DEEPSLATE, BlockType.CALCITE,
      BlockType.TUFF, BlockType.SMOOTH_BASALT
    ).build();

  private static final BlockTag TO_DIRT = BlockTag.builder("soft-to-dirt")
    .add(BlockType.DIRT, BlockType.MYCELIUM, BlockType.GRASS_BLOCK, BlockType.DIRT_PATH,
      BlockType.PODZOL, BlockType.COARSE_DIRT, BlockType.ROOTED_DIRT
    ).build();

  // Finds a suitable soft block type to replace a solid block
  public static BlockType softType(BlockType type) {
    if (type == BlockType.SAND || SANDSTONES.isTagged(type)) {
      return BlockType.SAND;
    } else if (type == BlockType.RED_SAND || RED_SANDSTONES.isTagged(type)) {
      return BlockType.RED_SAND;
    } else if (STAINED_TERRACOTTA.isTagged(type)) {
      return BlockType.CLAY;
    } else if (type.name().endsWith("_concrete")) {
      BlockType result = BlockType.registry().get(Key.key(type.key().namespace(), type.name() + "_powder"));
      return result == null ? BlockType.GRAVEL : result;
    } else if (type.name().endsWith("_ore")) { // TODO replace with ORES tag if added
      return BlockType.GRAVEL;
    }
    if (TO_GRAVEL.isTagged(type)) {
      return BlockType.GRAVEL;
    } else if (TO_DIRT.isTagged(type)) {
      return BlockType.COARSE_DIRT;
    } else {
      return BlockType.SAND;
    }
  }

  public static boolean isSourceBlock(Block block) {
    var level = block.state().property(BlockStateProperties.LEVEL);
    return level != null && level == 0;
  }

  public static BlockState lavaData(int level) {
    return BlockType.LAVA.defaultState().withProperty(BlockStateProperties.LEVEL, Math.clamp(level, 0, 15));
  }

  public static BlockState waterData(int level) {
    return BlockType.WATER.defaultState().withProperty(BlockStateProperties.LEVEL, Math.clamp(level, 0, 15));
  }
}
