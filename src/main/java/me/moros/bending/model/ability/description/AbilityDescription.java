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

package me.moros.bending.model.ability.description;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.cf.checker.nullness.qual.Nullable;
import me.moros.atlas.kyori.adventure.text.Component;
import me.moros.atlas.kyori.adventure.text.TranslatableComponent;
import me.moros.atlas.kyori.adventure.text.event.ClickEvent;
import me.moros.atlas.kyori.adventure.text.event.HoverEvent;
import me.moros.atlas.kyori.adventure.text.format.NamedTextColor;
import me.moros.bending.Bending;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.util.ActivationMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;
import java.util.function.Function;

/**
 * AbilityDescription is immutable and thread-safe.
 * Assume that all collections returning AbilityDescription are also immutable
 */
public class AbilityDescription {
	private final String name;
	private final Function<AbilityDescription, ? extends Ability> constructor;
	private final Element element;
	private final EnumSet<ActivationMethod> activationMethods;
	private final TranslatableComponent description;
	private final TranslatableComponent instructions;
	private final TranslatableComponent deathMessage;
	private final boolean hidden;
	private final boolean canBind;
	private final boolean harmless;
	private final boolean sourcesPlants;
	private final int hashcode;

	private AbilityDescription(AbilityDescriptionBuilder builder) {
		name = builder.name;
		constructor = builder.constructor;
		element = builder.element;
		activationMethods = builder.activationMethods;
		String descKey = "bending.ability." + name.toLowerCase() + ".description";
		String instKey = "bending.ability." + name.toLowerCase() + ".instructions";
		String deathKey = "bending.ability." + name.toLowerCase() + ".death";
		description = Bending.getTranslationManager().containsKey(descKey) ? Component.translatable(descKey) : null;
		instructions = Bending.getTranslationManager().containsKey(instKey) ? Component.translatable(instKey) : null;
		deathMessage = Bending.getTranslationManager().containsKey(deathKey) ? Component.translatable(deathKey) : null;
		canBind = builder.canBind && !isActivatedBy(ActivationMethod.SEQUENCE);
		hidden = builder.hidden;
		harmless = builder.harmless;
		sourcesPlants = builder.sourcesPlants;
		hashcode = Objects.hash(name, constructor, element, activationMethods, hidden, canBind, harmless, sourcesPlants);
		createAbility(); // Init config values
	}

	public @NonNull String getName() {
		return name;
	}

	public @NonNull Component getDisplayName() {
		return Component.text(name, element.getColor());
	}

	public @NonNull Element getElement() {
		return element;
	}

	public @Nullable TranslatableComponent getDescription() {
		return description;
	}

	public @Nullable TranslatableComponent getInstructions() {
		return instructions;
	}

	public @Nullable TranslatableComponent getDeathMessage() {
		return deathMessage;
	}

	public boolean canBind() {
		return canBind;
	}

	public boolean isHidden() {
		return hidden;
	}

	public boolean isHarmless() {
		return harmless;
	}

	public boolean canSourcePlant() {
		return sourcesPlants;
	}

	public boolean isActivatedBy(@NonNull ActivationMethod method) {
		return activationMethods.contains(method);
	}

	public @NonNull Ability createAbility() {
		return constructor.apply(this);
	}

	public @NonNull String getPermission() {
		return "bending.ability." + name;
	}

	public @NonNull Component getMeta() {
		String type = "Ability";
		if (isActivatedBy(ActivationMethod.PASSIVE)) {
			type = "Passive";
		} else if (isActivatedBy(ActivationMethod.SEQUENCE)) {
			type = "Sequence";
		}
		Component details = getDisplayName().append(Component.newline())
			.append(Component.text("Element: ", NamedTextColor.DARK_AQUA))
			.append(getElement().getDisplayName().append(Component.newline()))
			.append(Component.text("Type: ", NamedTextColor.DARK_AQUA))
			.append(Component.text(type, NamedTextColor.GREEN)).append(Component.newline())
			.append(Component.text("Permission: ", NamedTextColor.DARK_AQUA))
			.append(Component.text(getPermission(), NamedTextColor.GREEN)).append(Component.newline()).append(Component.newline())
			.append(Component.text("Click to view info about this ability.", NamedTextColor.GRAY));

		return Component.text(getName(), getElement().getColor())
			.hoverEvent(HoverEvent.showText(details))
			.clickEvent(ClickEvent.runCommand("/bending info " + getName()));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		AbilityDescription desc = (AbilityDescription) obj;
		return getName().equals(desc.getName()) && getElement() == desc.getElement();
	}

	@Override
	public int hashCode() {
		return hashcode;
	}

	/**
	 * Create a {@link AbilityDescriptionBuilder} with values matching that of this object
	 * @return a preconfigured builder
	 */
	public @NonNull AbilityDescriptionBuilder builder() {
		return new AbilityDescriptionBuilder(name, constructor)
			.setElement(element).setActivation(activationMethods)
			.setHidden(hidden).setHarmless(harmless)
			.setSourcesPlants(sourcesPlants);
	}

	public static <T extends Ability> @NonNull AbilityDescriptionBuilder builder(@NonNull String name, @NonNull Function<AbilityDescription, T> constructor) {
		return new AbilityDescriptionBuilder(name, constructor);
	}

	public static class AbilityDescriptionBuilder {
		private final String name;
		private final Function<AbilityDescription, ? extends Ability> constructor;
		private Element element;
		private EnumSet<ActivationMethod> activationMethods;
		private boolean canBind = true;
		private boolean hidden = false;
		private boolean harmless = false;
		private boolean sourcesPlants = false;

		public <T extends Ability> AbilityDescriptionBuilder(@NonNull String name, @NonNull Function<@NonNull AbilityDescription, @NonNull T> constructor) {
			this.name = name;
			this.constructor = constructor;
		}

		public @NonNull AbilityDescriptionBuilder setElement(@NonNull Element element) {
			this.element = element;
			return this;
		}

		public @NonNull AbilityDescriptionBuilder setActivation(@NonNull Collection<@NonNull ActivationMethod> methods) {
			activationMethods = EnumSet.copyOf(methods);
			return this;
		}

		public @NonNull AbilityDescriptionBuilder setActivation(@NonNull ActivationMethod method, @Nullable ActivationMethod @NonNull ... methods) {
			Collection<ActivationMethod> c = new ArrayList<>();
			if (methods != null) c.addAll(Arrays.asList(methods));
			c.add(method);
			return setActivation(c);
		}

		public @NonNull AbilityDescriptionBuilder setCanBind(boolean canBind) {
			this.canBind = canBind;
			return this;
		}

		public @NonNull AbilityDescriptionBuilder setHidden(boolean hidden) {
			this.hidden = hidden;
			return this;
		}

		public @NonNull AbilityDescriptionBuilder setHarmless(boolean harmless) {
			this.harmless = harmless;
			return this;
		}

		public @NonNull AbilityDescriptionBuilder setSourcesPlants(boolean sourcesPlants) {
			this.sourcesPlants = sourcesPlants;
			return this;
		}

		public @NonNull AbilityDescription build() {
			validate();
			return new AbilityDescription(this);
		}

		private void validate() {
			Objects.requireNonNull(element, "Element cannot be null");
			Objects.requireNonNull(activationMethods, "Activation Methods cannot be null");
			if (activationMethods.isEmpty()) throw new IllegalStateException("Activation methods cannot be empty");
		}
	}
}
