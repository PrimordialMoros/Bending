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

package me.moros.bending.api.platform;

import java.util.Optional;

import me.moros.bending.api.ability.element.ElementHandler;
import me.moros.bending.api.gui.Board;
import me.moros.bending.api.gui.ElementGui;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.item.ItemBuilder;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.user.BendingPlayer;

public interface PlatformFactory {
  Optional<Board> buildBoard(BendingPlayer player);

  Optional<ElementGui> buildMenu(ElementHandler handler, BendingPlayer player);

  ItemBuilder itemBuilder(Item item);

  ItemBuilder itemBuilder(ItemSnapshot snapshot);
}
