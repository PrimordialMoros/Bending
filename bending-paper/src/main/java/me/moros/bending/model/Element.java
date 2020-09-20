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

package me.moros.bending.model;

import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

	public TextComponent getDisplayName() {
		return TextComponent.of(elementName, color);
	}

	public TextColor getColor() {
		return color;
	}

	public static Optional<Element> getElementByName(String value) {
		if (value == null || value.isEmpty()) return Optional.empty();
		return Arrays.stream(values()).filter(e -> e.name().startsWith(value.toUpperCase())).findAny();
	}

	public static List<String> getElementNames() {
		return Arrays.stream(values()).map(Element::toString).collect(Collectors.toList());
	}

	public static Set<Element> getAll() {
		return EnumSet.allOf(Element.class);
	}
}
