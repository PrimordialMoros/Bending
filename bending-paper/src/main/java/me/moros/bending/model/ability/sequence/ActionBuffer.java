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

package me.moros.bending.model.ability.sequence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

public final class ActionBuffer {
	private final AbilityAction[] elements;
	private int start = 0;
	private int end = 0;
	private boolean full = false;
	private final int maxElements;

	public ActionBuffer() {
		elements = new AbilityAction[16];
		maxElements = elements.length;
	}

	public int size() {
		int size;
		if (end < start) {
			size = maxElements - start + end;
		} else if (end == start) {
			size = full ? maxElements : 0;
		} else {
			size = end - start;
		}
		return size;
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public void clear() {
		full = false;
		start = 0;
		end = 0;
		Arrays.fill(elements, null);
	}

	public ActionBuffer add(final AbilityAction element) {
		if (element == null) throw new NullPointerException("Attempted to add null object to queue");
		if (size() == maxElements) poll();
		elements[end++] = element;
		if (end >= maxElements) end = 0;
		if (end == start) full = true;
		return this;
	}

	public AbilityAction get(final int index) {
		if (index < 0 || index >= size()) throw new NoSuchElementException();
		final int idx = (start + index) % maxElements;
		return elements[idx];
	}

	public AbilityAction peek() {
		if (isEmpty()) return null;
		return elements[start];
	}

	public AbilityAction poll() {
		if (isEmpty()) return null;
		final AbilityAction element = elements[start];
		if (element != null) {
			elements[start++] = null;
			if (start >= maxElements) start = 0;
			full = false;
		}
		return element;
	}

	public List<AbilityAction> collect() {
		List<AbilityAction> actions = new ArrayList<>(size());
		for (int i = 0; i < size(); i++) {
			actions.add(get(i));
		}
		return actions;
	}

	public boolean matches(Sequence sequence) {
		final int size = size();
		if (size < sequence.size()) return false;
		List<AbilityAction> actions = sequence.getActions();
		for (int i = 0; i < sequence.size(); i++) {
			AbilityAction first = actions.get(sequence.size() - 1 - i);
			AbilityAction second = get(size - 1 - i);
			if (!first.equals(second)) return false;
		}
		return true;
	}
}
