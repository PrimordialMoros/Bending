/*
 * Copyright 2020-2026 Moros
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

package me.moros.bending.common.gui;

import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.User;
import me.moros.bending.common.command.Permissions;
import me.moros.bending.common.locale.Message;

public sealed interface ElementDialogCallback extends Consumer<UUID> permits ElementDialogCallbackImpl {
  Element element();

  @Override
  default void accept(UUID uuid) {
    User user = Registries.BENDERS.get(uuid);
    if (user != null) {
      Element element = element();
      if (!user.hasPermission(Permissions.CHOOSE) ||
        !user.hasPermission(Permissions.CHOOSE + "." + element.toString().toLowerCase(Locale.ROOT))
      ) {
        Message.ELEMENT_CHOOSE_NO_PERMISSION.send(user, element.displayName());
        return;
      }
      if (user.chooseElement(element)) {
        Message.ELEMENT_CHOOSE_SUCCESS.send(user, element.displayName());
      }
    }
  }

  static ElementDialogCallback of(Element element) {
    return new ElementDialogCallbackImpl(element);
  }
}
