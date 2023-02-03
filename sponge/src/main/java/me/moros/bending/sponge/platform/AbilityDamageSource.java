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

package me.moros.bending.sponge.platform;

import java.util.Objects;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.DamageSource;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.user.User;
import me.moros.bending.sponge.platform.entity.SpongeEntity;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.event.cause.entity.damage.DamageTypes;
import org.spongepowered.api.event.cause.entity.damage.source.common.AbstractEntityDamageSource;

public class AbilityDamageSource extends AbstractEntityDamageSource implements DamageSource {
  private final Component name;
  private final AbilityDescription desc;

  private AbilityDamageSource(Builder builder) {
    super(builder);
    this.name = builder.name;
    this.desc = builder.ability;
  }

  @Override
  public boolean isFire() {
    return desc.element() == Element.FIRE;
  }

  @Override
  public Component name() {
    return name;
  }

  @Override
  public AbilityDescription ability() {
    return desc;
  }

  public static Builder builder(User user, AbilityDescription ability) {
    Objects.requireNonNull(user);
    Objects.requireNonNull(ability);
    return new Builder(user.name(), ability).type(DamageTypes.CUSTOM).entity(((SpongeEntity) user.entity()).handle())
      .bypassesArmor();
  }

  public static class Builder extends AbstractEntityDamageSourceBuilder<AbilityDamageSource, Builder> {
    private final Component name;
    private final AbilityDescription ability;

    private Builder(Component name, AbilityDescription ability) {
      this.name = name;
      this.ability = ability;
    }

    @Override
    public @NonNull AbilityDamageSource build() throws IllegalStateException {
      return new AbilityDamageSource(this);
    }
  }
}
