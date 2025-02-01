/*
 * Copyright 2020-2025 Moros
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

package me.moros.bending.paper.platform.particle;

import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.particle.Particle;
import me.moros.bending.api.platform.particle.ParticleContext;
import me.moros.bending.api.platform.particle.ParticleDustData;
import me.moros.bending.api.platform.particle.ParticleDustData.Transitive;
import me.moros.bending.paper.platform.PlatformAdapter;
import org.bukkit.Color;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Particle.DustTransition;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ParticleMapper {
  public static <T> @Nullable Object mapParticleData(ParticleContext<T> context) {
    var p = context.particle();
    var data = context.data();
    if ((p == Particle.BLOCK || p == Particle.FALLING_DUST || p == Particle.BLOCK_MARKER) && data instanceof BlockState state) {
      return PlatformAdapter.toBukkitData(state);
    } else if (p == Particle.ITEM && data instanceof Item item) {
      return PlatformAdapter.toBukkitItem(item);
    } else if (p == Particle.DUST && data instanceof ParticleDustData dust) {
      return new DustOptions(Color.fromRGB(dust.red(), dust.green(), dust.blue()), dust.size());
    } else if (p == Particle.DUST_COLOR_TRANSITION && data instanceof Transitive dust) {
      var from = Color.fromRGB(dust.red(), dust.green(), dust.blue());
      var to = Color.fromRGB(dust.toRed(), dust.toGreen(), dust.toBlue());
      return new DustTransition(from, to, dust.size());
    } /*else if (p == Particle.VIBRATION && data instanceof Vibration v) { // TODO Add?
    }*/
    return null;
  }
}
