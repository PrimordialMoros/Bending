/*
 * Copyright 2020-2023 Moros
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

package me.moros.bending.event;

import java.util.Collection;
import java.util.List;

import me.moros.bending.event.base.BendingEvent;
import net.kyori.adventure.key.Key;

/**
 * Called during Bending's initialization, before registries are locked.
 */
public class RegistryLockEvent implements BendingEvent {
  private final Collection<Key> keys;

  protected RegistryLockEvent(Collection<Key> keys) {
    this.keys = List.copyOf(keys);
  }

  /**
   * Provides the keys of the registries that are going to be locked.
   * @return the collection of RegistryKeys
   */
  public Collection<Key> registryKeys() {
    return keys;
  }
}
