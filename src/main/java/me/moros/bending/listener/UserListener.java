/*
 * Copyright 2020-2021 Moros
 *
 * This file is part of Bending.
 *
 * Bending is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bending is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bending. If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.listener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import io.papermc.paper.event.entity.EntityMoveEvent;
import me.moros.bending.Bending;
import me.moros.bending.ability.fire.FireShield;
import me.moros.bending.event.BendingDamageEvent;
import me.moros.bending.game.Game;
import me.moros.bending.game.temporal.TempArmor;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.ActionType;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import me.moros.bending.model.user.profile.PlayerProfile;
import me.moros.bending.registry.Registries;
import me.moros.bending.util.metadata.Metadata;
import me.moros.bending.util.MovementHandler;
import me.moros.bending.util.material.MaterialUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.bukkit.Material;
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
import org.bukkit.event.player.PlayerToggleSprintEvent;
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
  public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
    UUID uuid = event.getUniqueId();
    long startTime = System.currentTimeMillis();
    try {
      // Timeout after 1000ms to not block the login thread excessively
      PlayerProfile profile = Registries.BENDERS.profile(uuid).get(1000, TimeUnit.MILLISECONDS);
      long deltaTime = System.currentTimeMillis() - startTime;
      if (profile != null && deltaTime > 500) {
        Bending.logger().warn("Processing login for " + uuid + " took " + deltaTime + "ms.");
      }
    } catch (TimeoutException e) {
      Bending.logger().warn("Timed out while retrieving data for " + uuid);
    } catch (CancellationException | ExecutionException | InterruptedException e) {
      Bending.logger().warn(e.getMessage(), e);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerJoin(PlayerJoinEvent event) {
    Timing timing = Timings.ofStart(Bending.plugin(), "BendingProfile on join");
    Player player = event.getPlayer();
    UUID uuid = player.getUniqueId();
    String name = player.getName();
    PlayerProfile profile = Registries.BENDERS.profileSync(uuid);
    if (profile != null) {
      BendingPlayer.createUser(player, profile).ifPresent(Registries.BENDERS::register);
    } else {
      Bending.logger().error("Could not create bending profile for: " + uuid + " (" + name + ")");
    }
    timing.stopTiming();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerLogout(PlayerQuitEvent event) {
    game.activationController().onUserDeconstruct(Registries.BENDERS.user(event.getPlayer()));
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onEntityDeath(EntityDeathEvent event) {
    event.getDrops().removeIf(item -> Bending.dataLayer().hasArmorKey(item.getItemMeta()));
    boolean keepInventory = event instanceof PlayerDeathEvent pde && pde.getKeepInventory();
    TempArmor.MANAGER.get(event.getEntity().getUniqueId()).ifPresent(tempArmor -> {
      if (!keepInventory) {
        event.getDrops().addAll(tempArmor.snapshot());
      }
      tempArmor.revert();
    });
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onEntityDeathHigh(EntityDeathEvent event) {
    if (event instanceof PlayerDeathEvent) {
      return;
    }
    EntityDamageEvent lastDamageCause = event.getEntity().getLastDamageCause();
    if (lastDamageCause instanceof BendingDamageEvent cause) {
      if (cause.ability().element() == Element.FIRE) {
        List<ItemStack> newDrops = new ArrayList<>();
        Iterator<ItemStack> it = event.getDrops().iterator();
        while (it.hasNext()) {
          ItemStack item = it.next();
          Material flamed = MaterialUtil.COOKABLE.get(item.getType());
          if (flamed != null) {
            newDrops.add(new ItemStack(flamed, item.getAmount()));
            it.remove();
          }
        }
        event.getDrops().addAll(newDrops);
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onUserDeath(EntityDeathEvent event) {
    LivingEntity entity = event.getEntity();
    if (entity instanceof Player) {
      return;
    }
    User user = Registries.BENDERS.user(entity);
    if (user != null) {
      game.activationController().onUserDeconstruct(user);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(PlayerDeathEvent event) {
    EntityDamageEvent lastDamageCause = event.getEntity().getLastDamageCause();
    if (lastDamageCause instanceof BendingDamageEvent cause) {
      AbilityDescription ability = cause.ability();
      String deathKey = "bending.ability." + ability.name().toLowerCase(Locale.ROOT) + ".death";
      TranslatableComponent msg = Bending.translationManager().translate(deathKey);
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
        if (!TempArmor.MANAGER.isTemp(inventory.getHolder().getUniqueId()) || event.getSlotType() != InventoryType.SlotType.ARMOR) {
          inventory.remove(item);
        }
      }
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onEntityDamageLow(EntityDamageEvent event) {
    if (event.getDamage() > 0 && event.getEntity() instanceof LivingEntity entity) {
      double oldDamage = event.getDamage();
      double newDamage = game.activationController().onEntityDamage(entity, event.getCause(), oldDamage);
      if (newDamage <= 0) {
        event.setCancelled(true);
      } else if (oldDamage != newDamage) {
        event.setDamage(newDamage);
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityDamage(EntityDamageEvent event) {
    if (event.getDamage() > 0 && event.getEntity() instanceof LivingEntity entity) {
      User user = Registries.BENDERS.user(entity);
      if (user != null) {
        game.activationController().onUserDamage(user);
      }
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onEntityDamageByExplosion(EntityDamageByEntityEvent event) {
    if (event.getDamage() > 0 && event.getEntity() instanceof LivingEntity entity) {
      if (event.getCause() == DamageCause.BLOCK_EXPLOSION || event.getCause() == DamageCause.ENTITY_EXPLOSION) {
        User user = Registries.BENDERS.user(entity);
        if (user != null) {
          double oldDmg = event.getDamage();
          double newDmg = FireShield.shieldFromExplosion(user, event.getDamager(), oldDmg);
          if (newDmg <= 0) {
            event.setCancelled(true);
          } else if (oldDmg != newDmg) {
            event.setDamage(newDmg);
          }
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onUserMove(EntityMoveEvent event) {
    if (!handleMovement(event)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onPlayerMove(PlayerMoveEvent event) {
    if (!handleMovement(new EntityMoveEvent(event.getPlayer(), event.getFrom(), event.getTo()))) {
      event.setCancelled(true);
    }
  }

  private boolean handleMovement(EntityMoveEvent event) {
    if (event.hasChangedBlock() && MovementHandler.isRestricted(event.getEntity(), ActionType.MOVE)) {
      return false;
    }
    User user = Registries.BENDERS.user(event.getEntity());
    if (user != null) {
      double x = event.getTo().getX() - event.getFrom().getX();
      double z = event.getTo().getZ() - event.getFrom().getZ();
      game.activationController().onUserMove(user, new Vector3d(x, 0, z));
    }
    return true;
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onUserSprint(PlayerToggleSprintEvent event) {
    if (event.isSprinting() && game.activationController().hasSpout(event.getPlayer().getUniqueId())) {
      event.setCancelled(true);
    }
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
    BendingPlayer player = Registries.BENDERS.user(event.getPlayer());
    switch (event.getAction()) {
      case RIGHT_CLICK_AIR -> game.activationController().onUserInteract(player, Activation.INTERACT);
      case RIGHT_CLICK_BLOCK -> game.activationController().onUserInteract(player, Activation.INTERACT_BLOCK, event.getClickedBlock());
      case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> game.activationController().onUserSwing(player);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayDropItem(PlayerDropItemEvent event) {
    game.activationController().ignoreNextSwing(Registries.BENDERS.user(event.getPlayer()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
    if (event.getHand() != EquipmentSlot.HAND) {
      return;
    }
    User user = Registries.BENDERS.user(event.getPlayer());
    game.activationController().onUserInteract(user, Activation.INTERACT_ENTITY, event.getRightClicked());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
    game.activationController().onUserSneak(Registries.BENDERS.user(event.getPlayer()), event.isSneaking());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
    BendingPlayer bendingPlayer = Registries.BENDERS.user(event.getPlayer());
    game.boardManager().tryEnableBoard(bendingPlayer);
    game.abilityManager(event.getFrom()).destroyUserInstances(bendingPlayer);
    game.abilityManager(event.getPlayer().getWorld()).createPassives(bendingPlayer);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerSlotChange(PlayerItemHeldEvent event) {
    BendingPlayer bendingPlayer = Registries.BENDERS.user(event.getPlayer());
    game.boardManager().changeActiveSlot(bendingPlayer, event.getPreviousSlot() + 1, event.getNewSlot() + 1);
  }
}
