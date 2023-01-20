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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

import io.papermc.paper.event.entity.EntityInsideBlockEvent;
import me.moros.bending.ability.earth.EarthGlove;
import me.moros.bending.ability.earth.MetalCable;
import me.moros.bending.ability.fire.FireShield;
import me.moros.bending.event.EventBus;
import me.moros.bending.event.TickEffectEvent;
import me.moros.bending.model.DamageSource;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.ActionType;
import me.moros.bending.model.manager.Game;
import me.moros.bending.model.registry.Registries;
import me.moros.bending.model.user.User;
import me.moros.bending.platform.DamageUtil;
import me.moros.bending.platform.PlatformAdapter;
import me.moros.bending.platform.potion.PotionEffect;
import me.moros.bending.temporal.ActionLimiter;
import me.moros.bending.temporal.TempArmor;
import me.moros.bending.temporal.TempBlock;
import me.moros.bending.temporal.TempEntity;
import me.moros.bending.util.BendingEffect;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.metadata.Metadata;
import me.moros.math.FastMath;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

public class EntityListener implements Listener {
  private final Game game;

  public EntityListener(Game game) {
    this.game = game;
    EventBus.INSTANCE.subscribe(TickEffectEvent.class, this::onEntityFreeze);
  }

  private boolean disabledWorld(EntityEvent event) {
    return !game.worldManager().isEnabled(PlatformAdapter.fromNsk(event.getEntity().getWorld().getKey()));
  }

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
    if (disabledWorld(event)) {
      return;
    }
    if (ActionLimiter.isLimited(event.getEntity().getUniqueId())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onEntityInteractEvent(EntityInteractEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    if (ActionLimiter.isLimited(event.getEntity().getUniqueId(), ActionType.INTERACT)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onProjectileLaunchEvent(ProjectileLaunchEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    if (ActionLimiter.isLimited(event.getEntity().getUniqueId(), ActionType.SHOOT)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onEntityShootBowEvent(EntityShootBowEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    if (ActionLimiter.isLimited(event.getEntity().getUniqueId(), ActionType.SHOOT)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onSlimeSplitEvent(SlimeSplitEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    if (ActionLimiter.isLimited(event.getEntity().getUniqueId())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onEntityTarget(EntityTargetEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    if (ActionLimiter.isLimited(event.getEntity().getUniqueId())) {
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
    Block block = event.getDamager();
    if (block == null || disabledWorld(event)) {
      return;
    }
    TempBlock.MANAGER.get(PlatformAdapter.fromBukkitBlock(block)).map(TempBlock::damageSource)
      .ifPresent(source -> DamageUtil.cacheDamageSource(event.getEntity().getUniqueId(), source));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityInsideBlock(EntityInsideBlockEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    TempBlock.MANAGER.get(PlatformAdapter.fromBukkitBlock(event.getBlock())).map(TempBlock::damageSource)
      .ifPresent(source -> DamageUtil.cacheDamageSource(event.getEntity().getUniqueId(), source));
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
      double newDamage = game.activationController().onEntityDamage(entity, cause, oldDamage);
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
  public void onEntityDamageByExplosion(EntityDamageByEntityEvent event) {
    if (event.getDamage() <= 0 || disabledWorld(event)) {
      return;
    }
    if (event.getEntity() instanceof LivingEntity entity) {
      if (event.getCause() == DamageCause.BLOCK_EXPLOSION || event.getCause() == DamageCause.ENTITY_EXPLOSION) {
        User user = Registries.BENDERS.get(entity.getUniqueId());
        if (user != null) {
          double oldDmg = event.getDamage();
          double newDmg = FireShield.shieldFromExplosion(user, PlatformAdapter.fromBukkitEntity(event.getDamager()), oldDmg);
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
  public void onUserGlide(EntityToggleGlideEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    if (!event.isGliding() && event.getEntity() instanceof LivingEntity entity) {
      if (ActionLimiter.isLimited(event.getEntity().getUniqueId(), ActionType.MOVE)) {
        return;
      }
      User user = Registries.BENDERS.get(entity.getUniqueId());
      if (user != null && game.activationController().onUserGlide(user)) {
        event.setCancelled(true);
      }
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onEntityDeath(EntityDeathEvent event) {
    var nsk = PlatformAdapter.nsk(Metadata.ARMOR_KEY);
    event.getDrops().removeIf(item -> item.getItemMeta().getPersistentDataContainer().has(nsk));
    boolean keepInventory = event instanceof PlayerDeathEvent pde && pde.getKeepInventory();
    TempArmor.MANAGER.get(event.getEntity().getUniqueId()).ifPresent(tempArmor -> {
      if (!keepInventory) {
        event.getDrops().addAll(tempArmor.snapshot().map(PlatformAdapter::toBukkitItem).toList());
      }
      tempArmor.revert();
    });
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onEntityDeathHigh(EntityDeathEvent event) {
    if (disabledWorld(event) || event instanceof PlayerDeathEvent) {
      return;
    }
    DamageSource cause = DamageUtil.cachedDamageSource(event.getEntity().getUniqueId());
    if (cause != null && cause.ability().element() == Element.FIRE) {
      Collection<ItemStack> newDrops = new ArrayList<>();
      Iterator<ItemStack> it = event.getDrops().iterator();
      while (it.hasNext()) {
        ItemStack item = it.next();
        var mapped = MaterialUtil.COOKABLE.get(PlatformAdapter.fromBukkitItem(item.getType()));
        Material flamed = mapped == null ? null : PlatformAdapter.toBukkitItemMaterial(mapped);
        if (flamed != null) {
          newDrops.add(new ItemStack(flamed, item.getAmount()));
          it.remove();
        }
        event.getDrops().addAll(newDrops);
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onUserDeath(EntityDeathEvent event) {
    UUID uuid = event.getEntity().getUniqueId();
    ActionLimiter.MANAGER.get(uuid).ifPresent(ActionLimiter::revert);
    if (disabledWorld(event) || event instanceof PlayerDeathEvent) {
      return;
    }
    User user = Registries.BENDERS.get(uuid);
    if (user != null) {
      game.activationController().onUserDeconstruct(user);
    }
  }

  private void onEntityFreeze(TickEffectEvent event) {
    if (!game.worldManager().isEnabled(event.target().worldKey())) {
      return;
    }
    if (event.type() == BendingEffect.FROST_TICK && event.target() instanceof me.moros.bending.platform.entity.LivingEntity entity) {
      int duration = event.duration();
      if (duration > 30) {
        int potionDuration = FastMath.round(0.5 * duration);
        int power = FastMath.floor(duration / 30.0);
        EntityUtil.tryAddPotion(entity, PotionEffect.SLOWNESS, potionDuration, power);
      }
    }
  }
}
