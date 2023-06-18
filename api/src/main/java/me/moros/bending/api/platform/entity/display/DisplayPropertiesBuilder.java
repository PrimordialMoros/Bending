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

package me.moros.bending.api.platform.entity.display;

import java.util.Objects;
import java.util.function.Function;

import me.moros.bending.api.platform.entity.display.DisplayProperties.Billboard;
import me.moros.bending.api.platform.entity.display.DisplayProperties.Transformation;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
import net.kyori.adventure.util.RGBLike;

public final class DisplayPropertiesBuilder<T> {
  private final T data;
  private final Function<DisplayPropertiesBuilder<T>, DisplayProperties<T>> factory;

  private float width = 1;
  private float height = 1;
  private float viewRange = 1;
  private float shadowRadius = 0;
  private float shadowStrength = 1;
  private int interpolationDelay = 0;
  private int interpolationDuration = 0;
  private int brightness = -1;
  private int glowColor = -1;
  private Billboard billboard = Billboard.FIXED;
  private Transformation transformation = new Transformation(Vector3d.ZERO, Vector3d.ONE);

  DisplayPropertiesBuilder(T data, Function<DisplayPropertiesBuilder<T>, DisplayProperties<T>> factory) {
    this.data = data;
    this.factory = factory;
  }

  public T data() {
    return data;
  }

  public float width() {
    return width;
  }

  public DisplayPropertiesBuilder<T> width(float width) {
    this.width = width;
    return this;
  }

  public float height() {
    return height;
  }

  public DisplayPropertiesBuilder<T> height(float height) {
    this.height = height;
    return this;
  }

  public float viewRange() {
    return viewRange;
  }

  public DisplayPropertiesBuilder<T> viewRange(float viewRange) {
    this.viewRange = viewRange;
    return this;
  }

  public float shadowRadius() {
    return shadowRadius;
  }

  public DisplayPropertiesBuilder<T> shadowRadius(float shadowRadius) {
    this.shadowRadius = shadowRadius;
    return this;
  }

  public float shadowStrength() {
    return shadowStrength;
  }

  public DisplayPropertiesBuilder<T> shadowStrength(float shadowStrength) {
    this.shadowStrength = shadowStrength;
    return this;
  }

  public int interpolationDelay() {
    return interpolationDelay;
  }

  public DisplayPropertiesBuilder<T> interpolationDelay(int interpolationDelay) {
    this.interpolationDelay = interpolationDelay;
    return this;
  }

  public int interpolationDuration() {
    return interpolationDuration;
  }

  public DisplayPropertiesBuilder<T> interpolationDuration(int interpolationDuration) {
    this.interpolationDuration = interpolationDuration;
    return this;
  }

  public int brightness() {
    return brightness;
  }

  public DisplayPropertiesBuilder<T> brightness(int blockLight, int skyLight) {
    return brightness(FastMath.clamp(blockLight, 0, 15) << 4 | FastMath.clamp(skyLight, 0, 15) << 20);
  }

  public DisplayPropertiesBuilder<T> brightness(int brightness) {
    this.brightness = brightness;
    return this;
  }

  public int glowColor() {
    return glowColor;
  }

  public DisplayPropertiesBuilder<T> glowColor(RGBLike color) {
    return glowColor(255 << 24 | color.red() << 16 | color.green() << 8 | color.blue());
  }

  public DisplayPropertiesBuilder<T> glowColor(int argb) {
    this.glowColor = argb;
    return this;
  }

  public Billboard billboard() {
    return billboard;
  }

  public DisplayPropertiesBuilder<T> billboard(Billboard billboard) {
    this.billboard = Objects.requireNonNull(billboard);
    return this;
  }

  public Transformation transformation() {
    return transformation;
  }

  public DisplayPropertiesBuilder<T> transformation(Transformation transformation) {
    this.transformation = Objects.requireNonNull(transformation);
    return this;
  }

  public DisplayProperties<T> build() {
    return factory.apply(this);
  }
}
