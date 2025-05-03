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

package me.moros.bending.fabric.platform.entity;

import java.util.Objects;

import me.moros.bending.api.platform.entity.EntityProperties;
import me.moros.bending.api.platform.entity.EntityType;
import me.moros.bending.common.data.DataProviderRegistry;
import me.moros.bending.fabric.mixin.accessor.CreeperAccess;
import me.moros.bending.fabric.mixin.accessor.EntityAccess;
import me.moros.bending.fabric.platform.PlatformAdapter;
import me.moros.bending.fabric.platform.world.FabricWorld;
import me.moros.math.Vector3d;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;

final class FabricEntityProperties {
  private FabricEntityProperties() {
  }

  static final DataProviderRegistry<Entity> PROPERTIES;

  static {
    PROPERTIES = DataProviderRegistry.builder(Entity.class)
      // boolean
      .create(EntityProperties.SNEAKING, Entity.class, b -> b
        .get(Entity::isShiftKeyDown)
        .set(Entity::setShiftKeyDown))
      .create(EntityProperties.SPRINTING, Entity.class, b -> b
        .get(Entity::isSprinting)
        .set(Entity::setSprinting))
      .create(EntityProperties.ALLOW_FLIGHT, ServerPlayer.class, b -> b
        .get(p -> p.getAbilities().mayfly)
        .set((player, allowFlight) -> {
          var handler = player.getAbilities();
          if (handler.flying && !allowFlight) {
            handler.flying = false;
          }
          handler.mayfly = allowFlight;
          player.onUpdateAbilities();
        }))
      .create(EntityProperties.FLYING, ServerPlayer.class, b -> b
        .get(e -> e.getAbilities().flying)
        .set((player, flying) -> {
          var handler = player.getAbilities();
          boolean needsUpdate = handler.flying != flying;
          if (!handler.mayfly && flying) {
            throw new IllegalArgumentException("Player is not allowed to fly.");
          }
          handler.flying = flying;
          if (needsUpdate) {
            player.onUpdateAbilities();
          }
        }))
      .create(EntityProperties.GLIDING, LivingEntity.class, b -> b
        .get(LivingEntity::isFallFlying)
        .set((e, v) -> ((EntityAccess) e).bending$setSharedFlag(7, v)))
      .create(EntityProperties.CHARGED, Creeper.class, b -> b
        .get(Creeper::isPowered)
        .set((e, v) -> e.getEntityData().set(CreeperAccess.bending$getDataIsPowered(), v)))
      .create(EntityProperties.ALLOW_PICKUP, ItemEntity.class, b -> b
        .get(ItemEntity::hasPickUpDelay)
        .set((e, v) -> {
          if (v) {
            e.setDefaultPickUpDelay();
          } else {
            e.setNeverPickUp();
          }
        }))
      .create(EntityProperties.AI, Mob.class, b -> b
        .get(e -> !e.isNoAi())
        .set((e, v) -> e.setNoAi(!v)))
      .create(EntityProperties.GRAVITY, Entity.class, b -> b
        .get(e -> !e.isNoGravity())
        .set((e, v) -> e.setNoGravity(!v)))
      .create(EntityProperties.INVULNERABLE, Entity.class, b -> b
        .get(Entity::isInvulnerable)
        .set(Entity::setInvulnerable))
      .create(EntityProperties.IN_WATER, Entity.class, b -> b
        .get(Entity::isInWater))
      .create(EntityProperties.IN_LAVA, Entity.class, b -> b
        .get(Entity::isInLava))
      .create(EntityProperties.INVISIBLE, Entity.class, b -> b
        .get(Entity::isInvisible)
        .set(Entity::setInvisible))
      .create(EntityProperties.DEAD, Entity.class, b -> b
        .get(e -> !e.isAlive()))
      // integer
      .create(EntityProperties.ENTITY_ID, Entity.class, b -> b
        .get(Entity::getId))
      .create(EntityProperties.MAX_OXYGEN, Entity.class, b -> b
        .get(Entity::getMaxAirSupply))
      .create(EntityProperties.REMAINING_OXYGEN, Entity.class, b -> b
        .get(Entity::getAirSupply)
        .set(Entity::setAirSupply))
      .create(EntityProperties.REQUIRED_TICKS_TO_FREEZE, Entity.class, b -> b
        .get(Entity::getTicksRequiredToFreeze))
      .create(EntityProperties.FREEZE_TICKS, Entity.class, b -> b
        .get(Entity::getTicksFrozen)
        .set(Entity::setTicksFrozen))
      .create(EntityProperties.FIRE_IMMUNE_TICKS, Entity.class, b -> b
        .get(e -> ((EntityAccess) e).bending$maxFireTicks()))
      .create(EntityProperties.FIRE_TICKS, Entity.class, b -> b
        .get(Entity::getRemainingFireTicks)
        .set(Entity::setRemainingFireTicks))
      // double
      .create(EntityProperties.WIDTH, Entity.class, b -> b
        .get(e -> (double) e.getBbWidth()))
      .create(EntityProperties.HEIGHT, Entity.class, b -> b
        .get(e -> (double) e.getBbHeight()))
      // float
      .create(EntityProperties.YAW, Entity.class, b -> b
        .get(Entity::getYRot))
      .create(EntityProperties.PITCH, Entity.class, b -> b
        .get(Entity::getXRot))
      .create(EntityProperties.FALL_DISTANCE, Entity.class, b -> b
        .get(e -> e.fallDistance)
        .set((e, v) -> e.fallDistance = v))
      .create(EntityProperties.MAX_HEALTH, LivingEntity.class, b -> b
        .get(LivingEntity::getMaxHealth))
      .create(EntityProperties.HEALTH, LivingEntity.class, b -> b
        .get(LivingEntity::getHealth))
      // misc
      .create(EntityProperties.NAME, Entity.class, b -> b
        .get(e -> MinecraftServerAudiences.of(Objects.requireNonNull(e.getServer())).asAdventure(e.getName())))
      .create(EntityProperties.POSITION, Entity.class, b -> b
        .get(e -> Vector3d.of(e.getX(), e.getY(), e.getZ()))
        .set((e, v) -> e.snapTo(v.x(), v.y(), v.z())))
      .create(EntityProperties.VELOCITY, Entity.class, b -> b
        .valueOperator(Vector3d::clampVelocity)
        .get(e -> {
          var vel = e.getDeltaMovement();
          return Vector3d.of(vel.x(), vel.y(), vel.z());
        })
        .set((e, v) -> {
          e.setDeltaMovement(v.x(), v.y(), v.z());
          e.hurtMarked = true;
        }))
      .create(EntityProperties.WORLD, Entity.class, b -> b
        .get(e -> new FabricWorld((ServerLevel) e.level())))
      .create(EntityProperties.UUID, Entity.class, b -> b
        .get(Entity::getUUID))
      .create(EntityProperties.ENTITY_TYPE, Entity.class, b -> b
        .get(e -> EntityType.registry().getOrThrow(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()))))
      .create(EntityProperties.RIGHT_HAND, ServerPlayer.class, b -> b
        .get(e -> e.getMainArm() == HumanoidArm.RIGHT))
      .create(EntityProperties.GAMEMODE, ServerPlayer.class, b -> b
        .get(e -> PlatformAdapter.fromFabricGameMode(e.gameMode.getGameModeForPlayer())))
      .build();
  }
}
