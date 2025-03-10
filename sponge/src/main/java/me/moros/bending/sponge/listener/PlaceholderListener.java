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

package me.moros.bending.sponge.listener;

import java.util.Optional;

import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.User;
import me.moros.bending.common.placeholder.DynamicPlaceholder;
import me.moros.bending.common.placeholder.Placeholder;
import me.moros.bending.common.placeholder.PlaceholderProvider;
import me.moros.bending.common.placeholder.StaticPlaceholder;
import me.moros.bending.sponge.platform.PlatformAdapter;
import net.kyori.adventure.key.KeyedValue;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterRegistryValueEvent;
import org.spongepowered.api.event.lifecycle.RegisterRegistryValueEvent.RegistryStep;
import org.spongepowered.api.placeholder.PlaceholderContext;
import org.spongepowered.api.placeholder.PlaceholderParser;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.util.Identifiable;

public class PlaceholderListener {
  private final PlaceholderProvider provider;

  public PlaceholderListener() {
    this.provider = PlaceholderProvider.defaultBuilder().build();
  }

  @Listener
  public void registerTokensEvent(RegisterRegistryValueEvent.GameScoped event) {
    event.registry(RegistryTypes.PLACEHOLDER_PARSER, r -> {
      for (var keyed : provider) {
        registerParser(r, keyed);
      }
    });
  }

  private void registerParser(RegistryStep<PlaceholderParser> registry, KeyedValue<? extends Placeholder> keyed) {
    if (keyed.value() instanceof StaticPlaceholder staticPlaceholder) {
      registry.register(PlatformAdapter.rsk(keyed.key()), new StaticParser(staticPlaceholder));
    } else if (keyed.value() instanceof DynamicPlaceholder dynamicPlaceholder) {
      registry.register(PlatformAdapter.rsk(keyed.key()), new DynamicParser(dynamicPlaceholder));
    }
  }

  private record StaticParser(StaticPlaceholder placeholder) implements PlaceholderParser {
    @Override
    public Component parse(PlaceholderContext placeholderContext) {
      return userFromContext(placeholderContext).map(placeholder::handle).orElseGet(Component::empty);
    }
  }

  private record DynamicParser(DynamicPlaceholder placeholder) implements PlaceholderParser {
    @Override
    public Component parse(PlaceholderContext placeholderContext) {
      String arg = placeholderContext.argumentString().orElse("");
      if (!arg.isEmpty()) {
        return userFromContext(placeholderContext).map(u -> placeholder.handle(u, arg)).orElseGet(Component::empty);
      }
      return Component.empty();
    }
  }

  private static Optional<User> userFromContext(PlaceholderContext context) {
    return context.associatedObject().filter(Identifiable.class::isInstance).map(Identifiable.class::cast)
      .map(Identifiable::uniqueId).map(Registries.BENDERS::get);
  }
}
