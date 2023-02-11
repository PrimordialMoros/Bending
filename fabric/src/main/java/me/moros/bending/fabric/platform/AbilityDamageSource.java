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

package me.moros.bending.fabric.platform;

import java.util.Objects;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.DamageSource;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.locale.Message;
import me.moros.bending.api.user.User;
import me.moros.bending.common.locale.TranslationManager;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.entity.LivingEntity;

public class AbilityDamageSource extends EntityDamageSource implements DamageSource {
  private static TranslationManager manager;

  private final Component name;
  private final AbilityDescription ability;
  private final TranslatableComponent deathMessage;

  private AbilityDamageSource(User user, AbilityDescription ability) {
    super("bending.ability", PlatformAdapter.toFabricEntity(user));
    bypassArmor();
    this.name = user.name();
    this.ability = ability;
    this.deathMessage = manager.translate(ability.deathKey()).orElseGet(Message.ABILITY_GENERIC_DEATH);
  }

  @Override
  public boolean isFire() {
    return ability.element() == Element.FIRE;
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
    return FabricServerAudiences.of(Objects.requireNonNull(livingEntity.getServer()))
      .toNative(deathMessage.args(livingEntity.getDisplayName().asComponent(), name, ability.displayName()));
  }

  public static AbilityDamageSource create(User user, AbilityDescription ability) {
    Objects.requireNonNull(user);
    Objects.requireNonNull(ability);
    return new AbilityDamageSource(user, ability);
  }
}
