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

package me.moros.bending.paper.platform.particle;

import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.platform.particle.Particle;
import me.moros.bending.api.platform.particle.ParticleContext;
import me.moros.bending.api.platform.particle.ParticleOptions;
import me.moros.bending.api.platform.particle.option.BlockPositionSource;
import me.moros.bending.api.platform.particle.option.EntityPositionSource;
import me.moros.bending.api.platform.particle.option.PositionSource;
import me.moros.bending.paper.platform.PlatformAdapter;
import me.moros.math.FastMath;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import net.kyori.adventure.util.RGBLike;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Particle.DustTransition;
import org.bukkit.Particle.Trail;
import org.bukkit.Vibration;
import org.bukkit.Vibration.Destination;
import org.bukkit.entity.Entity;
import org.jspecify.annotations.Nullable;

public final class ParticleMapper {
  private static final Color TRAIL_COLOR = Color.fromARGB(16545810);

  public static @Nullable Object mapParticleData(ParticleContext context) {
    var p = context.particle();
    if (p == Particle.BLOCK ||
      p == Particle.BLOCK_MARKER ||
      p == Particle.FALLING_DUST ||
      p == Particle.DUST_PILLAR ||
      p == Particle.BLOCK_CRUMBLE) {
      return context.option(ParticleOptions.BLOCK_STATE).map(PlatformAdapter::toBukkitData).orElseThrow();
    } else if (p == Particle.DUST) {
      RGBLike color = context.option(ParticleOptions.COLOR).orElseThrow();
      double scale = context.option(ParticleOptions.SCALE).orElse(1D);
      return new DustOptions(fromColor(color), (float) scale);
    } else if (p == Particle.DUST_COLOR_TRANSITION) {
      RGBLike color = context.option(ParticleOptions.COLOR).orElseThrow();
      RGBLike toColor = context.option(ParticleOptions.TO_COLOR).orElseThrow();
      double scale = context.option(ParticleOptions.SCALE).orElse(1D);
      return new DustTransition(fromColor(color), fromColor(toColor), (float) scale);
    } else if (p == Particle.ITEM) {
      ItemSnapshot item = context.option(ParticleOptions.ITEM_SNAPSHOT).orElseThrow();
      return PlatformAdapter.toBukkitItem(item);
    } else if (p == Particle.SCULK_CHARGE) {
      double roll = context.option(ParticleOptions.ROLL).orElse(0D);
      return (float) roll;
    } else if (p == Particle.SHRIEK) {
      int delay = context.option(ParticleOptions.DELAY).orElse(20);
      return delay;
    } else if (p == Particle.VIBRATION) {
      PositionSource source = context.option(ParticleOptions.DESTINATION).orElseThrow();
      int delay = context.option(ParticleOptions.TRAVEL_TIME).orElse(20);
      Vector3d pos = source.position().orElse(context.position().toVector3d());
      Destination destination = switch (source) {
        case BlockPositionSource _ -> {
          Location loc = toLocation(pos);
          yield new Vibration.Destination.BlockDestination(loc);
        }
        case EntityPositionSource entitySource -> {
          Entity entity = entitySource.entity().map(PlatformAdapter::toBukkitEntity).orElseThrow();
          yield new Vibration.Destination.EntityDestination(entity);
        }
      };
      return new Vibration(destination, delay);
    } else if (p == Particle.ENTITY_EFFECT) {
      RGBLike color = context.option(ParticleOptions.COLOR).orElseThrow();
      double opacity = context.option(ParticleOptions.OPACITY).orElseThrow();
      return fromColor(opacity, color);
    } else if (p == Particle.TRAIL) {
      Vector3d dest = context.option(ParticleOptions.TARGET).orElseGet(() -> context.position().toVector3d());
      Color color = context.option(ParticleOptions.COLOR).map(ParticleMapper::fromColor).orElse(TRAIL_COLOR);
      int duration = context.option(ParticleOptions.TRAVEL_TIME).orElse(20);
      return new Trail(toLocation(dest), color, duration);
    }
    return null;
  }

  private static Location toLocation(Position position) {
    return new Location(null, position.x(), position.y(), position.z());
  }

  private static Color fromColor(RGBLike rgbLike) {
    return Color.fromRGB(rgbLike.red(), rgbLike.green(), rgbLike.blue());
  }

  private static Color fromColor(double opacity, RGBLike rgbLike) {
    int alpha = FastMath.floor(opacity * 255);
    return Color.fromARGB(alpha, rgbLike.red(), rgbLike.green(), rgbLike.blue());
  }
}
