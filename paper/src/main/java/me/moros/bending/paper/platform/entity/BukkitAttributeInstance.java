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

import me.moros.bending.api.config.attribute.ModifierOperation;
import me.moros.bending.api.platform.entity.AttributeInstance;
import me.moros.bending.api.platform.entity.AttributeType;
import me.moros.bending.paper.platform.PlatformAdapter;
import net.kyori.adventure.key.Key;
import org.bukkit.attribute.AttributeModifier;

public record BukkitAttributeInstance(org.bukkit.attribute.AttributeInstance handle) implements AttributeInstance {
  @Override
  public AttributeType type() {
    return PlatformAdapter.fromFabricAttribute(handle().getAttribute());
  }

  @Override
  public double value() {
    return handle().getValue();
  }

  @Override
  public double baseValue() {
    return handle().getBaseValue();
  }

  @Override
  public void baseValue(double baseValue) {
    handle().setBaseValue(baseValue);
  }

  @Override
  public void modify(Key key, ModifierOperation operation, double value) {
    var id = PlatformAdapter.nsk(key);
    var op = PlatformAdapter.toBukkitAttributeOperation(operation);
    handle().addTransientModifier(new AttributeModifier(id, value, op));
  }
}
