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

package me.moros.bending.common.adapter;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

record EntityMeta<T>(int index, EntityDataSerializer<T> serializer) {
  // Entity
  static final EntityMeta<Byte> ENTITY_STATUS = create(0, EntityDataSerializers.BYTE);

  static final EntityMeta<Boolean> GRAVITY = create(5, EntityDataSerializers.BOOLEAN);

  // Display
  static final EntityMeta<Integer> INTERPOLATION_DELAY = create(8, EntityDataSerializers.INT);
  static final EntityMeta<Integer> TRANSFORMATION_INTERPOLATION_DURATION = create(9, EntityDataSerializers.INT);
  static final EntityMeta<Integer> POSITION_INTERPOLATION_DURATION = create(10, EntityDataSerializers.INT);
  static final EntityMeta<Vector3fc> TRANSLATION = create(11, EntityDataSerializers.VECTOR3);
  static final EntityMeta<Vector3fc> SCALE = create(12, EntityDataSerializers.VECTOR3);
  static final EntityMeta<Quaternionfc> ROTATION_LEFT = create(13, EntityDataSerializers.QUATERNION);
  static final EntityMeta<Quaternionfc> ROTATION_RIGHT = create(14, EntityDataSerializers.QUATERNION);
  static final EntityMeta<Byte> BILLBOARD = create(15, EntityDataSerializers.BYTE);
  static final EntityMeta<Integer> BRIGHTNESS = create(16, EntityDataSerializers.INT);
  static final EntityMeta<Float> VIEW_RANGE = create(17, EntityDataSerializers.FLOAT);
  static final EntityMeta<Float> SHADOW_RADIUS = create(18, EntityDataSerializers.FLOAT);
  static final EntityMeta<Float> SHADOW_STRENGTH = create(19, EntityDataSerializers.FLOAT);
  static final EntityMeta<Float> WIDTH = create(20, EntityDataSerializers.FLOAT);
  static final EntityMeta<Float> HEIGHT = create(21, EntityDataSerializers.FLOAT);
  static final EntityMeta<Integer> GLOW_COLOR_OVERRIDE = create(22, EntityDataSerializers.INT);
  // BlockDisplay
  static final EntityMeta<BlockState> BLOCK_STATE_ID = create(23, EntityDataSerializers.BLOCK_STATE);
  // ItemDisplay
  static final EntityMeta<ItemStack> DISPLAYED_ITEM = create(23, EntityDataSerializers.ITEM_STACK);
  static final EntityMeta<Byte> DISPLAY_TYPE = create(24, EntityDataSerializers.BYTE);
  // TextDisplay
  static final EntityMeta<Component> TEXT = create(23, EntityDataSerializers.COMPONENT);
  static final EntityMeta<Integer> LINE_WIDTH = create(24, EntityDataSerializers.INT);
  static final EntityMeta<Integer> BACKGROUND_COLOR = create(25, EntityDataSerializers.INT);
  static final EntityMeta<Byte> OPACITY = create(26, EntityDataSerializers.BYTE);
  static final EntityMeta<Byte> TEXT_FLAGS = create(27, EntityDataSerializers.BYTE);

  private static <T> EntityMeta<T> create(int index, EntityDataSerializer<T> serializer) {
    return new EntityMeta<>(index, serializer);
  }

  enum EntityStatus {
    ON_FIRE(0),
    SNEAKING(1),
    SPRINTING(3),
    SWIMMING(4),
    INVISIBLE(5),
    GLOWING(6),
    GLIDING(7);

    private final int index;

    EntityStatus(int index) {
      this.index = index;
    }

    int index() {
      return index;
    }
  }
}
