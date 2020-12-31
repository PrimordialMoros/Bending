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

package me.moros.bending.util;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.cf.checker.nullness.qual.Nullable;
import me.moros.atlas.kyori.adventure.audience.Audience;
import me.moros.atlas.kyori.adventure.bossbar.BossBar;
import me.moros.atlas.kyori.adventure.text.Component;
import me.moros.bending.Bending;
import me.moros.bending.model.ability.util.ActionType;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MovementHandler {
	public static Map<LivingEntity, MovementHandler> instances = new HashMap<>();

	private Set<ActionType> disabled;
	private BarInfo info;

	private final LivingEntity entity;
	private final boolean hadAI;

	private MovementHandler(@NonNull LivingEntity entity, long duration) {
		this.entity = entity;
		hadAI = entity.hasAI();
		if (entity instanceof Player) {
			info = new BarInfo((Player) entity, duration);
		} else {
			entity.setAI(false);
		}
		entity.setMetadata(Metadata.NO_MOVEMENT, Metadata.customMetadata(this));
		Tasker.newChain().delay((int) duration, TimeUnit.MILLISECONDS).sync(this::reset).execute();
	}

	private void reset() {
		resetWithoutRemoving();
		instances.remove(entity);
	}

	private void resetWithoutRemoving() {
		if (info != null) info.remove();
		if (!(entity instanceof Player)) entity.setAI(hadAI);
		if (entity.hasMetadata(Metadata.NO_MOVEMENT)) {
			entity.removeMetadata(Metadata.NO_MOVEMENT, Bending.getPlugin());
		}
	}

	public @NonNull MovementHandler disableActions(@NonNull Collection<ActionType> methods) {
		disabled = EnumSet.copyOf(methods);
		return this;
	}

	public @NonNull MovementHandler disableActions(@NonNull ActionType method, @Nullable ActionType @NonNull ... methods) {
		Collection<ActionType> c = new ArrayList<>();
		c.add(method);
		if (methods != null) c.addAll(Arrays.asList(methods));
		return disableActions(c);
	}

	/**
	 * This stops the movement of the entity once they land on the ground,
	 * acting as a "paralyze" with a duration for how long they should be
	 * stopped
	 */
	public static @NonNull MovementHandler restrictEntity(@NonNull LivingEntity entity, long duration) {
		return instances.computeIfAbsent(entity, e -> new MovementHandler(e, duration));
	}

	public static boolean isRestricted(@NonNull Entity entity) {
		return isRestricted(entity, null);
	}

	public static boolean isRestricted(@NonNull Entity entity, @Nullable ActionType method) {
		if (entity.hasMetadata(Metadata.NO_MOVEMENT)) {
			if (method == null) return true;
			MovementHandler handler = (MovementHandler) entity.getMetadata(Metadata.NO_MOVEMENT).get(0).value();
			if (handler != null) return handler.disabled.contains(method);
		}
		return false;
	}

	/**
	 * Resets all instances of MovementHandler
	 */
	public static void resetAll() {
		instances.values().forEach(MovementHandler::resetWithoutRemoving);
		instances.clear();
	}

	private static class BarInfo {
		private final Audience audience;
		private final BossBar bar;
		private final BukkitTask barTask;

		private final long endTime;
		private final long duration;

		private BarInfo(Player player, long duration) {
			this.duration = duration;
			audience = Bending.getAudiences().player(player);
			endTime = System.currentTimeMillis() + duration;
			Component name = Component.text("Restricted");
			bar = BossBar.bossBar(name, 1, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
			barTask = Tasker.createTaskTimer(this::updateBar, 0, 1);
		}

		private void updateBar() {
			long time = System.currentTimeMillis();
			if (time > endTime) {
				remove();
			} else {
				float factor = FastMath.max(0, FastMath.min(1, (endTime - time) / (float) duration));
				audience.showBossBar(bar.percent(factor));
			}
		}

		private void remove() {
			audience.hideBossBar(bar);
			barTask.cancel();
		}
	}
}
