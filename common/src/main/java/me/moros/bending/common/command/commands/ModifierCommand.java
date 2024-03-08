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

import me.moros.bending.api.ability.Ability;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.AttributeModifier;
import me.moros.bending.api.config.attribute.ModifierOperation;
import me.moros.bending.api.config.attribute.ModifyPolicy;
import me.moros.bending.api.user.User;
import me.moros.bending.common.command.CommandPermissions;
import me.moros.bending.common.command.Commander;
import me.moros.bending.common.command.parser.AbilityParser;
import me.moros.bending.common.command.parser.UserParser;
import me.moros.bending.common.locale.Message;
import me.moros.bending.common.util.Initializer;
import net.kyori.adventure.audience.Audience;
import org.incendo.cloud.component.DefaultValue;
import org.incendo.cloud.minecraft.extras.RichDescription;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.aggregate.AggregateParser;
import org.incendo.cloud.parser.standard.DoubleParser;
import org.incendo.cloud.parser.standard.EnumParser;

import static org.incendo.cloud.parser.ArgumentParseResult.successFuture;

public record ModifierCommand<C extends Audience>(Commander<C> commander) implements Initializer {
  @Override
  public void init() {
    var builder = commander().rootBuilder().literal("modifier")
      .commandDescription(RichDescription.of(Message.MODIFIER_DESC.build()))
      .permission(CommandPermissions.MODIFY);
    commander().register(builder
      .literal("add")
      .required("modifier", modifierParser())
      .optional("target", UserParser.parser(), DefaultValue.parsed("me"))
      .commandDescription(RichDescription.of(Message.MODIFIER_ADD_DESC.build()))
      .handler(c -> onModifierAdd(c.sender(), c.get("modifier"), c.get("target")))
    );
    commander().register(builder
      .literal("clear")
      .optional("target", UserParser.parser(), DefaultValue.parsed("me"))
      .commandDescription(RichDescription.of(Message.MODIFIER_CLEAR_DESC.build()))
      .handler(c -> onModifierClear(c.sender(), c.get("target")))
    );
  }

  private ParserDescriptor<C, AttributeModifier> modifierParser() {
    var policyParser = ArgumentParser.firstOf(EnumParser.enumParser(Element.class), AbilityParser.<C>parserGlobal())
      .flatMapSuccess(ModifyPolicy.class, (ctx, r) -> successFuture(r.mapEither(ModifyPolicy::of, ModifyPolicy::of)));
    return AggregateParser.<C>builder()
      .withComponent("policy", policyParser)
      .withComponent("attribute", EnumParser.enumParser(Attribute.class))
      .withComponent("operation", EnumParser.enumParser(ModifierOperation.class))
      .withComponent("amount", DoubleParser.doubleParser())
      .withMapper(AttributeModifier.class, (cmdCtx, ctx) -> successFuture(
        new AttributeModifier(ctx.get("policy"), ctx.get("attribute"), ctx.get("operation"), ctx.get("amount"))
      )).build();
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
