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

package me.moros.bending.common.adapter;

import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

record EntityMeta<T>(int index, EntityDataSerializer<T> serializer) {
  // Entity
  static final EntityMeta<Byte> ENTITY_STATUS = create(0, EntityDataSerializers.BYTE);
  static final EntityMeta<Boolean> GRAVITY = create(5, EntityDataSerializers.BOOLEAN);

  // ArmorStand
  static final EntityMeta<Byte> ARMOR_STAND_STATUS = create(15, EntityDataSerializers.BYTE);

  // Display
  static final EntityMeta<Integer> INTERPOLATION_DELAY = create(8, EntityDataSerializers.INT);
  static final EntityMeta<Integer> INTERPOLATION_DURATION = create(9, EntityDataSerializers.INT);
  static final EntityMeta<Vector3f> TRANSLATION = create(10, EntityDataSerializers.VECTOR3);
  static final EntityMeta<Vector3f> SCALE = create(11, EntityDataSerializers.VECTOR3);
  static final EntityMeta<Quaternionf> ROTATION_LEFT = create(12, EntityDataSerializers.QUATERNION);
  static final EntityMeta<Quaternionf> ROTATION_RIGHT = create(13, EntityDataSerializers.QUATERNION);
  static final EntityMeta<Byte> BILLBOARD = create(14, EntityDataSerializers.BYTE);
  static final EntityMeta<Integer> BRIGHTNESS = create(15, EntityDataSerializers.INT);
  static final EntityMeta<Float> VIEW_RANGE = create(16, EntityDataSerializers.FLOAT);
  static final EntityMeta<Float> SHADOW_RADIUS = create(17, EntityDataSerializers.FLOAT);
  static final EntityMeta<Float> SHADOW_STRENGTH = create(18, EntityDataSerializers.FLOAT);
  static final EntityMeta<Float> WIDTH = create(19, EntityDataSerializers.FLOAT);
  static final EntityMeta<Float> HEIGHT = create(20, EntityDataSerializers.FLOAT);
  static final EntityMeta<Integer> GLOW_COLOR_OVERRIDE = create(21, EntityDataSerializers.INT);
  // BlockDisplay
  static final EntityMeta<BlockState> BLOCK_STATE_ID = create(22, EntityDataSerializers.BLOCK_STATE);
  // ItemDisplay
  static final EntityMeta<ItemStack> DISPLAYED_ITEM = create(22, EntityDataSerializers.ITEM_STACK);
  static final EntityMeta<Byte> DISPLAY_TYPE = create(23, EntityDataSerializers.BYTE);

  private static <T> EntityMeta<T> create(int index, EntityDataSerializer<T> serializer) {
    return new EntityMeta<>(index, serializer);
  }
}
