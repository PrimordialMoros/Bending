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

package me.moros.bending.storage;

import me.moros.atlas.cf.checker.nullness.qual.Nullable;
import me.moros.atlas.configurate.commented.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.config.ConfigManager;
import me.moros.storage.ConnectionBuilder;
import me.moros.storage.StorageType;

import java.io.File;

/**
 * Factory class that constructs and returns a Hikari-based database storage.
 * @see BendingStorage
 * @see StorageImpl
 */
public final class StorageFactory {
	public static @Nullable BendingStorage createInstance() {
		CommentedConfigurationNode storageNode = ConfigManager.getConfig().getNode("storage");
		String configValue = storageNode.getNode("engine").getString("h2");
		StorageType engine = StorageType.parse(configValue, StorageType.H2);
		if (!configValue.equalsIgnoreCase(engine.toString())) {
			Bending.getLog().warn("Failed to parse: " + configValue + ". Defaulting to H2.");
		}

		CommentedConfigurationNode connectionNode = storageNode.getNode("connection");
		String host = connectionNode.getNode("host").getString("localhost");
		int port = connectionNode.getNode("port").getInt(engine == StorageType.POSTGRESQL ? 5432 : 3306);
		String username = connectionNode.getNode("username").getString("bending");
		String password = connectionNode.getNode("password").getString("password");
		String database = connectionNode.getNode("database").getString("bending");

		String path = "";
		if (engine == StorageType.H2) {
			path = Bending.getConfigFolder() + File.separator + "bending-h2";
		} else if (engine == StorageType.SQLITE) {
			path = Bending.getConfigFolder() + File.separator + "bending-sqlite.db";
		}

		String poolName = engine.name() + " Bending Hikari Connection Pool";

		return ConnectionBuilder.create(StorageImpl::new, engine)
			.setPath(path).setDatabase(database).setHost(host).setPort(port)
			.setUsername(username).setPassword(password)
			.build(poolName, Bending.getLog());
	}
}

