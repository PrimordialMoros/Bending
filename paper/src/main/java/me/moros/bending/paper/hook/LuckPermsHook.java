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

package me.moros.bending.paper.hook;

import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.User;
import me.moros.bending.common.hook.AbstractLuckPermsHook;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.context.ContextManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class LuckPermsHook extends AbstractLuckPermsHook<Player> {
  private LuckPermsHook(ContextManager manager) {
    super(manager);
  }

  @Override
  protected @Nullable User adapt(Player user) {
    return Registries.BENDERS.get(user.getUniqueId());
  }

  public static boolean register(ServicesManager manager) {
    RegisteredServiceProvider<LuckPerms> provider = manager.getRegistration(LuckPerms.class);
    if (provider != null) {
      new LuckPermsHook(provider.getProvider().getContextManager());
      return true;
    }
    return false;
  }
}
