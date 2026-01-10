/*
 * Copyright 2020-2026 Moros
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

package me.moros.bending.fabric.platform.entity;

import java.util.OptionalDouble;

import me.moros.bending.api.config.attribute.ModifierOperation;
import me.moros.bending.api.platform.entity.AttributeProperties;
import me.moros.bending.api.platform.entity.AttributeType;
import me.moros.bending.fabric.platform.PlatformAdapter;
import net.kyori.adventure.key.Key;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public record FabricAttributes(AttributeMap handle) implements AttributeProperties {
  @Override
  public OptionalDouble value(AttributeType type) {
    var t = PlatformAdapter.toFabricAttribute(type);
    return handle().hasAttribute(t) ? OptionalDouble.of(handle().getValue(t)) : OptionalDouble.empty();
  }

  @Override
  public OptionalDouble baseValue(AttributeType type) {
    var t = PlatformAdapter.toFabricAttribute(type);
    return handle().hasAttribute(t) ? OptionalDouble.of(handle().getBaseValue(t)) : OptionalDouble.empty();
  }

  @Override
  public boolean baseValue(AttributeType type, double baseValue) {
    var instance = handle().getInstance(PlatformAdapter.toFabricAttribute(type));
    if (instance == null) {
      return false;
    }
    instance.setBaseValue(baseValue);
    return true;
  }

  @Override
  public boolean addModifier(AttributeType type, Key key, ModifierOperation operation, double value) {
    var instance = handle().getInstance(PlatformAdapter.toFabricAttribute(type));
    if (instance == null) {
      return false;
    }
    var id = PlatformAdapter.identifier(key);
    var op = PlatformAdapter.toFabricAttributeOperation(operation);
    instance.addTransientModifier(new AttributeModifier(id, value, op));
    return true;
  }

  @Override
  public boolean removeModifier(AttributeType type, Key key) {
    var instance = handle().getInstance(PlatformAdapter.toFabricAttribute(type));
    return instance != null && instance.removeModifier(PlatformAdapter.identifier(key));
  }
}
