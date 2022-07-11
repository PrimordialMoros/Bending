/*
 * Copyright 2020-2022 Moros
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.papermc.paper.event.entity.EntityMoveEvent;
import me.moros.bending.Bending;
import me.moros.bending.ability.fire.FireShield;
import me.moros.bending.adapter.NativeAdapter;
import me.moros.bending.event.BendingDamageEvent;
import me.moros.bending.game.temporal.ActionLimiter;
import me.moros.bending.game.temporal.TempArmor;
import me.moros.bending.game.temporal.TempEntity;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.ActionType;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.manager.Game;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import me.moros.bending.model.user.profile.PlayerProfile;
import me.moros.bending.registry.Registries;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.metadata.Metadata;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

public class UserListener implements Listener {
  private final Bending plugin;
  private final Game game;
  private final AsyncLoadingCache<UUID, PlayerProfile> profileCache;

  public UserListener(Bending plugin, Game game) {
    this.plugin = plugin;
    this.game = game;
    this.profileCache = Caffeine.newBuilder().maximumSize(100).expireAfterWrite(Duration.ofMinutes(2))
      .buildAsync(game.storage()::createProfile);
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
    UUID uuid = event.getUniqueId();
    long startTime = System.currentTimeMillis();
    try {
      // Timeout after 1000ms to not block the login thread excessively
      PlayerProfile profile = profileCache.get(uuid).get(1000, TimeUnit.MILLISECONDS);
      long deltaTime = System.currentTimeMillis() - startTime;
      if (profile != null && deltaTime > 500) {
        plugin.logger().warn("Processing login for " + uuid + " took " + deltaTime + "ms.");
      }
    } catch (TimeoutException e) {
      plugin.logger().warn("Timed out while retrieving data for " + uuid);
    } catch (CancellationException | ExecutionException | InterruptedException e) {
      plugin.logger().warn(e.getMessage(), e);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    UUID uuid = player.getUniqueId();
    String name = player.getName();
    PlayerProfile profile = profileCache.synchronous().get(uuid);
    if (profile != null) {
      User user = BendingPlayer.createUser(game, player, profile).orElse(null);
      if (user != null) {
        Registries.BENDERS.register(user);
        game.abilityManager(user.world()).createPassives(user);
      }
    } else {
      plugin.logger().error("Could not create bending profile for: " + uuid + " (" + name + ")");
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerLogout(PlayerQuitEvent event) {
    UUID uuid = event.getPlayer().getUniqueId();
    User user = Registries.BENDERS.get(uuid);
    if (user != null) {
      game.activationController().onUserDeconstruct(user);
    }
    profileCache.synchronous().invalidate(uuid);
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onEntityDeath(EntityDeathEvent event) {
    event.getDrops().removeIf(item -> Metadata.hasArmorKey(item.getItemMeta()));
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
        Collection<ItemStack> newDrops = new ArrayList<>();
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
    ActionLimiter.MANAGER.get(entity.getUniqueId()).ifPresent(ActionLimiter::revert);
    if (entity instanceof Player) {
      return;
    }
    User user = Registries.BENDERS.get(entity.getUniqueId());
    if (user != null) {
      game.activationController().onUserDeconstruct(user);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(PlayerDeathEvent event) {
    EntityDamageEvent lastDamageCause = event.getEntity().getLastDamageCause();
    if (lastDamageCause instanceof BendingDamageEvent cause) {
      AbilityDescription ability = cause.ability();
      TranslatableComponent msg = plugin.translationManager().translate(ability.key() + ".death");
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
    if (Metadata.hasArmorKey(meta)) {
      Inventory inventory = event.getClickedInventory();
      if (inventory.getHolder() instanceof Player player) {
        if (!TempArmor.MANAGER.isTemp(player.getUniqueId()) || event.getSlotType() != InventoryType.SlotType.ARMOR) {
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
      User user = Registries.BENDERS.get(entity.getUniqueId());
      if (user != null) {
        game.activationController().onUserDamage(user);
      }
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onEntityDamageByExplosion(EntityDamageByEntityEvent event) {
    if (event.getDamage() > 0 && event.getEntity() instanceof LivingEntity entity) {
      if (event.getCause() == DamageCause.BLOCK_EXPLOSION || event.getCause() == DamageCause.ENTITY_EXPLOSION) {
        User user = Registries.BENDERS.get(entity.getUniqueId());
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

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onPlayerMove(PlayerJumpEvent event) {
    if (ActionLimiter.isLimited(event.getPlayer(), ActionType.MOVE)) {
      event.setCancelled(true);
    }
  }

  private boolean handleMovement(EntityMoveEvent event) {
    if (event.hasChangedBlock() && ActionLimiter.isLimited(event.getEntity(), ActionType.MOVE)) {
      return false;
    }
    User user = Registries.BENDERS.get(event.getEntity().getUniqueId());
    if (user != null) {
      double x = event.getTo().getX() - event.getFrom().getX();
      double z = event.getTo().getZ() - event.getFrom().getZ();
      game.activationController().onUserMove(user, new Vector3d(x, 0, z));
    }
    return true;
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onUserGlide(EntityToggleGlideEvent event) {
    if (!event.isGliding() && event.getEntity() instanceof LivingEntity entity) {
      if (ActionLimiter.isLimited(event.getEntity(), ActionType.MOVE)) {
        return;
      }
      User user = Registries.BENDERS.get(entity.getUniqueId());
      if (user != null && game.activationController().onUserGlide(user)) {
        event.setCancelled(true);
      }
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onUserSprint(PlayerToggleSprintEvent event) {
    if (event.isSprinting() && game.activationController().hasSpout(event.getPlayer().getUniqueId())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onPlayerInteractLow(PlayerInteractEvent event) {
    if (ActionLimiter.isLimited(event.getPlayer(), ActionType.INTERACT)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getHand() == EquipmentSlot.HAND || (event.getHand() == EquipmentSlot.OFF_HAND && event.getAction() == Action.RIGHT_CLICK_AIR)) {
      User user = Registries.BENDERS.get(event.getPlayer().getUniqueId());
      if (user != null) {
        switch (event.getAction()) {
          case RIGHT_CLICK_AIR -> game.activationController().onUserInteract(user, Activation.INTERACT);
          case RIGHT_CLICK_BLOCK ->
            game.activationController().onUserInteract(user, Activation.INTERACT_BLOCK, event.getClickedBlock());
          case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> game.activationController().onUserSwing(user);
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayDropItem(PlayerDropItemEvent event) {
    game.activationController().ignoreNextSwing(event.getPlayer().getUniqueId());
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onPlayerInteractEntityLow(PlayerInteractEntityEvent event) {
    if (NativeAdapter.hasNativeSupport()) {
      return;
    }
    if (TempEntity.MANAGER.isTemp(event.getRightClicked().getEntityId())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
    if (event.getHand() != EquipmentSlot.HAND) {
      return;
    }
    User user = Registries.BENDERS.get(event.getPlayer().getUniqueId());
    if (user != null) {
      game.activationController().onUserInteract(user, Activation.INTERACT_ENTITY, event.getRightClicked());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
    User user = Registries.BENDERS.get(event.getPlayer().getUniqueId());
    if (user != null) {
      game.activationController().onUserSneak(user, event.isSneaking());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
    User user = Registries.BENDERS.get(event.getPlayer().getUniqueId());
    if (user != null) {
      user.board().updateAll();
      game.abilityManager(event.getFrom()).destroyUserInstances(user);
      game.abilityManager(event.getPlayer().getWorld()).createPassives(user);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerSlotChange(PlayerItemHeldEvent event) {
    User user = Registries.BENDERS.get(event.getPlayer().getUniqueId());
    if (user != null) {
      user.board().activeSlot(event.getPreviousSlot() + 1, event.getNewSlot() + 1);
    }
  }
}
