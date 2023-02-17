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

package me.moros.bending.api.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import me.moros.bending.api.ability.Ability;
import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.collision.CollisionUtil;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.collision.geometry.Sphere;
import me.moros.bending.api.config.BendingProperties;
import me.moros.bending.api.event.BendingExplosionEvent;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.particle.Particle;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.platform.world.World;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents an explosion with pre-configured parameters.
 * BendingExplosions are visually similar to vanilla explosions but work slightly differently.
 * For an explanation of vanilla explosion mechanics check the <a href="https://minecraft.fandom.com/wiki/Explosion">wiki</a>.
 * BendingExplosions utilize a simplified block breaking model which destroys all blocks
 * (except {@link MaterialUtil#isUnbreakable(Block)}) within the specified radius no matter their blast resistance.
 * Moreover, there are 2 main damage zones for the explosion. Every entity within half the radius distance from the
 * explosion center will be damaged for the flat amount. Outside that area, damage scales down to a minimum of
 * half the original amount at max distance.
 */
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

  private void playParticles(World world, Vector3d center) {
    if (size <= 1.5) {
      Particle.POOF.builder(center).count(FastMath.ceil(10 * size)).offset(0.75).spawn(world);
    } else if (size <= 3) {
      Particle.EXPLOSION.builder(center).count(FastMath.ceil(3 * size)).offset(0.5).spawn(world);
    } else if (size <= 5) {
      Particle.EXPLOSION_EMITTER.builder(center).spawn(world);
    } else {
      Particle.EXPLOSION_EMITTER.builder(center).count(FastMath.ceil(size / 5)).spawn(world);
    }
  }

  /**
   * Apply this explosion from an ability.
   * <p>Note: Calls a {@link BendingExplosionEvent}
   * @param source the ability causing the explosion
   * @param center the center of the explosion
   * @return true if explosion was successful, false otherwise
   */
  public boolean explode(Ability source, Vector3d center) {
    User user = source.user();
    World world = user.world();
    AbilityDescription desc = source.description();
    Predicate<Block> predicate = b -> !MaterialUtil.isAir(b) && !MaterialUtil.isUnbreakable(b) && !b.type().isLiquid();
    Collection<Block> blocks = breakBlocks ? world.nearbyBlocks(center, size, predicate) : new ArrayList<>();

    if (user.game().eventBus().postExplosionEvent(user, desc, center, blocks).cancelled()) {
      return false;
    }

    if (particles) {
      playParticles(world, center);
    }
    if (sound != null) {
      sound.play(world, center);
    }

    if (breakBlocks && !world.blockAt(center).type().isLiquid()) {
      Collection<Block> filteredBlocks = blocks.stream().filter(predicate).filter(user::canBuild).toList();
      ThreadLocalRandom rand = ThreadLocalRandom.current();
      for (Block block : filteredBlocks) {
        TempBlock.air().fixWater(false).duration(BendingProperties.instance().explosionRevertTime(1000)).build(block);
      }
      if (placeFire) {
        for (Block block : filteredBlocks) {
          if (MaterialUtil.isIgnitable(block) && rand.nextInt(3) == 0) {
            TempBlock.fire().duration(BendingProperties.instance().fireRevertTime(1000)).build(block);
          }
        }
      }
    }

    return CollisionUtil.handle(user, new Sphere(center, size), entity -> {
      Vector3d entityCenter = entity.center();
      double distance = center.distance(entityCenter);
      double distanceFactor = (distance <= halfSize) ? 1 : (1 - (distance - halfSize) / size);
      if (ignoreInside == null || !ignoreInside.contains(entityCenter)) {
        entity.damage(damage * distanceFactor, user, desc);
        BendingEffect.FIRE_TICK.apply(user, entity, fireTicks);
      } else {
        distanceFactor *= 0.75; // Reduce impact for those inside the collider
      }
      double knockback = BendingProperties.instance().explosionKnockback(sizeFactor * distanceFactor);
      if (entity.uuid().equals(user.uuid())) {
        knockback *= selfKnockbackFactor;
      }
      Vector3d dir = entityCenter.subtract(center).normalize().multiply(knockback);
      entity.applyVelocity(source, dir);
      return true;
    }, livingOnly, true);
  }

  /**
   * Create a new builder to configure an explosion.
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * A builder to configure explosions.
   */
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

    /**
     * Set the size of the explosion.
     * @param size the radius of the explosion
     * @return the modified builder
     */
    public Builder size(double size) {
      this.size = Math.abs(size);
      return this;
    }

    /**
     * Set the explosion damage to entities.
     * @param damage the damage the explosion will apply
     * @return the modified builder
     */
    public Builder damage(double damage) {
      this.damage = Math.abs(damage);
      return this;
    }

    /**
     * Set the knockback factor for the user who caused the explosion
     * @param selfKnockbackFactor the knockback multiplier
     * @return the modified builder
     */
    public Builder selfKnockbackFactor(double selfKnockbackFactor) {
      this.selfKnockbackFactor = Math.abs(selfKnockbackFactor);
      return this;
    }

    /**
     * Set the fire ticks of the explosion.
     * @param fireTicks the duration of fire ticks the explosion will apply
     * @return the modified builder
     */
    public Builder fireTicks(int fireTicks) {
      this.fireTicks = Math.abs(fireTicks);
      return this;
    }

    /**
     * Set whether the explosion will only affect living entities.
     * @param livingOnly the boolean value to set
     * @return the modified builder
     */
    public Builder livingOnly(boolean livingOnly) {
      this.livingOnly = livingOnly;
      return this;
    }

    /**
     * Set whether the explosion will emit particles.
     * @param particles the boolean value to set
     * @return the modified builder
     */
    public Builder particles(boolean particles) {
      this.particles = particles;
      return this;
    }

    /**
     * Set whether the explosion can break blocks.
     * @param breakBlocks the boolean value to set
     * @return the modified builder
     */
    public Builder breakBlocks(boolean breakBlocks) {
      this.breakBlocks = breakBlocks;
      return this;
    }

    /**
     * Set whether the explosion can place fires. Placing fire requires the explosion to be able to break blocks.
     * @param placeFire the boolean value to set
     * @return the modified builder
     */
    public Builder placeFire(boolean placeFire) {
      this.placeFire = placeFire;
      return this;
    }

    /**
     * Sets the explosion to ignore all entities inside the specified area represented by the collider.
     * @param ignoreInside the collider to use or null to disable this mechanic
     * @return the modified builder
     */
    public Builder ignoreInsideCollider(@Nullable Collider ignoreInside) {
      this.ignoreInside = ignoreInside;
      return this;
    }

    /**
     * Sets the explosion sound effect.
     * @param sound the sound effect to play when the explosion occurs
     * @return the modified builder
     */
    public Builder sound(@Nullable SoundEffect sound) {
      this.sound = sound;
      return this;
    }

    /**
     * Sets the volume and pitch for the default explosion sound.
     * @param volume the new volume
     * @param pitch the new pitch
     * @return the modified builder
     */
    public Builder sound(float volume, float pitch) {
      this.sound = SoundEffect.EXPLOSION.with(volume, pitch);
      return this;
    }

    /**
     * Build an explosion with the current parameters.
     * @return the created explosion
     * @see #buildAndExplode(Ability, Vector3d)
     */
    public BendingExplosion build() {
      if (size <= 0) {
        size = 2.0;
      }
      return new BendingExplosion(this);
    }

    /**
     * Build an explosion with the current parameters and apply it.
     * @param source the ability causing the explosion
     * @param center the center of the explosion
     * @return true if explosion was successful, false otherwise
     * @see #explode(Ability, Vector3d)
     */
    public boolean buildAndExplode(Ability source, Vector3d center) {
      return build().explode(source, center);
    }
  }
}
