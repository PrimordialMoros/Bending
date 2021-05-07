/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.util;

import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.FireTick;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.methods.EntityMethods;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.util.NumberConversions;
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
  private final Collider ignoreInside;
  private final SoundEffect soundEffect;

  private BendingExplosion(ExplosionBuilder builder) {
    this.size = builder.size;
    this.damage = builder.damage;
    this.selfKnockbackFactor = builder.selfKnockbackFactor;
    this.fireTicks = builder.fireTicks;
    this.livingOnly = builder.livingOnly;
    this.particles = builder.particles;
    this.ignoreInside = builder.ignoreInside;
    this.soundEffect = builder.soundEffect;

    halfSize = size / 2;
    sizeFactor = Math.sqrt(size);
  }

  private void playParticles(Location center) {
    if (size <= 1.5) {
      ParticleUtil.create(Particle.EXPLOSION_NORMAL, center).count(NumberConversions.ceil(10 * size))
        .offset(0.75, 0.75, 0.75).spawn();
    } else if (size <= 3) {
      ParticleUtil.create(Particle.EXPLOSION_LARGE, center).count(NumberConversions.ceil(3 * size))
        .offset(1.5, 1.5, 1.5).spawn();
    } else if (size <= 5) {
      ParticleUtil.create(Particle.EXPLOSION_HUGE, center).spawn();
    } else {
      ParticleUtil.create(Particle.EXPLOSION_HUGE, center).count(NumberConversions.ceil(size / 5))
        .offset(2.5, 2.5, 2.5).spawn();
    }
  }

  public boolean explode(@NonNull User source, @NonNull AbilityDescription sourceDesc, @NonNull Vector3 center) {
    if (particles) {
      playParticles(center.toLocation(source.world()));
    }
    if (soundEffect != null) {
      soundEffect.play(center.toLocation(source.world()));
    }
    return CollisionUtil.handleEntityCollisions(source, new Sphere(center, size), entity -> {
      Vector3 entityCenter = EntityMethods.entityCenter(entity);
      double distance = center.distance(entityCenter);
      double distanceFactor = (distance <= halfSize) ? 1 : 1 - ((distance - halfSize)) / size;
      if (ignoreInside == null || !ignoreInside.contains(entityCenter)) {
        DamageUtil.damageEntity(entity, source, damage * distanceFactor, sourceDesc);
        FireTick.ignite(source, entity, fireTicks);
      } else {
        distanceFactor *= 0.75; // Reduce impact for those inside the collider
      }
      double knockback = sizeFactor * distanceFactor * BendingProperties.EXPLOSION_KNOCKBACK;
      if (entity.equals(source.entity())) {
        knockback *= selfKnockbackFactor;
      }
      Vector3 dir = entityCenter.subtract(center).normalize().multiply(knockback);
      entity.setVelocity(dir.clampVelocity());
      return true;
    }, livingOnly, true);
  }

  public static @NonNull ExplosionBuilder builder() {
    return new ExplosionBuilder();
  }

  public static class ExplosionBuilder {
    private double size = 2.0;
    private double damage = 4.0;
    private double selfKnockbackFactor = 0.5;
    private int fireTicks = 40;
    private boolean livingOnly = true;
    private boolean particles = true;
    private Collider ignoreInside = null;
    private SoundEffect soundEffect = null;

    private ExplosionBuilder() {
    }

    public @NonNull ExplosionBuilder size(double size) {
      this.size = Math.abs(size);
      return this;
    }

    public @NonNull ExplosionBuilder damage(double damage) {
      this.damage = Math.abs(damage);
      return this;
    }

    public @NonNull ExplosionBuilder selfKnockbackFactor(double selfKnockbackFactor) {
      this.selfKnockbackFactor = Math.abs(selfKnockbackFactor);
      return this;
    }

    public @NonNull ExplosionBuilder fireTicks(int fireTicks) {
      this.fireTicks = Math.abs(fireTicks);
      return this;
    }

    public @NonNull ExplosionBuilder livingOnly(boolean livingOnly) {
      this.livingOnly = livingOnly;
      return this;
    }

    public @NonNull ExplosionBuilder particles(boolean particles) {
      this.particles = particles;
      return this;
    }

    public @NonNull ExplosionBuilder ignoreInsideCollider(@Nullable Collider ignoreInside) {
      this.ignoreInside = ignoreInside;
      return this;
    }

    public @NonNull ExplosionBuilder soundEffect(@Nullable SoundEffect soundEffect) {
      this.soundEffect = soundEffect;
      return this;
    }

    public @NonNull BendingExplosion build() {
      if (size <= 0) {
        size = 2.0;
      }
      return new BendingExplosion(this);
    }

    public boolean buildAndExplode(@NonNull User source, @NonNull AbilityDescription sourceDesc, @NonNull Vector3 center) {
      return build().explode(source, sourceDesc, center);
    }
  }
}
