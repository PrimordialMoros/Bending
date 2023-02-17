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

package me.moros.bending.common.event;

import java.util.Collection;
import java.util.Set;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.event.BendingExplosionEvent;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.user.User;
import me.moros.bending.common.event.base.AbstractCancellableAbilityEvent;
import me.moros.math.Position;

public class BendingExplosionEventImpl extends AbstractCancellableAbilityEvent implements BendingExplosionEvent {
  private final Position center;
  private final Collection<Block> affectedBlocks;

  public BendingExplosionEventImpl(User user, AbilityDescription desc, Position center, Collection<Block> affectedBlocks) {
    super(user, desc);
    this.center = center;
    this.affectedBlocks = Set.copyOf(affectedBlocks);
  }

  @Override
  public Position center() {
    return center;
  }

  @Override
  public Collection<Block> affectedBlocks() {
    return affectedBlocks;
  }
}
