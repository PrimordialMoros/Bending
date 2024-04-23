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

package me.moros.bending.common.hook;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.addon.Addon;
import me.moros.bending.api.config.BendingProperties;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.User;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.user.UserManager;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class LuckPermsHook<T> {
  private final Function<T, UUID> uuidExtractor;
  private final UserManager userManager;

  private LuckPermsHook(Function<T, UUID> uuidExtractor, LuckPerms luckPerms) {
    this.uuidExtractor = uuidExtractor;
    this.userManager = luckPerms.getUserManager();
    setupContexts(luckPerms.getContextManager());
  }

  private void setupContexts(ContextManager manager) {
    manager.registerCalculator(new Builder("element")
      .suggestions(Element.NAMES)
      .build(u -> u.elements().stream().map(Element::toString).toList())
    );
    manager.registerCalculator(new Builder("avatar")
      .suggestions(List.of("true", "false"))
      .buildWithPredicate(u -> u.elements().size() >= Element.VALUES.size())
    );
  }

  private @Nullable User adapt(T user) {
    return Registries.BENDERS.get(uuidExtractor.apply(user));
  }

  private int limits(User user) {
    var lpUser = userManager.getUser(user.uuid());
    return lpUser != null ? lpUser.getCachedData().getMetaData(lpUser.getQueryOptions())
      .getMetaValue("bending-max-presets", Integer::parseInt)
      .orElseGet(BendingProperties.instance()::maxPresets) : 0;
  }

  public Addon presetLimits() {
    return new PresetLimits(this::limits);
  }

  private final class Builder {
    private final String key;
    private Collection<String> suggestions;

    private Builder(String key) {
      this.key = key.toLowerCase(Locale.ROOT);
    }

    private Builder suggestions(Collection<String> suggestions) {
      this.suggestions = Objects.requireNonNull(suggestions);
      return this;
    }

    private ContextCalculator<T> build(Function<User, Iterable<String>> mapper) {
      return new ContextCalculator<>() {
        @Override
        public void calculate(T target, ContextConsumer consumer) {
          User user = adapt(target);
          if (user != null) {
            consumer.accept(createContextSet(mapper.apply(user)));
          }
        }

        @Override
        public ContextSet estimatePotentialContexts() {
          return suggestions == null ? ImmutableContextSet.empty() : createContextSet(suggestions);
        }
      };
    }

    private ContextCalculator<T> buildWithPredicate(Predicate<User> predicate) {
      return new ContextCalculator<>() {
        @Override
        public void calculate(T target, ContextConsumer consumer) {
          User user = adapt(target);
          if (user != null) {
            consumer.accept(createContextSet(predicate.test(user)));
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

  public static <T> LuckPermsHook<T> register(Function<T, UUID> uuidExtractor) throws IllegalStateException {
    return new LuckPermsHook<>(uuidExtractor, LuckPermsProvider.get());
  }
}
