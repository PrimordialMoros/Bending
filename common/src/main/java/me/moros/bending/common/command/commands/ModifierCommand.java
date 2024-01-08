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

package me.moros.bending.common.command.commands;

import cloud.commandframework.Command.Builder;
import cloud.commandframework.arguments.standard.DoubleArgument;
import cloud.commandframework.arguments.standard.EnumArgument;
import cloud.commandframework.meta.CommandMeta;
import me.moros.bending.api.ability.Ability;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.AttributeModifier;
import me.moros.bending.api.config.attribute.ModifierOperation;
import me.moros.bending.api.locale.Message;
import me.moros.bending.api.user.User;
import me.moros.bending.common.command.CommandPermissions;
import me.moros.bending.common.command.Commander;
import me.moros.bending.common.command.argument.ModifyPolicyArgument;
import me.moros.bending.common.command.argument.UserArgument;
import me.moros.bending.common.util.Initializer;
import net.kyori.adventure.audience.Audience;

public record ModifierCommand<C extends Audience>(Commander<C> commander) implements Initializer {
  @Override
  public void init() {
    Builder<C> builder = commander().rootBuilder().literal("modifier", "modifiers")
      .permission(CommandPermissions.MODIFY);
    commander().register(builder.literal("add", "a")
      .meta(CommandMeta.DESCRIPTION, "Add a new modifier to the specified user")
      .argument(ModifyPolicyArgument.of("policy"))
      .argument(EnumArgument.of(Attribute.class, "attribute"))
      .argument(EnumArgument.of(ModifierOperation.class, "operation"))
      .argument(DoubleArgument.of("amount"))
      .argument(UserArgument.optional("target", "me"))
      .handler(c -> {
        AttributeModifier modifier = new AttributeModifier(c.get("policy"), c.get("attribute"), c.get("operation"), c.get("amount"));
        onModifierAdd(c.getSender(), modifier, c.get("target"));
      })
    );
    commander().register(builder.literal("clear", "c")
      .meta(CommandMeta.DESCRIPTION, "Clear all existing modifiers for a user")
      .argument(UserArgument.optional("target", "me"))
      .handler(c -> onModifierClear(c.getSender(), c.get("target")))
    );
  }

  private void onModifierAdd(C sender, AttributeModifier modifier, User user) {
    user.addAttribute(modifier);
    recalculate(user);
    Message.MODIFIER_ADD.send(sender, user.name());
  }

  private void onModifierClear(C sender, User user) {
    user.clearAttributes();
    recalculate(user);
    Message.MODIFIER_CLEAR.send(sender, user.name());
  }

  private void recalculate(User user) {
    user.game().abilityManager(user.worldKey()).userInstances(user).forEach(Ability::loadConfig);
  }
}
