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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.moros.bending.Bending;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.storage.implementation.sql.SqlStorage;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.jdbi.v3.core.Jdbi;

import java.io.File;

public class StorageFactory {
	private static Jdbi jdbi;
	private static HikariDataSource dataSource;

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
		createHikari(engine);
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

		HikariConfig config = new HikariConfig();
		config.setMaximumPoolSize(5);
		config.setMinimumIdle(3);
		config.setPoolName(engine.name() + " Bending Hikari Connection Pool");
		config.addDataSourceProperty("serverName", host);
		config.addDataSourceProperty("portNumber", port);
		config.addDataSourceProperty("databaseName", database);
		config.addDataSourceProperty("user", username);
		config.addDataSourceProperty("password", password);

		String path = Bending.getConfigFolder() + File.separator + "bending.db";

		switch (engine) {
			case POSTGRESQL:
				config.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
				break;
			case MARIADB:
				config.setDataSourceClassName("org.mariadb.jdbc.MariaDbDataSource");
				break;
			case MYSQL:
				config.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
				break;
			case H2:
				config.setDataSourceClassName("org.h2.Driver");
				config.setJdbcUrl("jdbc:h2:" + path);
				//TODO add H2
				break;
			case SQLITE:
				config.setJdbcUrl("jdbc:sqlite:" + path);
				break;
		}
		dataSource = new HikariDataSource(config);
		jdbi = Jdbi.create(dataSource);
	}

	public static Jdbi getJdbi() {
		return jdbi;
	}

	public static HikariDataSource getHikari() {
		return dataSource;
	}
}

