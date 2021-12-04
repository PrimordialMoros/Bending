/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.util;

import com.destroystokyo.paper.ParticleBuilder;
import me.moros.bending.model.user.User;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Utility class to provide create and render {@link Particle}.
 * You should prefer this over using a custom ParticleBuilder to ensure uniform rendering across different abilities.
 * @see ParticleBuilder
 */
public final class ParticleUtil extends ParticleBuilder {
  public static final Color AIR = fromHex("EEEEEE");

  private ParticleUtil(@NonNull Particle particle) {
    super(particle);
  }

  public static @NonNull ParticleBuilder fire(@NonNull User user, @NonNull Location center) {
    Particle effect = user.hasPermission("bending.bluefire") ? Particle.SOUL_FIRE_FLAME : Particle.FLAME;
    return new ParticleUtil(effect).location(center).extra(0);
  }

  public static @NonNull ParticleBuilder air(@NonNull Location center) {
    return new ParticleUtil(Particle.REDSTONE).location(center).extra(0).color(AIR, 1.8F);
  }

  public static @NonNull ParticleBuilder rgb(@NonNull Location center, @NonNull String hexVal) {
    return new ParticleUtil(Particle.REDSTONE).location(center).extra(0).color(fromHex(hexVal));
  }

  public static @NonNull ParticleBuilder rgb(@NonNull Location center, @NonNull String hexVal, float size) {
    return new ParticleUtil(Particle.REDSTONE).location(center).extra(0).color(fromHex(hexVal), size);
  }

  public static @NonNull ParticleBuilder of(@NonNull Particle effect, @NonNull Location center) {
    return new ParticleUtil(effect).location(center).extra(0);
  }

  public static @NonNull ParticleBuilder bubble(@NonNull Block center) {
    return new ParticleUtil(Particle.WATER_BUBBLE).location(center.getLocation().add(0.5, 0.5, 0.5)).extra(0)
      .count(3).offset(0.25, 0.25, 0.25);
  }

  /**
   * Asynchronously spawns and sends the given particle effect to its receivers.
   * Make sure you have collected the receivers in a sync thread first.
   */
  public void drawAsync() {
    if (hasReceivers()) {
      Tasker.async(this::spawn);
    }
  }

  /**
   * Convert a hex string into a {@link Color}.
   * @param hexValue the string holding the hex value, needs to be in the format "RRGGBB"
   * @return the color from the provided hex value or {@link Color#BLACK} if hex value was invalid
   */
  public static @NonNull Color fromHex(@NonNull String hexValue) {
    if (hexValue.length() < 6) {
      return Color.BLACK;
    }
    int r = Integer.valueOf(hexValue.substring(0, 2), 16);
    int g = Integer.valueOf(hexValue.substring(2, 4), 16);
    int b = Integer.valueOf(hexValue.substring(4, 6), 16);
    return Color.fromRGB(r, g, b);
  }
}
