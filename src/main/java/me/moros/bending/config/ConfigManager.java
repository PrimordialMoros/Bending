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

package me.moros.bending.config;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.commented.CommentedConfigurationNode;
import me.moros.atlas.configurate.hocon.HoconConfigurationLoader;
import me.moros.bending.Bending;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

public final class ConfigManager {
	private static HoconConfigurationLoader loader;
	private static CommentedConfigurationNode configRoot;

	private static final Collection<Configurable> instances = new ArrayList<>();

	public static void init(@NonNull String directory) {
		Path path = Paths.get(directory, "bending.conf");
		loader = HoconConfigurationLoader.builder()
			.setDefaultOptions(o -> o.withShouldCopyDefaults(true)).setPath(path).build();
		try {
			Files.createDirectories(path.getParent());
			configRoot = loader.load();
		} catch (IOException e) {
			Bending.getLog().warn(e.getMessage());
		}
	}

	public static void reload() {
		try {
			configRoot = loader.load();
			instances.forEach(Configurable::reload);
		} catch (IOException e) {
			Bending.getLog().warn(e.getMessage());
		}
	}

	public static void save() {
		try {
			Bending.getLog().info("Saving bending config");
			loader.save(configRoot);
		} catch (IOException e) {
			Bending.getLog().warn(e.getMessage());
		}
	}

	public static @NonNull CommentedConfigurationNode getConfig() {
		return configRoot;
	}

	public static void add(@NonNull Configurable c) {
		instances.add(c);
	}
}
