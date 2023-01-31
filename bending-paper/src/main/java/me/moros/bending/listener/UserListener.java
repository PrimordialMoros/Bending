/*
 * Copyright 2020-2023 Moros
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

import java.util.ListIterator;
import java.util.UUID;
import java.util.function.Predicate;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import io.papermc.paper.event.entity.EntityInsideBlockEvent;
import io.papermc.paper.event.entity.EntityMoveEvent;
import me.moros.bending.BendingPlugin;
import me.moros.bending.ability.earth.EarthGlove;
import me.moros.bending.ability.earth.MetalCable;
import me.moros.bending.adapter.NativeAdapter;
import me.moros.bending.model.BlockInteraction;
import me.moros.bending.model.DamageSource;
import me.moros.bending.model.Element;
import me.moros.bending.model.EntityInteraction;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.ActionType;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.manager.Game;
import me.moros.bending.model.registry.Registries;
import me.moros.bending.model.user.User;
import me.moros.bending.platform.DamageUtil;
import me.moros.bending.platform.PlatformAdapter;
import me.moros.bending.temporal.ActionLimiter;
import me.moros.bending.temporal.TempArmor;
import me.moros.bending.temporal.TempBlock;
import me.moros.bending.temporal.TempEntity;
import me.moros.bending.util.BendingEffect;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.metadata.Metadata;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
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

public record UserListener(Game game, BendingPlugin plugin) implements Listener, BukkitListener {
  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onArrowHit(ProjectileHitEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    if (event.getEntity() instanceof Arrow && event.getEntity().hasMetadata(MetalCable.CABLE_KEY.value())) {
      MetalCable cable = (MetalCable) event.getEntity().getMetadata(MetalCable.CABLE_KEY.value()).get(0).value();
      if (cable != null) {
        var block = event.getHitBlock();
        if (block != null) {
          cable.hitBlock(PlatformAdapter.fromBukkitBlock(block));
        } else if (event.getHitEntity() instanceof LivingEntity living) {
          cable.hitEntity(PlatformAdapter.fromBukkitEntity(living));
        } else {
          event.getEntity().remove();
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onEntityExplodeEvent(EntityExplodeEvent event) {
    cancelIfLimited(event, null);
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onEntityInteractEvent(EntityInteractEvent event) {
    cancelIfLimited(event, ActionType.INTERACT);
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onProjectileLaunchEvent(ProjectileLaunchEvent event) {
    cancelIfLimited(event, ActionType.SHOOT);
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onEntityShootBowEvent(EntityShootBowEvent event) {
    cancelIfLimited(event, ActionType.SHOOT);
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onSlimeSplitEvent(SlimeSplitEvent event) {
    cancelIfLimited(event, null);
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onEntityTarget(EntityTargetEvent event) {
    cancelIfLimited(event, null);
  }

  private <E extends EntityEvent & Cancellable> void cancelIfLimited(E event, @Nullable ActionType type) {
    if (disabledWorld(event)) {
      return;
    }
    if (ActionLimiter.isLimited(event.getEntity().getUniqueId(), type)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onEntityCombustByBlock(EntityCombustByBlockEvent event) {
    Block block = event.getCombuster();
    if (block == null || disabledWorld(event)) {
      return;
    }
    TempBlock tb = TempBlock.MANAGER.get(PlatformAdapter.fromBukkitBlock(block)).orElse(null);
    if (tb != null) {
      int ticks = event.getDuration() * 20;
      if (ticks > BendingEffect.MAX_BLOCK_FIRE_TICKS) {
        event.setDuration(FastMath.ceil(BendingEffect.MAX_BLOCK_FIRE_TICKS / 20.0));
      }
      DamageSource source = tb.damageSource();
      if (source != null) {
        DamageUtil.cacheDamageSource(event.getEntity().getUniqueId(), source);
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityDamageByBlock(EntityDamageByBlockEvent event) {
    onEntityDamageByBlock(event.getEntity(), event.getDamager());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityInsideBlock(EntityInsideBlockEvent event) {
    onEntityDamageByBlock(event.getEntity(), event.getBlock());
  }

  private void onEntityDamageByBlock(Entity entity, @Nullable Block block) {
    if (block == null || disabledWorld(entity.getWorld())) {
      return;
    }
    TempBlock.MANAGER.get(PlatformAdapter.fromBukkitBlock(block)).map(TempBlock::damageSource)
      .ifPresent(source -> DamageUtil.cacheDamageSource(entity.getUniqueId(), source));
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    if (event.getDamager() instanceof Arrow && event.getDamager().hasMetadata(MetalCable.CABLE_KEY.value())) {
      event.setCancelled(true);
    } else if (ActionLimiter.isLimited(event.getDamager().getUniqueId(), ActionType.DAMAGE)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onItemMerge(ItemMergeEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    if (event.getEntity().hasMetadata(EarthGlove.GLOVE_KEY.value()) || event.getTarget().hasMetadata(EarthGlove.GLOVE_KEY.value())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onEntityChangeBlock(EntityChangeBlockEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    if (event.getEntity() instanceof FallingBlock fallingBlock) {
      TempEntity.MANAGER.get(fallingBlock.getEntityId()).ifPresent(temp -> {
        event.setCancelled(true);
        temp.revert();
      });
    } else {
      if (ActionLimiter.isLimited(event.getEntity().getUniqueId(), ActionType.INTERACT_BLOCK)) {
        event.setCancelled(true);
      }
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onEntityDamageLow(EntityDamageEvent event) {
    if (event.getDamage() <= 0 || disabledWorld(event)) {
      return;
    }
    if (event.getEntity() instanceof LivingEntity bukkitEntity) {
      double oldDamage = event.getDamage();
      var entity = PlatformAdapter.fromBukkitEntity(bukkitEntity);
      var cause = PlatformAdapter.fromBukkitCause(event.getCause());
      Vector3d origin = null;
      if (event instanceof EntityDamageByEntityEvent entityEvent) {
        origin = PlatformAdapter.fromBukkitEntity(entityEvent.getDamager()).center();
      }
      double newDamage = game.activationController().onEntityDamage(entity, cause, oldDamage, origin);
      if (newDamage <= 0) {
        event.setCancelled(true);
      } else if (oldDamage != newDamage) {
        event.setDamage(newDamage);
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityDamage(EntityDamageEvent event) {
    if (event.getDamage() <= 0 || disabledWorld(event)) {
      return;
    }
    if (event.getEntity() instanceof LivingEntity entity) {
      User user = Registries.BENDERS.get(entity.getUniqueId());
      if (user != null) {
        game.activationController().onUserDamage(user);
      }
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onUserGlide(EntityToggleGlideEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    if (!event.isGliding() && event.getEntity() instanceof LivingEntity entity) {
      if (ActionLimiter.isLimited(event.getEntity().getUniqueId(), ActionType.MOVE)) {
        event.setCancelled(true);
        return;
      }
      User user = Registries.BENDERS.get(entity.getUniqueId());
      if (user != null && game.activationController().onUserGlide(user)) {
        event.setCancelled(true);
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onEntityDeathHigh(EntityDeathEvent event) {
    var nsk = PlatformAdapter.nsk(Metadata.ARMOR_KEY);
    event.getDrops().removeIf(item -> item.getItemMeta().getPersistentDataContainer().has(nsk));
    if (disabledWorld(event) || event instanceof PlayerDeathEvent) {
      return;
    }
    DamageSource cause = DamageUtil.cachedDamageSource(event.getEntity().getUniqueId());
    if (cause != null && cause.ability().element() == Element.FIRE) {
      ListIterator<ItemStack> it = event.getDrops().listIterator();
      while (it.hasNext()) {
        ItemStack item = it.next();
        var mapped = MaterialUtil.COOKABLE.get(PlatformAdapter.fromBukkitItem(item.getType()));
        Material flamed = mapped == null ? null : PlatformAdapter.toBukkitItemMaterial(mapped);
        if (flamed != null) {
          it.set(new ItemStack(flamed, item.getAmount()));
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onUserDeath(EntityDeathEvent event) {
    UUID uuid = event.getEntity().getUniqueId();
    ActionLimiter.MANAGER.get(uuid).ifPresent(ActionLimiter::revert);
    TempArmor.MANAGER.get(uuid).ifPresent(TempArmor::revert);
    if (disabledWorld(event) || event instanceof PlayerDeathEvent) {
      return;
    }
    User user = Registries.BENDERS.get(uuid);
    if (user != null) {
      game.activationController().onUserDeconstruct(user);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(PlayerDeathEvent event) {
    Player player = event.getEntity();
    DamageSource cause = DamageUtil.cachedDamageSource(player.getUniqueId());
    if (cause != null) {
      Boolean showMessage = player.getWorld().getGameRuleValue(GameRule.SHOW_DEATH_MESSAGES);
      if (showMessage != null && showMessage) {
        Predicate<Audience> predicate = x -> true;
        Team team = player.getScoreboard().getPlayerTeam(player);
        if (team != null) {
          OptionStatus status = team.getOption(Option.DEATH_MESSAGE_VISIBILITY);
          predicate = u -> canSend(status, team, u);
        }
        AbilityDescription ability = cause.ability();
        TranslatableComponent msg = plugin.translationManager().translate(ability.translationKey() + ".death");
        if (msg == null) {
          msg = Component.translatable("bending.ability.generic.death");
        }
        Component message = msg.args(player.name(), cause.name(), ability.displayName());
        // Client isn't aware of custom death message translations, so we have to manually broadcast
        player.getServer().filterAudience(predicate).sendMessage(message);
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
    onUserMove(event, event);
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onUserMove(PlayerMoveEvent event) {
    onUserMove(event, new EntityMoveEvent(event.getPlayer(), event.getFrom(), event.getTo()));
  }

  private void onUserMove(Cancellable originalEvent, EntityMoveEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    if (event.hasChangedBlock() && ActionLimiter.isLimited(event.getEntity().getUniqueId(), ActionType.MOVE)) {
      originalEvent.setCancelled(true);
      return;
    }
    User user = Registries.BENDERS.get(event.getEntity().getUniqueId());
    if (user != null) {
      double x = event.getTo().getX() - event.getFrom().getX();
      double z = event.getTo().getZ() - event.getFrom().getZ();
      game.activationController().onUserMove(user, Vector3d.of(x, 0, z));
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onPlayerJump(PlayerJumpEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    if (ActionLimiter.isLimited(event.getPlayer().getUniqueId(), ActionType.MOVE)) {
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
    if (ActionLimiter.isLimited(event.getPlayer().getUniqueId(), ActionType.INTERACT)) {
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
            Block b = event.getClickedBlock();
            var block = b == null ? null : PlatformAdapter.fromBukkitBlock(b);
            if (block != null) {
              Location loc = event.getInteractionPoint();
              Vector3d point = loc == null ? null : Vector3d.from(loc);
              user.store().add(BlockInteraction.KEY, new BlockInteraction(block, point));
            }
            game.activationController().onUserInteract(user, null, block);
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
    onPlayerInteractEntity(event, Vector3d.from(event.getClickedPosition()));
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
      var target = PlatformAdapter.fromBukkitEntity(event.getRightClicked());
      user.store().add(EntityInteraction.KEY, new EntityInteraction(target, point));
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
        game.abilityManager(user.worldKey()).destroyUserInstances(user, a -> !a.description().isActivatedBy(Activation.PASSIVE));
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
  public void onHopperItemPickup(InventoryPickupItemEvent event) {
    var item = event.getItem();
    if (item.getItemStack().getType() == Material.STONE && item.hasMetadata(EarthGlove.GLOVE_KEY.value())) {
      event.setCancelled(true);
      event.getItem().remove();
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onInventoryClick(InventoryClickEvent event) {
    ItemStack item = event.getCurrentItem();
    if (item == null || !(event.getClickedInventory() instanceof PlayerInventory)) {
      return;
    }
    ItemMeta meta = item.getItemMeta();
    if (meta != null && meta.getPersistentDataContainer().has(PlatformAdapter.nsk(Metadata.ARMOR_KEY))) {
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
