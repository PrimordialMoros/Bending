/*
 * Copyright 2020-2024 Moros
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

package me.moros.bending.common.adapter;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

final class AbilityDamageSource extends net.minecraft.world.damagesource.DamageSource {
  private final Component deathMessage;

  AbilityDamageSource(Entity userEntity, Component deathMessage) {
    super(userEntity.damageSources().generic().typeHolder(), userEntity);
    this.deathMessage = deathMessage;
  }

  @Override
  public Component getLocalizedDeathMessage(LivingEntity livingEntity) {
    return deathMessage;
  }
}
