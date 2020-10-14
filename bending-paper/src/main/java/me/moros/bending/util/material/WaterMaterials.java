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
import me.moros.bending.Bending;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class WaterMaterials {
	public static final MaterialSetTag PLANT_BENDABLE;
	public static final MaterialSetTag ICE_BENDABLE;
	public static final MaterialSetTag ALL;
	public static final MaterialSetTag WATER_ICE_SOURCES;

	static {
		PLANT_BENDABLE = new MaterialSetTag(Bending.getKey(), MaterialSetTag.FLOWERS.getValues());
		PLANT_BENDABLE.add(MaterialSetTag.SAPLINGS.getValues())
			.add(MaterialSetTag.CROPS.getValues())
			.add(MaterialSetTag.LEAVES.getValues())
			.add(MaterialTags.MUSHROOMS, MaterialTags.MUSHROOM_BLOCKS, MaterialTags.PUMPKINS)
			.add(Material.DEAD_BUSH, Material.CACTUS, Material.MELON, Material.VINE);

		ICE_BENDABLE = new MaterialSetTag(Bending.getKey(), MaterialSetTag.ICE.getValues());

		ALL = new MaterialSetTag(Bending.getKey(), Material.WATER);
		ALL.add(PLANT_BENDABLE, ICE_BENDABLE);

		WATER_ICE_SOURCES = new MaterialSetTag(Bending.getKey(), Material.WATER);
		WATER_ICE_SOURCES.add(ICE_BENDABLE);
	}

	public static boolean isWaterBendable(Block block) {
		return ALL.isTagged(block);
	}

	public static boolean isIceBendable(Block block) {
		return ICE_BENDABLE.isTagged(block);
	}

	public static boolean isPlantBendable(Block block) {
		return PLANT_BENDABLE.isTagged(block);
	}
}
