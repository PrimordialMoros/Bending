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

package me.moros.bending.api.platform.entity;

import me.moros.bending.api.platform.property.BooleanProperty;
import me.moros.bending.api.platform.property.DoubleProperty;
import me.moros.bending.api.platform.property.FloatProperty;
import me.moros.bending.api.platform.property.IntegerProperty;
import me.moros.bending.api.platform.property.Property;
import net.kyori.adventure.text.Component;

import static me.moros.bending.api.platform.property.Property.*;

public final class EntityProperties {
  private EntityProperties() {
  }

  public static final BooleanProperty SNEAKING = boolProp("sneaking");
  public static final BooleanProperty SPRINTING = boolProp("sprinting");
  public static final BooleanProperty ALLOW_FLIGHT = boolProp("allow_flight");
  public static final BooleanProperty FLYING = boolProp("flying");
  public static final BooleanProperty GLIDING = boolProp("gliding");
  public static final BooleanProperty CHARGED = boolProp("charged");
  public static final BooleanProperty ALLOW_PICKUP = boolProp("allow_pickup");
  public static final BooleanProperty AI = boolProp("ai");
  public static final BooleanProperty GRAVITY = boolProp("gravity");
  public static final BooleanProperty INVULNERABLE = boolProp("invulnerable");
  public static final BooleanProperty IN_WATER = boolProp("in_water");
  public static final BooleanProperty IN_LAVA = boolProp("in_lava");
  public static final BooleanProperty VISIBLE = boolProp("visible");

  public static final IntegerProperty MAX_OXYGEN = intProp("max_oxygen");
  public static final IntegerProperty REMAINING_OXYGEN = intProp("remaining_oxygen", -20, 300);
  public static final IntegerProperty REQUIRED_TICKS_TO_FREEZE = intProp("required_ticks_to_freeze");
  public static final IntegerProperty FREEZE_TICKS = intProp("freeze_ticks", 0, Integer.MAX_VALUE);
  public static final IntegerProperty FIRE_IMMUNE_TICKS = intProp("fire_immune_ticks");
  public static final IntegerProperty FIRE_TICKS = intProp("fire_ticks");

  public static final DoubleProperty WIDTH = doubleProp("width", 0, 256);
  public static final DoubleProperty HEIGHT = doubleProp("height", 0, 256);

  public static final FloatProperty YAW = floatProp("yaw", -180, 180);
  public static final FloatProperty PITCH = floatProp("pitch", -180, 180);
  public static final FloatProperty FALL_DISTANCE = floatProp("fall_distance");

  public static final Property<Component> NAME = prop("name", Component.class);
}
