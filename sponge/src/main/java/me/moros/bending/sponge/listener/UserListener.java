/*
 * Copyright 2020-2025 Moros
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

package me.moros.bending.sponge.listener;

import me.moros.bending.api.ability.ActionType;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.DamageSource;
import me.moros.bending.api.game.Game;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.temporal.ActionLimiter;
import me.moros.bending.api.temporal.TempArmor;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.temporal.TempEntity;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.BendingEffect;
import me.moros.bending.api.util.metadata.BlockInteraction;
import me.moros.bending.api.util.metadata.EntityInteraction;
import me.moros.bending.api.util.metadata.Metadata;
import me.moros.bending.common.ability.earth.EarthGlove;
import me.moros.bending.common.ability.earth.MetalCable;
import me.moros.bending.common.locale.Message;
import me.moros.bending.sponge.platform.PlatformAdapter;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.value.Value.Immutable;
import org.spongepowered.api.data.value.Value.Mutable;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.entity.projectile.arrow.Arrow;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.action.CollideEvent;
import org.spongepowered.api.event.block.CollideBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.entity.SpawnTypes;
import org.spongepowered.api.event.data.ChangeDataHolderEvent;
import org.spongepowered.api.event.entity.ChangeEntityEquipmentEvent;
import org.spongepowered.api.event.entity.CollideEntityEvent;
import org.spongepowered.api.event.entity.ConstructEntityEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.ItemMergeWithItemEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.entity.ai.SetAITargetEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.item.inventory.InteractItemEvent;
import org.spongepowered.api.event.item.inventory.container.ClickContainerEvent;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.LocatableSnapshot;
import org.spongepowered.api.world.server.ServerLocation;

public class UserListener extends SpongeListener {
  public UserListener(Game game) {
    super(game);
  }

  @Listener(order = Order.EARLY)
  public void onArrowHit(CollideEvent.Impact event, @First Arrow entity) {
    if (disabledWorld(event.impactPoint().world())) {
      return;
    }
    var data = entity.get(PlatformAdapter.dataKey(MetalCable.CABLE_KEY));
    if (data.isPresent()) {
      MetalCable cable = data.get();
      if (event instanceof CollideBlockEvent.Impact blockImpact) {
        cable.hitBlock(PlatformAdapter.fromSpongeBlock(blockImpact.targetLocation().asLocatableBlock()));
      } else if (event instanceof CollideEntityEvent.Impact entityImpact) {
        for (var e : entityImpact.entities()) {
          if (e instanceof Living living) {
            cable.hitEntity(PlatformAdapter.fromSpongeEntity(living));
            return;
          }
        }
        entity.remove();
      } else {
        entity.remove();
      }
    }
  }

  @Listener(order = Order.EARLY)
  public void onEntityExplodeEvent(ExplosionEvent.Pre event, @First Living cause) {
    if (disabledWorld(event.world())) {
      return;
    }
    if (ActionLimiter.isLimited(cause.uniqueId())) {
      event.setCancelled(true);
    }
  }

  @Listener(order = Order.EARLY)
  public void onEntityInteract(InteractEntityEvent event, @First Living entity) {
    if (disabledWorld(entity)) {
      return;
    }
    if (ActionLimiter.isLimited(entity.uniqueId(), ActionType.INTERACT)) {
      event.setCancelled(true);
    }
  }

  @Listener(order = Order.EARLY)
  public void onProjectileLaunchEvent(SpawnEntityEvent.Pre event, @First Living entity) {
    if (disabledWorld(entity)) {
      return;
    }
    if (ActionLimiter.isLimited(entity.uniqueId())) {
      event.setCancelled(true);
    } else if (ActionLimiter.isLimited(entity.uniqueId(), ActionType.SHOOT)) {
      event.filterEntities(e -> !(e instanceof Projectile));
    } else {
      var key = PlatformAdapter.dataKey(Metadata.ARMOR_KEY);
      event.filterEntities(e -> !(e instanceof Item item) || item.get(key).isEmpty());
    }
  }

  @Listener(order = Order.EARLY)
  public void onEntityTarget(SetAITargetEvent event) {
    if (disabledWorld(event.agent())) {
      return;
    }
    if (ActionLimiter.isLimited(event.agent().uniqueId())) {
      event.setCancelled(true);
    }
  }

  @Listener(order = Order.EARLY)
  public void onEntityDamageByEntity(DamageEntityEvent event, @First org.spongepowered.api.event.cause.entity.damage.source.DamageSource cause) {
    var entity = event.entity();
    if (disabledWorld(entity)) {
      return;
    }
    var source = cause.source().orElse(null);
    if (source == null) {
      return;
    }
    if (source instanceof Arrow && source.get(PlatformAdapter.dataKey(MetalCable.CABLE_KEY)).isPresent()) {
      event.setCancelled(true);
    } else if (ActionLimiter.isLimited(source.uniqueId(), ActionType.DAMAGE)) {
      event.setCancelled(true);
    }
  }

  @Listener(order = Order.EARLY)
  public void onFallingBlockSpawn(ConstructEntityEvent.Pre event) {
    var loc = event.location();
    if (disabledWorld(loc.world())) {
      return;
    }
    if (SpawnTypes.FALLING_BLOCK.get() == event.context().get(EventContextKeys.SPAWN_TYPE).orElse(null)) {
      var block = PlatformAdapter.fromSpongeWorld(loc.world()).blockAt(loc.blockX(), loc.blockY(), loc.blockZ());
      if (TempBlock.MANAGER.isTemp(block)) {
        event.setCancelled(true);
      }
    }
  }

  @Listener(order = Order.EARLY)
  public void onEntityDamageLow(DamageEntityEvent event, @Getter("entity") Living target, @Root org.spongepowered.api.event.cause.entity.damage.source.DamageSource source) {
    double oldDamage = event.baseDamage();
    if (oldDamage <= 0 || disabledWorld(event.entity())) {
      return;
    }
    var entity = PlatformAdapter.fromSpongeEntity(target);
    var cause = PlatformAdapter.fromSpongeCause(source);
    Vector3d origin = null;
    var sourceEntity = source.source().orElse(null);
    if (sourceEntity != null) {
      origin = PlatformAdapter.fromSpongeEntity(sourceEntity).center();
    }
    var sourceBlock = source.blockSnapshot()
      .flatMap(BlockSnapshot::location)
      .map(ServerLocation::asLocatableBlock)
      .orElse(null);
    if (source.isFire() && sourceBlock != null) {
      onFireDamage(target, sourceBlock);
    }
    double newDamage = game.activationController().onEntityDamage(entity, cause, oldDamage, origin);
    if (newDamage <= 0) {
      event.setCancelled(true);
    } else if (oldDamage != newDamage) {
      event.setBaseDamage(newDamage);
    }
  }

  private void onFireDamage(Living living, LocatableBlock block) {
    TempBlock tb = TempBlock.MANAGER.get(PlatformAdapter.fromSpongeBlock(block)).orElse(null);
    Mutable<Ticks> ticks = living.fireTicks().orElse(null);
    if (tb != null && ticks != null) {
      if (ticks.get().ticks() > BendingEffect.MAX_BLOCK_FIRE_TICKS) {
        ticks.set(Ticks.of(FastMath.ceil(BendingEffect.MAX_BLOCK_FIRE_TICKS)));
      }
    }
  }

  @Listener(order = Order.POST)
  public void onEntityDamage(DamageEntityEvent event, @Getter("entity") Living target) {
    if (event.finalDamage() <= 0 || disabledWorld(event.entity())) {
      return;
    }
    User user = Registries.BENDERS.get(target.uniqueId());
    if (user != null) {
      game.activationController().onUserDamage(user);
    }
  }

  @Listener(order = Order.POST)
  public void onUserDeath(DestructEntityEvent.Death event) {
    var entity = event.entity();
    ActionLimiter.MANAGER.get(entity.uniqueId()).ifPresent(ActionLimiter::revert);
    TempArmor.MANAGER.get(entity.uniqueId()).ifPresent(TempArmor::revert);
    if (disabledWorld(entity) || entity instanceof ServerPlayer) {
      return;
    }
    User user = Registries.BENDERS.get(entity.uniqueId());
    if (user != null) {
      game.activationController().onUserDeconstruct(user);
    }
  }

  @Listener(order = Order.POST)
  public void onPlayerDeath(DestructEntityEvent.Death event, @Getter("entity") ServerPlayer player) {
    DamageSource source = event.cause().first(DamageSource.class).orElseGet(() -> blockCause(event.cause()));
    if (source != null) {
      // TODO check rendering
      event.setMessage(Message.ABILITY_DEATH_MESSAGE.build(player.displayName().get(), source.name(), source.ability()));
    }
  }

  private @Nullable DamageSource blockCause(Cause cause) {
    return cause.first(org.spongepowered.api.event.cause.entity.damage.source.DamageSource.class)
      .flatMap(org.spongepowered.api.event.cause.entity.damage.source.DamageSource::blockSnapshot)
      .flatMap(LocatableSnapshot::location)
      .map(ServerLocation::asLocatableBlock)
      .map(PlatformAdapter::fromSpongeBlock).flatMap(TempBlock.MANAGER::get)
      .map(TempBlock::damageSource).orElse(null);
  }

  @Listener(order = Order.EARLY)
  public void onUserMove(MoveEntityEvent event) {
    if (disabledWorld(event.entity())) {
      return;
    }
    if (cancelMovement(event)) {
      event.setCancelled(true);
    }
  }

  private boolean hasChangedBlock(MoveEntityEvent event) {
    return !event.originalPosition().toInt().equals(event.destinationPosition().toInt());
  }

  private boolean cancelMovement(MoveEntityEvent event) {
    if (hasChangedBlock(event) && ActionLimiter.isLimited(event.entity().uniqueId(), ActionType.MOVE)) {
      return true;
    }
    User user = Registries.BENDERS.get(event.entity().uniqueId());
    if (user != null) {
      double x = event.destinationPosition().x() - event.originalPosition().x();
      double z = event.destinationPosition().z() - event.originalPosition().z();
      game.activationController().onUserMove(user, Vector3d.of(x, 0, z));
    }
    return false;
  }

  @Listener(order = Order.LAST)
  public void onPickupItem(ChangeInventoryEvent.Pickup.Pre event, @Root Living entity) {
    if (disabledWorld(entity)) {
      return;
    }
    game.activationController().ignoreNextSwing(entity.uniqueId());
  }

  @Listener(order = Order.LAST)
  public void onItemDrop(DropItemEvent.Destruct event) {
    var key = PlatformAdapter.dataKey(Metadata.ARMOR_KEY);
    event.filterEntities(e -> e.get(key).isEmpty());
  }

  @Listener(order = Order.EARLY)
  public void onItemMerge(ItemMergeWithItemEvent event) {
    if (disabledWorld(event.item())) {
      return;
    }
    var key = PlatformAdapter.dataKey(EarthGlove.GLOVE_KEY);
    if (event.item().get(key).isPresent() || event.itemToMerge().get(key).isPresent()) {
      event.setCancelled(true);
    }
  }

  @Listener(order = Order.EARLY)
  public void onEntityInteractEarly(InteractEntityEvent event, @Root Living entity) {
    if (disabledWorld(entity)) {
      return;
    }
    if (ActionLimiter.isLimited(entity.uniqueId(), ActionType.INTERACT)) {
      event.setCancelled(true);
    } else if (TempEntity.MANAGER.isTemp(((net.minecraft.world.entity.Entity) event.entity()).getId())) {
      event.setCancelled(true);
    }
  }

  @Listener(order = Order.EARLY)
  public void onBlockInteractEarly(InteractBlockEvent.Primary.Start event, @Root Living entity) {
    onInteraction(event, entity);
  }

  @Listener(order = Order.EARLY)
  public void onBlockInteractEarly(InteractBlockEvent.Secondary event, @Root Living entity) {
    onInteraction(event, entity);
  }

  @Listener(order = Order.EARLY)
  public void onItemInteractEarly(InteractItemEvent.Secondary event, @Root Living entity) {
    onInteraction(event, entity);
  }

  private void onInteraction(Cancellable event, Living entity) {
    if (disabledWorld(entity)) {
      return;
    }
    if (ActionLimiter.isLimited(entity.uniqueId(), ActionType.INTERACT)) {
      event.setCancelled(true);
    }
  }

  @Listener(order = Order.POST)
  public void onEntityInteract(InteractEntityEvent.Secondary.At event, @Root Living entity) {
    if (disabledWorld(entity)) {
      return;
    }
    User user = Registries.BENDERS.get(entity.uniqueId());
    if (user != null) {
      var target = PlatformAdapter.fromSpongeEntity(event.entity());
      var point = event.interactionPoint();
      user.store().add(EntityInteraction.KEY, new EntityInteraction(target, Vector3d.of(point.x(), point.y(), point.z())));
      game.activationController().onUserInteract(user, target, null);
    }
  }

  @Listener(order = Order.POST)
  public void onBlockInteract(InteractBlockEvent event, @Root Living entity) {
    if (disabledWorld(entity)) {
      return;
    }
    User user = Registries.BENDERS.get(entity.uniqueId());
    if (user != null) {
      var loc = event.block().location().orElse(null);
      if (loc != null && event instanceof InteractBlockEvent.Secondary secondary) {
        var target = PlatformAdapter.fromSpongeWorld(loc.world()).blockAt(loc.blockX(), loc.blockY(), loc.blockZ());
        var point = secondary.interactionPoint();
        user.store().add(BlockInteraction.KEY, new BlockInteraction(target, Vector3d.of(point.x(), point.y(), point.z())));
        game.activationController().onUserInteract(user, null, target);
      } else if (event instanceof InteractBlockEvent.Primary.Start) {
        game.activationController().onUserSwing(user);
      }
    }
  }

  @Listener(order = Order.POST)
  public void onItemInteract(InteractItemEvent event, @Root Living entity) {
    if (disabledWorld(entity)) {
      return;
    }
    User user = Registries.BENDERS.get(entity.uniqueId());
    if (user != null) {
      if (event instanceof InteractItemEvent.Primary) {
        game.activationController().onUserSwing(user);
      } else {
        game.activationController().onUserInteract(user, null, null);
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Listener(order = Order.EARLY)
  public void onValueChange(ChangeDataHolderEvent.ValueChange event, @Getter("targetHolder") Living entity) {
    if (disabledWorld(entity)) {
      return;
    }
    var result = event.endResult();
    var builder = DataTransactionResult.builder().from(result);
    for (var entry : result.successfulData()) {
      var key = entry.key();
      if (key == Keys.IS_SPRINTING && onUserSprint(entity, ((Immutable<Boolean>) entry).get())) {
        builder.reject(entry);
      } else if (key == Keys.IS_ELYTRA_FLYING && onUserGlide(entity, ((Immutable<Boolean>) entry).get())) {
        builder.reject(entry);
      }
    }
    event.proposeChanges(builder.build());
  }

  private boolean onUserSprint(Living entity, boolean sprinting) {
    return sprinting && game.activationController().hasSpout(entity.uniqueId());
  }

  private boolean onUserGlide(Living entity, boolean gliding) {
    var uuid = entity.uniqueId();
    if (ActionLimiter.isLimited(entity.uniqueId(), ActionType.MOVE)) {
      return true;
    }
    if (!gliding) {
      User user = Registries.BENDERS.get(uuid);
      return user != null && game.activationController().onUserGlide(user);
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  @Listener(order = Order.POST)
  public void onValueChangeMonitor(ChangeDataHolderEvent.ValueChange event, @Getter("targetHolder") Living entity) {
    if (disabledWorld(entity)) {
      return;
    }
    var result = event.endResult();
    for (var entry : result.successfulData()) {
      var key = entry.key();
      if (key == Keys.IS_SNEAKING) {
        onUserSneak(entity, ((Immutable<Boolean>) entry).get());
      } else if (key == Keys.GAME_MODE) {
        onUserGamemodeChange(entity, ((Immutable<GameMode>) entry).get());
      }
    }
  }

  private void onUserSneak(Living entity, boolean sneaking) {
    User user = Registries.BENDERS.get(entity.uniqueId());
    if (user != null) {
      game.activationController().onUserSneak(user, sneaking);
    }
  }

  private void onUserGamemodeChange(Living entity, GameMode gamemode) {
    if (gamemode == GameModes.SPECTATOR.get()) {
      User user = Registries.BENDERS.get(entity.uniqueId());
      if (user != null) {
        user.board().updateAll();
        game.abilityManager(user.world().key()).destroyUserInstances(user, a -> !a.description().isActivatedBy(Activation.PASSIVE));
      }
    }
  }

  @Listener(order = Order.POST)
  public void onPlayerSlotChange(ChangeInventoryEvent.Held event, @First Living entity) {
    if (disabledWorld(entity)) {
      return;
    }
    var originalSlot = event.originalSlot().getInt(Keys.SLOT_INDEX).orElse(-1);
    var finalSlot = event.finalSlot().getInt(Keys.SLOT_INDEX).orElse(-1);
    User user = Registries.BENDERS.get(entity.uniqueId());
    if (user != null) {
      user.board().activeSlot(originalSlot + 1, finalSlot + 1);
    }
  }

  @Listener(order = Order.EARLY)
  public void onArmorChange(ChangeEntityEquipmentEvent event) {
    boolean isTemp = TempArmor.MANAGER.isTemp(event.entity().uniqueId());
    var tr = event.transaction();
    var item = tr.original();
    var item2 = tr.finalReplacement();
    var key = PlatformAdapter.dataKey(Metadata.ARMOR_KEY);
    if (tr.isValid() && !item.equals(item2) && item.get(key).isPresent()) {
      if (isTemp) {
        tr.invalidate();
        event.setCancelled(true);
      } else {
        tr.setCustom(ItemStackSnapshot.empty());
      }
    }
  }

  @Listener(order = Order.EARLY)
  public void onInventoryClick(ClickContainerEvent event, @First ServerPlayer player) {
    for (var tr : event.transactions()) {
      var key = PlatformAdapter.dataKey(Metadata.ARMOR_KEY);
      if (tr.original().get(key).isPresent() || tr.finalReplacement().get(key).isPresent()) {
        tr.invalidate();
        tr.setCustom(ItemStackSnapshot.empty());
      }
    }
  }
}
