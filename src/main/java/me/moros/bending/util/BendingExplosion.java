/*
 * Copyright 2020-2022 Moros
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

package me.moros.bending.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import me.moros.bending.Bending;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.user.User;
import me.moros.bending.util.SoundUtil.SoundEffect;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class BendingExplosion {
  private final double size;
  private final double damage;
  private final double selfKnockbackFactor;
  private final double halfSize;
  private final double sizeFactor;
  private final int fireTicks;
  private final boolean livingOnly;
  private final boolean particles;
  private final boolean breakBlocks;
  private final boolean placeFire;
  private final Collider ignoreInside;
  private final SoundEffect sound;

  private BendingExplosion(Builder builder) {
    this.size = builder.size;
    this.damage = builder.damage;
    this.selfKnockbackFactor = builder.selfKnockbackFactor;
    this.fireTicks = builder.fireTicks;
    this.livingOnly = builder.livingOnly;
    this.particles = builder.particles;
    this.breakBlocks = builder.breakBlocks;
    this.placeFire = builder.placeFire;
    this.ignoreInside = builder.ignoreInside;
    this.sound = builder.sound;

    halfSize = size / 2;
    sizeFactor = Math.sqrt(size);
  }

  private void playParticles(Location center) {
    if (size <= 1.5) {
      ParticleUtil.of(Particle.EXPLOSION_NORMAL, center).count(FastMath.ceil(10 * size))
        .offset(0.75, 0.75, 0.75).spawn();
    } else if (size <= 3) {
      ParticleUtil.of(Particle.EXPLOSION_LARGE, center).count(FastMath.ceil(3 * size))
        .offset(1.5, 1.5, 1.5).spawn();
    } else if (size <= 5) {
      ParticleUtil.of(Particle.EXPLOSION_HUGE, center).spawn();
    } else {
      ParticleUtil.of(Particle.EXPLOSION_HUGE, center).count(FastMath.ceil(size / 5))
        .offset(2.5, 2.5, 2.5).spawn();
    }
  }

  public boolean explode(@NonNull Ability source, @NonNull Vector3d center) {
    User user = source.user();
    AbilityDescription desc = source.description();
    Location bukkitLoc = center.toLocation(user.world());

    Predicate<Block> predicate = b -> !MaterialUtil.isAir(b) && !MaterialUtil.isUnbreakable(b) && !b.isLiquid();
    Collection<Block> blocks = breakBlocks ? WorldUtil.nearbyBlocks(bukkitLoc, size, predicate) : new ArrayList<>();

    if (Bending.eventBus().postExplosionEvent(user, bukkitLoc, blocks, size).isCancelled()) {
      return false;
    }

    if (particles) {
      playParticles(bukkitLoc);
    }
    if (sound != null) {
      sound.play(bukkitLoc);
    }

    if (breakBlocks && !blocks.isEmpty() && !bukkitLoc.getBlock().isLiquid()) {
      ThreadLocalRandom rand = ThreadLocalRandom.current();
      blocks.removeIf(b -> !user.canBuild(b));
      for (Block block : blocks) {
        long delay = BendingProperties.EXPLOSION_REVERT_TIME + rand.nextInt(1000);
        TempBlock.air().duration(delay).build(block);
      }
      if (placeFire) {
        for (Block block : blocks) {
          if (MaterialUtil.isIgnitable(block) && rand.nextInt(3) == 0) {
            long delay = BendingProperties.FIRE_REVERT_TIME + rand.nextInt(1000);
            TempBlock.fire().duration(delay).build(block);
          }
        }
      }
    }

    return CollisionUtil.handle(user, new Sphere(center, size), entity -> {
      Vector3d entityCenter = EntityUtil.entityCenter(entity);
      double distance = center.distance(entityCenter);
      double distanceFactor = (distance <= halfSize) ? 1 : 1 - ((distance - halfSize)) / size;
      if (ignoreInside == null || !ignoreInside.contains(entityCenter)) {
        DamageUtil.damageEntity(entity, user, damage * distanceFactor, desc);
        BendingEffect.FIRE_TICK.apply(user, entity, fireTicks);
      } else {
        distanceFactor *= 0.75; // Reduce impact for those inside the collider
      }
      double knockback = sizeFactor * distanceFactor * BendingProperties.EXPLOSION_KNOCKBACK;
      if (entity.equals(user.entity())) {
        knockback *= selfKnockbackFactor;
      }
      Vector3d dir = entityCenter.subtract(center).normalize().multiply(knockback);
      EntityUtil.applyVelocity(source, entity, dir);
      return true;
    }, livingOnly, true);
  }

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private double size = 2.0;
    private double damage = 4.0;
    private double selfKnockbackFactor = 0.5;
    private int fireTicks = 40;
    private boolean livingOnly = true;
    private boolean particles = true;
    private boolean breakBlocks = false;
    private boolean placeFire = false;
    private Collider ignoreInside = null;
    private SoundEffect sound = null;

    private Builder() {
    }

    public @NonNull Builder size(double size) {
      this.size = Math.abs(size);
      return this;
    }

    public @NonNull Builder damage(double damage) {
      this.damage = Math.abs(damage);
      return this;
    }

    public @NonNull Builder selfKnockbackFactor(double selfKnockbackFactor) {
      this.selfKnockbackFactor = Math.abs(selfKnockbackFactor);
      return this;
    }

    public @NonNull Builder fireTicks(int fireTicks) {
      this.fireTicks = Math.abs(fireTicks);
      return this;
    }

    public @NonNull Builder livingOnly(boolean livingOnly) {
      this.livingOnly = livingOnly;
      return this;
    }

    public @NonNull Builder particles(boolean particles) {
      this.particles = particles;
      return this;
    }

    public @NonNull Builder breakBlocks(boolean breakBlocks) {
      this.breakBlocks = breakBlocks;
      return this;
    }

    public @NonNull Builder placeFire(boolean placeFire) {
      this.placeFire = placeFire;
      return this;
    }

    public @NonNull Builder ignoreInsideCollider(@Nullable Collider ignoreInside) {
      this.ignoreInside = ignoreInside;
      return this;
    }

    public @NonNull Builder sound(@Nullable SoundEffect sound) {
      this.sound = sound;
      return this;
    }

    public @NonNull Builder sound(float volume, float pitch) {
      this.sound = SoundUtil.explosion(volume, pitch);
      return this;
    }

    public @NonNull BendingExplosion build() {
      if (size <= 0) {
        size = 2.0;
      }
      return new BendingExplosion(this);
    }

    public boolean buildAndExplode(@NonNull Ability source, @NonNull Vector3d center) {
      return build().explode(source, center);
    }
  }
}
