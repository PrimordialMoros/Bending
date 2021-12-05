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

import java.util.List;

import me.moros.bending.Bending;
import me.moros.bending.model.Element;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.context.ContextManager;
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
    manager.registerCalculator(BendingContextBuilder.of("element")
      .suggestions(Element.NAMES)
      .build(u -> u.elements().stream().map(Element::toString).toList())
    );
    manager.registerCalculator(BendingContextBuilder.of("avatar")
      .suggestions(List.of("true", "false"))
      .buildWithPredicate(u -> u.elements().size() >= 4)
    );
  }
}
