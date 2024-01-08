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

package me.moros.bending.fabric.hook;

import java.util.Objects;
import java.util.function.Function;

import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.PlaceholderHandler;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.User;
import me.moros.bending.common.placeholder.DynamicPlaceholder;
import me.moros.bending.common.placeholder.PlaceholderProvider;
import me.moros.bending.common.placeholder.StaticPlaceholder;
import me.moros.bending.common.util.Initializer;
import me.moros.bending.fabric.platform.PlatformAdapter;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.text.Component;
import net.minecraft.server.MinecraftServer;

public record PlaceholderHook(PlaceholderProvider provider) implements Initializer {
  public PlaceholderHook() {
    this(PlaceholderProvider.defaultBuilder().build());
  }

  @Override
  public void init() {
    for (var keyed : provider) {
      if (keyed.value() instanceof StaticPlaceholder staticPlaceholder) {
        Placeholders.register(PlatformAdapter.rsl(keyed.key()), staticParser(staticPlaceholder));
      } else if (keyed.value() instanceof DynamicPlaceholder dynamicPlaceholder) {
        Placeholders.register(PlatformAdapter.rsl(keyed.key()), dynamicParser(dynamicPlaceholder));
      }
    }
  }

  private PlaceholderHandler staticParser(StaticPlaceholder placeholder) {
    return (ctx, args) -> parse(ctx, placeholder);
  }

  private PlaceholderHandler dynamicParser(DynamicPlaceholder placeholder) {
    return (ctx, args) -> (args == null || args.isEmpty()) ? PlaceholderResult.invalid() : parse(ctx, u -> placeholder.handle(u, args));
  }

  private PlaceholderResult parse(PlaceholderContext ctx, Function<User, Component> placeholder) {
    var entity = ctx.entity();
    User user = entity == null ? null : Registries.BENDERS.get(entity.getUUID());
    if (user != null) {
      Component result = placeholder.apply(user);
      if (result != Component.empty()) {
        return PlaceholderResult.value(toNative(Objects.requireNonNull(entity.getServer()), result));
      }
    }
    return PlaceholderResult.invalid();
  }

  private static net.minecraft.network.chat.Component toNative(MinecraftServer server, Component component) {
    return FabricServerAudiences.of(server).toNative(component);
  }
}
