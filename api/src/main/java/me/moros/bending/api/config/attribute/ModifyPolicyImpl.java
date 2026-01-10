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

package me.moros.bending.api.config.attribute;

import java.util.function.Predicate;

import me.moros.bending.api.ability.AbilityDescription;
import net.kyori.adventure.key.Key;

record ModifyPolicyImpl(Key key, Predicate<AbilityDescription> predicate) implements ModifyPolicy {
  @Override
  public boolean shouldModify(AbilityDescription desc) {
    return predicate.test(desc);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    ModifyPolicyImpl other = (ModifyPolicyImpl) obj;
    return key.equals(other.key);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }
}
