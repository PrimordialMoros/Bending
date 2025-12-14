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

package me.moros.bending.paper.platform.entity;

import java.util.Objects;

import me.moros.bending.api.platform.entity.EntityProperties;
import me.moros.bending.api.platform.entity.EntityType;
import me.moros.bending.common.data.DataProviderRegistry;
import me.moros.bending.paper.platform.PlatformAdapter;
import me.moros.bending.paper.platform.world.BukkitWorld;
import me.moros.math.Vector3d;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MainHand;
import org.bukkit.util.Vector;

final class BukkitEntityProperties {
  private BukkitEntityProperties() {
  }

  static final DataProviderRegistry<Entity> PROPERTIES;

  static {
    PROPERTIES = DataProviderRegistry.builder(Entity.class)
      // boolean
      .create(EntityProperties.SNEAKING, Entity.class, b -> b
        .get(Entity::isSneaking)
        .set(Entity::setSneaking))
      .create(EntityProperties.SPRINTING, Player.class, b -> b
        .get(Player::isSprinting)
        .set(Player::setSprinting))
      .create(EntityProperties.ALLOW_FLIGHT, Player.class, b -> b
        .get(Player::getAllowFlight)
        .set(Player::setAllowFlight))
      .create(EntityProperties.FLYING, Player.class, b -> b
        .get(Player::isFlying)
        .set(Player::setFlying))
      .create(EntityProperties.GLIDING, LivingEntity.class, b -> b
        .get(LivingEntity::isGliding)
        .set(LivingEntity::setGliding))
      .create(EntityProperties.CHARGED, Creeper.class, b -> b
        .get(Creeper::isPowered)
        .set(Creeper::setPowered))
      .create(EntityProperties.ALLOW_PICKUP, Item.class, b -> b
        .get(Item::canPlayerPickup)
        .set(Item::setCanPlayerPickup))
      .create(EntityProperties.AI, LivingEntity.class, b -> b
        .get(LivingEntity::hasAI)
        .set(LivingEntity::setAI))
      .create(EntityProperties.GRAVITY, Entity.class, b -> b
        .get(Entity::hasGravity)
        .set(Entity::setGravity))
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
        .get(Entity::isDead))
      // integer
      .create(EntityProperties.ENTITY_ID, Entity.class, b -> b
        .get(Entity::getEntityId))
      .create(EntityProperties.MAX_OXYGEN, LivingEntity.class, b -> b
        .get(LivingEntity::getMaximumAir))
      .create(EntityProperties.REMAINING_OXYGEN, LivingEntity.class, b -> b
        .get(LivingEntity::getRemainingAir)
        .set(LivingEntity::setRemainingAir))
      .create(EntityProperties.REQUIRED_TICKS_TO_FREEZE, Entity.class, b -> b
        .get(Entity::getMaxFreezeTicks))
      .create(EntityProperties.FREEZE_TICKS, Entity.class, b -> b
        .get(Entity::getFreezeTicks)
        .set(Entity::setFreezeTicks))
      .create(EntityProperties.FIRE_IMMUNE_TICKS, Entity.class, b -> b
        .get(Entity::getMaxFireTicks))
      .create(EntityProperties.FIRE_TICKS, Entity.class, b -> b
        .get(Entity::getFireTicks)
        .set(Entity::setFireTicks))
      // double
      .create(EntityProperties.WIDTH, Entity.class, b -> b
        .get(Entity::getWidth))
      .create(EntityProperties.HEIGHT, Entity.class, b -> b
        .get(Entity::getHeight))
      // float
      .create(EntityProperties.YAW, Entity.class, b -> b
        .get(Entity::getYaw))
      .create(EntityProperties.PITCH, Entity.class, b -> b
        .get(Entity::getPitch))
      .create(EntityProperties.FALL_DISTANCE, Entity.class, b -> b
        .get(e -> (double) e.getFallDistance())
        .set((e, d) -> e.setFallDistance(d.floatValue())))
      .create(EntityProperties.MAX_HEALTH, LivingEntity.class, b -> b
        .get(e -> (float) Objects.requireNonNull(e.getAttribute(Attribute.MAX_HEALTH)).getValue()))
      .create(EntityProperties.HEALTH, LivingEntity.class, b -> b
        .get(e -> (float) e.getHealth()))
      // misc
      .create(EntityProperties.NAME, Entity.class, b -> b
        .get(Entity::name))
      .create(EntityProperties.POSITION, Entity.class, b -> b
        .get(e -> Vector3d.of(e.getX(), e.getY(), e.getZ()))
        .set((e, v) -> e.teleport(new Location(e.getWorld(), v.x(), v.y(), v.z()))))
      .create(EntityProperties.VELOCITY, Entity.class, b -> b
        .valueOperator(Vector3d::clampVelocity)
        .get(e -> {
          var vel = e.getVelocity();
          return Vector3d.of(vel.getX(), vel.getY(), vel.getZ());
        })
        .set((e, v) -> e.setVelocity(new Vector(v.x(), v.y(), v.z()))))
      .create(EntityProperties.WORLD, Entity.class, b -> b
        .get(e -> new BukkitWorld(e.getWorld())))
      .create(EntityProperties.UUID, Entity.class, b -> b
        .get(Entity::getUniqueId))
      .create(EntityProperties.ENTITY_TYPE, Entity.class, b -> b
        .get(e -> EntityType.registry().getOrThrow(e.getType().key())))
      .create(EntityProperties.RIGHT_HAND, Player.class, b -> b
        .get(e -> e.getMainHand() == MainHand.RIGHT))
      .create(EntityProperties.GAMEMODE, Player.class, b -> b
        .get(e -> PlatformAdapter.fromBukkitGameMode(e.getGameMode())))
      .create(EntityProperties.ATTRIBUTES, LivingEntity.class, b -> b
        .get(BukkitAttributes::new))
      .build();
  }
}
