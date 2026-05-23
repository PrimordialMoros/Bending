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

package me.moros.bending.fabric.platform.particle;

import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.platform.particle.Particle;
import me.moros.bending.api.platform.particle.ParticleContext;
import me.moros.bending.api.platform.particle.ParticleOptions;
import me.moros.bending.api.platform.particle.option.BlockPositionSource;
import me.moros.bending.api.platform.particle.option.EntityPositionSource;
import me.moros.bending.api.platform.particle.option.PositionSource;
import me.moros.bending.fabric.platform.PlatformAdapter;
import me.moros.math.Vector3d;
import net.kyori.adventure.util.RGBLike;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SculkChargeParticleOptions;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class ParticleMapper {
  private static final int TRAIL_COLOR = 16545810;

  @SuppressWarnings("unchecked")
  public static net.minecraft.core.particles.@Nullable ParticleOptions mapParticleOptions(ParticleContext context) {
    var p = context.particle();
    var fabricParticle = BuiltInRegistries.PARTICLE_TYPE.getValue(PlatformAdapter.identifier(p.key()));
    if (fabricParticle == null) {
      return null;
    }
    if (p == Particle.BLOCK ||
      p == Particle.BLOCK_MARKER ||
      p == Particle.FALLING_DUST ||
      p == Particle.DUST_PILLAR ||
      p == Particle.BLOCK_CRUMBLE) {
      BlockState state = context.option(ParticleOptions.BLOCK_STATE).orElseThrow();
      return new BlockParticleOption((ParticleType<BlockParticleOption>) fabricParticle, PlatformAdapter.toFabricData(state));
    } else if (p == Particle.DUST_COLOR_TRANSITION) {
      RGBLike color = context.option(ParticleOptions.COLOR).orElseThrow();
      RGBLike toColor = context.option(ParticleOptions.TO_COLOR).orElseThrow();
      double scale = context.option(ParticleOptions.SCALE).orElse(1D);
      return new DustColorTransitionOptions(fromColor(color), fromColor(toColor), (float) scale);
    } else if (p == Particle.DUST) {
      RGBLike color = context.option(ParticleOptions.COLOR).orElseThrow();
      double scale = context.option(ParticleOptions.SCALE).orElse(1D);
      return new DustParticleOptions(fromColor(color), (float) scale);
    } else if (p == Particle.ITEM) {
      ItemSnapshot item = context.option(ParticleOptions.ITEM_SNAPSHOT).orElseThrow();
      ItemStackTemplate itemStackTemplate = ItemStackTemplate.fromNonEmptyStack(PlatformAdapter.toFabricItem(item));
      return new ItemParticleOption((ParticleType<ItemParticleOption>) fabricParticle, itemStackTemplate);
    } else if (p == Particle.SCULK_CHARGE) {
      double roll = context.option(ParticleOptions.ROLL).orElse(0D);
      return new SculkChargeParticleOptions((float) roll);
    } else if (p == Particle.SHRIEK) {
      int delay = context.option(ParticleOptions.DELAY).orElse(20);
      return new ShriekParticleOption(delay);
    } else if (p == Particle.VIBRATION) {
      PositionSource source = context.option(ParticleOptions.DESTINATION).orElseThrow();
      int delay = context.option(ParticleOptions.TRAVEL_TIME).orElse(20);
      Vector3d pos = source.position().orElse(context.position().toVector3d());
      net.minecraft.world.level.gameevent.PositionSource vanillaSource = switch (source) {
        case BlockPositionSource _ -> {
          BlockPos blockPos = new BlockPos(pos.blockX(), pos.blockY(), pos.blockZ());
          yield new net.minecraft.world.level.gameevent.BlockPositionSource(blockPos);
        }
        case EntityPositionSource entitySource -> {
          Entity entity = entitySource.entity().map(PlatformAdapter::toFabricEntity).orElseThrow();
          yield new net.minecraft.world.level.gameevent.EntityPositionSource(entity, (float) entitySource.yOffset());
        }
      };
      return new VibrationParticleOption(vanillaSource, delay);
    } else if (p == Particle.ENTITY_EFFECT) {
      RGBLike color = context.option(ParticleOptions.COLOR).orElseThrow();
      double opacity = context.option(ParticleOptions.OPACITY).orElseThrow();
      return ColorParticleOption.create((ParticleType<ColorParticleOption>) fabricParticle, fromColor(opacity, color));
    } else if (p == Particle.TRAIL) {
      Vector3d dest = context.option(ParticleOptions.TARGET).orElseGet(() -> context.position().toVector3d());
      int color = context.option(ParticleOptions.COLOR).map(ParticleMapper::fromColor).orElse(TRAIL_COLOR);
      int duration = context.option(ParticleOptions.TRAVEL_TIME).orElse(20);
      return new TrailParticleOption(new Vec3(dest.x(), dest.y(), dest.z()), color, duration);
    }
    return (SimpleParticleType) fabricParticle;
  }

  private static int fromColor(RGBLike rgbLike) {
    return ARGB.color(rgbLike.red(), rgbLike.green(), rgbLike.blue());
  }

  private static int fromColor(double opacity, RGBLike rgbLike) {
    return ARGB.color(ARGB.as8BitChannel((float) opacity), rgbLike.red(), rgbLike.green(), rgbLike.blue());
  }
}
