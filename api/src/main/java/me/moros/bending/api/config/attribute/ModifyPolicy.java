/*
 * Copyright 2020-2025 Moros
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

package me.moros.bending.api.config.attribute;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.util.collect.ElementSet;
import net.kyori.adventure.key.Keyed;

/**
 * Represents a policy that determines whether an ability can be modified by an attribute modifier.
 */
public interface ModifyPolicy extends Keyed {
  boolean shouldModify(AbilityDescription desc);

  static ModifyPolicy of(Element element) {
    var singleElementSet = ElementSet.of(element);
    return new ModifyPolicyImpl(element.key(), d -> d.elements().equals(singleElementSet));
  }

  static ModifyPolicy of(AbilityDescription desc) {
    return new ModifyPolicyImpl(desc.key(), d -> d.equals(desc));
  }
}
