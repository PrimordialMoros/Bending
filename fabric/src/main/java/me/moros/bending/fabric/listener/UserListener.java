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

package me.moros.bending.fabric.listener;

import java.util.UUID;
import java.util.function.Supplier;

import me.moros.bending.api.ability.ActionType;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.game.Game;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.temporal.ActionLimiter;
import me.moros.bending.api.temporal.TempArmor;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.metadata.BlockInteraction;
import me.moros.bending.api.util.metadata.EntityInteraction;
import me.moros.bending.api.util.metadata.Metadata;
import me.moros.bending.common.ability.earth.EarthGlove;
import me.moros.bending.common.ability.earth.MetalCable;
import me.moros.bending.fabric.event.ServerInventoryEvents;
import me.moros.bending.fabric.event.ServerMobEvents;
import me.moros.bending.fabric.event.ServerPlayerEvents;
import me.moros.bending.fabric.platform.FabricMetadata;
import me.moros.bending.fabric.platform.PlatformAdapter;
import me.moros.bending.fabric.platform.entity.FabricEntity;
import me.moros.bending.fabric.platform.item.ItemUtil;
import me.moros.math.Vector3d;
import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.checkerframework.checker.nullness.qual.Nullable;

public record UserListener(Supplier<Game> gameSupplier) implements FabricListener {
  public UserListener(Supplier<Game> gameSupplier) {
    this.gameSupplier = gameSupplier;
    net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.COPY_FROM.register(this::onPlayerRespawn);
    me.moros.bending.fabric.event.ServerEntityEvents.PROJECTILE_HIT.register(this::onArrowHit);
    me.moros.bending.fabric.event.ServerEntityEvents.FALLING_BLOCK.register(this::onFallingBlock);
    ServerPlayerEvents.INTERACT.register(this::onLeftClickAir);
    ServerPlayerEvents.TOGGLE_SNEAK.register(this::onUserSneak);
    ServerPlayerEvents.TOGGLE_SPRINT.register(this::onUserSprint);
    EntityElytraEvents.ALLOW.register(this::onUserGlide);
    AttackBlockCallback.EVENT.register(this::onLeftClickBlock);
    UseBlockCallback.EVENT.register(this::onRightClickBlock);
    UseItemCallback.EVENT.register(this::onRightClickAir);
    UseEntityCallback.EVENT.register(this::onRightClickEntity);
    ServerPlayerEvents.CHANGE_GAMEMODE.register(this::onUserGameModeChange);
    ServerPlayerEvents.CHANGE_SLOT.register(this::onHeldSlotChange);
    me.moros.bending.fabric.event.ServerEntityEvents.MERGE.register(this::onItemMerge);
    ServerMobEvents.TARGET.register(this::onEntityTarget);
    ServerEntityEvents.EQUIPMENT_CHANGE.register(this::onArmorChange);
    ServerInventoryEvents.HOPPER.register(this::onHopperItemPickup);
    me.moros.bending.fabric.event.ServerEntityEvents.DROP_ITEM.register(this::onDropItem);
    me.moros.bending.fabric.event.ServerEntityEvents.DAMAGE.register(this::onEntityDamage);
    ServerLivingEntityEvents.ALLOW_DAMAGE.register(this::onEntityAllowDamage);
    ServerLivingEntityEvents.AFTER_DEATH.register(this::onUserDeath);
  }

  private boolean validGameMode(ServerPlayer player) {
    return player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR;
  }

  private void onPlayerRespawn(Entity originalEntity, Entity newEntity, boolean alive) {
    User user = Registries.BENDERS.get(originalEntity.getUUID());
    if (user != null) {
      ((FabricEntity) user.entity()).setHandle(newEntity);
    }
  }

  private boolean onArrowHit(Projectile projectile, HitResult hitResult) {
    if (!disabledWorld(projectile) && projectile instanceof Arrow) {
      var data = FabricMetadata.INSTANCE.metadata(projectile).get(MetalCable.CABLE_KEY);
      if (data.isPresent()) {
        MetalCable cable = data.get();
        if (hitResult instanceof BlockHitResult blockHit) {
          var pos = blockHit.getBlockPos();
          var world = PlatformAdapter.fromFabricWorld((ServerLevel) projectile.level);
          cable.hitBlock(world.blockAt(pos.getX(), pos.getY(), pos.getZ()));
        } else if (hitResult instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living) {
          cable.hitEntity(PlatformAdapter.fromFabricEntity(living));
          return false;
        } else {
          projectile.discard();
        }
      }
    }
    return true;
  }

  private boolean onFallingBlock(ServerLevel level, BlockPos blockPos) {
    if (!disabledWorld(level)) {
      var block = PlatformAdapter.fromFabricWorld(level).blockAt(blockPos.getX(), blockPos.getY(), blockPos.getZ());
      return !TempBlock.MANAGER.isTemp(block);
    }
    return true;
  }

  private InteractionResult onLeftClickAir(ServerPlayer player, InteractionHand hand) {
    return onUserSwing(player, hand);
  }

  private InteractionResult onLeftClickBlock(Player playerEntity, Level world, InteractionHand hand, BlockPos blockPos, Direction direction) {
    if (!world.isClientSide && playerEntity instanceof ServerPlayer player) {
      return onUserSwing(player, hand);
    }
    return InteractionResult.PASS;
  }

  private InteractionResult onUserSwing(ServerPlayer player, InteractionHand hand) {
    if (validGameMode(player) && hand != InteractionHand.OFF_HAND && !disabledWorld(player)) {
      User user = Registries.BENDERS.get(player.getUUID());
      if (user != null) {
        game().activationController().onUserSwing(user);
      }
    }
    return InteractionResult.PASS;
  }

  private InteractionResult onRightClickBlock(Player playerEntity, Level world, InteractionHand hand, BlockHitResult blockHitResult) {
    if (hand == InteractionHand.MAIN_HAND && !world.isClientSide && playerEntity instanceof ServerPlayer player) {
      var pos = blockHitResult.getBlockPos();
      var block = PlatformAdapter.fromFabricWorld(player.getLevel()).blockAt(pos.getX(), pos.getY(), pos.getZ());
      Vector3d point = Vector3d.from(blockHitResult.getLocation());
      return onUserInteract(player, null, new BlockInteraction(block, point));
    }
    return InteractionResult.PASS;
  }

  private InteractionResultHolder<ItemStack> onRightClickAir(Player playerEntity, Level world, InteractionHand hand) {
    ItemStack stackInHand = playerEntity.getItemInHand(hand);
    if (!world.isClientSide && playerEntity instanceof ServerPlayer player) {
      onUserInteract(player, null, null);
    }
    return InteractionResultHolder.pass(stackInHand);
  }

  private InteractionResult onRightClickEntity(Player playerEntity, Level world, InteractionHand hand, Entity entity, @Nullable EntityHitResult hitResult) {
    if (hand == InteractionHand.MAIN_HAND && !world.isClientSide && playerEntity instanceof ServerPlayer player) {
      var target = PlatformAdapter.fromFabricEntity(entity);
      Vector3d point = hitResult == null ? null : Vector3d.from(hitResult.getLocation());
      return onUserInteract(player, new EntityInteraction(target, point), null);
    }
    return InteractionResult.PASS;
  }

  private InteractionResult onUserInteract(ServerPlayer player, @Nullable EntityInteraction entityInteraction, @Nullable BlockInteraction blockInteraction) {
    if (validGameMode(player) && !disabledWorld(player)) {
      User user = Registries.BENDERS.get(player.getUUID());
      if (user != null) {
        me.moros.bending.api.platform.entity.Entity entity = null;
        if (entityInteraction != null) {
          entity = entityInteraction.value();
          user.store().add(EntityInteraction.KEY, entityInteraction);
        }
        Block block = null;
        if (blockInteraction != null) {
          block = blockInteraction.value();
          user.store().add(BlockInteraction.KEY, blockInteraction);
        }
        game().activationController().onUserInteract(user, entity, block);
      }
    }
    return InteractionResult.PASS;
  }

  private boolean onUserSneak(ServerPlayer player, boolean sneaking) {
    if (validGameMode(player) && !disabledWorld(player)) {
      User user = Registries.BENDERS.get(player.getUUID());
      if (user != null) {
        game().activationController().onUserSneak(user, sneaking);
      }
    }
    return true;
  }

  private boolean onUserSprint(ServerPlayer player, boolean sprinting) {
    if (sprinting && validGameMode(player) && !disabledWorld(player)) {
      return !game().activationController().hasSpout(player.getUUID());
    }
    return true;
  }

  private boolean onUserGlide(LivingEntity entity) {
    if (entity.getLevel() instanceof ServerLevel world && !disabledWorld(world)) {
      if (ActionLimiter.isLimited(entity.getUUID(), ActionType.MOVE)) {
        return false;
      }
      User user = Registries.BENDERS.get(entity.getUUID());
      return user == null || !game().activationController().onUserGlide(user);
    }
    return true;
  }

  private void onUserGameModeChange(ServerPlayer player, GameType gameType) {
    if (!disabledWorld(player) && gameType == GameType.SPECTATOR) {
      User user = Registries.BENDERS.get(player.getUUID());
      if (user != null) {
        user.board().updateAll();
        game().abilityManager(user.world().key()).destroyUserInstances(user, a -> !a.description().isActivatedBy(Activation.PASSIVE));
      }
    }
  }

  private void onHeldSlotChange(ServerPlayer player, int oldSlot, int newSlot) {
    if (disabledWorld(player)) {
      return;
    }
    User user = Registries.BENDERS.get(player.getUUID());
    if (user != null) {
      user.board().activeSlot(oldSlot + 1, newSlot + 1);
    }
  }

  private boolean onEntityTarget(LivingEntity entity, Entity target) {
    return disabledWorld(entity) || !ActionLimiter.isLimited(entity.getUUID());
  }

  private boolean onItemMerge(ItemEntity first, ItemEntity second) {
    if (!disabledWorld(first)) {
      return isNotGlove(first) && isNotGlove(second);
    }
    return true;
  }

  private boolean isNotGlove(Entity entity) {
    return !FabricMetadata.INSTANCE.has(entity, EarthGlove.GLOVE_KEY);
  }

  private void onArmorChange(LivingEntity livingEntity, EquipmentSlot equipmentSlot, ItemStack previousStack, ItemStack currentStack) {
    if (equipmentSlot.isArmor() && ItemUtil.hasKey(previousStack, Metadata.ARMOR_KEY)) {
      if (TempArmor.MANAGER.isTemp(livingEntity.getUUID())) {
        livingEntity.setItemSlot(equipmentSlot, previousStack); // TODO mixin a cancellable event instead?
      }
    }
  }

  private boolean onHopperItemPickup(Container container, ItemEntity itemEntity) {
    if (!disabledWorld(itemEntity)) {
      var item = itemEntity.getItem().getItem();
      if (item == Items.STONE && !isNotGlove(itemEntity)) {
        itemEntity.discard();
        return false;
      }
    }
    return true;
  }

  private boolean onDropItem(ServerLevel level, ItemStack stack) {
    return disabledWorld(level) || !ItemUtil.hasKey(stack, Metadata.ARMOR_KEY);
  }

  private double onEntityDamage(LivingEntity entity, DamageSource source, double damage) {
    if (disabledWorld(entity)) {
      return damage;
    }
    var livingEntity = PlatformAdapter.fromFabricEntity(entity);
    var cause = PlatformAdapter.fromFabricCause(source);
    Vector3d origin = null;
    if (source instanceof EntityDamageSource entityDamageSource) {
      var sourceEntity = entityDamageSource.getEntity();
      if (sourceEntity != null) {
        if (sourceEntity instanceof Arrow && FabricMetadata.INSTANCE.has(sourceEntity, MetalCable.CABLE_KEY)) {
          return 0;
        } else if (ActionLimiter.isLimited(sourceEntity.getUUID(), ActionType.DAMAGE)) {
          return 0;
        }
        origin = PlatformAdapter.fromFabricEntity(sourceEntity).center();
      }
    }
    return game().activationController().onEntityDamage(livingEntity, cause, damage, origin);
  }

  private boolean onEntityAllowDamage(LivingEntity entity, DamageSource source, float damage) {
    if (damage <= 0) {
      return false;
    }
    if (!disabledWorld(entity)) {
      User user = Registries.BENDERS.get(entity.getUUID());
      if (user != null) {
        game().activationController().onUserDamage(user);
      }
    }
    return true;
  }

  private void onUserDeath(LivingEntity entity, DamageSource damageSource) {
    UUID uuid = entity.getUUID();
    ActionLimiter.MANAGER.get(uuid).ifPresent(ActionLimiter::revert);
    TempArmor.MANAGER.get(uuid).ifPresent(TempArmor::revert);
    if (disabledWorld(entity) || entity instanceof ServerPlayer) {
      return;
    }
    User user = Registries.BENDERS.get(uuid);
    if (user != null) {
      game().activationController().onUserDeconstruct(user);
    }
  }
}
