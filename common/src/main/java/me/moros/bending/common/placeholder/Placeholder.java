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

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import me.moros.bending.api.user.User;
import net.kyori.adventure.text.Component;

public sealed interface Placeholder extends Placeholders permits StaticPlaceholder, DynamicPlaceholder {
  static StaticPlaceholder of(Function<User, Component> function) {
    if (function instanceof StaticPlaceholder placeholder) {
      return placeholder;
    }
    return new StaticPlaceholderImpl(Objects.requireNonNull(function));
  }

  static DynamicPlaceholder of(BiFunction<User, String, Component> function) {
    if (function instanceof DynamicPlaceholder placeholder) {
      return placeholder;
    }
    return new DynamicPlaceholderImpl(Objects.requireNonNull(function));
  }
}
