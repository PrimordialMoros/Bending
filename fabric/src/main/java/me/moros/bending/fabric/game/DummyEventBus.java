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

package me.moros.bending.fabric.game;

import java.util.function.Consumer;

import me.moros.bending.api.event.BendingEvent;
import me.moros.bending.api.event.EventBus;
import me.moros.bending.common.event.EventBusImpl;

final class DummyEventBus extends EventBusImpl {
  static final EventBus INSTANCE = new DummyEventBus();

  private DummyEventBus() {
  }

  @Override
  public void shutdown() {
  }

  @Override
  public <T extends BendingEvent> void subscribe(Class<T> event, Consumer<? super T> subscriber, int priority) {
  }

  @Override
  public <T extends BendingEvent> boolean post(T event) {
    return false;
  }
}
