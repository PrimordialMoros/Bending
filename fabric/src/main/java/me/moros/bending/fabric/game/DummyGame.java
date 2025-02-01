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

package me.moros.bending.fabric.game;

import me.moros.bending.api.config.ConfigProcessor;
import me.moros.bending.api.event.EventBus;
import me.moros.bending.api.game.ActivationController;
import me.moros.bending.api.game.FlightManager;
import me.moros.bending.api.game.Game;
import me.moros.bending.api.game.WorldManager;
import me.moros.bending.api.storage.BendingStorage;

public record DummyGame(EventBus eventBus, BendingStorage storage, FlightManager flightManager,
                        WorldManager worldManager,
                        ActivationController activationController, ConfigProcessor configProcessor) implements Game {
  public static final Game INSTANCE = new DummyGame(DummyEventBus.INSTANCE, DummyStorage.INSTANCE, DummyFlightManager.INSTANCE,
    DummyWorldManager.INSTANCE, DummyActivationController.INSTANCE, DummyConfigProcessor.INSTANCE);

  @Override
  public void reload() {
  }

  @Override
  public void cleanup() {
  }
}
