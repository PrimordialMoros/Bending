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

package me.moros.bending.model.attribute;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Attributes {
	Attribute[] value();

	String ABILITY_COLLISION_RADIUS = "AbilityCollisionRadius";
	String RANGE = "Range";
	String SELECTION = "Selection";
	String COOLDOWN = "Cooldown";
	String SPEED = "Speed";
	String STRENGTH = "Strength";
	String DAMAGE = "Damage";
	String CHARGE_TIME = "ChargeTime";
	String DURATION = "Duration";
	String RADIUS = "Radius";
	String HEIGHT = "Height";
	String AMOUNT = "Amount";

	String[] TYPES = {
		Attributes.ABILITY_COLLISION_RADIUS, Attributes.RANGE,
		Attributes.SELECTION, Attributes.COOLDOWN, Attributes.SPEED, Attributes.STRENGTH, Attributes.DAMAGE,
		Attributes.CHARGE_TIME, Attributes.DURATION, Attributes.RADIUS, Attributes.HEIGHT, Attributes.AMOUNT
	};
}
