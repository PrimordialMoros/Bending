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

package me.moros.bending.paper.adapter.v1_20_R1;

import io.papermc.paper.adventure.PaperAdventure;
import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.DamageSource;
import me.moros.bending.api.user.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

final class AbilityDamageSource extends net.minecraft.world.damagesource.DamageSource implements DamageSource {
  private final Component name;
  private final AbilityDescription ability;
  private final TranslatableComponent deathMessage;

  AbilityDamageSource(Entity userEntity, User user, AbilityDescription ability, TranslatableComponent deathMessage) {
    super(userEntity.damageSources().generic().typeHolder(), userEntity);
    this.name = user.name();
    this.ability = ability;
    this.deathMessage = deathMessage;
  }

  @Override
  public Component name() {
    return name;
  }

  @Override
  public AbilityDescription ability() {
    return ability;
  }

  public net.minecraft.network.chat.Component getLocalizedDeathMessage(LivingEntity livingEntity) {
    var advName = PaperAdventure.asAdventure(livingEntity.getDisplayName());
    return PaperAdventure.asVanilla(deathMessage.args(advName, name, ability.displayName()));
  }
}


