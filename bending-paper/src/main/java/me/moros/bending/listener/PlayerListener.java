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
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.papermc.paper.event.entity.EntityMoveEvent;
import me.moros.bending.Bending;
import me.moros.bending.adapter.NativeAdapter;
import me.moros.bending.event.BendingDamageEvent;
import me.moros.bending.model.BlockInteraction;
import me.moros.bending.model.EntityInteraction;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.ActionType;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.manager.Game;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import me.moros.bending.model.user.profile.PlayerProfile;
import me.moros.bending.registry.Registries;
import me.moros.bending.temporal.ActionLimiter;
import me.moros.bending.temporal.TempArmor;
import me.moros.bending.temporal.TempEntity;
import me.moros.bending.util.metadata.Metadata;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
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
import org.bukkit.scoreboard.Team;
import org.bukkit.scoreboard.Team.Option;
import org.bukkit.scoreboard.Team.OptionStatus;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PlayerListener implements Listener {
  private final Bending plugin;
  private final Game game;
  private final AsyncLoadingCache<UUID, PlayerProfile> profileCache;

  public PlayerListener(Bending plugin, Game game) {
    this.plugin = plugin;
    this.game = game;
    this.profileCache = Caffeine.newBuilder().maximumSize(100).expireAfterWrite(Duration.ofMinutes(2))
      .buildAsync(game.storage()::createProfile);
  }

  private boolean disabledWorld(PlayerEvent event) {
    return !game.worldManager().isEnabled(event.getPlayer().getWorld());
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

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(PlayerDeathEvent event) {
    Player player = event.getEntity();
    if (player.getLastDamageCause() instanceof BendingDamageEvent cause) {
      Boolean showMessage = player.getWorld().getGameRuleValue(GameRule.SHOW_DEATH_MESSAGES);
      if (showMessage != null && showMessage) {
        Predicate<Audience> predicate = x -> true;
        Team team = player.getScoreboard().getPlayerTeam(player);
        if (team != null) {
          OptionStatus status = team.getOption(Option.DEATH_MESSAGE_VISIBILITY);
          predicate = u -> canSend(status, team, u);
        }
        AbilityDescription ability = cause.ability();
        TranslatableComponent msg = plugin.translationManager().translate(ability.key() + ".death");
        if (msg == null) {
          msg = Component.translatable("bending.ability.generic.death");
        }
        Component message = msg.args(player.name(), cause.user().entity().name(), ability.displayName());
        // Client isn't aware of custom death message translations, so we have to manually broadcast
        plugin.getServer().filterAudience(predicate).sendMessage(message);
        event.deathMessage(Component.empty());
      }
    }
  }

  private boolean canSend(OptionStatus status, Team team, Audience other) {
    if (other instanceof Player player) {
      return switch (status) {
        case ALWAYS -> true;
        case NEVER -> false;
        case FOR_OTHER_TEAMS -> team.equals(player.getScoreboard().getPlayerTeam(player));
        case FOR_OWN_TEAM -> !team.equals(player.getScoreboard().getPlayerTeam(player));
      };
    }
    return false;
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onUserMove(EntityMoveEvent event) {
    if (!game.worldManager().isEnabled(event.getEntity().getWorld())) {
      return;
    }
    if (cancelMovement(event)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onPlayerMove(PlayerMoveEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    if (cancelMovement(new EntityMoveEvent(event.getPlayer(), event.getFrom(), event.getTo()))) {
      event.setCancelled(true);
    }
  }

  private boolean cancelMovement(EntityMoveEvent event) {
    if (event.hasChangedBlock() && ActionLimiter.isLimited(event.getEntity(), ActionType.MOVE)) {
      return true;
    }
    User user = Registries.BENDERS.get(event.getEntity().getUniqueId());
    if (user != null) {
      double x = event.getTo().getX() - event.getFrom().getX();
      double z = event.getTo().getZ() - event.getFrom().getZ();
      game.activationController().onUserMove(user, new Vector3d(x, 0, z));
    }
    return false;
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onPlayerJump(PlayerJumpEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    if (ActionLimiter.isLimited(event.getPlayer(), ActionType.MOVE)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onUserSprint(PlayerToggleSprintEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    if (event.isSprinting() && game.activationController().hasSpout(event.getPlayer().getUniqueId())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onPlayerInteractLow(PlayerInteractEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    if (ActionLimiter.isLimited(event.getPlayer(), ActionType.INTERACT)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    if (event.getHand() == EquipmentSlot.HAND || (event.getHand() == EquipmentSlot.OFF_HAND && event.getAction() == Action.RIGHT_CLICK_AIR)) {
      User user = Registries.BENDERS.get(event.getPlayer().getUniqueId());
      if (user != null) {
        switch (event.getAction()) {
          case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> {
            Block block = event.getClickedBlock();
            if (block != null) {
              Location loc = event.getInteractionPoint();
              Vector3d point = loc == null ? null : new Vector3d(loc);
              user.store().put(BlockInteraction.KEY, new BlockInteraction(block, event.getBlockFace(), point));
            }
            game.activationController().onUserInteract(user, null, event.getClickedBlock());
          }
          case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> game.activationController().onUserSwing(user);
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDropItem(PlayerDropItemEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    game.activationController().ignoreNextSwing(event.getPlayer().getUniqueId());
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onPlayerInteractAtEntityLow(PlayerInteractAtEntityEvent event) {
    onPlayerInteractEntityLow(event);
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onPlayerInteractEntityLow(PlayerInteractEntityEvent event) {
    if (NativeAdapter.hasNativeSupport() || disabledWorld(event)) {
      return;
    }
    if (TempEntity.MANAGER.isTemp(event.getRightClicked().getEntityId())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
    onPlayerInteractEntity(event, new Vector3d(event.getClickedPosition()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
    onPlayerInteractEntity(event, null);
  }

  private void onPlayerInteractEntity(PlayerInteractEntityEvent event, @Nullable Vector3d point) {
    if (event.getHand() != EquipmentSlot.HAND || disabledWorld(event)) {
      return;
    }
    User user = Registries.BENDERS.get(event.getPlayer().getUniqueId());
    if (user != null) {
      Entity target = event.getRightClicked();
      user.store().put(EntityInteraction.KEY, new EntityInteraction(target, point));
      game.activationController().onUserInteract(user, target, null);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    User user = Registries.BENDERS.get(event.getPlayer().getUniqueId());
    if (user != null) {
      game.activationController().onUserSneak(user, event.isSneaking());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    if (event.getNewGameMode() == GameMode.SPECTATOR) {
      User user = Registries.BENDERS.get(event.getPlayer().getUniqueId());
      if (user != null) {
        user.board().updateAll();
        game.abilityManager(user.world()).destroyUserInstances(user, a -> !a.description().isActivatedBy(Activation.PASSIVE));
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerSlotChange(PlayerItemHeldEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    User user = Registries.BENDERS.get(event.getPlayer().getUniqueId());
    if (user != null) {
      user.board().activeSlot(event.getPreviousSlot() + 1, event.getNewSlot() + 1);
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
}
