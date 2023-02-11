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

package me.moros.bending.sponge.hook;

import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.User;
import me.moros.bending.common.hook.AbstractLuckPermsHook;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.context.ContextManager;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.ServiceProvider;
import org.spongepowered.api.service.ServiceRegistration;
import org.spongepowered.api.service.permission.Subject;

public final class LuckPermsHook extends AbstractLuckPermsHook<Subject> {
  private LuckPermsHook(ContextManager contextManager) {
    super(contextManager);
  }

  @Override
  protected @Nullable User adapt(Subject user) {
    return user instanceof Player player ? Registries.BENDERS.get(player.uniqueId()) : null;
  }

  public static boolean register(ServiceProvider serviceProvider) {
    ServiceRegistration<LuckPerms> provider = serviceProvider.registration(LuckPerms.class).orElse(null);
    if (provider != null) {
      new LuckPermsHook(provider.service().getContextManager());
      return true;
    }
    return false;
  }
}
