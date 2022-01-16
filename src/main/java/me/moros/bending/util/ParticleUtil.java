/*
 * Copyright 2020-2022 Moros
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

package me.moros.bending.util;

import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.user.User;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Utility class to provide create and render {@link Particle}.
 * You should prefer this over using a custom ParticleBuilder to ensure uniform rendering across different abilities.
 */
public final class ParticleUtil {
  public static final Color AIR = fromHex("EEEEEE");

  private final Particle particle;
  private Vector3d location;
  private double offsetX = 0, offsetY = 0, offsetZ = 0;
  private int count = 1;
  private double extra = 0;
  private Object data;

  private ParticleUtil(Particle particle) {
    this.particle = particle;
  }

  private ParticleUtil(Color color, float size) {
    this.particle = Particle.REDSTONE;
    data = new Particle.DustOptions(color, size);
  }

  public @NonNull ParticleUtil spawn(@NonNull World world) {
    if (location == null) {
      throw new IllegalStateException("Please specify location for this particle");
    }
    world.spawnParticle(particle, null, null,
      location.getX(), location.getY(), location.getZ(),
      count, offsetX, offsetY, offsetZ, extra, data, true
    );
    return this;
  }

  public @NonNull ParticleUtil spawnAsync(@NonNull World world) {
    Tasker.async(() -> spawn(world));
    return this;
  }

  public @NonNull ParticleUtil location(@NonNull Vector3d location) {
    this.location = location;
    return this;
  }

  public @NonNull ParticleUtil location(double x, double y, double z) {
    return location(new Vector3d(x, y, z));
  }

  public @NonNull ParticleUtil count(int count) {
    this.count = count;
    return this;
  }

  public @NonNull ParticleUtil offset(@NonNull Vector3d offset) {
    return offset(offset.getX(), offset.getY(), offset.getZ());
  }

  public @NonNull ParticleUtil offset(double offset) {
    return offset(offset, offset, offset);
  }

  public @NonNull ParticleUtil offset(double offsetX, double offsetY, double offsetZ) {
    this.offsetX = offsetX;
    this.offsetY = offsetY;
    this.offsetZ = offsetZ;
    return this;
  }

  public @NonNull ParticleUtil extra(double extra) {
    this.extra = extra;
    return this;
  }

  public @NonNull <T> ParticleUtil data(@Nullable T data) {
    this.data = data;
    return this;
  }

  public static @NonNull ParticleUtil fire(@NonNull User user, @NonNull Vector3d center) {
    Particle effect = user.hasPermission("bending.bluefire") ? Particle.SOUL_FIRE_FLAME : Particle.FLAME;
    return new ParticleUtil(effect).location(center);
  }

  public static @NonNull ParticleUtil air(@NonNull Vector3d center) {
    return new ParticleUtil(AIR, 1.8F).location(center);
  }

  public static @NonNull ParticleUtil rgb(@NonNull Vector3d center, @NonNull String hexVal) {
    return rgb(center, hexVal, 1);
  }

  public static @NonNull ParticleUtil rgb(@NonNull Vector3d center, @NonNull String hexVal, float size) {
    return new ParticleUtil(fromHex(hexVal), size).location(center);
  }

  public static @NonNull ParticleUtil of(@NonNull Particle effect, @NonNull Vector3d center) {
    return new ParticleUtil(effect).location(center);
  }

  public static @NonNull ParticleUtil bubble(@NonNull Block center) {
    return new ParticleUtil(Particle.WATER_BUBBLE).location(Vector3d.center(center)).count(3).offset(0.25);
  }

  private static Color fromHex(@NonNull String hexValue) {
    if (hexValue.length() < 6) {
      return Color.BLACK;
    }
    int r = Integer.valueOf(hexValue.substring(0, 2), 16);
    int g = Integer.valueOf(hexValue.substring(2, 4), 16);
    int b = Integer.valueOf(hexValue.substring(4, 6), 16);
    return Color.fromRGB(r, g, b);
  }
}
