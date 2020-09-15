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

import co.aikar.idb.DB;
import co.aikar.idb.DatabaseOptions;
import co.aikar.idb.HikariPooledDatabase;
import co.aikar.idb.PooledDatabaseOptions;
import me.moros.bending.Bending;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.storage.implementation.sql.SqlStorage;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class StorageFactory {
	private static final String poolName = "bending-hikari";

	// TODO implement database redundancy
	public static Storage createInstance() {
		CommentedConfigurationNode storageNode = ConfigManager.getConfig().getNode("storage");
		String configValue = storageNode.getNode("engine").getString("H2");
		StorageType engine = StorageType.parse(configValue, StorageType.H2);
		if (!configValue.equalsIgnoreCase(engine.toString())) {
			Bending.getLog().warning("Failed to load storage type: " + engine.toString() + ". Defaulting to H2.");
		}
		Bending.getLog().info("Loading storage provider... [" + engine + "]");
		engine = StorageType.POSTGRESQL; // TODO remove this line
		switch (engine) {
			case POSTGRESQL:
			case MARIADB:
			case MYSQL:
				createHikari(engine);
				break;
			case H2:
			case SQLITE:
				createSQLite();
				break;
		}

		return new Storage(new SqlStorage(engine));
	}

	private static void createHikari(StorageType engine) {
		boolean postgre = engine == StorageType.POSTGRESQL;

		CommentedConfigurationNode storageNode = ConfigManager.getConfig().getNode("storage").getNode(engine.toString().toLowerCase());
		String host = storageNode.getNode("host").getString("localhost");
		int port = storageNode.getNode("port").getInt(postgre ? 5432 : 3306);
		String username = storageNode.getNode("username").getString("bending");
		String password = storageNode.getNode("password").getString("password");
		String database = storageNode.getNode("database").getString("bending");

		DatabaseOptions.DatabaseOptionsBuilder optionsBuilder = DatabaseOptions.builder().poolName(poolName).logger(Bending.getLog());
		PooledDatabaseOptions poolOptions;
		if (postgre) {
			Map<String, Object> properties = new HashMap<>(5);
			properties.put("serverName", host);
			properties.put("portNumber", port);
			properties.put("databaseName", database);
			properties.put("user", username);
			properties.put("password", password);

			optionsBuilder
				.dataSourceClassName("org.postgresql.ds.PGSimpleDataSource")
				.driverClassName("org.postgresql.Driver")
				.dsn("postgresql://" + host + ":" + port + "/" + database);
			poolOptions = PooledDatabaseOptions.builder().options(optionsBuilder.build())
				.dataSourceProperties(properties).build();
		} else {
			// IDB handles datasources
			optionsBuilder.mysql(username, password, database, host + ":" + port);
			poolOptions = PooledDatabaseOptions.builder().options(optionsBuilder.build()).build();
		}
		DB.setGlobalDatabase(new HikariPooledDatabase(poolOptions));
	}

	private static void createSQLite() {
		String path = Bending.getConfigFolder() + File.separator + "bending.db";
		DatabaseOptions options = DatabaseOptions.builder().poolName(poolName).logger(Bending.getLog())
			.sqlite(path).build();
		PooledDatabaseOptions poolOptions = PooledDatabaseOptions.builder().options(options).build();
		DB.setGlobalDatabase(new HikariPooledDatabase(poolOptions));
	}

	private static void createH2() {
		// TODO Implement H2 db
	}
}

