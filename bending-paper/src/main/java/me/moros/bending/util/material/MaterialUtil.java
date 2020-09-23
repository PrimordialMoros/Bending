/*
 *   Copyright 2020 Moros <https://github.com/PrimordialMoros>
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

import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.util.collision.AABBUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.inventory.InventoryHolder;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class MaterialUtil {
	public static final Map<Material, Material> COOKABLE = new HashMap<>();

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
	}

	private static final Set<Material> containers = EnumSet.of(
		Material.CHEST, Material.TRAPPED_CHEST, Material.ENDER_CHEST, Material.BARREL, Material.SHULKER_BOX, Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER,
		Material.DISPENSER, Material.DROPPER, Material.ENCHANTING_TABLE, Material.BREWING_STAND, Material.BEACON, Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL,
		Material.GRINDSTONE, Material.CARTOGRAPHY_TABLE, Material.LOOM, Material.SMITHING_TABLE
	);

	private static final Set<Material> unbreakables = EnumSet.of(
		Material.BARRIER, Material.BEDROCK, Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.NETHER_PORTAL,
		Material.END_PORTAL, Material.END_PORTAL_FRAME, Material.END_GATEWAY
	);

	public static boolean isUnbreakable(Block block) {
		return unbreakables.contains(block.getType()) || (block.getState() instanceof InventoryHolder) ||
			containers.contains(block.getType()) || (block.getState() instanceof CreatureSpawner);
	}

	//TODO change to Paper's MaterialTagSet and split into better categories
	public static final Set<Material> TRANSPARENT_MATERIALS = EnumSet.of(
		Material.AIR, Material.CAVE_AIR, Material.OAK_SAPLING, Material.SPRUCE_SAPLING, Material.BIRCH_SAPLING,
		Material.JUNGLE_SAPLING, Material.ACACIA_SAPLING, Material.DARK_OAK_SAPLING,
		Material.COBWEB, Material.TALL_GRASS, Material.GRASS, Material.FERN, Material.DEAD_BUSH,
		Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID, Material.ALLIUM,
		Material.AZURE_BLUET, Material.RED_TULIP, Material.ORANGE_TULIP, Material.WHITE_TULIP, Material.PINK_TULIP,
		Material.OXEYE_DAISY, Material.BROWN_MUSHROOM, Material.RED_MUSHROOM, Material.TORCH, Material.FIRE,
		Material.WHEAT, Material.SNOW, Material.SUGAR_CANE, Material.VINE, Material.SUNFLOWER, Material.LILAC,
		Material.LARGE_FERN, Material.ROSE_BUSH, Material.PEONY, Material.CAVE_AIR, Material.VOID_AIR,
		Material.ACACIA_SIGN, Material.ACACIA_WALL_SIGN, Material.BIRCH_SIGN, Material.BIRCH_WALL_SIGN,
		Material.DARK_OAK_SIGN, Material.DARK_OAK_WALL_SIGN, Material.JUNGLE_SIGN, Material.JUNGLE_WALL_SIGN,
		Material.OAK_SIGN, Material.OAK_WALL_SIGN
	);

	private static final Set<Material> FIRE = EnumSet.of(
		Material.FIRE, Material.SOUL_FIRE
	);

	// These are materials that must be attached to a block.
	private final static Set<Material> ATTACH_MATERIALS = EnumSet.of(
		Material.ACACIA_SAPLING, Material.BIRCH_SAPLING, Material.DARK_OAK_SAPLING, Material.JUNGLE_SAPLING,
		Material.SPRUCE_SAPLING, Material.GRASS, Material.POPPY, Material.DANDELION, Material.DEAD_BUSH,
		Material.BROWN_MUSHROOM, Material.RED_MUSHROOM, Material.FIRE, Material.SOUL_FIRE, Material.SNOW, Material.TORCH,
		Material.SUNFLOWER, Material.LILAC, Material.LARGE_FERN, Material.FERN, Material.ROSE_BUSH, Material.PEONY
	);

	// These are materials that fire can be placed on.
	private static final Set<Material> IGNITABLE_MATERIALS = EnumSet.of(
		Material.BEDROCK, Material.BOOKSHELF,
		Material.BRICK, Material.CLAY, Material.BRICKS, Material.COAL_ORE, Material.COBBLESTONE,
		Material.DIAMOND_ORE, Material.DIAMOND_BLOCK, Material.DIRT, Material.END_STONE, Material.REDSTONE_ORE,
		Material.GOLD_BLOCK, Material.GRAVEL, Material.GRASS_BLOCK, Material.BROWN_MUSHROOM_BLOCK,
		Material.RED_MUSHROOM_BLOCK, Material.LAPIS_BLOCK, Material.LAPIS_ORE, Material.OAK_LOG,
		Material.ACACIA_LOG, Material.BIRCH_LOG, Material.DARK_OAK_LOG, Material.JUNGLE_LOG, Material.SPRUCE_LOG,
		Material.MOSSY_COBBLESTONE, Material.MYCELIUM, Material.NETHER_BRICK, Material.NETHERRACK,
		Material.OBSIDIAN, Material.SAND, Material.SANDSTONE, Material.STONE_BRICKS, Material.STONE,
		Material.SOUL_SAND, Material.ACACIA_WOOD, Material.BIRCH_WOOD, Material.DARK_OAK_WOOD, Material.JUNGLE_WOOD,
		Material.OAK_WOOD, Material.SPRUCE_WOOD, Material.BLACK_WOOL, Material.BLUE_WOOL, Material.BROWN_WOOL,
		Material.CYAN_WOOL, Material.GRAY_WOOL, Material.GREEN_WOOL, Material.LIGHT_BLUE_WOOL, Material.LIGHT_GRAY_WOOL,
		Material.LIME_WOOL, Material.MAGENTA_WOOL, Material.ORANGE_WOOL, Material.PINK_WOOL, Material.PURPLE_WOOL,
		Material.RED_WOOL, Material.WHITE_WOOL, Material.YELLOW_WOOL, Material.ACACIA_LEAVES, Material.BIRCH_LEAVES,
		Material.DARK_OAK_LEAVES, Material.JUNGLE_LEAVES, Material.OAK_LEAVES, Material.SPRUCE_LEAVES,
		Material.MELON, Material.PUMPKIN, Material.JACK_O_LANTERN, Material.NOTE_BLOCK, Material.GLOWSTONE,
		Material.IRON_BLOCK, Material.DISPENSER, Material.SPONGE, Material.IRON_ORE, Material.GOLD_ORE,
		Material.COAL_BLOCK, Material.CRAFTING_TABLE, Material.HAY_BLOCK, Material.REDSTONE_LAMP,
		Material.EMERALD_ORE, Material.EMERALD_BLOCK, Material.REDSTONE_BLOCK, Material.QUARTZ_BLOCK,
		Material.NETHER_QUARTZ_ORE, Material.TERRACOTTA, Material.BLACK_TERRACOTTA, Material.BLUE_TERRACOTTA,
		Material.BROWN_TERRACOTTA, Material.CYAN_TERRACOTTA, Material.GRAY_TERRACOTTA, Material.GREEN_TERRACOTTA,
		Material.LIGHT_BLUE_TERRACOTTA, Material.LIGHT_GRAY_TERRACOTTA, Material.LIME_TERRACOTTA,
		Material.MAGENTA_TERRACOTTA, Material.ORANGE_TERRACOTTA, Material.PINK_TERRACOTTA,
		Material.PURPLE_TERRACOTTA, Material.RED_TERRACOTTA, Material.WHITE_TERRACOTTA, Material.YELLOW_TERRACOTTA
	);

	public static boolean isTransparent(Block block) {
		return TRANSPARENT_MATERIALS.contains(block.getType());
	}

	public static boolean isSolid(Block block) {
		AABB blockBounds = AABBUtils.getBlockBounds(block);
		// The block bounding box will have width if it's solid.
		if (blockBounds.min() == null || blockBounds.max() == null) return false;
		return blockBounds.min().distanceSq(blockBounds.max()) > 0;
	}

	public static boolean isIgnitable(Block block) {
		if (isAir(block) || ATTACH_MATERIALS.contains(block.getType())) {
			return IGNITABLE_MATERIALS.contains(block.getRelative(BlockFace.DOWN).getType());
		} else {
			return false;
		}
	}

	public static boolean isEarthbendable(Block block) {
		return isEarthbendable(block.getType());
	}

	public static boolean isEarthbendable(Material type) {
		// TODO Implement checks for subelement materials, will need to pass extra info
		return EarthMaterials.EARTH_BENDABLE.isTagged(type);
	}

	public static boolean isFire(Block block) {
		return isFire(block.getType());
	}

	public static boolean isFire(Material material) {
		return FIRE.contains(material);
	}

	public static boolean isLava(Block block) {
		return isLava(block.getType());
	}

	public static boolean isLava(Material type) {
		return type == Material.LAVA;
	}

	public static boolean isWater(Block block) {
		return isWater(block.getType());
	}

	public static boolean isWater(Material type) {
		return type == Material.WATER;
	}

	public static boolean isIce(Block block) {
		return isIce(block.getType());
	}

	public static boolean isIce(Material material) {
		return WaterMaterials.ICE_BENDABLE.isTagged(material);
	}

	public static boolean isPlant(Block block) {
		return isPlant(block.getType());
	}

	public static boolean isPlant(Material type) {
		return WaterMaterials.PLANT_BENDABLE.isTagged(type);
	}

	// Finds a suitable solid block type to replace a falling-type block with.
	public static BlockData getSolidType(BlockData data) {
		switch (data.getMaterial()) { // TODO implement concrete powder maybe (if it ever becomes bendable)?
			case SAND:
				return Material.SANDSTONE.createBlockData();
			case RED_SAND:
				return Material.RED_SANDSTONE.createBlockData();
			case GRAVEL:
				return Material.STONE.createBlockData();
			default:
				return data;
		}
	}

	public static boolean isAir(Block block) {
		return isAir(block.getType());
	}

	public static boolean isAir(Material material) {
		return material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR;
	}

	public static boolean isSourceBlock(Block block) {
		BlockData blockData = block.getBlockData();
		if (blockData instanceof Levelled) {
			return ((Levelled) blockData).getLevel() == 0;
		}
		return false;
	}

	public static BlockData getLavaData(int level) {
		return Material.LAVA.createBlockData(d -> ((Levelled) d).setLevel((level < 0 || level > ((Levelled) d).getMaximumLevel()) ? 0 : level));
	}

	public static BlockData getWaterData(int level) {
		return Material.WATER.createBlockData(d -> ((Levelled) d).setLevel((level < 0 || level > ((Levelled) d).getMaximumLevel()) ? 0 : level));
	}
}
