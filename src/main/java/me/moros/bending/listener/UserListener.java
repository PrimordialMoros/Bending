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

import java.util.Optional;
import java.util.UUID;

import me.moros.atlas.acf.lib.timings.MCTiming;
import me.moros.bending.Bending;
import me.moros.bending.ability.fire.FireShield;
import me.moros.bending.events.BendingDamageEvent;
import me.moros.bending.events.CooldownAddEvent;
import me.moros.bending.events.CooldownRemoveEvent;
import me.moros.bending.events.ElementChangeEvent;
import me.moros.bending.game.Game;
import me.moros.bending.game.temporal.TempArmor;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActionType;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import me.moros.bending.model.user.profile.BendingProfile;
import me.moros.bending.util.Metadata;
import me.moros.bending.util.MovementHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
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
import org.checkerframework.checker.nullness.qual.NonNull;

public class UserListener implements Listener {
  private final Game game;

  public UserListener(@NonNull Game game) {
    this.game = game;
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onPrePlayerJoin(AsyncPlayerPreLoginEvent event) {
    MCTiming timing = Bending.timingManager().ofStart("BendingProfile on pre-login");
    UUID uuid = event.getUniqueId();
    long startTime = System.currentTimeMillis();
    game.playerManager().profile(uuid);
    long time = System.currentTimeMillis() - startTime;
    if (time >= 1000) {
      Bending.logger().warn("Processing login for " + uuid + " took " + time + "ms.");
    }
    timing.stopTiming();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerJoin(PlayerJoinEvent event) {
    MCTiming timing = Bending.timingManager().ofStart("BendingProfile on join");
    Player player = event.getPlayer();
    UUID uuid = player.getUniqueId();
    String name = player.getName();
    BendingProfile profile = game.playerManager().profile(uuid);
    if (profile != null) {
      game.playerManager().createPlayer(player, profile);
    } else {
      Bending.logger().severe("Could not create bending profile for: " + uuid + " (" + name + ")");
    }
    timing.stopTiming();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerLogout(PlayerQuitEvent event) {
    game.activationController().onPlayerLogout(game.playerManager().player(event.getPlayer()));
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onEntityDeath(EntityDeathEvent event) {
    event.getDrops().removeIf(item -> Bending.dataLayer().hasArmorKey(item.getItemMeta()));
    boolean keepInventory = (event instanceof PlayerDeathEvent) && ((PlayerDeathEvent) event).getKeepInventory();
    TempArmor.MANAGER.get(event.getEntity()).ifPresent(tempArmor -> {
      if (!keepInventory) {
        event.getDrops().addAll(tempArmor.snapshot());
      }
      tempArmor.revert();
    });
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(PlayerDeathEvent event) {
    EntityDamageEvent lastDamageCause = event.getEntity().getLastDamageCause();
    if (lastDamageCause instanceof BendingDamageEvent) {
      BendingDamageEvent cause = (BendingDamageEvent) lastDamageCause;
      AbilityDescription ability = cause.ability();
      String deathKey = "bending.ability." + ability.name().toLowerCase() + ".death";
      TranslatableComponent msg = Bending.translationManager().getTranslation(deathKey);
      if (msg == null) {
        msg = Component.translatable("bending.ability.generic.death");
      }
      Component target = Component.text(event.getEntity().getName());
      Component source = Component.text(cause.user().entity().getName());
      event.deathMessage(msg.args(target, source, ability.displayName()));
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onInventoryClick(InventoryClickEvent event) {
    ItemStack item = event.getCurrentItem();
    if (item == null || !(event.getClickedInventory() instanceof PlayerInventory)) {
      return;
    }

    ItemMeta meta = item.getItemMeta();
    if (meta != null && Bending.dataLayer().hasArmorKey(meta)) {
      PlayerInventory inventory = (PlayerInventory) event.getClickedInventory();
      if (inventory.getHolder() instanceof Player) {
        Player player = ((Player) inventory.getHolder()).getPlayer();
        if (!TempArmor.MANAGER.isTemp(player) || event.getSlotType() != InventoryType.SlotType.ARMOR) {
          inventory.remove(item);
        }
      }
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onEntityDamageLow(EntityDamageEvent event) {
    if (event.getDamage() <= 0 || !(event.getEntity() instanceof LivingEntity)) {
      return;
    }
    LivingEntity entity = (LivingEntity) event.getEntity();
    double oldDamage = event.getDamage();
    double newDamage = game.activationController().onEntityDamage(entity, event.getCause(), oldDamage);
    if (newDamage <= 0) {
      event.setCancelled(true);
    } else if (oldDamage != newDamage) {
      event.setDamage(newDamage);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityDamage(EntityDamageEvent event) {
    if (event.getDamage() <= 0 || !(event.getEntity() instanceof LivingEntity)) {
      return;
    }
    game.benderRegistry().user((LivingEntity) event.getEntity())
      .ifPresent(game.activationController()::onUserDamage);
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onEntityDamageByExplosion(EntityDamageByEntityEvent event) {
    if (event.getDamage() <= 0 || !(event.getEntity() instanceof LivingEntity)) {
      return;
    }
    if (event.getCause() == DamageCause.BLOCK_EXPLOSION || event.getCause() == DamageCause.ENTITY_EXPLOSION) {
      Optional<User> user = game.benderRegistry().user((LivingEntity) event.getEntity());
      if (user.isPresent()) {
        double oldDmg = event.getDamage();
        double newDmg = FireShield.shieldFromExplosion(user.get(), event.getDamager(), oldDmg);
        if (newDmg <= 0) {
          event.setCancelled(true);
        } else if (oldDmg != newDmg) {
          event.setDamage(newDmg);
        }
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

    final Vector3 velocity = new Vector3(to).subtract(new Vector3(from));
    game.activationController().onUserMove(game.playerManager().player(event.getPlayer()), velocity);
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

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getHand() != EquipmentSlot.HAND) {
      return;
    }
    BendingPlayer player = game.playerManager().player(event.getPlayer());
    switch (event.getAction()) {
      case RIGHT_CLICK_AIR:
        game.activationController().onUserInteract(player, ActivationMethod.INTERACT);
        break;
      case RIGHT_CLICK_BLOCK:
        game.activationController().onUserInteract(player, ActivationMethod.INTERACT_BLOCK, event.getClickedBlock());
        break;
      case LEFT_CLICK_AIR:
      case LEFT_CLICK_BLOCK:
        game.activationController().onUserSwing(player);
        break;
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayDropItem(PlayerDropItemEvent event) {
    game.activationController().ignoreNextSwing(game.playerManager().player(event.getPlayer()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
    if (event.getHand() != EquipmentSlot.HAND) {
      return;
    }
    User user = game.playerManager().player(event.getPlayer());
    game.activationController().onUserInteract(user, ActivationMethod.INTERACT_ENTITY, event.getRightClicked());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
    game.activationController().onUserSneak(game.playerManager().player(event.getPlayer()), event.isSneaking());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
    game.boardManager().forceToggleScoreboard(event.getPlayer());
    BendingPlayer bendingPlayer = game.playerManager().player(event.getPlayer());
    game.abilityManager(event.getFrom()).destroyUserInstances(bendingPlayer);
    game.abilityManager(event.getPlayer().getWorld()).createPassives(bendingPlayer);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerSlotChange(PlayerItemHeldEvent event) {
    game.boardManager().changeActiveSlot(event.getPlayer(), event.getPreviousSlot(), event.getNewSlot());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onCooldownAdd(CooldownAddEvent event) {
    if (event.user() instanceof BendingPlayer) {
      game.boardManager().updateBoardSlot((Player) event.user().entity(), event.ability(), true);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onCooldownRemove(CooldownRemoveEvent event) {
    if (event.user() instanceof BendingPlayer) {
      game.boardManager().updateBoardSlot((Player) event.user().entity(), event.ability(), false);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onElementChange(ElementChangeEvent event) {
    if (event.user() instanceof BendingPlayer && event.result() != ElementChangeEvent.Result.ADD) {
      game.boardManager().updateBoard((Player) event.user().entity());
    }
  }
}
