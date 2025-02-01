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

package me.moros.bending.sponge.platform.entity;

import java.util.Objects;

import me.moros.bending.api.platform.entity.EntityProperties;
import me.moros.bending.api.platform.entity.EntityType;
import me.moros.bending.api.platform.entity.player.GameMode;
import me.moros.bending.common.data.DataProviderRegistry;
import me.moros.bending.sponge.platform.PlatformAdapter;
import me.moros.bending.sponge.platform.world.SpongeWorld;
import me.moros.math.Vector3d;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandPreferences;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.Agent;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.monster.Creeper;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.util.Ticks;

import static me.moros.bending.sponge.platform.entity.PropertyMapper.property;
import static me.moros.bending.sponge.platform.entity.PropertyMapper.readOnly;

final class SpongeEntityProperties {
  private SpongeEntityProperties() {
  }

  static final DataProviderRegistry<Entity> PROPERTIES;

  static {
    PROPERTIES = DataProviderRegistry.builder(Entity.class)
      // boolean
      .create(EntityProperties.SNEAKING, Entity.class, b -> property(b, Keys.IS_SNEAKING))
      .create(EntityProperties.SPRINTING, Entity.class, b -> property(b, Keys.IS_SPRINTING))
      .create(EntityProperties.ALLOW_FLIGHT, Player.class, b -> property(b, Keys.CAN_FLY))
      .create(EntityProperties.FLYING, Player.class, b -> property(b, Keys.IS_FLYING))
      .create(EntityProperties.GLIDING, Player.class, b -> property(b, Keys.IS_ELYTRA_FLYING))
      .create(EntityProperties.CHARGED, Creeper.class, b -> property(b, Keys.IS_CHARGED))
      .create(EntityProperties.ALLOW_PICKUP, Item.class, b -> property(b, Keys.PICKUP_DELAY,
        v -> !v.isInfinite(), v -> v ? Ticks.of(10) : Ticks.infinite())) // default pickup delay = 10
      .create(EntityProperties.AI, Agent.class, b -> property(b, Keys.IS_AI_ENABLED))
      .create(EntityProperties.GRAVITY, Entity.class, b -> property(b, Keys.IS_GRAVITY_AFFECTED))
      .create(EntityProperties.INVULNERABLE, Entity.class, b -> property(b, Keys.INVULNERABLE))
      .create(EntityProperties.IN_WATER, Entity.class, b -> b
        .get(e -> ((net.minecraft.world.entity.Entity) e).isInWaterOrBubble()))
      .create(EntityProperties.IN_LAVA, Entity.class, b -> b
        .get(e -> ((net.minecraft.world.entity.Entity) e).isInLava()))
      .create(EntityProperties.INVISIBLE, Entity.class, b -> property(b, Keys.IS_INVISIBLE))
      .create(EntityProperties.DEAD, Entity.class, b -> b
        .get(e -> !((net.minecraft.world.entity.Entity) e).isAlive()))
      // integer
      .create(EntityProperties.ENTITY_ID, Entity.class, b -> b
        .get(e -> ((net.minecraft.world.entity.Entity) e).getId()))
      .create(EntityProperties.MAX_OXYGEN, Living.class, b -> readOnly(b, Keys.MAX_AIR))
      .create(EntityProperties.REMAINING_OXYGEN, Living.class, b -> property(b, Keys.REMAINING_AIR))
      .create(EntityProperties.REQUIRED_TICKS_TO_FREEZE, Entity.class, b -> readOnly(b, Keys.MAX_FROZEN_TIME,
        v -> (int) v.ticks()))
      .create(EntityProperties.FREEZE_TICKS, Entity.class, b -> property(b, Keys.FROZEN_TIME,
        v -> (int) v.ticks(), v -> Ticks.of((long) v)))
      .create(EntityProperties.FIRE_IMMUNE_TICKS, Entity.class, b -> readOnly(b, Keys.FIRE_DAMAGE_DELAY,
        v -> (int) v.ticks()))
      .create(EntityProperties.FIRE_TICKS, Entity.class, b -> property(b, Keys.FIRE_TICKS,
        v -> (int) v.ticks(), v -> Ticks.of((long) v)))
      // double
      .create(EntityProperties.WIDTH, Entity.class, b -> readOnly(b, Keys.BASE_SIZE))
      .create(EntityProperties.HEIGHT, Entity.class, b -> readOnly(b, Keys.HEIGHT))
      // float
      .create(EntityProperties.YAW, Entity.class, b -> b
        .get(e -> ((net.minecraft.world.entity.Entity) e).getYRot()))
      .create(EntityProperties.PITCH, Entity.class, b -> b
        .get(e -> ((net.minecraft.world.entity.Entity) e).getXRot()))
      .create(EntityProperties.FALL_DISTANCE, Entity.class, b -> property(b, Keys.FALL_DISTANCE,
        Double::floatValue, v -> (double) v))
      .create(EntityProperties.MAX_HEALTH, Living.class, b -> readOnly(b, Keys.MAX_HEALTH,
        Double::floatValue))
      .create(EntityProperties.HEALTH, Living.class, b -> readOnly(b, Keys.HEALTH,
        Double::floatValue))
      // misc
      .create(EntityProperties.NAME, Entity.class, b -> readOnly(b, Keys.CUSTOM_NAME))
      .create(EntityProperties.POSITION, Entity.class, b -> b
        .get(e -> {
          var pos = e.position();
          return Vector3d.of(pos.x(), pos.y(), pos.z());
        })
        .set((e, v) -> e.setPosition(org.spongepowered.math.vector.Vector3d.from(v.x(), v.y(), v.z()))))
      .create(EntityProperties.VELOCITY, Entity.class, b -> b
        .valueOperator(Vector3d::clampVelocity)
        .get(e -> {
          var vel = e.velocity().get();
          return Vector3d.of(vel.x(), vel.y(), vel.z());
        })
        .set((e, v) -> e.velocity().set(org.spongepowered.math.vector.Vector3d.from(v.x(), v.y(), v.z()))))
      .create(EntityProperties.WORLD, Entity.class, b -> b
        .get(e -> new SpongeWorld(e.serverLocation().world())))
      .create(EntityProperties.UUID, Entity.class, b -> b
        .get(Entity::uniqueId))
      .create(EntityProperties.ENTITY_TYPE, Entity.class, b -> readOnly(b, Keys.ENTITY_TYPE,
        v -> EntityType.registry().getOrThrow(PlatformAdapter.fromRsk(v.key(RegistryTypes.ENTITY_TYPE)))))
      .create(EntityProperties.RIGHT_HAND, Player.class, b -> readOnly(b, Keys.DOMINANT_HAND,
        v -> v == HandPreferences.RIGHT.get()))
      .create(EntityProperties.GAMEMODE, Player.class, b -> readOnly(b, Keys.GAME_MODE,
        v -> Objects.requireNonNull(GameMode.registry().fromString(v.key(RegistryTypes.GAME_MODE).value()))))
      .build();
  }
}
