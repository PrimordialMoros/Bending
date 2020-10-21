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
import me.moros.bending.util.logging.PluginLogger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;

public class ConnectionBuilder<T extends Storage> {
	private static final Set<String> poolNames = new HashSet<>();

	private final BiFunction<StorageType, HikariDataSource, T> constructor;
	private final StorageType engine;
	private PluginLogger logger;
	private String path = "";
	private String host = "localhost";
	private String database = "";
	private String username = "";
	private String password = "";
	private int port;

	private ConnectionBuilder(BiFunction<StorageType, HikariDataSource, T> constructor, StorageType engine) {
		this.constructor = constructor;
		this.engine = engine;
		this.logger = Bending.getLog();
	}

	public ConnectionBuilder<T> setLogger(@NonNull PluginLogger logger) {
		this.logger = logger;
		return this;
	}

	public @NonNull ConnectionBuilder<T> setPath(@NonNull String path) {
		this.path = path;
		return this;
	}

	public @NonNull ConnectionBuilder<T> setHost(@NonNull String host) {
		this.host = host;
		return this;
	}

	public @NonNull ConnectionBuilder<T> setDatabase(@NonNull String database) {
		this.database = database;
		return this;
	}

	public @NonNull ConnectionBuilder<T> setUsername(@NonNull String username) {
		this.username = username;
		return this;
	}

	public @NonNull ConnectionBuilder<T> setPassword(@NonNull String password) {
		this.password = password;
		return this;
	}

	public @NonNull ConnectionBuilder<T> setPort(int port) {
		this.port = port;
		return this;
	}

	public @Nullable T build(@NonNull String poolName) {
		if (poolNames.contains(poolName)) {
			logger.warn(poolName + " is already registered!");
			return null;
		}
		if (host.isEmpty() || database.isEmpty() || username.isEmpty() || password.isEmpty()) {
			logger.warn("Connection info is invalid! One or more values is empty!");
			return null;
		}
		if ((engine == StorageType.H2 || engine == StorageType.SQLITE) && path.isEmpty()) {
			logger.warn("Connection path is missing!");
			return null;
		}

		logger.info("Loading storage provider... [" + engine + "]");

		HikariConfig config = new HikariConfig();
		config.setPoolName(poolName);
		config.setMaximumPoolSize(5);
		config.setMinimumIdle(3);
		config.addDataSourceProperty("serverName", host);
		config.addDataSourceProperty("portNumber", port);
		config.addDataSourceProperty("databaseName", database);
		config.addDataSourceProperty("user", username);
		config.addDataSourceProperty("password", password);

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
				config.setDriverClassName("org.h2.Driver");
				config.setJdbcUrl("jdbc:h2:./" + path + ";MODE=PostgreSQL");
				break;
			case SQLITE:
				config.setDriverClassName("org.sqlite.JDBC");
				config.setJdbcUrl("jdbc:sqlite:" + path + "?autoReconnect=true");
				break;
		}

		T storage = constructor.apply(engine, new HikariDataSource(config));
		if (storage.init(logger)) {
			poolNames.add(poolName);
			return storage;
		}
		return null;
	}

	/**
	 * Constructs a new connection info builder
	 */
	public static @NonNull <T extends Storage> ConnectionBuilder<T> create(@NonNull BiFunction<StorageType, HikariDataSource, @NonNull T> constructor, @NonNull StorageType engine) {
		return new ConnectionBuilder<>(constructor, engine);
	}
}
