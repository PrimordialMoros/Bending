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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import co.aikar.commands.BukkitCommandCompletionContext;
import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.CommandCompletions;
import co.aikar.commands.CommandContexts;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.PaperCommandManager;
import me.moros.bending.Bending;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.ModifierOperation;
import me.moros.bending.model.attribute.ModifyPolicy;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.registry.Registries;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

public class Commands {
  private final PaperCommandManager commandManager;

  public Commands(@NonNull Bending plugin) {
    commandManager = new PaperCommandManager(plugin);
    commandManager.enableUnstableAPI("help");

    registerCommandContexts();
    registerCommandCompletions();
    registerCommandConditions();
    commandManager.getCommandReplacements().addReplacement("bendingcommand", "bending|bend|b|avatar|atla|tla");
    commandManager.getCommandReplacements().addReplacement("elementcommand", "elements|element|elem|ele");
    commandManager.getCommandReplacements().addReplacement("presetcommand", "presets|preset|pr|p");
    commandManager.getCommandReplacements().addReplacement("modifycommand", "bmodify|bmod|bm|modify|mod");

    commandManager.registerCommand(new BendingCommand());
    commandManager.registerCommand(new ElementCommand());
    commandManager.registerCommand(new PresetCommand());
    commandManager.registerCommand(new ModifyCommand());
  }

  private Collection<String> abilityCompletions(Player player, boolean validOnly) {
    Predicate<AbilityDescription> predicate = x -> true;
    if (validOnly) {
      predicate = AbilityDescription::canBind;
    }
    Predicate<AbilityDescription> hasPermission = x -> true;
    Predicate<AbilityDescription> hasElement = x -> true;
    if (player != null) {
      BendingPlayer bendingPlayer = Registries.BENDERS.user(player);
      hasPermission = bendingPlayer::hasPermission;
      if (validOnly) {
        hasElement = d -> bendingPlayer.hasElement(d.element());
      }
    }
    return Registries.ABILITIES.stream().filter(desc -> !desc.hidden()).filter(predicate)
      .filter(hasElement).filter(hasPermission).map(AbilityDescription::name).collect(Collectors.toList());
  }

  private void registerCommandCompletions() {
    CommandCompletions<BukkitCommandCompletionContext> commandCompletions = commandManager.getCommandCompletions();

    commandCompletions.registerAsyncCompletion("abilities", c -> abilityCompletions(c.getPlayer(), true));

    commandCompletions.registerAsyncCompletion("allabilities", c -> abilityCompletions(c.getPlayer(), false));

    commandCompletions.registerAsyncCompletion("presets", c -> {
      Player player = c.getPlayer();
      return player == null ? List.of() : Registries.BENDERS.user(player).presets();
    });

    commandCompletions.registerStaticCompletion("elements", List.of("Air", "Water", "Earth", "Fire"));
  }

  private void registerCommandContexts() {
    CommandContexts<BukkitCommandExecutionContext> commandContexts = commandManager.getCommandContexts();

    commandContexts.registerIssuerOnlyContext(BendingPlayer.class, c -> {
      Player player = c.getPlayer();
      if (player == null) {
        throw new UserException();
      }
      return Registries.BENDERS.user(player);
    });

    commandContexts.registerContext(Element.class, c -> {
      String name = c.popFirstArg();
      return Element.fromName(name)
        .orElseThrow(() -> new InvalidCommandArgument("Could not find element " + name));
    });

    commandContexts.registerIssuerAwareContext(AbilityDescription.class, c -> {
      Player player = c.getPlayer();
      Predicate<AbilityDescription> permissionPredicate = player == null ? x -> true : d -> player.hasPermission(d.permission());
      String name = c.popFirstArg();
      AbilityDescription check = Registries.ABILITIES.ability(name);
      if (check == null || check.hidden() || !permissionPredicate.test(check)) {
        throw new InvalidCommandArgument("Could not find ability " + name);
      }
      return check;
    });

    commandContexts.registerIssuerAwareContext(Preset.class, c -> {
      Player player = c.getPlayer();
      if (player == null) {
        throw new UserException();
      }
      String name = c.popFirstArg();
      return Registries.BENDERS.user(player).presetByName(name)
        .orElseThrow(() -> new InvalidCommandArgument("Could not find preset " + name));
    });

    commandContexts.registerContext(ModifyPolicy.class, c -> {
      String name = c.popFirstArg();
      Optional<Element> element = Element.fromName(name);
      if (element.isPresent()) {
        return ModifyPolicy.of(element.get());
      }
      AbilityDescription desc = Registries.ABILITIES.ability(name);
      if (desc == null) {
        throw new InvalidCommandArgument("Invalid policy. Policy must be an element or ability name");
      }
      return ModifyPolicy.of(desc);
    });

    commandContexts.registerContext(ModifierOperation.class, c -> {
      String name = c.popFirstArg().toLowerCase();
      if (name.startsWith("m")) {
        return ModifierOperation.MULTIPLICATIVE;
      }
      return ModifierOperation.ADDITIVE;
    });
  }

  private void registerCommandConditions() {
    commandManager.getCommandConditions().addCondition(Integer.class, "slot", (c, exec, value) -> {
      if (value == null) {
        return;
      }
      if (value < 0 || value > 9) { // 0 is reserved for current slot
        throw new InvalidCommandArgument("Invalid slot number " + value + " . Slots must be in the 1-9 range!");
      }
    });
  }

  static class UserException extends InvalidCommandArgument {
    UserException() {
      super("You must be a player!");
    }
  }
}
