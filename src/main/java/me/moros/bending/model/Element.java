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

package me.moros.bending.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

/**
 * An immutable and thread-safe object that represents a bending element
 */
public enum Element {
	AIR("Air", NamedTextColor.GRAY),
	WATER("Water", NamedTextColor.AQUA),
	EARTH("Earth", NamedTextColor.GREEN),
	FIRE("Fire", NamedTextColor.RED);

	private final String elementName;
	private final TextColor color;

	Element(String elementName, TextColor color) {
		this.elementName = elementName;
		this.color = color;
	}

	@Override
	public String toString() {
		return elementName;
	}

	public @NonNull Component getDisplayName() {
		return Component.text(elementName, color);
	}

	public @NonNull TextColor getColor() {
		return color;
	}

	public static Optional<Element> getElementByName(@NonNull String value) {
		if (value.isEmpty()) return Optional.empty();
		return getAll().stream().filter(e -> e.name().startsWith(value.toUpperCase())).findAny();
	}

	public static Collection<String> getElementNames() {
		return Arrays.asList("Air", "Water", "Earth", "Fire");
	}

	public static @NonNull Set<@NonNull Element> getAll() {
		return EnumSet.allOf(Element.class);
	}
}
