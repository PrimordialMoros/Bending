/*
 * Copyright 2020-2022 Moros
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

import me.moros.bending.model.key.Keyed;
import me.moros.bending.model.key.RegistryKey;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Called during Bending's initialization, before registries are locked.
 */
public class RegistryLockEvent extends Event {
  private static final HandlerList HANDLERS = new HandlerList();

  private final Collection<RegistryKey<? extends Keyed>> keys;

  RegistryLockEvent(Collection<RegistryKey<? extends Keyed>> keys) {
    this.keys = List.copyOf(keys);
  }

  /**
   * Provides the keys of the registries that are going to be locked.
   * @return the collection of RegistryKeys
   */
  public Collection<RegistryKey<? extends Keyed>> registryKeys() {
    return keys;
  }

  @Override
  public @NonNull HandlerList getHandlers() {
    return HANDLERS;
  }

  public static HandlerList getHandlerList() {
    return HANDLERS;
  }
}
