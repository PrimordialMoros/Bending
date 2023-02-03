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

package me.moros.bending.api.platform.particle;

import me.moros.bending.api.platform.particle.ParticleDustDataImpl.TransitiveDataImpl;
import net.kyori.adventure.util.RGBLike;

public interface ParticleDustData extends RGBLike {
  float size();

  interface Transitive extends ParticleDustData {
    int toRed();

    int toGreen();

    int toBlue();
  }

  static ParticleDustData simple(RGBLike rgb, float size) {
    return new ParticleDustDataImpl(rgb.red(), rgb.green(), rgb.blue(), size);
  }

  static Transitive transitive(RGBLike from, RGBLike to, float size) {
    return new TransitiveDataImpl(from.red(), from.green(), from.blue(), to.red(), to.green(), to.blue(), size);
  }
}
