/*
 *   Copyright 2020 Moros <https://github.com/PrimordialMoros>
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

import co.aikar.commands.BendingCommandIssuer;
import co.aikar.commands.BukkitCommandCompletionContext;
import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.CommandCompletions;
import co.aikar.commands.CommandContexts;
import co.aikar.commands.InvalidCommandArgument;
import me.moros.bending.Bending;
import me.moros.bending.game.AttributeSystem;
import me.moros.bending.game.Game;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attributes;
import me.moros.bending.model.attribute.ModifierOperation;
import me.moros.bending.model.attribute.ModifyPolicy;
import me.moros.bending.model.user.player.BendingPlayer;
import me.moros.bending.model.exception.command.InvalidSlotException;
import me.moros.bending.model.exception.command.UserException;
import me.moros.bending.model.preset.Preset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Commands {
	public static void initialize() {
		registerCommandContexts();
		registerCommandCompletions();
		registerCommandConditions();
		Bending.getCommandManager().getCommandReplacements().addReplacement("bendingcommand", "bending|bend|b|avatar|atla|tla");
		Bending.getCommandManager().getCommandReplacements().addReplacement("presetcommand", "presets|preset|pr|p");
		Bending.getCommandManager().getCommandReplacements().addReplacement("modifycommand", "bmodify|bmod|bm|modify|mod");

		Bending.getCommandManager().registerCommand(new BendingCommand());
		Bending.getCommandManager().registerCommand(new PresetCommand());
		Bending.getCommandManager().registerCommand(new ModifyCommand());
	}

	private static void registerCommandCompletions() {
		CommandCompletions<BukkitCommandCompletionContext> commandCompletions = Bending.getCommandManager().getCommandCompletions();
		commandCompletions.registerAsyncCompletion("abilities", c -> {
			if (c.getIssuer() instanceof BendingCommandIssuer) {
				BendingPlayer bendingPlayer = ((BendingCommandIssuer) c.getIssuer()).getBendingPlayer();
				Predicate<AbilityDescription> abilityPredicate = getAbilityPredicate();
				return Game.getAbilityRegistry().getAbilities()
					.filter(abilityPredicate)
					.filter(bendingPlayer::hasPermission)
					.map(AbilityDescription::getName).collect(Collectors.toList());
			}
			return Collections.emptyList();
		});

		commandCompletions.registerAsyncCompletion("presets", c -> {
			if (c.getIssuer() instanceof BendingCommandIssuer) {
				return ((BendingCommandIssuer) c.getIssuer()).getBendingPlayer().getPresets();
			}
			return Collections.emptyList();
		});

		commandCompletions.registerStaticCompletion("elements", Element.getElementNames());
		commandCompletions.registerStaticCompletion("attributes", Arrays.asList(Attributes.TYPES));
		commandCompletions.registerStaticCompletion("policies", Arrays.asList("<ability>", "<element>")); // TODO add them individually
	}

	private static void registerCommandContexts() {
		CommandContexts<BukkitCommandExecutionContext> commandContexts = Bending.getCommandManager().getCommandContexts();
		commandContexts.registerIssuerOnlyContext(BendingCommandIssuer.class, c -> (BendingCommandIssuer) c.getIssuer());

		commandContexts.registerContext(Element.class, c -> {
				String name = c.popFirstArg().toLowerCase();
				return Element.getElementByName(name)
					.orElseThrow(() -> new InvalidCommandArgument("Could not find element " + name));
			}
		);

		commandContexts.registerIssuerAwareContext(AbilityDescription.class, c -> {
			if (!(c.getIssuer() instanceof BendingCommandIssuer)) throw new UserException("You must be player!");
			BendingPlayer bendingPlayer = ((BendingCommandIssuer) c.getIssuer()).getBendingPlayer();
			String name = c.popFirstArg();
			Predicate<AbilityDescription> abilityPredicate = getAbilityPredicate(FilterType.parse(c.getFlagValue("filter", "ALL")));
			return Game.getAbilityRegistry().getAbilities()
				.filter(abilityPredicate)
				.filter(desc -> desc.getName().equalsIgnoreCase(name)) // TODO aliases for desc names?
				.filter(bendingPlayer::hasPermission)
				.findAny().orElseThrow(() -> new InvalidCommandArgument("Could not find ability " + name));
		});

		commandContexts.registerIssuerAwareContext(Preset.class, c -> {
			if (!(c.getSender() instanceof BendingCommandIssuer)) throw new UserException("You must be player!");
			String name = c.popFirstArg().toLowerCase();
			return ((BendingCommandIssuer) c.getIssuer()).getBendingPlayer().getPresetByName(name)
				.orElseThrow(() -> new InvalidCommandArgument("Could not find preset " + name));
		});

		commandContexts.registerContext(ModifyPolicy.class, c -> {
			String name = c.popFirstArg().toLowerCase();
			Optional<Element> element = Element.getElementByName(name);
			if (element.isPresent()) {
				return AttributeSystem.getElementPolicy(element.get());
			}
			AbilityDescription desc = Game.getAbilityRegistry().getAbilityDescription(name)
				.orElseThrow(() -> new InvalidCommandArgument("Invalid policy. Policy must be an element or ability name"));
			return AttributeSystem.getAbilityPolicy(desc);
		});

		commandContexts.registerContext(ModifierOperation.class, c -> {
			String name = c.popFirstArg().toLowerCase();
			if (name.startsWith("m")) {
				return ModifierOperation.MULTIPLICATIVE;
			} else if (name.startsWith("s")) {
				return ModifierOperation.SUMMED_MULTIPLICATIVE;
			}
			return ModifierOperation.ADDITIVE;
		});

		// TODO add context for attributes?
	}

	private static void registerCommandConditions() {
		Bending.getCommandManager().getCommandConditions().addCondition(Integer.class, "slot", (c, exec, value) -> {
			if (value == null) return;
			if (value < 0 || value > 9) { // 0 is reserved for current slot
				throw new InvalidSlotException(value);
			}
		});
	}

	public static Predicate<AbilityDescription> getAbilityPredicate() {
		return getAbilityPredicate(FilterType.ALL);
	}

	public static Predicate<AbilityDescription> getAbilityPredicate(FilterType type) {
		List<Predicate<AbilityDescription>> predicates = new ArrayList<>();
		predicates.add(desc -> !desc.isHidden());
		switch (type) {
			case NORMAL:
				predicates.add(desc -> !desc.isActivatedBy(ActivationMethod.SEQUENCE) && !desc.isActivatedBy(ActivationMethod.PASSIVE));
				break;
			case PASSIVE:
				predicates.add(desc -> desc.isActivatedBy(ActivationMethod.PASSIVE));
				break;
			case SEQUENCE:
				predicates.add(desc -> desc.isActivatedBy(ActivationMethod.SEQUENCE));
				break;
		}
		return predicates.stream().reduce(x -> true, Predicate::and);
	}

	public static FilterType getMainType(AbilityDescription desc) {
		if (desc.isActivatedBy(ActivationMethod.PASSIVE)) {
			return FilterType.PASSIVE;
		} else if (desc.isActivatedBy(ActivationMethod.SEQUENCE)) {
			return FilterType.SEQUENCE;
		} else {
			return FilterType.NORMAL;
		}
	}

	public enum FilterType {
		ALL, NORMAL, PASSIVE, SEQUENCE;

		private static FilterType parse(String name) {
			return Arrays.stream(values()).filter(t -> name.equals(t.toString())).findAny().orElse(FilterType.ALL);
		}
	}
}
