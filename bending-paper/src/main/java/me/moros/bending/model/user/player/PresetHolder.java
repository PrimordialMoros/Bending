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

package me.moros.bending.model.user.player;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.moros.bending.game.Game;
import me.moros.bending.model.preset.Preset;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public final class PresetHolder {
	private final AsyncLoadingCache<String, Preset> presetCache = Caffeine.newBuilder()
		.maximumSize(8) // Average player will probably have 2-5 presets, this should be enough
		.buildAsync(this::loadPreset);

	private final Set<String> presets;
	private final int id;

	protected PresetHolder(int id, Set<String> presets) {
		this.id = id;
		this.presets = presets;
	}

	protected Set<String> getPresets() {
		return Collections.unmodifiableSet(presets);
	}

	protected boolean hasPreset(String name) {
		return presets.contains(name);
	}

	protected Preset getPresetByName(String name) {
		if (!hasPreset(name)) return null;
		return presetCache.synchronous().get(name);
	}

	protected boolean addPreset(String name) {
		Objects.requireNonNull(name);
		return presets.add(name);
	}

	protected boolean removePreset(String name) {
		Objects.requireNonNull(name);
		return presets.remove(name);
	}

	private Preset loadPreset(String name) {
		if (!hasPreset(name)) return null;
		return Game.getStorage().loadPreset(id, name);
	}
}
