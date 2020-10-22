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

package me.moros.bending;

import me.moros.atlas.acf.PaperCommandManager;
import me.moros.atlas.acf.lib.timings.TimingManager;
import me.moros.atlas.kyori.adventure.platform.bukkit.BukkitAudiences;
import me.moros.bending.command.Commands;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.events.BendingEventBus;
import me.moros.bending.game.Game;
import me.moros.bending.listener.BlockListener;
import me.moros.bending.listener.UserListener;
import me.moros.bending.listener.WorldListener;
import me.moros.bending.protection.WorldGuardFlag;
import me.moros.bending.storage.BendingStorage;
import me.moros.bending.storage.StorageFactory;
import me.moros.bending.util.Tasker;
import me.moros.storage.logging.Logger;
import me.moros.storage.logging.Slf4jLogger;
import org.bstats.bukkit.MetricsLite;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class Bending extends JavaPlugin {
	private static Bending plugin;
	private TimingManager timingManager;
	private PaperCommandManager commandManager;

	private BukkitAudiences audiences;
	private NamespacedKey key;
	private String author;
	private String version;
	private Logger logger;

	private BendingEventBus eventBus;
	private Game game;

	@Override
	public void onEnable() {
		new MetricsLite(this, 8717);
		plugin = this;
		logger = new Slf4jLogger(LoggerFactory.getLogger(getClass()));
		version = getDescription().getVersion();
		author = getDescription().getAuthors().get(0);
		key = new NamespacedKey(this, "bending-core");
		eventBus = new BendingEventBus();
		audiences = BukkitAudiences.create(this);

		timingManager = TimingManager.of(this);
		Tasker.init(this);
		ConfigManager.init(getConfigFolder());

		BendingStorage storage = Objects.requireNonNull(StorageFactory.createInstance(), "Unable to connect to database!");
		game = new Game(storage);

		getServer().getPluginManager().registerEvents(new UserListener(game), this);
		getServer().getPluginManager().registerEvents(new BlockListener(game), this);
		getServer().getPluginManager().registerEvents(new WorldListener(game), this);

		commandManager = new PaperCommandManager(this);
		commandManager.enableUnstableAPI("help");
		Commands.initialize();
	}

	@Override
	public void onDisable() {
		if (game != null) game.cleanup();
		getServer().getScheduler().cancelTasks(this);
	}

	@Override
	public void onLoad() {
		if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
			WorldGuardFlag.registerFlag();
		}
	}

	public static BendingEventBus getEventBus() {
		return plugin.eventBus;
	}

	public static TimingManager getTimingManager() {
		return plugin.timingManager;
	}

	public static PaperCommandManager getCommandManager() {
		return plugin.commandManager;
	}

	public static Bending getPlugin() {
		return plugin;
	}

	public static BukkitAudiences getAudiences() {
		return plugin.audiences;
	}

	public static String getAuthor() {
		return plugin.author;
	}

	public static String getVersion() {
		return plugin.version;
	}

	public static Logger getLog() {
		return plugin.logger;
	}

	public static NamespacedKey getKey() {
		return plugin.key;
	}

	public static Game getGame() {
		return plugin.game;
	}

	public static String getConfigFolder() {
		return plugin.getDataFolder().toString();
	}
}
