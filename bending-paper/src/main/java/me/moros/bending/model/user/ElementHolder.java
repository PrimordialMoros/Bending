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

package me.moros.bending.model.user;

import me.moros.bending.model.Element;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public final class ElementHolder {
	private final Set<Element> elements;

	protected ElementHolder() {
		elements = EnumSet.noneOf(Element.class);
	}

	protected Set<Element> getElements() {
		return Collections.unmodifiableSet(elements);
	}

	protected boolean hasElement(Element element) {
		return elements.contains(element);
	}

	protected boolean addElement(Element element) {
		Objects.requireNonNull(element);
		return elements.add(element);
	}

	protected boolean removeElement(Element element) {
		Objects.requireNonNull(element);
		return elements.remove(element);
	}

	protected void clear() {
		elements.clear();
	}
}
