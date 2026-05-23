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

import java.util.Objects;

import me.moros.bending.api.platform.world.World;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.FeaturePermissions;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.RGBLike;

/**
 * Utility class to provide create and render {@link Particle} with a fluent builder.
 * You should prefer this over using a custom ParticleBuilder to ensure uniform rendering across different abilities.
 */
public final class ParticleBuilder {
  private static final RGBLike AIR = fromHex("#EEEEEE");

  private final Particle particle;
  private final Position position;
  private final ParticleOptionHolder.Builder options;
  private double extra = 0;

  private ParticleBuilder(Particle particle, Position position) {
    this.particle = particle;
    this.position = position;
    this.options = ParticleOptionHolder.emptyMutable();
  }

  /**
   * Set the amount of particles to spawn.
   * @param count the amount
   * @return the modified builder
   */
  public ParticleBuilder count(int count) {
    return option(ParticleOptions.QUANTITY, count);
  }

  /**
   * Set the offset for particles.
   * @param offset vector with 3 offset components
   * @return the modified builder
   */
  public ParticleBuilder offset(Position offset) {
    return option(ParticleOptions.OFFSET, offset.toVector3d());
  }

  /**
   * Set the offset for particles.
   * @param offset the offset to use for all components
   * @return the modified builder
   */
  public ParticleBuilder offset(double offset) {
    return offset(offset, offset, offset);
  }

  /**
   * Set the offset for particles
   * @param offsetX the offset in the x component
   * @param offsetY the offset in the y component
   * @param offsetZ the offset in the z component
   * @return the modified builder
   */
  public ParticleBuilder offset(double offsetX, double offsetY, double offsetZ) {
    return offset(Vector3d.of(offsetX, offsetY, offsetZ));
  }

  /**
   * Set the extra value for particles, usually denotes speed.
   * @param extra the extra value to use
   * @return the modified builder
   */
  public ParticleBuilder extra(double extra) {
    this.extra = extra;
    return this;
  }

  /**
   * Add an option for this particle.
   * @param option the particle option
   * @param value the value
   * @param <V> the type of particle option and value
   * @return the modified builder
   */
  public <V> ParticleBuilder option(ParticleOption<V> option, V value) throws IllegalArgumentException {
    this.options.put(option, value);
    return this;
  }

  /**
   * Build a particle context from this builder.
   */
  public ParticleContext build() {
    return new ParticleContextImpl(particle, position, extra, options.build());
  }

  /**
   * Spawn and render the particle
   * @param world the world to spawn the particles in
   */
  public void spawn(World world) {
    world.spawnParticle(build());
  }

  /**
   * Create a new particle builder with air particles pre-configured.
   * @param center the location to spawn the particles in
   * @return a new builder instance
   */
  public static ParticleBuilder air(Position center) {
    return of(Particle.DUST, center)
      .option(ParticleOptions.COLOR, AIR)
      .option(ParticleOptions.SCALE, 1.8);
  }

  /**
   * Create a new particle builder with water bubble particles pre-configured.
   * @param center the block location to spawn the particles in
   * @return a new builder instance
   */
  public static ParticleBuilder bubble(Position center) {
    return Particle.BUBBLE.builder(center.center()).count(3).offset(0.25);
  }

  /**
   * Create a new particle builder with fire particles pre-configured for the specified user.
   * <p>Note: If user has access to blue fire, then soul fire particles will be used instead of the normal ones.
   * @param user the user creating the particles
   * @param center the location to spawn the particles in
   * @return a new builder instance
   */
  public static ParticleBuilder fire(User user, Position center) {
    Particle effect = user.hasPermission(FeaturePermissions.BLUE_FIRE) ? Particle.SOUL_FIRE_FLAME : Particle.FLAME;
    return effect.builder(center);
  }

  /**
   * Create a new {@link Particle#DUST} particle builder with the specified rgb color.
   * @param center the location to spawn the particles in
   * @param hexVal the rgb color in hex format
   * @return a new builder instance
   */
  public static ParticleBuilder rgb(Position center, String hexVal) {
    return of(Particle.DUST, center)
      .option(ParticleOptions.COLOR, fromHex(hexVal));
  }

  /**
   * Create a new {@link Particle#DUST} particle builder with the specified rgb color.
   * @param center the location to spawn the particles in
   * @param color the rgb color
   * @return a new builder instance
   */
  @Deprecated(forRemoval = true)
  public static ParticleBuilder rgb(Position center, RGBLike color) {
    return of(Particle.DUST, center)
      .option(ParticleOptions.COLOR, color);
  }

  /**
   * Create a new {@link Particle#DUST} particle builder with the specified rgb color and size.
   * <p>Note: Particle size also affects particle lifetime.
   * @param center the location to spawn the particles in
   * @param hexVal the rgb color in hex format
   * @param size the particle size
   * @return a new builder instance
   */
  @Deprecated(forRemoval = true)
  public static ParticleBuilder rgb(Position center, String hexVal, float size) {
    return rgb(center, hexVal)
      .option(ParticleOptions.SCALE, (double) size);
  }

  /**
   * Create a new {@link Particle#DUST} particle builder with the specified rgb color and size.
   * <p>Note: Particle size also affects particle lifetime.
   * @param center the location to spawn the particles in
   * @param hexVal the rgb color in hex format
   * @param size the particle size
   * @return a new builder instance
   */
  public static ParticleBuilder rgb(Position center, String hexVal, double size) {
    return rgb(center, hexVal)
      .option(ParticleOptions.SCALE, size);
  }

  /**
   * Create a new {@link Particle#DUST} particle builder with the specified rgb color and size.
   * <p>Note: Particle size also affects particle lifetime.
   * @param center the location to spawn the particles in
   * @param color the rgb color
   * @param size the particle size
   * @return a new builder instance
   */
  @Deprecated(forRemoval = true)
  public static ParticleBuilder rgb(Position center, RGBLike color, float size) {
    return rgb(center, color)
      .option(ParticleOptions.SCALE, (double) size);
  }

  /**
   * Create a new {@link Particle#DUST_COLOR_TRANSITION} particle builder with the specified rgb color.
   * @param center the location to spawn the particles in
   * @param fromHexVal the starting rgb color in hex format
   * @param toHexVal the target rgb color in hex format
   * @return a new builder instance
   */
  @Deprecated(forRemoval = true)
  public static ParticleBuilder rgb(Position center, String fromHexVal, String toHexVal) {
    return rgb(center, fromHexVal, toHexVal, 1);
  }

  /**
   * Create a new {@link Particle#DUST_COLOR_TRANSITION} particle builder with the specified rgb color.
   * @param center the location to spawn the particles in
   * @param from the starting rgb color
   * @param to the target rgb color
   * @return a new builder instance
   */
  @Deprecated(forRemoval = true)
  public static ParticleBuilder rgb(Position center, RGBLike from, RGBLike to) {
    return rgb(center, from, to, 1);
  }

  /**
   * Create a new {@link Particle#DUST_COLOR_TRANSITION} particle builder with the specified rgb color and size.
   * <p>Note: Particle size also affects particle lifetime.
   * @param center the location to spawn the particles in
   * @param fromHexVal the starting rgb color in hex format
   * @param toHexVal the target rgb color in hex format
   * @param size the particle size
   * @return a new builder instance
   */
  @Deprecated(forRemoval = true)
  public static ParticleBuilder rgb(Position center, String fromHexVal, String toHexVal, float size) {
    return rgb(center, fromHex(fromHexVal), fromHex(toHexVal), size);
  }

  /**
   * Create a new {@link Particle#DUST_COLOR_TRANSITION} particle builder with the specified rgb colors and size.
   * <p>Note: Particle size also affects particle lifetime.
   * @param center the location to spawn the particles in
   * @param from the starting rgb color
   * @param to the target rgb color
   * @param size the particle size
   * @return a new builder instance
   */
  @Deprecated(forRemoval = true)
  public static ParticleBuilder rgb(Position center, RGBLike from, RGBLike to, float size) {
    return of(Particle.DUST_COLOR_TRANSITION, center)
      .option(ParticleOptions.COLOR, from)
      .option(ParticleOptions.TO_COLOR, to)
      .option(ParticleOptions.SCALE, (double) size);
  }

  static ParticleBuilder of(Particle effect, Position center) {
    Objects.requireNonNull(effect);
    Objects.requireNonNull(center);
    return new ParticleBuilder(effect, center);
  }

  @Deprecated(forRemoval = true)
  static <T> ParticleBuilder of(Particle effect, T data, Position center) {
    return of(effect, center);
  }

  private static RGBLike fromHex(String hexValue) {
    var color = TextColor.fromHexString(hexValue);
    return color == null ? NamedTextColor.BLACK : color;
  }
}
