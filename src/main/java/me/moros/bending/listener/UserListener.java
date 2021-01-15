/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
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

package me.moros.bending.listener;

import me.moros.atlas.acf.lib.timings.MCTiming;
import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.Bending;
import me.moros.bending.events.CooldownAddEvent;
import me.moros.bending.events.CooldownRemoveEvent;
import me.moros.bending.events.ElementChangeEvent;
import me.moros.bending.game.Game;
import me.moros.bending.game.temporal.TempArmor;
import me.moros.bending.model.ability.util.ActionType;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.BendingUser;
import me.moros.bending.model.user.profile.BendingProfile;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.Metadata;
import me.moros.bending.util.MovementHandler;
import me.moros.bending.util.Tasker;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.Optional;
import java.util.UUID;

public class UserListener implements Listener {
	private final Game game;

	public UserListener(@NonNull Game game) {
		this.game = game;
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onPrePlayerJoin(AsyncPlayerPreLoginEvent event) {
		Tasker.newChain().delay(3 * 20).async(event::allow).execute(); // After 3 seconds allow the player to login anyway
		MCTiming timing = Bending.getTimingManager().ofStart("BendingProfile on pre-login");
		if (!game.getPlayerManager().getProfile(event.getUniqueId()).isPresent()) {
			Bending.getLog().severe("Could not create bending profile for: " + event.getUniqueId() + " (" + event.getName() + ")");
		}
		event.allow();
		timing.stopTiming();
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		String name = player.getName();
		MCTiming timing = Bending.getTimingManager().ofStart("BendingProfile on join");
		Optional<BendingProfile> profile = game.getPlayerManager().getProfile(uuid);
		if (profile.isPresent()) {
			game.getPlayerManager().createPlayer(player, profile.get());
		} else {
			Bending.getLog().severe("Could not create bending profile for: " + uuid + " (" + name + ")");
		}
		timing.stopTiming();
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerLogout(PlayerQuitEvent event) {
		game.getActivationController().onPlayerLogout(game.getPlayerManager().getPlayer(event.getPlayer().getUniqueId()));
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onEntityDeath(EntityDeathEvent event) {
		event.getDrops().removeIf(item -> Bending.getLayer().hasArmorKey(item.getItemMeta()));
		boolean keepInventory = (event instanceof PlayerDeathEvent) && ((PlayerDeathEvent) event).getKeepInventory();
		TempArmor.manager.get(event.getEntity()).ifPresent(tempArmor -> {
			if (!keepInventory) event.getDrops().addAll(tempArmor.getSnapshot());
			tempArmor.revert();
		});
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerDeath(PlayerDeathEvent event) {
		if (DamageUtil.handleBendingDeath(event.getEntity())) {
			event.setDeathMessage(null);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent event) {
		ItemStack item = event.getCurrentItem();
		if (item == null || !(event.getClickedInventory() instanceof PlayerInventory)) {
			return;
		}

		ItemMeta meta = item.getItemMeta();
		if (meta != null && Bending.getLayer().hasArmorKey(meta)) {
			PlayerInventory inventory = (PlayerInventory) event.getClickedInventory();
			if (inventory.getHolder() instanceof Player) {
				Player player = ((Player) inventory.getHolder()).getPlayer();
				if (!TempArmor.manager.isTemp(player) || event.getSlotType() != InventoryType.SlotType.ARMOR) {
					inventory.remove(item);
				}
			}
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onFallDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof LivingEntity)) return;
		if (event.getCause() == DamageCause.FALL) {
			Optional<BendingUser> user = game.getBenderRegistry().getBendingUser((LivingEntity) event.getEntity());
			if (user.isPresent() && !game.getActivationController().onFallDamage(user.get())) {
				event.setCancelled(true);
			}
		} else if (event.getCause() == DamageCause.FIRE || event.getCause() == DamageCause.FIRE_TICK) {
			Optional<BendingUser> user = game.getBenderRegistry().getBendingUser((LivingEntity) event.getEntity());
			if (user.isPresent() && !game.getActivationController().onFireTickDamage(user.get())) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent event) {
		Location from = event.getFrom();
		Location to = event.getTo();
		if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
			return;
		}
		if (MovementHandler.isRestricted(event.getPlayer(), ActionType.MOVE)) {
			event.setCancelled(true);
			return;
		}

		final Vector velocity = to.toVector().subtract(from.toVector());
		game.getActivationController().onUserMove(game.getPlayerManager().getPlayer(event.getPlayer().getUniqueId()), velocity);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onPlayerInteractLow(PlayerInteractEvent event) {
		if (MovementHandler.isRestricted(event.getPlayer(), ActionType.INTERACT)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onPlayerInteractEntityLow(PlayerInteractEntityEvent event) {
		if (event.getRightClicked().hasMetadata(Metadata.NO_INTERACT)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getHand() != EquipmentSlot.HAND) return;
		BendingPlayer player = game.getPlayerManager().getPlayer(event.getPlayer().getUniqueId());
		switch (event.getAction()) {
			case RIGHT_CLICK_AIR:
				game.getActivationController().onUserInteract(player, ActivationMethod.INTERACT);
				break;
			case RIGHT_CLICK_BLOCK:
				game.getActivationController().onUserInteract(player, ActivationMethod.INTERACT_BLOCK);
				break;
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayDropItem(PlayerDropItemEvent event) {
		game.getActivationController().ignoreNextSwing(game.getPlayerManager().getPlayer(event.getPlayer().getUniqueId()));
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
		if (event.getHand() != EquipmentSlot.HAND) return;
		game.getActivationController().onUserInteract(game.getPlayerManager().getPlayer(event.getPlayer().getUniqueId()), ActivationMethod.INTERACT_ENTITY);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
		game.getActivationController().onUserSneak(game.getPlayerManager().getPlayer(event.getPlayer().getUniqueId()), event.isSneaking());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerSwing(PlayerAnimationEvent event) {
		game.getActivationController().onUserSwing(game.getPlayerManager().getPlayer(event.getPlayer().getUniqueId()));
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
		game.getBoardManager().forceToggleScoreboard(event.getPlayer());
		game.getAbilityManager(event.getFrom()).destroyUserInstances(game.getPlayerManager().getPlayer(event.getPlayer().getUniqueId()));
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerSlotChange(PlayerItemHeldEvent event) {
		game.getBoardManager().changeActiveSlot(event.getPlayer(), event.getPreviousSlot(), event.getNewSlot());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onCooldownAdd(CooldownAddEvent event) {
		if (event.getUser() instanceof BendingPlayer) {
			game.getBoardManager().updateBoardSlot((Player) event.getUser().getEntity(), event.getAbility(), true);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onCooldownRemove(CooldownRemoveEvent event) {
		if (event.getUser() instanceof BendingPlayer) {
			game.getBoardManager().updateBoardSlot((Player) event.getUser().getEntity(), event.getAbility(), false);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onElementChange(ElementChangeEvent event) {
		if (event.getUser() instanceof BendingPlayer && event.getResult() != ElementChangeEvent.Result.ADD) {
			game.getBoardManager().updateBoard((Player) event.getUser().getEntity());
		}
	}
}
