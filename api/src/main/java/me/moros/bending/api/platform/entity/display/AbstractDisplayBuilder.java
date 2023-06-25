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

import me.moros.math.Vector3d;

@SuppressWarnings("unchecked")
sealed class AbstractDisplayBuilder<V, T extends AbstractDisplayBuilder<V, T>> implements DisplayBuilder<V, T> permits BlockDisplayBuilder, ItemDisplayBuilder, TextDisplayBuilder {
  private final Function<? super T, Display<? super V>> factory;

  private V data;
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

  AbstractDisplayBuilder(Function<T, Display<? super V>> factory) {
    this.factory = factory;
  }

  AbstractDisplayBuilder(Function<T, Display<? super V>> factory, Display<V> display) {
    this(factory);
    data(display.data());
    width(display.width());
    height(display.height());
    viewRange(display.viewRange());
    shadowRadius(display.shadowRadius());
    shadowStrength(display.shadowStrength());
    interpolationDelay(display.interpolationDelay());
    interpolationDuration(display.interpolationDuration());
    brightness(display.brightness());
    glowColor(display.glowColor());
    billboard(display.billboard());
    transformation(display.transformation());
  }

  @Override
  public V data() {
    return data;
  }

  @Override
  public T data(V data) {
    this.data = Objects.requireNonNull(data);
    return (T) this;
  }

  @Override
  public float width() {
    return width;
  }

  @Override
  public T width(float width) {
    this.width = width;
    return (T) this;
  }

  @Override
  public float height() {
    return height;
  }

  @Override
  public T height(float height) {
    this.height = height;
    return (T) this;
  }

  @Override
  public float viewRange() {
    return viewRange;
  }

  @Override
  public T viewRange(float viewRange) {
    this.viewRange = viewRange;
    return (T) this;
  }

  @Override
  public float shadowRadius() {
    return shadowRadius;
  }

  @Override
  public T shadowRadius(float shadowRadius) {
    this.shadowRadius = shadowRadius;
    return (T) this;
  }

  @Override
  public float shadowStrength() {
    return shadowStrength;
  }

  @Override
  public T shadowStrength(float shadowStrength) {
    this.shadowStrength = shadowStrength;
    return (T) this;
  }

  @Override
  public int interpolationDelay() {
    return interpolationDelay;
  }

  @Override
  public T interpolationDelay(int interpolationDelay) {
    this.interpolationDelay = interpolationDelay;
    return (T) this;
  }

  @Override
  public int interpolationDuration() {
    return interpolationDuration;
  }

  @Override
  public T interpolationDuration(int interpolationDuration) {
    this.interpolationDuration = interpolationDuration;
    return (T) this;
  }

  @Override
  public int brightness() {
    return brightness;
  }

  @Override
  public T brightness(int brightness) {
    this.brightness = brightness;
    return (T) this;
  }

  @Override
  public int glowColor() {
    return glowColor;
  }

  @Override
  public T glowColor(int argb) {
    this.glowColor = argb;
    return (T) this;
  }

  @Override
  public Billboard billboard() {
    return billboard;
  }

  @Override
  public T billboard(Billboard billboard) {
    this.billboard = Objects.requireNonNull(billboard);
    return (T) this;
  }

  @Override
  public Transformation transformation() {
    return transformation;
  }

  @Override
  public T transformation(Transformation transformation) {
    this.transformation = Objects.requireNonNull(transformation);
    return (T) this;
  }

  @Override
  public Display<? super V> build() {
    return factory.apply((T) this);
  }
}
