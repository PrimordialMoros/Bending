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

import me.moros.bending.api.platform.entity.display.Display;
import me.moros.bending.api.platform.entity.display.TextDisplay;
import me.moros.math.Quaternion;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class DisplayUtil {
  private DisplayUtil() {
  }

  static EntityDataBuilder applyCommon(EntityDataBuilder builder, Display<?> properties) {
    // calculate offset to center according to scale
    var offset = properties.transformation().scale().toVector3d().multiply(-0.5);
    var translation = offset.add(properties.transformation().translation()).add(0, 0.5, 0);
    return builder.setRaw(EntityMeta.INTERPOLATION_DELAY, properties.interpolationDelay())
      .setRaw(EntityMeta.INTERPOLATION_DURATION, properties.interpolationDuration())
      .setRaw(EntityMeta.TRANSLATION, translation.to(Vector3f.class))
      .setRaw(EntityMeta.SCALE, properties.transformation().scale().to(Vector3f.class))
      .setRaw(EntityMeta.ROTATION_LEFT, adapt(properties.transformation().left()))
      .setRaw(EntityMeta.ROTATION_RIGHT, adapt(properties.transformation().right()))
      .setRaw(EntityMeta.BILLBOARD, properties.billboard().getId())
      .setRaw(EntityMeta.BRIGHTNESS, properties.brightness())
      .setRaw(EntityMeta.VIEW_RANGE, properties.viewRange())
      .setRaw(EntityMeta.SHADOW_RADIUS, properties.shadowRadius())
      .setRaw(EntityMeta.SHADOW_STRENGTH, properties.shadowStrength())
      .setRaw(EntityMeta.WIDTH, properties.width())
      .setRaw(EntityMeta.HEIGHT, properties.height())
      .setRaw(EntityMeta.GLOW_COLOR_OVERRIDE, properties.glowColor());
  }

  static byte packTextDisplayFlagsIntoByte(TextDisplay textDisplay) {
    byte packedByte = 0;
    if (textDisplay.hasShadow()) {
      packedByte |= 0x01;
    }
    if (textDisplay.isSeeThrough()) {
      packedByte |= 0x02;
    }
    if (textDisplay.hasDefaultBackground()) {
      packedByte |= 0x04;
    }
    packedByte |= switch (textDisplay.alignment()) {
      case CENTER -> packedByte;
      case LEFT -> 0x08;
      case RIGHT -> 0x16;
    };
    return packedByte;
  }

  private static Quaternionf adapt(Quaternion rot) {
    return new Quaternionf(rot.q1(), rot.q1(), rot.q2(), rot.q0());
  }
}
