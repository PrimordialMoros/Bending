/*
 * Copyright 2024-2025 Moros
 *
 * This file is part of Bifrost.
 *
 * Bifrost is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bifrost is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bifrost. If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.paper.platform.entity;

import java.util.OptionalDouble;

import me.moros.bending.api.config.attribute.ModifierOperation;
import me.moros.bending.api.platform.entity.AttributeProperties;
import me.moros.bending.api.platform.entity.AttributeType;
import me.moros.bending.paper.platform.PlatformAdapter;
import net.kyori.adventure.key.Key;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public record BukkitAttributes(LivingEntity handle) implements AttributeProperties {
  @Override
  public OptionalDouble value(AttributeType type) {
    var instance = attributeInstance(type);
    return instance == null ? OptionalDouble.empty() : OptionalDouble.of(instance.getValue());
  }

  @Override
  public OptionalDouble baseValue(AttributeType type) {
    var instance = attributeInstance(type);
    return instance == null ? OptionalDouble.empty() : OptionalDouble.of(instance.getBaseValue());
  }

  @Override
  public boolean baseValue(AttributeType type, double baseValue) {
    var instance = attributeInstance(type);
    if (instance == null) {
      return false;
    }
    instance.setBaseValue(baseValue);
    return true;
  }

  @Override
  public boolean modify(AttributeType type, Key key, ModifierOperation operation, double value) {
    var instance = attributeInstance(type);
    if (instance == null) {
      return false;
    }
    var id = PlatformAdapter.nsk(key);
    var op = PlatformAdapter.toBukkitAttributeOperation(operation);
    instance.addTransientModifier(new AttributeModifier(id, value, op));
    return true;
  }

  private AttributeInstance attributeInstance(AttributeType type) {
    return handle().getAttribute(PlatformAdapter.toBukkitAttribute(type));
  }
}
