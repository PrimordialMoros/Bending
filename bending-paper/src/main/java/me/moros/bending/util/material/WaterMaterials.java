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

public class WaterMaterials {
	// TODO complete and validate, make them configurable
	public static final MaterialSetTag PLANT_BENDABLE;
	public static final MaterialSetTag ICE_BENDABLE;

	static {
		PLANT_BENDABLE = new MaterialSetTag(Bending.getKey(), MaterialSetTag.FLOWERS.getValues());
		PLANT_BENDABLE.add(MaterialSetTag.SAPLINGS.getValues())
			.add(MaterialSetTag.CROPS.getValues())
			.add(MaterialSetTag.LEAVES.getValues())
			.add(MaterialTags.MUSHROOMS, MaterialTags.MUSHROOM_BLOCKS, MaterialTags.PUMPKINS)
			.add(Material.DEAD_BUSH, Material.CACTUS, Material.MELON, Material.VINE);

		ICE_BENDABLE = new MaterialSetTag(Bending.getKey(), MaterialSetTag.ICE.getValues());
	}
}
