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

package me.moros.bending.fabric.platform.particle;

import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.particle.Particle;
import me.moros.bending.api.platform.particle.ParticleContext;
import me.moros.bending.api.platform.particle.ParticleDustData;
import me.moros.bending.api.platform.particle.ParticleDustData.Transitive;
import me.moros.bending.fabric.platform.PlatformAdapter;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SculkChargeParticleOptions;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ARGB;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ParticleMapper {
  @SuppressWarnings("unchecked")
  public static @Nullable ParticleOptions mapParticleOptions(ParticleContext<?> context) {
    var p = context.particle();
    var fabricParticle = BuiltInRegistries.PARTICLE_TYPE.getValue(PlatformAdapter.rsl(p.key()));
    if (fabricParticle != null) {
      var data = context.data();
      if ((p == Particle.BLOCK || p == Particle.FALLING_DUST || p == Particle.BLOCK_MARKER) && data instanceof BlockState state) {
        return new BlockParticleOption((ParticleType<BlockParticleOption>) fabricParticle, PlatformAdapter.toFabricData(state));
      } else if (p == Particle.ITEM && data instanceof Item item) {
        return new ItemParticleOption((ParticleType<ItemParticleOption>) fabricParticle, PlatformAdapter.toFabricItem(item));
      } else if (p == Particle.DUST && data instanceof ParticleDustData dust) {
        return new DustParticleOptions(fromColor(dust.red(), dust.green(), dust.blue()), dust.size());
      } else if (p == Particle.DUST_COLOR_TRANSITION && data instanceof Transitive dust) {
        var from = fromColor(dust.red(), dust.green(), dust.blue());
        var to = fromColor(dust.toRed(), dust.toGreen(), dust.toBlue());
        return new DustColorTransitionOptions(from, to, dust.size());
      } else if (p == Particle.SCULK_CHARGE && data instanceof Float number) {
        return new SculkChargeParticleOptions(number);
      } else if (p == Particle.SHRIEK && data instanceof Integer number) {
        return new ShriekParticleOption(number);
      }/* else if (p == Particle.VIBRATION && data instanceof Vibration v) { // TODO Add?
      }*/
      return (SimpleParticleType) fabricParticle;
    }
    return null;
  }

  private static int fromColor(int red, int green, int blue) {
    return ARGB.color(red, green, blue);
  }
}
