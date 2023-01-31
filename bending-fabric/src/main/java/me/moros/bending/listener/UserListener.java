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

import me.moros.bending.fabric.event.ServerMobEvents;
import me.moros.bending.fabric.event.ServerPlayerEvents;
import me.moros.bending.model.BlockInteraction;
import me.moros.bending.model.EntityInteraction;
import me.moros.bending.model.ability.ActionType;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.manager.Game;
import me.moros.bending.model.registry.Registries;
import me.moros.bending.model.user.User;
import me.moros.bending.platform.PlatformAdapter;
import me.moros.bending.platform.block.Block;
import me.moros.bending.platform.item.ItemUtil;
import me.moros.bending.temporal.ActionLimiter;
import me.moros.bending.temporal.TempArmor;
import me.moros.bending.util.metadata.Metadata;
import me.moros.math.Vector3d;
import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.checkerframework.checker.nullness.qual.Nullable;

public record UserListener(Game game) implements FabricListener {
  public UserListener(Game game) {
    this.game = game;
    ServerPlayerEvents.INTERACT.register(this::onLeftClickAir);
    ServerPlayerEvents.TOGGLE_SNEAK.register(this::onUserSneak);
    ServerPlayerEvents.TOGGLE_SPRINT.register(this::onUserSprint);
    EntityElytraEvents.ALLOW.register(this::onUserGlide);
    AttackBlockCallback.EVENT.register(this::onLeftClickBlock);
    UseBlockCallback.EVENT.register(this::onRightClickBlock);
    UseItemCallback.EVENT.register(this::onRightClickAir);
    UseEntityCallback.EVENT.register(this::onRightClickEntity);
    ServerPlayerEvents.CHANGE_GAMEMODE.register(this::onUserGameModeChange);
    ServerMobEvents.TARGET.register(this::onEntityTarget);
    ServerEntityEvents.EQUIPMENT_CHANGE.register(this::onArmorChange);
  }

  private boolean validGameMode(ServerPlayer player) {
    return player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR;
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
        game.activationController().onUserSwing(user);
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
        me.moros.bending.platform.entity.Entity entity = null;
        if (entityInteraction != null) {
          entity = entityInteraction.value();
          user.store().add(EntityInteraction.KEY, entityInteraction);
        }
        Block block = null;
        if (blockInteraction != null) {
          block = blockInteraction.value();
          user.store().add(BlockInteraction.KEY, blockInteraction);
        }
        game.activationController().onUserInteract(user, entity, block);
      }
    }
    return InteractionResult.PASS;
  }

  private boolean onUserSneak(ServerPlayer player, boolean sneaking) {
    if (validGameMode(player) && !disabledWorld(player)) {
      User user = Registries.BENDERS.get(player.getUUID());
      if (user != null) {
        game.activationController().onUserSneak(user, sneaking);
      }
    }
    return true;
  }

  private boolean onUserSprint(ServerPlayer player, boolean sprinting) {
    if (sprinting && validGameMode(player) && !disabledWorld(player)) {
      return !game.activationController().hasSpout(player.getUUID());
    }
    return true;
  }

  private boolean onUserGlide(LivingEntity entity) {
    if (entity.getLevel() instanceof ServerLevel world && !disabledWorld(world)) {
      if (ActionLimiter.isLimited(entity.getUUID(), ActionType.MOVE)) {
        return false;
      }
      User user = Registries.BENDERS.get(entity.getUUID());
      return user == null || !game.activationController().onUserGlide(user);
    }
    return true;
  }

  private void onUserGameModeChange(ServerPlayer player, GameType gameType) {
    if (!disabledWorld(player) && gameType == GameType.SPECTATOR) {
      User user = Registries.BENDERS.get(player.getUUID());
      if (user != null) {
        user.board().updateAll();
        game.abilityManager(user.world().key()).destroyUserInstances(user, a -> !a.description().isActivatedBy(Activation.PASSIVE));
      }
    }
  }

  private boolean onEntityTarget(LivingEntity entity, Entity target) {
    return disabledWorld(entity) || !ActionLimiter.isLimited(entity.getUUID());
  }

  private void onArmorChange(LivingEntity livingEntity, EquipmentSlot equipmentSlot, ItemStack previousStack, ItemStack currentStack) {
    if (equipmentSlot.isArmor() && ItemUtil.hasKey(previousStack, Metadata.ARMOR_KEY)) {
      if (TempArmor.MANAGER.isTemp(livingEntity.getUUID())) {
        livingEntity.setItemSlot(equipmentSlot, previousStack); // TODO mixin a cancellable event instead?
      }
    }
  }
}
