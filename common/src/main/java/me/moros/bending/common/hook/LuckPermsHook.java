/*
 * Copyright 2020-2025 Moros
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
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.IntStream;

import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.addon.Addon;
import me.moros.bending.api.config.BendingProperties;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.KeyUtil;
import net.kyori.adventure.key.Key;
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
    builder("element")
      .suggestions(Element.NAMES)
      .build(u -> u.elements().stream().map(Element::toString).toList())
      .register(manager);
    builder("avatar")
      .suggestions(List.of("true", "false"))
      .build(fromSingleValue(u -> String.valueOf(u.elements().size() >= Element.VALUES.size())))
      .register(manager);
    builder("element-count")
      .suggestions(IntStream.rangeClosed(0, Element.VALUES.size()).mapToObj(String::valueOf).toList())
      .build(fromSingleValue(u -> String.valueOf(u.elements().size())))
      .register(manager);
  }

  private Function<User, Iterable<String>> fromSingleValue(Function<User, String> valueMapper) {
    return u -> List.of(valueMapper.apply(u));
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

  private Builder<T> builder(String key) {
    return new Builder<>(key, this::adapt);
  }

  private static final class Builder<T> {
    private final Key key;
    private final Function<T, User> userAdapter;
    private ContextSet suggestions;

    private Builder(String key, Function<T, User> userAdapter) {
      this.key = KeyUtil.simple(key.toLowerCase(Locale.ROOT));
      this.userAdapter = userAdapter;
    }

    private Builder<T> suggestions(Collection<String> suggestions) {
      this.suggestions = createContextSet(key.asString(), suggestions);
      return this;
    }

    private BendingContextCalculator<T> build(Function<User, Iterable<String>> mapper) {
      return new MappingContextCalculator<>(key.asString(), suggestions, userAdapter, mapper);
    }
  }

  private interface BendingContextCalculator<T> extends ContextCalculator<T> {
    String key();

    default void calculate(T target, ContextConsumer consumer) {
      User user = userAdapter().apply(target);
      if (user != null) {
        consumer.accept(createContextSet(key(), mapper().apply(user)));
      }
    }

    Function<T, @Nullable User> userAdapter();

    Function<User, Iterable<String>> mapper();

    default void register(ContextManager contextManager) {
      contextManager.registerCalculator(this);
    }
  }

  private record MappingContextCalculator<T>(String key, ContextSet estimatePotentialContexts,
                                             Function<T, User> userAdapter,
                                             Function<User, Iterable<String>> mapper) implements BendingContextCalculator<T> {
  }

  private static ContextSet createContextSet(String key, Iterable<String> values) {
    ImmutableContextSet.Builder builder = ImmutableContextSet.builder();
    values.forEach(value -> builder.add(key, value.toLowerCase(Locale.ROOT)));
    return builder.build();
  }

  public static <T> LuckPermsHook<T> register(Function<T, UUID> uuidExtractor) throws IllegalStateException {
    return new LuckPermsHook<>(uuidExtractor, LuckPermsProvider.get());
  }
}
