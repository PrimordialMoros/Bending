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

import me.moros.bending.model.user.User;
import me.moros.math.Vector3d;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Utility class to provide create and render {@link Particle} with a fluent builder.
 * You should prefer this over using a custom ParticleBuilder to ensure uniform rendering across different abilities.
 */
public final class ParticleUtil {
  public static final Color AIR = fromHex("EEEEEE");

  private final Particle particle;
  private final Vector3d location;
  private double offsetX = 0, offsetY = 0, offsetZ = 0;
  private int count = 1;
  private double extra = 0;
  private Object data;

  private ParticleUtil(Particle particle, Vector3d location) {
    this.particle = particle;
    this.location = location;
  }

  private ParticleUtil(Vector3d location, Color color, float size) {
    this(Particle.REDSTONE, location);
    data = new Particle.DustOptions(color, size);
  }

  /**
   * Spawn and render the particle
   * @param world the world to spawn the particles in
   */
  public void spawn(World world) {
    if (location == null) {
      throw new IllegalStateException("Please specify location for this particle");
    }
    world.spawnParticle(particle, null, null,
      location.x(), location.y(), location.z(),
      count, offsetX, offsetY, offsetZ, extra, data, true
    );
  }

  /**
   * Set the amount of particles to spawn.
   * @param count the amount
   * @return the modified builder
   */
  public ParticleUtil count(int count) {
    this.count = count;
    return this;
  }

  /**
   * Set the offset for particles.
   * @param offset vector with 3 offset components
   * @return the modified builder
   */
  public ParticleUtil offset(Vector3d offset) {
    return offset(offset.x(), offset.y(), offset.z());
  }

  /**
   * Set the offset for particles.
   * @param offset the offset to use for all components
   * @return the modified builder
   */
  public ParticleUtil offset(double offset) {
    return offset(offset, offset, offset);
  }

  /**
   * Set the offset for particles
   * @param offsetX the offset in the x component
   * @param offsetY the offset in the y component
   * @param offsetZ the offset in the z component
   * @return the modified builder
   */
  public ParticleUtil offset(double offsetX, double offsetY, double offsetZ) {
    this.offsetX = offsetX;
    this.offsetY = offsetY;
    this.offsetZ = offsetZ;
    return this;
  }

  /**
   * Set the extra value for particles, usually denotes speed.
   * @param extra the extra value to use
   * @return the modified builder
   */
  public ParticleUtil extra(double extra) {
    this.extra = extra;
    return this;
  }

  /**
   * Set the particle data, usually {@link BlockData} for {@link Particle#BLOCK_CRACK} or {@link Particle#BLOCK_DUST}
   * type of particles.
   * @param data the data to use
   * @param <T> the type of data
   * @return the modified builder
   */
  public <T> ParticleUtil data(@Nullable T data) {
    this.data = data;
    return this;
  }

  /**
   * Create a new particle builder with air particles pre-configured.
   * @param center the location to spawn the particles in
   * @return a new builder instance
   */
  public static ParticleUtil air(Vector3d center) {
    return new ParticleUtil(center, AIR, 1.8F);
  }

  /**
   * Create a new particle builder with water bubble particles pre-configured.
   * @param center the block location to spawn the particles in
   * @return a new builder instance
   */
  public static ParticleUtil bubble(Block center) {
    return new ParticleUtil(Particle.WATER_BUBBLE, Vector3d.fromCenter(center)).count(3).offset(0.25);
  }

  /**
   * Create a new particle builder with fire particles pre-configured for the specified user.
   * <p>Note: If user has access to blue fire, then soul fire particles will be used instead of the normal ones.
   * @param user the user creating the particles
   * @param center the location to spawn the particles in
   * @return a new builder instance
   */
  public static ParticleUtil fire(User user, Vector3d center) {
    Particle effect = user.hasPermission("bending.bluefire") ? Particle.SOUL_FIRE_FLAME : Particle.FLAME;
    return new ParticleUtil(effect, center);
  }

  /**
   * Create a new {@link Particle#REDSTONE} particle builder with the specified rgb color.
   * @param center the location to spawn the particles in
   * @param hexVal the rgb color in hex format
   * @return a new builder instance
   */
  public static ParticleUtil rgb(Vector3d center, String hexVal) {
    return rgb(center, hexVal, 1);
  }

  /**
   * Create a new {@link Particle#REDSTONE} particle builder with the specified rgb color and size.
   * <p>Note: Particle size also affects particle lifetime.
   * @param center the location to spawn the particles in
   * @param hexVal the rgb color in hex format
   * @param size the particle size
   * @return a new builder instance
   */
  public static ParticleUtil rgb(Vector3d center, String hexVal, float size) {
    return new ParticleUtil(center, fromHex(hexVal), size);
  }

  /**
   * Create a new particle builder.
   * @param effect the type of particle to use
   * @param center the location to spawn the particles in
   * @return a new builder instance
   */
  public static ParticleUtil of(Particle effect, Vector3d center) {
    return new ParticleUtil(effect, center);
  }

  private static Color fromHex(String hexValue) {
    if (hexValue.length() < 6) {
      return Color.BLACK;
    }
    int r = Integer.valueOf(hexValue.substring(0, 2), 16);
    int g = Integer.valueOf(hexValue.substring(2, 4), 16);
    int b = Integer.valueOf(hexValue.substring(4, 6), 16);
    return Color.fromRGB(r, g, b);
  }
}
