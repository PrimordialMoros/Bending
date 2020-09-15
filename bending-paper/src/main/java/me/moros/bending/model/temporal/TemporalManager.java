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

package me.moros.bending.model.temporal;

import me.moros.bending.util.Tasker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;

public class TemporalManager<K, V extends Temporary> {
	private final Queue<V> QUEUE = new PriorityQueue<>(100, Comparator.comparingLong(V::getRevertTime));
	private final Map<K, V> instances = new HashMap<>();

	public TemporalManager() {
		Tasker.createTaskTimer(() -> {
			final long currentTime = System.currentTimeMillis();
			while (!QUEUE.isEmpty()) {
				final V temp = QUEUE.peek();
				if (currentTime > temp.getRevertTime()) {
					QUEUE.poll();
					temp.revert();
				} else {
					return;
				}
			}
		}, 1, 1);
	}

	public boolean isTemp(K key) {
		return instances.containsKey(key);
	}

	public Optional<V> get(K key) {
		return Optional.ofNullable(instances.get(key));
	}

	public void addEntry(K key, V value) {
		instances.put(key, value);
	}

	/**
	 * Only use this inside Temporary::revert
	 *
	 * @param key the key that marks the entry to remove
	 */
	public void removeEntry(K key) {
		instances.remove(key);
	}

	public void enqueue(V value) {
		QUEUE.add(value);
	}

	public void removeAll() {
		QUEUE.clear();
		List<V> list = new ArrayList<>(instances.values());
		list.forEach(V::revert);
		instances.clear();
	}
}
