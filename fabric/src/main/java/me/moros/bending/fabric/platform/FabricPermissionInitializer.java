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

package me.moros.bending.fabric.platform;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import me.lucko.fabric.api.permissions.v0.PermissionCheckEvent;
import me.moros.bending.common.util.PermissionInitializer;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.resources.ResourceLocation;

public class FabricPermissionInitializer extends PermissionInitializer {
  private final Map<String, TriState> defaultPermissions = new ConcurrentHashMap<>();

  public FabricPermissionInitializer() {
    var fallback = ResourceLocation.fromNamespaceAndPath("bending", "fallback");
    PermissionCheckEvent.EVENT.register(fallback, this::onPermissionCheck);
    PermissionCheckEvent.EVENT.addPhaseOrdering(Event.DEFAULT_PHASE, fallback);
  }

  private TriState onPermissionCheck(SharedSuggestionProvider source, String permission) {
    return defaultPermissions.getOrDefault(permission, TriState.DEFAULT);
  }

  @Override
  protected void registerDefault(String node, Collection<String> children, net.kyori.adventure.util.TriState def) {
    var permDef = switch (def) {
      case TRUE -> TriState.TRUE;
      case NOT_SET -> TriState.DEFAULT;
      case FALSE -> TriState.FALSE;
    };
    var map = children.stream().collect(Collectors.toMap(Function.identity(), v -> permDef));
    defaultPermissions.putAll(map);
  }
}
