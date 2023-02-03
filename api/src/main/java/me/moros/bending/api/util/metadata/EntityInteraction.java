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

package me.moros.bending.api.util.metadata;

import java.lang.ref.WeakReference;

import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.util.KeyUtil;
import me.moros.bending.api.util.data.DataKey;
import me.moros.math.Vector3d;
import org.checkerframework.checker.nullness.qual.Nullable;

public record EntityInteraction(WeakReference<Entity> entity, @Nullable Vector3d point) implements Interaction<Entity> {
  public static final DataKey<EntityInteraction> KEY = KeyUtil.data("last-interacted-enttity", EntityInteraction.class);

  public EntityInteraction(Entity entity, @Nullable Vector3d point) {
    this(new WeakReference<>(entity), point);
  }

  @Override
  public @Nullable Entity value() {
    return entity.get();
  }
}
