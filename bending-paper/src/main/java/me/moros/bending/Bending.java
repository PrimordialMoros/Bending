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

import co.aikar.commands.PaperCommandManager;
import co.aikar.commands.lib.timings.TimingManager;
import me.moros.bending.command.Commands;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.events.BendingEventBus;
import me.moros.bending.game.Game;
import me.moros.bending.listeners.BlockListener;
import me.moros.bending.listeners.PlayerListener;
import me.moros.bending.listeners.TempArmorListener;
import me.moros.bending.listeners.WorldListener;
import me.moros.bending.protection.WorldGuardFlag;
import me.moros.bending.util.Tasker;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bstats.bukkit.MetricsLite;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class Bending extends JavaPlugin {
	public static final Component PREFIX = Component.text("[", NamedTextColor.DARK_GRAY)
		.append(Component.text("Bending", NamedTextColor.DARK_AQUA))
		.append(Component.text("] ", NamedTextColor.DARK_GRAY));

	private static Bending plugin;
	private TimingManager timingManager;
	private PaperCommandManager commandManager;

	private BukkitAudiences audiences;
	private NamespacedKey key;
	private String author;
	private String version;
	private Logger log;

	private BendingEventBus eventBus;
	private Game game;

	@Override
	public void onEnable() {
		new MetricsLite(this, 8717);
		plugin = this;
		log = getLogger();
		version = getDescription().getVersion();
		author = getDescription().getAuthors().get(0);
		key = new NamespacedKey(this, "bending-core");
		eventBus = new BendingEventBus();
		audiences = BukkitAudiences.create(this);

		timingManager = TimingManager.of(this);
		Tasker.init(this);
		ConfigManager.init(getConfigFolder());
		game = new Game();

		getServer().getPluginManager().registerEvents(new PlayerListener(), this);
		getServer().getPluginManager().registerEvents(new BlockListener(), this);
		getServer().getPluginManager().registerEvents(new TempArmorListener(), this);
		getServer().getPluginManager().registerEvents(new WorldListener(), this);

		commandManager = new PaperCommandManager(this);
		commandManager.enableUnstableAPI("help");
		Commands.initialize();
	}

	@Override
	public void onDisable() {
		Game.cleanup();
		getServer().getScheduler().cancelTasks(this);
	}

	@Override
	public void onLoad() {
		if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
			WorldGuardFlag.registerFlag();
		}
	}

	public static BendingEventBus getEventBus() {
		return getPlugin().eventBus;
	}

	public static TimingManager getTimingManager() {
		return getPlugin().timingManager;
	}

	public static PaperCommandManager getCommandManager() {
		return getPlugin().commandManager;
	}

	public static Bending getPlugin() {
		return plugin;
	}

	public static BukkitAudiences getAudiences() {
		return getPlugin().audiences;
	}

	public static String getAuthor() {
		return getPlugin().author;
	}

	public static String getVersion() {
		return getPlugin().version;
	}

	public static Logger getLog() {
		return getPlugin().log;
	}

	public static NamespacedKey getKey() {
		return getPlugin().key;
	}

	public static Game getGame() {
		return getPlugin().game;
	}

	public static String getConfigFolder() {
		return getPlugin().getDataFolder().toString();
	}
}
