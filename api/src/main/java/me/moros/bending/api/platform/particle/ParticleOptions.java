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

package me.moros.bending.api.platform.particle;

import java.util.function.Function;

import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.platform.particle.option.PositionSource;
import me.moros.bending.api.util.KeyUtil;
import me.moros.math.Vector3d;
import net.kyori.adventure.util.RGBLike;

public final class ParticleOptions {
  private ParticleOptions() {
  }

  public static final ParticleOption<BlockState> BLOCK_STATE = create("block_state", BlockState.class);

  public static final ParticleOption<RGBLike> COLOR = create("color", RGBLike.class);

  public static final ParticleOption<Integer> DELAY = create("delay", Integer.class);

  public static final ParticleOption<PositionSource> DESTINATION = create("destination", PositionSource.class);

  public static final ParticleOption<ItemSnapshot> ITEM_SNAPSHOT = create("item", ItemSnapshot.class,
    v -> v.isEmpty() ? "Item snapshot must not be empty" : null);

  public static final ParticleOption<Vector3d> OFFSET = create("offset", Vector3d.class);

  public static final ParticleOption<Double> OPACITY = create("opacity", Double.class,
    v -> v < 0 || v > 1 ? "Opacity must be between 0 and 1" : null);

  public static final ParticleOption<Integer> QUANTITY = create("quantity", Integer.class,
    v -> v < 0 ? "Quantity must be at least zero" : null);

  public static final ParticleOption<Double> ROLL = create("roll", Double.class);

  public static final ParticleOption<Double> SCALE = create("scale", Double.class,
    v -> v < 0 ? "Scale must not be negative" : null);

  public static final ParticleOption<Vector3d> TARGET = create("target", Vector3d.class);

  public static final ParticleOption<RGBLike> TO_COLOR = create("to_color", RGBLike.class);

  public static final ParticleOption<Integer> TRAVEL_TIME = create("travel_time", Integer.class);

  private static <V> ParticleOption<V> create(String key, Class<? extends V> valueType) {
    return new ParticleOptionImpl<>(KeyUtil.simple(key), valueType);
  }

  private static <V> ParticleOption<V> create(String key, Class<? extends V> valueType, Function<V, String> validator) {
    return new ParticleOptionImpl<>(KeyUtil.simple(key), valueType, validator);
  }
}
