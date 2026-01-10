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

package me.moros.bending.api.util.metadata;

import java.lang.ref.WeakReference;

import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.util.KeyUtil;
import me.moros.bending.api.util.data.DataKey;
import me.moros.math.Vector3d;
import org.jspecify.annotations.Nullable;

public record EntityInteraction(WeakReference<Entity> entity, @Nullable Vector3d point,
                                long timestamp) implements Interaction<Entity> {
  public static final DataKey<EntityInteraction> KEY = KeyUtil.data("last-interacted-entity", EntityInteraction.class);

  public EntityInteraction(Entity entity, @Nullable Vector3d point) {
    this(new WeakReference<>(entity), point, System.currentTimeMillis());
  }

  @Override
  public @Nullable Entity value() {
    return entity.get();
  }
}
