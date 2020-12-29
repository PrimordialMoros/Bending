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

package me.moros.bending.util;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.cf.checker.nullness.qual.Nullable;
import me.moros.bending.Bending;
import org.bukkit.metadata.FixedMetadataValue;

/**
 * Utility class to provide and construct metadata for the {@link Bending} plugin.
 * @see FixedMetadataValue
 */
public final class Metadata {
	public static final String NO_INTERACT = "bending-no-interact";
	public static final String NO_PICKUP = "bending-no-pickup";
	public static final String GLOVE_KEY = "bending-earth-glove";
	public static final String FALLING_BLOCK = "bending-falling-block";
	public static final String METAL_CABLE = "bending-metal-cable";
	public static final String DESTRUCTIBLE = "bending-destructible";

	public static FixedMetadataValue emptyMetadata() {
		return new FixedMetadataValue(Bending.getPlugin(), "");
	}

	public static @NonNull FixedMetadataValue customMetadata(@Nullable Object obj) {
		return new FixedMetadataValue(Bending.getPlugin(), obj);
	}
}
