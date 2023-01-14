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

package me.moros.bending.hook;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

import me.moros.bending.model.Element;
import me.moros.bending.model.registry.Registries;
import me.moros.bending.model.user.User;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.ServiceProvider;
import org.spongepowered.api.service.permission.Subject;

public final class LuckPermsHook {
  public LuckPermsHook(ServiceProvider serviceProvider) {
    serviceProvider.registration(LuckPerms.class).ifPresent(p -> setupContexts(p.service().getContextManager()));
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

    private ContextCalculator<Subject> build(Function<User, Iterable<String>> mapper) {
      return new ContextCalculator<>() {
        @Override
        public void calculate(Subject target, ContextConsumer consumer) {
          if (target instanceof Player player) {
            UUID uuid = player.uniqueId();
            if (Registries.BENDERS.containsKey(uuid)) {
              consumer.accept(createContextSet(mapper.apply(Registries.BENDERS.get(uuid))));
            }
          }
        }

        @Override
        public ContextSet estimatePotentialContexts() {
          return suggestions == null ? ImmutableContextSet.empty() : createContextSet(suggestions);
        }
      };
    }

    private ContextCalculator<Subject> buildWithPredicate(Predicate<User> predicate) {
      return new ContextCalculator<>() {
        @Override
        public void calculate(Subject target, ContextConsumer consumer) {
          if (target instanceof Player player) {
            UUID uuid = player.uniqueId();
            if (Registries.BENDERS.containsKey(uuid)) {
              consumer.accept(createContextSet(predicate.test(Registries.BENDERS.get(uuid))));
            }
          }
        }

        @Override
        public ContextSet estimatePotentialContexts() {
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
