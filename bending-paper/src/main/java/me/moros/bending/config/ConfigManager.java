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


import me.moros.bending.Bending;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class ConfigManager {
	private static ConfigurationLoader<CommentedConfigurationNode> loader;
	private static CommentedConfigurationNode configRoot;

	private static final List<Configurable> instances = new ArrayList<>();

	public static void init(String directory) {
		Path path = Paths.get(directory, "bending.conf");
		loader = HoconConfigurationLoader.builder()
			.setDefaultOptions(o -> o.withShouldCopyDefaults(true))
			.setPath(path)
			.build();
		try (InputStream is = Bending.getPlugin().getResource("default.conf")) {
			Files.createDirectories(path.getParent());
			if (is != null && Files.notExists(path)) Files.copy(is, path);
			configRoot = loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void load() {
		try {
			configRoot = loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void reload() {
		load();
		instances.forEach(Configurable::reload);
	}

	public static void save() {
		try {
			Bending.getLog().info("Saving bending config");
			loader.save(configRoot);
		} catch (IOException e) {
			e.printStackTrace();
		}
		reload();
	}

	public static CommentedConfigurationNode getConfig() {
		return configRoot;
	}

	public static void add(Configurable c) {
		instances.add(c);
	}
}
