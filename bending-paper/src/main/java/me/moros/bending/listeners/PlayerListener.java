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

package me.moros.bending.listeners;

import co.aikar.commands.lib.timings.MCTiming;
import me.moros.bending.Bending;
import me.moros.bending.events.CooldownAddEvent;
import me.moros.bending.events.CooldownRemoveEvent;
import me.moros.bending.events.ElementChangeEvent;
import me.moros.bending.game.Game;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.player.BendingPlayer;
import me.moros.bending.model.user.player.BendingProfile;
import me.moros.bending.util.Tasker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;

import java.util.Optional;
import java.util.UUID;

public class PlayerListener implements Listener {
	@EventHandler
	public void onPrePlayerJoin(AsyncPlayerPreLoginEvent event) {
		Tasker.newChain().delay(3 * 20).async(event::allow); // After 3 seconds allow the player to login anyway
		MCTiming timing = Bending.getTimingManager().ofStart("BendingProfile on pre-login");
		if (!Game.getPlayerManager().getProfile(event.getUniqueId()).isPresent()) {
			Bending.getLog().severe("Could not create bending profile for: " + event.getUniqueId() + " (" + event.getName() + ")");
		}
		event.allow();
		timing.stopTiming();
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		String name = player.getName();
		MCTiming timing = Bending.getTimingManager().ofStart("BendingProfile on join");
		Optional<BendingProfile> profile = Game.getPlayerManager().getProfile(uuid);
		if (profile.isPresent()) {
			Game.getPlayerManager().createPlayer(player, profile.get());
		} else {
			Bending.getLog().severe("Could not create bending profile for: " + uuid + " (" + name + ")");
		}
		timing.stopTiming();
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerLogout(PlayerQuitEvent event) {
		Game.getActivationController().onPlayerLogout(Game.getPlayerManager().getPlayer(event.getPlayer().getUniqueId()));
	}

	@EventHandler(ignoreCancelled = true)
	public void onFallDamage(EntityDamageEvent event) {
		if (event.getCause() != DamageCause.FALL || !(event.getEntity() instanceof Player)) return;
		Player player = (Player) event.getEntity();
		if (!Game.getActivationController().onFallDamage(Game.getPlayerManager().getPlayer(player.getUniqueId()))) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onFireTickDamage(EntityDamageEvent event) {
		if (event.getCause() != DamageCause.FIRE && event.getCause() != DamageCause.FIRE_TICK) return;
		if (!(event.getEntity() instanceof Player)) return;
		Player player = (Player) event.getEntity();
		if (!Game.getActivationController().onFireTickDamage(Game.getPlayerManager().getPlayer(player.getUniqueId()))) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent event) {
		final Vector velocity = event.getTo().clone().subtract(event.getFrom()).toVector();
		Game.getActivationController().onUserMove(Game.getPlayerManager().getPlayer(event.getPlayer().getUniqueId()), velocity);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getHand() != EquipmentSlot.HAND) return;
		BendingPlayer player = Game.getPlayerManager().getPlayer(event.getPlayer().getUniqueId());
		switch (event.getAction()) {
			case RIGHT_CLICK_AIR:
				Game.getActivationController().onUserInteract(player, ActivationMethod.INTERACT);
				break;
			case RIGHT_CLICK_BLOCK:
				Game.getActivationController().onUserInteract(player, ActivationMethod.INTERACT_BLOCK);
				break;
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayDropItem(PlayerDropItemEvent event) {
		Game.getActivationController().ignoreNextSwing(Game.getPlayerManager().getPlayer(event.getPlayer().getUniqueId()));
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
		if (event.getHand() != EquipmentSlot.HAND) return;
		Game.getActivationController().onUserInteract(Game.getPlayerManager().getPlayer(event.getPlayer().getUniqueId()), ActivationMethod.INTERACT_ENTITY);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
		Game.getActivationController().onUserSneak(Game.getPlayerManager().getPlayer(event.getPlayer().getUniqueId()), event.isSneaking());
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerSwing(PlayerAnimationEvent event) {
		Game.getActivationController().onUserSwing(Game.getPlayerManager().getPlayer(event.getPlayer().getUniqueId()));
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
		Game.getBoardManager().forceToggleScoreboard(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerSlotChange(final PlayerItemHeldEvent event) {
		Game.getBoardManager().changeActiveSlot(event.getPlayer(), event.getPreviousSlot(), event.getNewSlot());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onCooldownAdd(CooldownAddEvent event) {
		if (event.getUser() instanceof BendingPlayer) {
			AbilityDescription desc = event.getAbilityDescription();
			Game.getBoardManager().updateBoardSlot((Player) event.getUser().getEntity(), desc, true);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onCooldownRemove(CooldownRemoveEvent event) {
		if (event.getUser() instanceof BendingPlayer) {
			AbilityDescription desc = event.getAbilityDescription();
			Game.getBoardManager().updateBoardSlot((Player) event.getUser().getEntity(), desc, false);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onElementChange(ElementChangeEvent event) {
		if (event.getUser() instanceof BendingPlayer && event.getResult() != ElementChangeEvent.Result.ADD) {
			Game.getBoardManager().updateBoard((Player) event.getUser().getEntity());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onWorldChange(PlayerChangedWorldEvent event) {
		Game.getAbilityManager(event.getFrom()).destroyUserInstances(Game.getPlayerManager().getPlayer(event.getPlayer().getUniqueId()));
	}
}
