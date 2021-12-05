/*
 * Copyright 2020-2021 Moros
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

package me.moros.bending.hook;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import me.moros.bending.model.user.User;
import me.moros.bending.registry.Registries;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

final class BendingContextBuilder {
  private final String key;
  private Collection<String> suggestions;

  private BendingContextBuilder(String key) {
    this.key = key;
  }

  static @NonNull BendingContextBuilder of(@NonNull String key) {
    return new BendingContextBuilder(key.toLowerCase(Locale.ROOT));
  }

  @NonNull BendingContextBuilder suggestions(@NonNull Collection<@NonNull String> suggestions) {
    this.suggestions = Objects.requireNonNull(suggestions);
    return this;
  }

  @NonNull ContextCalculator<@NonNull Player> build(@NonNull Function<@NonNull User, @NonNull Iterable<@NonNull String>> mapper) {
    return new ContextCalculator<>() {
      @Override
      public void calculate(@NonNull Player target, @NonNull ContextConsumer consumer) {
        if (Registries.BENDERS.contains(target.getUniqueId())) {
          consumer.accept(createContextSet(mapper.apply(Registries.BENDERS.user(target))));
        }
      }

      @Override
      public @NonNull ContextSet estimatePotentialContexts() {
        return suggestions == null ? ImmutableContextSet.empty() : createContextSet(suggestions);
      }
    };
  }

  @NonNull ContextCalculator<@NonNull Player> buildWithPredicate(@NonNull Predicate<@NonNull User> predicate) {
    return new ContextCalculator<>() {
      @Override
      public void calculate(@NonNull Player target, @NonNull ContextConsumer consumer) {
        if (Registries.BENDERS.contains(target.getUniqueId())) {
          consumer.accept(createContextSet(predicate.test(Registries.BENDERS.user(target))));
        }
      }

      @Override
      public @NonNull ContextSet estimatePotentialContexts() {
        return suggestions == null ? ImmutableContextSet.empty() : createContextSet(suggestions);
      }
    };
  }

  private ContextSet createContextSet(Iterable<String> values) {
    ImmutableContextSet.Builder builder = ImmutableContextSet.builder();
    values.forEach(value -> builder.add("bending:" + key, value.toLowerCase(Locale.ROOT)));
    return builder.build();
  }

  private ContextSet createContextSet(boolean value) {
    return ImmutableContextSet.of("bending:" + key, String.valueOf(value));
  }
}
