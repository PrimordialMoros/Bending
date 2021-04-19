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

package me.moros.bending.model.temporal;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.cf.checker.nullness.qual.Nullable;
import me.moros.atlas.expiringmap.ExpirationPolicy;
import me.moros.atlas.expiringmap.ExpiringMap;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class TemporalManager<K, V extends Temporary> {
	private final ExpiringMap<K, V> instances;

	public TemporalManager() {
		instances = ExpiringMap.builder().variableExpiration().build();
		instances.addExpirationListener((key, value) -> value.revert());
	}

	public boolean isTemp(@Nullable K key) {
		if (key == null) return false;
		return instances.containsKey(key);
	}

	public Optional<V> get(@NonNull K key) {
		return Optional.ofNullable(instances.get(key));
	}

	public void addEntry(@NonNull K key, @NonNull V value, long duration) {
		if (duration <= 0) duration = Temporary.DEFAULT_REVERT;
		if (instances.containsKey(key)) {
			instances.setExpiration(key, duration, TimeUnit.MILLISECONDS);
			return;
		}
		instances.put(key, value, ExpirationPolicy.CREATED, duration, TimeUnit.MILLISECONDS);
	}

	/**
	 * This is used inside {@link Temporary#revert}
	 * @param key the key of the entry to remove
	 */
	public void removeEntry(@NonNull K key) {
		instances.remove(key);
	}

	public void removeAll() {
		instances.values().forEach(Temporary::revert);
		instances.clear();
	}

	protected Collection<V> getInstances() {
		return instances.values();
	}
}
