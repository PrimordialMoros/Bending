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

package me.moros.bending.sponge.platform.particle;

import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.particle.Particle;
import me.moros.bending.api.platform.particle.ParticleContext;
import me.moros.bending.api.platform.particle.ParticleDustData;
import me.moros.bending.sponge.platform.PlatformAdapter;
import org.jspecify.annotations.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleOptions;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.util.Color;
import org.spongepowered.math.vector.Vector3d;

public final class ParticleMapper {
  public static @Nullable ParticleEffect mapParticleEffect(ParticleContext<?> context) {
    var p = context.particle();
    var spongeParticle = Sponge.game().registry(RegistryTypes.PARTICLE_TYPE).value(PlatformAdapter.rsk(p.key()));
    if (spongeParticle != null) {
      var data = context.data();
      var offset = Vector3d.from(context.offset().x(), context.offset().y(), context.offset().z());
      var builder = ParticleEffect.builder().type(spongeParticle);
      if (context.count() <= 0) {
        builder.quantity(1).velocity(offset);
      } else {
        builder.quantity(context.count()).offset(offset);
      }
      if ((p == Particle.BLOCK || p == Particle.FALLING_DUST || p == Particle.BLOCK_MARKER) && data instanceof BlockState state) {
        builder.option(ParticleOptions.BLOCK_STATE, PlatformAdapter.toSpongeData(state));
      } else if (p == Particle.ITEM && data instanceof Item item) {
        var snapshot = PlatformAdapter.toSpongeItem(item).asImmutable();
        builder.option(ParticleOptions.ITEM_STACK_SNAPSHOT, snapshot);
      } else if (p == Particle.DUST && data instanceof ParticleDustData dust) {
        builder.option(ParticleOptions.COLOR, Color.ofRgb(dust.red(), dust.green(), dust.blue()));
        builder.option(ParticleOptions.SCALE, (double) dust.size());
      } /*else if (p == Particle.DUST_COLOR_TRANSITION && data instanceof Transitive dust) { // TODO no api, use nms
        var from = Color.ofRgb(dust.red(), dust.green(), dust.blue());
        var to = Color.ofRgb(dust.toRed(), dust.toGreen(), dust.toBlue());
        return new DustTransition(from, to, dust.size());
      } else if (p == Particle.VIBRATION && data instanceof Vibration v) { // TODO Add?
      }*/

      return builder.build();
    }
    return null;
  }
}
