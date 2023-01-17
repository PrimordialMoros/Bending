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

package me.moros.bending.platform;

import java.util.Objects;
import java.util.function.Predicate;

import me.moros.bending.FabricBending;
import me.moros.bending.model.DamageSource;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.user.User;
import me.moros.bending.platform.entity.FabricEntity;
import net.kyori.adventure.text.Component;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.entity.LivingEntity;

public class AbilityDamageSource extends EntityDamageSource implements DamageSource {
  private static Predicate<String> hasTranslation;

  private final Component name;
  private final AbilityDescription ability;
  private final String deathMessage;

  private AbilityDamageSource(User user, AbilityDescription ability, String deathMessage) {
    super("bending.ability", ((FabricEntity) user.entity()).handle());
    bypassArmor();
    this.name = user.name();
    this.ability = ability;
    this.deathMessage = deathMessage;
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
    var userName = FabricBending.audiences().toNative(name);
    var abilityName = FabricBending.audiences().toNative(ability.displayName());
    return net.minecraft.network.chat.Component.translatable(deathMessage, livingEntity.getDisplayName(), userName, abilityName);
  }

  public static AbilityDamageSource create(User user, AbilityDescription ability) {
    Objects.requireNonNull(user);
    Objects.requireNonNull(ability);
    var msgId = ability.translationKey() + ".death";
    if (!hasTranslation.test(msgId)) {
      msgId = "bending.ability.generic.death";
    }
    return new AbilityDamageSource(user, ability, msgId);
  }

  public static void inject(Predicate<String> hasTranslation) {
    Objects.requireNonNull(hasTranslation);
    if (AbilityDamageSource.hasTranslation == null) {
      AbilityDamageSource.hasTranslation = hasTranslation;
    }
  }
}
