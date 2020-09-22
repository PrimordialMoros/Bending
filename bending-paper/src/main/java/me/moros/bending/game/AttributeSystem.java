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

package me.moros.bending.game;

import me.moros.bending.config.Configurable;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.AttributeConverter;
import me.moros.bending.model.attribute.AttributeModifier;
import me.moros.bending.model.attribute.Attributes;
import me.moros.bending.model.attribute.ModifierOperation;
import me.moros.bending.model.attribute.ModifyPolicy;
import me.moros.bending.model.user.User;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// TODO Expand system to include rounding, range checking etc and profile performance
public final class AttributeSystem {
	private final Map<User, List<UserModifier>> modifierMap = new HashMap<>();
	private static final Map<Class<? extends Number>, AttributeConverter> converters = new HashMap<>(); // Converts a double into some other numeric type

	static {
		converters.put(Double.class, AttributeConverter.DOUBLE);
		converters.put(Integer.class, AttributeConverter.INT);
		converters.put(Long.class, AttributeConverter.LONG);
		converters.put(double.class, AttributeConverter.DOUBLE);
		converters.put(int.class, AttributeConverter.INT);
		converters.put(long.class, AttributeConverter.LONG);
	}

	// Add a modifier that's only active according to some policy.
	public UserModifier addModifier(User user, AttributeModifier modifier, ModifyPolicy policy) {
		List<UserModifier> modifiers = modifierMap.computeIfAbsent(user, key -> new ArrayList<>());
		UserModifier userModifier = new UserModifier(modifier, policy);
		modifiers.add(userModifier);
		return userModifier;
	}

	public void clearModifiers(User user) {
		modifierMap.remove(user);
	}

	public boolean removeModifier(User user, AttributeModifier modifier, ModifyPolicy policy) {
		return removeModifier(user, new UserModifier(modifier, policy));
	}

	public boolean removeModifier(User user, UserModifier modifier) {
		List<UserModifier> modifiers = modifierMap.get(user);
		if (modifiers == null) {
			return false;
		}
		boolean result = modifiers.remove(modifier);
		if (modifiers.isEmpty()) {
			modifierMap.remove(user);
		}
		return result;
	}

	// Recalculates all of the config values for the user's instances.
	public void recalculate(User user) {
		Game.getAbilityManager(user.getWorld()).getUserInstances(user).forEach(Ability::recalculateConfig);
	}

	public <T extends Configurable> T calculate(Ability ability, T config) {
		User user = ability.getUser();
		if (user == null || !modifierMap.containsKey(user)) {
			return config;
		}
		List<UserModifier> activeModifiers = modifierMap.get(user).stream()
			.filter(modifier -> modifier.policy.shouldModify(ability))
			.collect(Collectors.toList());

		for (Field field : config.getClass().getDeclaredFields()) {
			if (field.isAnnotationPresent(Attribute.class) || field.isAnnotationPresent(Attributes.class)) {
				boolean wasAccessible = field.isAccessible();
				field.setAccessible(true);
				modifyField(field, config, activeModifiers);
				field.setAccessible(wasAccessible);
			}
		}

		return config;
	}

	private boolean modifyField(Field field, Configurable config, List<UserModifier> userModifiers) {
		double value;
		try {
			value = ((Number) field.get(config)).doubleValue();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return false;
		}

		double addOperation = 0.0;
		double multiplyOperation = 1.0;
		List<Double> multiplicativeOperations = new ArrayList<>();
		for (UserModifier userModifier : userModifiers) {
			AttributeModifier modifier = userModifier.modifier;
			if (hasAttribute(field, modifier.getAttribute())) {
				if (modifier.getType() == ModifierOperation.ADDITIVE) {
					addOperation += modifier.getAmount();
				} else if (modifier.getType() == ModifierOperation.SUMMED_MULTIPLICATIVE) {
					multiplyOperation += modifier.getAmount();
				} else if (modifier.getType() == ModifierOperation.MULTIPLICATIVE) {
					multiplicativeOperations.add(modifier.getAmount());
				}
			}
		}
		value = (value + addOperation) * multiplyOperation;
		for (double amount : multiplicativeOperations) {
			value *= amount;
		}
		try {
			field.set(config, converters.getOrDefault(field.getType(), AttributeConverter.DOUBLE).apply(value));
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	private boolean hasAttribute(Field field, String attributeName) {
		for (Attribute attribute : field.getAnnotationsByType(Attribute.class)) {
			if (attribute.value().equals(attributeName)) {
				return true;
			}
		}
		return false;
	}

	private static class UserModifier {
		final AttributeModifier modifier;
		final ModifyPolicy policy;

		UserModifier(AttributeModifier modifier, ModifyPolicy policy) {
			this.modifier = modifier;
			this.policy = policy;
		}
	}

	public static ModifyPolicy getElementPolicy(Element element) {
		return ability -> ability.getDescription().getElement() == element;
	}

	public static ModifyPolicy getAbilityPolicy(AbilityDescription desc) {
		return ability -> ability.getDescription().equals(desc);
	}
}
