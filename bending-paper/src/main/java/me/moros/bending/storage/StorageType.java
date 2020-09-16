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

import java.util.Arrays;

public enum StorageType {
	// Remote databases
	MARIADB("MariaDB", "mariadb.sql"),
	MYSQL("MySQL", "mariadb.sql"),
	POSTGRESQL("PostgreSQL", "postgre.sql"),

	// Local databases
	SQLITE("SQLite", "sqlite.sql"),
	H2("H2", "h2.sql");


	private final String name;
	private final String path;

	StorageType(String name, String schemaFileName) {
		this.name = name;
		this.path = schemaFileName;
	}

	public static StorageType parse(String name, StorageType def) {
		return Arrays.stream(values()).filter(t -> name.equalsIgnoreCase(t.name)).findAny().orElse(def);
	}

	public String getSchemaPath() {
		return path;
	}

	@Override
	public String toString() {
		return name;
	}
}

