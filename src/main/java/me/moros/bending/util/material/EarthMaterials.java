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

import com.destroystokyo.paper.MaterialSetTag;
import com.destroystokyo.paper.MaterialTags;
import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.Bending;
import me.moros.bending.model.user.User;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.EnumSet;

public class EarthMaterials {
	public static final MaterialSetTag EARTH_BENDABLE;
	public static final MaterialSetTag SAND_BENDABLE;
	public static final MaterialSetTag METAL_BENDABLE;
	public static final MaterialSetTag LAVA_BENDABLE;
	public static final MaterialSetTag ALL;
	public static final MaterialSetTag EARTH_SAND_SOURCES;

	static {
		EARTH_BENDABLE = new MaterialSetTag(Bending.getLayer().getCoreKey(), EnumSet.of(
			Material.DIRT, Material.COARSE_DIRT, Material.MYCELIUM, Material.GRASS_BLOCK, Material.GRASS_PATH,
			Material.GRANITE, Material.POLISHED_GRANITE, Material.DIORITE, Material.POLISHED_DIORITE,
			Material.ANDESITE, Material.POLISHED_ANDESITE, Material.GRAVEL, Material.CLAY,
			Material.COAL_ORE, Material.IRON_ORE, Material.GOLD_ORE, Material.REDSTONE_ORE,
			Material.LAPIS_ORE, Material.DIAMOND_ORE, Material.NETHER_QUARTZ_ORE, Material.EMERALD_ORE,
			Material.NETHER_GOLD_ORE, Material.NETHERRACK, Material.STONE_BRICK_STAIRS,
			Material.STONE, Material.COBBLESTONE, Material.COBBLESTONE_STAIRS
		));
		EARTH_BENDABLE.add(MaterialSetTag.STONE_BRICKS.getValues());

		SAND_BENDABLE = new MaterialSetTag(Bending.getLayer().getCoreKey(), MaterialSetTag.SAND.getValues());
		SAND_BENDABLE.add(MaterialTags.SANDSTONES, MaterialTags.RED_SANDSTONES);

		METAL_BENDABLE = new MaterialSetTag(Bending.getLayer().getCoreKey(), EnumSet.of(
			Material.IRON_BLOCK, Material.GOLD_BLOCK, Material.QUARTZ_BLOCK
		));

		LAVA_BENDABLE = new MaterialSetTag(Bending.getLayer().getCoreKey(), EnumSet.of(
			Material.LAVA, Material.MAGMA_BLOCK
		));

		ALL = new MaterialSetTag(Bending.getLayer().getCoreKey(), EARTH_BENDABLE.getValues());
		ALL.add(SAND_BENDABLE, METAL_BENDABLE, LAVA_BENDABLE);

		EARTH_SAND_SOURCES = new MaterialSetTag(Bending.getLayer().getCoreKey(), EARTH_BENDABLE.getValues());
		EARTH_SAND_SOURCES.add(SAND_BENDABLE);
	}

	public static boolean isEarthbendable(@NonNull User user, @NonNull Block block) {
		if (isMetalBendable(block) && !user.hasPermission("bending.metal")) return false;
		if (isLavaBendable(block) && !user.hasPermission("bending.lava")) return false;
		return ALL.isTagged(block);
	}

	public static boolean isEarthNotLava(@NonNull User user, @NonNull Block block) {
		if (isLavaBendable(block)) return false;
		if (isMetalBendable(block) && !user.hasPermission("bending.metal")) return false;
		return ALL.isTagged(block);
	}

	public static boolean isSandBendable(@NonNull Block block) {
		return SAND_BENDABLE.isTagged(block);
	}

	public static boolean isMetalBendable(@NonNull Block block) {
		return METAL_BENDABLE.isTagged(block);
	}

	public static boolean isLavaBendable(@NonNull Block block) {
		return LAVA_BENDABLE.isTagged(block);
	}
}
