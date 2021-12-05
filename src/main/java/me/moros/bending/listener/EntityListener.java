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

import me.moros.bending.ability.earth.MetalCable;
import me.moros.bending.event.BendingTickEffectEvent;
import me.moros.bending.game.Game;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.ActionType;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.util.BendingEffect;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.metadata.Metadata;
import me.moros.bending.util.MovementHandler;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.potion.PotionEffectType;
import org.checkerframework.checker.nullness.qual.NonNull;

public class EntityListener implements Listener {
  private final Game game;

  public EntityListener(@NonNull Game game) {
    this.game = game;
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onArrowHit(ProjectileHitEvent event) {
    if (event.getEntity() instanceof Arrow && event.getEntity().hasMetadata(Metadata.METAL_CABLE)) {
      MetalCable cable = (MetalCable) event.getEntity().getMetadata(Metadata.METAL_CABLE).get(0).value();
      if (cable != null) {
        if (event.getHitBlock() != null) {
          cable.hitBlock(event.getHitBlock());
        } else if (event.getHitEntity() instanceof LivingEntity) {
          cable.hitEntity(event.getHitEntity());
        } else {
          event.getEntity().remove();
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityFreeze(BendingTickEffectEvent event) {
    if (event.type() == BendingEffect.FROST_TICK && event.target() instanceof LivingEntity entity) {
      int duration = event.duration();
      if (duration > 30) {
        int potionDuration = FastMath.round(0.5 * duration);
        int power = FastMath.floor(duration / 30.0);
        EntityUtil.tryAddPotion(entity, PotionEffectType.SLOW, potionDuration, power);
      }
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onEntityExplodeEvent(EntityExplodeEvent event) {
    if (MovementHandler.isRestricted(event.getEntity())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onEntityInteractEvent(EntityInteractEvent event) {
    if (MovementHandler.isRestricted(event.getEntity(), ActionType.INTERACT)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onProjectileLaunchEvent(ProjectileLaunchEvent event) {
    if (MovementHandler.isRestricted(event.getEntity(), ActionType.SHOOT)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onEntityShootBowEvent(EntityShootBowEvent event) {
    if (MovementHandler.isRestricted(event.getEntity(), ActionType.SHOOT)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onSlimeSplitEvent(SlimeSplitEvent event) {
    if (MovementHandler.isRestricted(event.getEntity())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onEntityTarget(EntityTargetEvent event) {
    if (MovementHandler.isRestricted(event.getEntity())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onEntityTargetLiving(EntityTargetLivingEntityEvent event) {
    if (MovementHandler.isRestricted(event.getEntity())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onEntityCombustByBlock(EntityCombustByBlockEvent event) {
    if (TempBlock.MANAGER.isTemp(event.getCombuster())) {
      int ticks = event.getDuration() * 20;
      if (ticks > BendingEffect.MAX_BLOCK_FIRE_TICKS) {
        event.setDuration(FastMath.ceil(BendingEffect.MAX_BLOCK_FIRE_TICKS / 20.0));
      }
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
    if (event.getDamager() instanceof Arrow && event.getDamager().hasMetadata(Metadata.METAL_CABLE)) {
      event.setCancelled(true);
    } else if (MovementHandler.isRestricted(event.getDamager(), ActionType.DAMAGE)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onItemMerge(ItemMergeEvent event) {
    if (event.getEntity().hasMetadata(Metadata.GLOVE_KEY) || event.getTarget().hasMetadata(Metadata.GLOVE_KEY)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onItemPickup(EntityPickupItemEvent event) {
    if (event.getItem().hasMetadata(Metadata.NO_PICKUP)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onHopperItemPickup(InventoryPickupItemEvent event) {
    if (event.getItem().hasMetadata(Metadata.NO_PICKUP)) {
      event.setCancelled(true);
      event.getItem().remove();
    }
  }
}
