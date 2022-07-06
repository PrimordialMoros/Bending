/*
 * Copyright 2020-2022 Moros
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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import me.moros.bending.Bending;
import me.moros.bending.model.Element;
import me.moros.bending.model.user.User;
import me.moros.bending.registry.Registries;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class LuckPermsHook {
  public LuckPermsHook(@NonNull Bending plugin) {
    RegisteredServiceProvider<LuckPerms> provider = plugin.getServer().getServicesManager().getRegistration(LuckPerms.class);
    if (provider != null) {
      setupContexts(provider.getProvider().getContextManager());
      Bending.logger().info("Successfully registered LuckPerms contexts!");
    }
  }

  private void setupContexts(ContextManager manager) {
    manager.registerCalculator(new Builder("element")
      .suggestions(Element.NAMES)
      .build(u -> u.elements().stream().map(Element::toString).toList())
    );
    manager.registerCalculator(new Builder("avatar")
      .suggestions(List.of("true", "false"))
      .buildWithPredicate(u -> u.elements().size() >= 4)
    );
  }

  private static final class Builder {
    private final String key;
    private Collection<String> suggestions;

    private Builder(String key) {
      this.key = key.toLowerCase(Locale.ROOT);
    }

    private Builder suggestions(Collection<String> suggestions) {
      this.suggestions = Objects.requireNonNull(suggestions);
      return this;
    }

    private ContextCalculator<Player> build(Function<User, Iterable<String>> mapper) {
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

    private ContextCalculator<Player> buildWithPredicate(Predicate<User> predicate) {
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
}
