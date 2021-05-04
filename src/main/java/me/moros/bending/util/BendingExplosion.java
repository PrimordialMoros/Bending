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
import org.apache.commons.math3.util.FastMath;
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
  private final Collider ignoreInside;

  private BendingExplosion(ExplosionBuilder builder) {
    this.size = builder.size;
    this.damage = builder.damage;
    this.selfKnockbackFactor = builder.selfKnockbackFactor;
    this.fireTicks = builder.fireTicks;
    this.livingOnly = builder.livingOnly;
    this.ignoreInside = builder.ignoreInside;

    halfSize = size / 2;
    sizeFactor = FastMath.sqrt(size);
  }

  public boolean explode(@NonNull User source, @NonNull AbilityDescription sourceDesc, @NonNull Vector3 center) {
    return CollisionUtil.handleEntityCollisions(source, new Sphere(center, size), entity -> {
      Vector3 entityCenter = EntityMethods.entityCenter(entity);
      double distance = center.distance(entityCenter);
      double distanceFactor = (distance <= halfSize) ? 1 : 1 - ((distance - halfSize)) / size;
      if (ignoreInside == null || !ignoreInside.contains(entityCenter)) {
        DamageUtil.damageEntity(entity, source, damage * distanceFactor, sourceDesc);
        FireTick.LARGER.apply(source, entity, fireTicks);
      }
      double knockback = sizeFactor * distanceFactor * BendingProperties.EXPLOSION_KNOCKBACK;
      if (entity.equals(source.entity())) {
        knockback *= selfKnockbackFactor;
      }
      Vector3 dir = entityCenter.subtract(center).normalize().scalarMultiply(knockback);
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
    private Collider ignoreInside = null;

    private ExplosionBuilder() {
    }

    public @NonNull ExplosionBuilder size(double size) {
      this.size = FastMath.abs(size);
      return this;
    }

    public @NonNull ExplosionBuilder damage(double damage) {
      this.damage = FastMath.abs(damage);
      return this;
    }

    public @NonNull ExplosionBuilder selfKnockbackFactor(double selfKnockbackFactor) {
      this.selfKnockbackFactor = FastMath.abs(selfKnockbackFactor);
      return this;
    }

    public @NonNull ExplosionBuilder fireTicks(int fireTicks) {
      this.fireTicks = FastMath.abs(fireTicks);
      return this;
    }

    public @NonNull ExplosionBuilder livingOnly(boolean livingOnly) {
      this.livingOnly = livingOnly;
      return this;
    }

    public @NonNull ExplosionBuilder ignoreInsideCollider(@Nullable Collider ignoreInside) {
      this.ignoreInside = ignoreInside;
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
