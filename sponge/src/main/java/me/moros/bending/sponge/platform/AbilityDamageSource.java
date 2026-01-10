/*
 * Copyright 2020-2026 Moros
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

package me.moros.bending.sponge.platform;

import java.util.Objects;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.user.User;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;

public class AbilityDamageSource extends net.minecraft.world.damagesource.DamageSource implements me.moros.bending.api.ability.DamageSource {
  public static final ResourceKey BENDING_DAMAGE = ResourceKey.of("bending", "damage");

  private final Component name;
  private final AbilityDescription desc;

  private AbilityDamageSource(net.minecraft.world.damagesource.DamageSource original, Component name, AbilityDescription desc) {
    super(original.typeHolder(), original.getDirectEntity(), original.getEntity());
    this.name = name;
    this.desc = desc;
  }

  @Override
  public Component name() {
    return name;
  }

  @Override
  public AbilityDescription ability() {
    return desc;
  }

  public static AbilityDamageSource wrap(DamageSource source, User user, AbilityDescription ability) {
    Objects.requireNonNull(user);
    Objects.requireNonNull(ability);
    return new AbilityDamageSource((net.minecraft.world.damagesource.DamageSource) source, user.name(), ability);
  }
}
