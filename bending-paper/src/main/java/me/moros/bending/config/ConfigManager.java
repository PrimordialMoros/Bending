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

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ConfigManager {
	private static HoconConfigurationLoader loader;
	private static CommentedConfigurationNode configRoot;

	private static final List<Configurable> instances = new ArrayList<>();

	public static void init(String directory) {
		Path path = Paths.get(directory, "bending.conf");
		URL url = Objects.requireNonNull(Bending.class.getClassLoader().getResource("default.conf"));
		loader = HoconConfigurationLoader.builder()
			.setDefaultOptions(o -> o.withShouldCopyDefaults(true)).setPath(path).build();
		try {
			Files.createDirectories(path.getParent());
			configRoot = loader.load().mergeValuesFrom(HoconConfigurationLoader.builder().setURL(url).build().load());
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
		save();
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
	}

	public static CommentedConfigurationNode getConfig() {
		return configRoot;
	}

	public static void add(Configurable c) {
		instances.add(c);
	}
}
