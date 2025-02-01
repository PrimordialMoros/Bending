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

package me.moros.bending.common.ability.earth.util;

import java.util.Collection;
import java.util.List;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.collision.CollisionUtil;
import me.moros.bending.api.collision.geometry.AABB;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.config.BendingProperties;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.temporal.TempEntity.TempFallingBlock;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.functional.OutOfRangeRemovalPolicy;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.material.EarthMaterials;
import me.moros.math.Vector3d;

public class Projectile extends AbilityInstance {
  private static final AABB BOX = AABB.BLOCK_BOUNDS.grow(Vector3d.of(0.25, 0.25, 0.25));

  private final RemovalPolicy removalPolicy;

  private final TempFallingBlock projectile;
  private final double damage;

  private Vector3d location;

  public Projectile(User user, AbilityDescription desc, TempFallingBlock projectile, double range, double damage) {
    super(desc);
    this.user = user;
    this.projectile = projectile;
    this.damage = damage;
    this.location = projectile.center();
    this.removalPolicy = Policies.builder()
      .add(OutOfRangeRemovalPolicy.of(range, location, () -> location))
      .build();
  }

  @Override
  public boolean activate(User user, Activation method) {
    return false;
  }

  @Override
  public void loadConfig() {
  }

  @Override
  public UpdateResult update() {
    if (removalPolicy.test(user, description()) || !projectile.valid()) {
      return UpdateResult.REMOVE;
    }
    location = projectile.center();
    if (CollisionUtil.handle(user, BOX.at(location), this::onProjectileHit)) {
      projectile.state().asParticle(location).count(8).offset(0.25, 0.15, 0.25).spawn(user.world());
      return UpdateResult.REMOVE;
    }
    return UpdateResult.CONTINUE;
  }

  @Override
  public void onDestroy() {
    projectile.revert();
  }

  @Override
  public Collection<Collider> colliders() {
    return List.of(BOX.at(location));
  }

  private boolean onProjectileHit(Entity entity) {
    BlockType mat = projectile.state().type();
    double modifiedDamage;
    if (EarthMaterials.METAL_BENDABLE.isTagged(mat)) {
      modifiedDamage = BendingProperties.instance().metalModifier(damage);
    } else if (EarthMaterials.LAVA_BENDABLE.isTagged(mat)) {
      modifiedDamage = BendingProperties.instance().magmaModifier(damage);
    } else {
      modifiedDamage = damage;
    }
    entity.applyVelocity(this, projectile.velocity().normalize().multiply(0.4));
    entity.damage(modifiedDamage, user, description());
    return true;
  }
}
