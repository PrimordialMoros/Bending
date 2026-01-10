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

package me.moros.bending.api.platform.entity.display;

import java.util.Objects;

import me.moros.bending.api.platform.entity.display.TextDisplay.Alignment;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.RGBLike;

public final class TextDisplayBuilder extends AbstractDisplayBuilder<Component, TextDisplayBuilder> {
  private int lineWidth = 200;
  private int backgroundColor = 0x40000000;
  private byte opacity = -1;
  private boolean hasShadow = false;
  private boolean isSeeThrough = false;
  private boolean hasDefaultBackground = false;
  private Alignment alignment = Alignment.CENTER;

  TextDisplayBuilder() {
    super(TextDisplayImpl::new);
  }

  TextDisplayBuilder(TextDisplay display) {
    super(TextDisplayImpl::new, display);
    lineWidth(display.lineWidth());
    backgroundColor(display.backgroundColor());
    opacity(display.opacity());
    hasShadow(display.textFlags().hasShadow());
    isSeeThrough(display.textFlags().isSeeThrough());
    hasDefaultBackground(display.textFlags().hasDefaultBackground());
    alignment(display.textFlags().alignment());
  }

  public int lineWidth() {
    return lineWidth;
  }

  public TextDisplayBuilder lineWidth(int lineWidth) {
    this.lineWidth = lineWidth;
    return this;
  }

  public int backgroundColor() {
    return backgroundColor;
  }

  public TextDisplayBuilder backgroundColor(RGBLike color) {
    return backgroundColor(rgbToInt(color));
  }

  public TextDisplayBuilder backgroundColor(int backgroundColor) {
    this.backgroundColor = backgroundColor;
    return this;
  }

  public byte opacity() {
    return opacity;
  }

  public TextDisplayBuilder opacity(byte opacity) {
    this.opacity = opacity;
    return this;
  }

  public boolean hasShadow() {
    return hasShadow;
  }

  public TextDisplayBuilder hasShadow(boolean hasShadow) {
    this.hasShadow = hasShadow;
    return this;
  }

  public boolean isSeeThrough() {
    return isSeeThrough;
  }

  public TextDisplayBuilder isSeeThrough(boolean isSeeThrough) {
    this.isSeeThrough = isSeeThrough;
    return this;
  }

  public boolean hasDefaultBackground() {
    return hasDefaultBackground;
  }

  public TextDisplayBuilder hasDefaultBackground(boolean hasDefaultBackground) {
    this.hasDefaultBackground = hasDefaultBackground;
    return this;
  }

  public Alignment alignment() {
    return alignment;
  }

  public TextDisplayBuilder alignment(Alignment alignment) {
    this.alignment = Objects.requireNonNull(alignment);
    return this;
  }
}
