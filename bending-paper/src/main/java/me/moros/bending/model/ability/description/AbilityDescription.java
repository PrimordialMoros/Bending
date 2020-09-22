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

package me.moros.bending.model.ability.description;

import me.moros.bending.config.ConfigManager;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.user.User;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.apache.commons.math3.util.FastMath;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;

/**
 * AbilityDescription is immutable and thread-safe.
 * Assume that all collections returning AbilityDescription are also immutable
 */
public class AbilityDescription {
	private final String name;
	private final Class<? extends Ability> type;
	private final Element element;
	private final EnumSet<ActivationMethod> activationMethods;
	private final long cooldown;
	private final String description;
	private final String instructions;
	private final boolean hidden;
	private final boolean harmless;
	private final boolean canBypassCooldown;
	private final boolean sourcesPlants;
	private final int hashcode;

	public AbilityDescription(AbilityDescriptionBuilder builder) {
		Objects.requireNonNull(builder);
		name = builder.name;
		type = builder.type;
		element = builder.element;
		activationMethods = builder.activationMethods;
		cooldown = builder.cooldown;
		if (builder.description.isEmpty()) {
			description = getConfigNode().getNode("description").getString("");
		} else {
			description = builder.description;
		}
		if (builder.instructions.isEmpty()) {
			instructions = getConfigNode().getNode("instructions").getString("");
		} else {
			instructions = builder.instructions;
		}
		hidden = builder.hidden;
		harmless = builder.harmless;
		canBypassCooldown = builder.canBypassCooldown;
		sourcesPlants = builder.sourcesPlants;
		hashcode = Objects.hash(name, type, element, activationMethods, cooldown, description, instructions, hidden, harmless);
	}

	public String getName() {
		return name;
	}

	public TextComponent getDisplayName() {
		return TextComponent.of(name, element.getColor());
	}

	public Element getElement() {
		return element;
	}

	public long getCooldown() {
		return cooldown;
	}

	public String getDescription() {
		return description;
	}

	public String getInstructions() {
		return instructions;
	}

	public boolean isHidden() {
		return hidden;
	}

	public boolean isHarmless() {
		return harmless;
	}

	public boolean canBypassCooldown() {
		return canBypassCooldown;
	}

	public boolean canSourcePlant(User user) {
		return sourcesPlants && !user.isOnCooldown(this);
	}

	public boolean isActivatedBy(ActivationMethod method) {
		return activationMethods.contains(method);
	}

	public boolean isAbility(Ability ability) {
		if (ability == null) return false;
		return type.isAssignableFrom(ability.getClass());
	}

	public Ability createAbility() {
		try {
			return type.getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			e.printStackTrace();
			return null;
		}
	}

	public CommentedConfigurationNode getConfigNode() {
		CommentedConfigurationNode elementNode = ConfigManager.getConfig().getNode("abilities", getElement().toString().toLowerCase());
		CommentedConfigurationNode node;
		if (isActivatedBy(ActivationMethod.SEQUENCE)) {
			node = elementNode.getNode("sequences", getName().toLowerCase());
		} else if (isActivatedBy(ActivationMethod.PASSIVE)) {
			node = elementNode.getNode("passives", getName().toLowerCase());
		} else {
			node = elementNode.getNode(getName().toLowerCase());
		}
		return node;
	}

	public String getPermission() {
		return "bending.ability." + name;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AbilityDescription) {
			AbilityDescription desc = (AbilityDescription) obj;
			return getName().equals(desc.getName()) && getElement() == desc.getElement();
		}
		return false;
	}

	@Override
	public int hashCode() {
		return hashcode;
	}

	/**
	 * Create a {@link AbilityDescriptionBuilder} with values matching that of this object
	 * @return a preconfigured builder
	 */
	public AbilityDescriptionBuilder builder() {
		return new AbilityDescriptionBuilder(name, type)
			.setElement(element).setActivation(activationMethods).setCooldown(cooldown)
			.setDescription(description).setInstructions(instructions)
			.setHidden(hidden).setHarmless(harmless)
			.setCanBypassCooldown(canBypassCooldown).setSourcesPlants(sourcesPlants);
	}

	public static <T extends Ability> AbilityDescriptionBuilder builder(String name, Class<T> type) {
		return new AbilityDescriptionBuilder(name, type);
	}

	public static class AbilityDescriptionBuilder {
		private final String name;
		private final Class<? extends Ability> type;
		private Element element;
		private EnumSet<ActivationMethod> activationMethods;
		private long cooldown = 0;
		private String description = "";
		private String instructions = "";
		private boolean hidden = false;
		private boolean harmless = false;
		private boolean canBypassCooldown = false;
		private boolean sourcesPlants = false;

		public <T extends Ability> AbilityDescriptionBuilder(String name, Class<T> type) {
			this.name = Objects.requireNonNull(name);
			this.type = Objects.requireNonNull(type);
		}

		public AbilityDescriptionBuilder setElement(Element element) {
			this.element = Objects.requireNonNull(element);
			return this;
		}

		public AbilityDescriptionBuilder setActivation(Collection<ActivationMethod> methods) {
			if (methods == null || methods.isEmpty()) throw new NullPointerException();
			for (ActivationMethod m : methods) {
				Objects.requireNonNull(m);
			}
			activationMethods = EnumSet.copyOf(methods);
			return this;
		}

		public AbilityDescriptionBuilder setActivation(ActivationMethod method, ActivationMethod... methods) {
			activationMethods = EnumSet.of(Objects.requireNonNull(method));
			if (methods != null) {
				for (ActivationMethod m : methods) {
					activationMethods.add(Objects.requireNonNull(m));
				}
			}
			return this;
		}

		public AbilityDescriptionBuilder setCooldown(long cooldown) {
			this.cooldown = FastMath.max(0, cooldown);
			return this;
		}

		public AbilityDescriptionBuilder setDescription(String description) {
			this.description = Objects.requireNonNull(description);
			return this;
		}

		public AbilityDescriptionBuilder setInstructions(String instructions) {
			this.instructions = Objects.requireNonNull(instructions);
			return this;
		}

		public AbilityDescriptionBuilder setHidden(boolean hidden) {
			this.hidden = hidden;
			return this;
		}

		public AbilityDescriptionBuilder setHarmless(boolean harmless) {
			this.harmless = harmless;
			return this;
		}

		public AbilityDescriptionBuilder setCanBypassCooldown(boolean canBypassCooldown) {
			this.canBypassCooldown = canBypassCooldown;
			return this;
		}

		public AbilityDescriptionBuilder setSourcesPlants(boolean sourcesPlants) {
			this.sourcesPlants = sourcesPlants;
			return this;
		}

		public AbilityDescription build() {
			Objects.requireNonNull(element);
			Objects.requireNonNull(activationMethods);
			return new AbilityDescription(this);
		}

		public AbilityDescription buildMulti(String displayName, String parent, String sub) {
			Objects.requireNonNull(element);
			Objects.requireNonNull(activationMethods);
			return new MultiAbilityDescription(this, displayName, parent, sub);
		}
	}

	public static TextComponent getMeta(AbilityDescription desc) {
		if (desc == null) return TextComponent.empty();
		String type = "Ability";
		if (desc.isActivatedBy(ActivationMethod.PASSIVE)) {
			type = "Passive";
		} else if (desc.isActivatedBy(ActivationMethod.SEQUENCE)) {
			type = "Sequence";
		}
		TextComponent details = TextComponent.builder()
			.append(desc.getElement().getDisplayName()).append(TextComponent.newline())
			.append("Type: ", NamedTextColor.DARK_AQUA)
			.append(type, NamedTextColor.GREEN).append(TextComponent.newline())
			.append("Permission: ", NamedTextColor.DARK_AQUA)
			.append(desc.getPermission(), NamedTextColor.GREEN).append(TextComponent.newline()).append(TextComponent.newline())
			.append("Click to view info about this ability.", NamedTextColor.GRAY).build();

		return TextComponent.builder(desc.getName(), desc.getElement().getColor())
			.hoverEvent(HoverEvent.of(HoverEvent.Action.SHOW_TEXT, details))
			.clickEvent(ClickEvent.of(ClickEvent.Action.RUN_COMMAND, "/bending info " + desc.getName()))
			.build();
	}
}
