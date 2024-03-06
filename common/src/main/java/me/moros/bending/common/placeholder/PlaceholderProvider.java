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

package me.moros.bending.common.placeholder;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import me.moros.bending.api.user.User;
import me.moros.bending.api.util.KeyUtil;
import net.kyori.adventure.key.KeyedValue;
import net.kyori.adventure.text.Component;

public final class PlaceholderProvider implements Iterable<KeyedValue<? extends Placeholder>> {
  private final Set<KeyedValue<? extends Placeholder>> placeholders;

  private PlaceholderProvider(Builder builder) {
    this.placeholders = Set.copyOf(builder.placeholders);
  }

  public Component onPlaceholderRequest(User user, String placeholder) {
    for (var keyed : placeholders) {
      String id = keyed.key().value();
      Placeholder p = keyed.value();
      int dynamicLength = id.length() + 1;
      if (p instanceof DynamicPlaceholder dp && placeholder.startsWith(id + "_") && placeholder.length() > dynamicLength) {
        return dp.handle(user, placeholder.substring(dynamicLength));
      } else if (p instanceof StaticPlaceholder sp && placeholder.equals(id)) {
        return sp.handle(user);
      }
    }
    return Component.empty();
  }

  @Override
  public Iterator<KeyedValue<? extends Placeholder>> iterator() {
    return placeholders.iterator();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static Builder defaultBuilder() {
    return builder()
      .add(Placeholder.ELEMENTS)
      .add(Placeholder.ELEMENT)
      .add(Placeholder.DISPLAY_NAME)
      .add(Placeholder.SELECTED_ABILITY)
      .add(Placeholder.SLOT)
      .add(Placeholder.ABILITY_INFO);
  }

  public static final class Builder {
    private final Set<KeyedValue<? extends Placeholder>> placeholders;

    private Builder() {
      placeholders = new LinkedHashSet<>();
    }

    public Builder add(KeyedValue<? extends Placeholder> keyed) {
      this.placeholders.add(keyed);
      return this;
    }

    public Builder addStatic(String id, Function<User, Component> function) {
      this.placeholders.add(KeyedValue.keyedValue(KeyUtil.simple(id), Placeholder.of(function)));
      return this;
    }

    public Builder addDynamic(String id, BiFunction<User, String, Component> function) {
      this.placeholders.add(KeyedValue.keyedValue(KeyUtil.simple(id), Placeholder.of(function)));
      return this;
    }

    public PlaceholderProvider build() {
      return new PlaceholderProvider(this);
    }
  }
}
