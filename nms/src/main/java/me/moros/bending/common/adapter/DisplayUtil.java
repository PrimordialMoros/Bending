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

package me.moros.bending.common.adapter;

import me.moros.bending.api.platform.entity.display.BlockDisplay;
import me.moros.bending.api.platform.entity.display.Display;
import me.moros.bending.api.platform.entity.display.ItemDisplay;
import me.moros.bending.api.platform.entity.display.TextDisplay;
import me.moros.bending.api.platform.entity.display.TextDisplay.TextFlags;
import me.moros.bending.api.platform.entity.display.Transformation;
import me.moros.bending.common.adapter.EntityMeta.EntityStatus;
import me.moros.math.Position;
import me.moros.math.Quaternion;
import net.minecraft.world.entity.EntityType;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class DisplayUtil {
  private DisplayUtil() {
  }

  static EntityType<?> mapProperties(AbstractPacketUtil packetUtil, EntityDataBuilder builder, Display<?> properties) {
    applyCommon(builder, properties);
    return switch (properties) {
      case BlockDisplay display -> {
        builder.setRaw(EntityMeta.BLOCK_STATE_ID, packetUtil.adapt(display.data()));
        yield EntityType.BLOCK_DISPLAY;
      }
      case ItemDisplay display -> {
        builder.setRaw(EntityMeta.DISPLAYED_ITEM, packetUtil.adapt(display.data()));
        builder.setRaw(EntityMeta.DISPLAY_TYPE, display.displayType().getId());
        yield EntityType.ITEM_DISPLAY;
      }
      case TextDisplay display -> {
        builder.setRaw(EntityMeta.TEXT, packetUtil.adapt(display.data()));
        builder.setRaw(EntityMeta.LINE_WIDTH, display.lineWidth());
        builder.setRaw(EntityMeta.BACKGROUND_COLOR, display.backgroundColor());
        builder.setRaw(EntityMeta.OPACITY, display.opacity());
        builder.setRaw(EntityMeta.TEXT_FLAGS, DisplayUtil.packTextDisplayFlagsIntoByte(display.textFlags()));
        yield EntityType.TEXT_DISPLAY;
      }
    };
  }

  static EntityDataBuilder applyTransformation(EntityDataBuilder builder, Transformation transformation) {
    // calculate offset to center according to scale
    var offset = transformation.scale().toVector3d().multiply(-0.5);
    var translation = offset.add(transformation.translation()).add(0, 0.5, 0);
    return builder.setRaw(EntityMeta.TRANSLATION, adapt(translation))
      .setRaw(EntityMeta.SCALE, adapt(transformation.scale()))
      .setRaw(EntityMeta.ROTATION_LEFT, adapt(transformation.left()))
      .setRaw(EntityMeta.ROTATION_RIGHT, adapt(transformation.right()));
  }

  static EntityDataBuilder applyCommon(EntityDataBuilder builder, Display<?> properties) {
    if (properties.glowColor() != -1) {
      builder.withStatus(EntityStatus.GLOWING);
    }
    return applyTransformation(builder, properties.transformation())
      .setRaw(EntityMeta.INTERPOLATION_DELAY, properties.interpolationDelay())
      .setRaw(EntityMeta.TRANSFORMATION_INTERPOLATION_DURATION, properties.transformationInterpolationDuration())
      .setRaw(EntityMeta.POSITION_INTERPOLATION_DURATION, properties.positionInterpolationDuration())
      .setRaw(EntityMeta.BILLBOARD, properties.billboard().getId())
      .setRaw(EntityMeta.BRIGHTNESS, properties.brightness())
      .setRaw(EntityMeta.VIEW_RANGE, properties.viewRange())
      .setRaw(EntityMeta.SHADOW_RADIUS, properties.shadowRadius())
      .setRaw(EntityMeta.SHADOW_STRENGTH, properties.shadowStrength())
      .setRaw(EntityMeta.WIDTH, properties.width())
      .setRaw(EntityMeta.HEIGHT, properties.height())
      .setRaw(EntityMeta.GLOW_COLOR_OVERRIDE, properties.glowColor());
  }

  static byte packTextDisplayFlagsIntoByte(TextFlags textFlags) {
    byte packedByte = 0;
    if (textFlags.hasShadow()) {
      packedByte |= 0x01;
    }
    if (textFlags.isSeeThrough()) {
      packedByte |= 0x02;
    }
    if (textFlags.hasDefaultBackground()) {
      packedByte |= 0x04;
    }
    packedByte |= switch (textFlags.alignment()) {
      case CENTER -> packedByte;
      case LEFT -> 0x08;
      case RIGHT -> 0x16;
    };
    return packedByte;
  }

  private static Quaternionf adapt(Quaternion rot) {
    return new Quaternionf(rot.q1(), rot.q1(), rot.q2(), rot.q0());
  }

  private static Vector3f adapt(Position vector) {
    return new Vector3f((float) vector.x(), (float) vector.y(), (float) vector.z());
  }
}
