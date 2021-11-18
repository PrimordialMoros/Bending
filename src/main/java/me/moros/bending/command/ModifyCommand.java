/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.command;

import cloud.commandframework.Command.Builder;
import cloud.commandframework.arguments.standard.DoubleArgument;
import cloud.commandframework.arguments.standard.EnumArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import me.moros.bending.Bending;
import me.moros.bending.locale.Message;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.AttributeModifier;
import me.moros.bending.model.attribute.ModifierOperation;
import me.moros.bending.model.attribute.ModifyPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.registry.Registries;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ModifyCommand {
  ModifyCommand(PaperCommandManager<CommandSender> manager) {
    Builder<CommandSender> builder = manager.commandBuilder("modifiers", "modifier", "modify", "mod")
      .meta(CommandMeta.DESCRIPTION, "Base command for bending modifiers")
      .permission("bending.command.modify");
    manager
      .command(builder.literal("add", "a")
        .meta(CommandMeta.DESCRIPTION, "Add a new modifier to the specified user")
        .senderType(Player.class)
        .argument(manager.argumentBuilder(ModifyPolicy.class, "policy"))
        .argument(EnumArgument.of(Attribute.class, "attribute"))
        .argument(EnumArgument.of(ModifierOperation.class, "operation"))
        .argument(DoubleArgument.of("amount"))
        .handler(c -> {
          AttributeModifier modifier = new AttributeModifier(c.get("policy"), c.get("attribute"), c.get("operation"), c.get("amount"));
          onModify(c.get(ContextKeys.BENDING_PLAYER), modifier);
        })
      ).command(builder.literal("clear", "c")
        .meta(CommandMeta.DESCRIPTION, "Clear all existing modifiers for a user")
        .senderType(Player.class)
        .handler(c -> onClear(c.get(ContextKeys.BENDING_PLAYER)))
      );
  }

  private static void onModify(User user, AttributeModifier modifier) {
    Registries.ATTRIBUTES.add(user, modifier);
    recalculate(user);
    Message.MODIFIER_ADD.send(user, user.entity().getName());
  }

  private static void onClear(User user) {
    Registries.ATTRIBUTES.invalidate(user);
    recalculate(user);
    Message.MODIFIER_CLEAR.send(user, user.entity().getName());
  }

  private static void recalculate(User user) {
    Bending.game().abilityManager(user.world()).userInstances(user).forEach(Ability::loadConfig);
  }
}
