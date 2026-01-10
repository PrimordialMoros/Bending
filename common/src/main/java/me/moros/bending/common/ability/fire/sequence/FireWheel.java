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

package me.moros.bending.common.ability.fire.sequence;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.common.basic.AbstractWheel;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.collision.geometry.Ray;
import me.moros.bending.api.config.BendingProperties;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.particle.ParticleBuilder;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.BendingEffect;
import me.moros.bending.api.util.functional.OutOfRangeRemovalPolicy;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.math.Vector3d;
import me.moros.math.VectorUtil;
import org.spongepowered.configurate.objectmapping.meta.Comment;

public class FireWheel extends AbilityInstance {
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Wheel wheel;

  public FireWheel(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    loadConfig();

    Vector3d direction = user.direction().withY(0).normalize();
    Vector3d location = user.location().add(direction);
    location = location.add(0, userConfig.radius, 0);
    if (user.world().blockAt(location).type().isLiquid()) {
      return false;
    }

    wheel = new Wheel(Ray.of(location, direction));
    if (!wheel.resolveMovement()) {
      return false;
    }

    removalPolicy = Policies.builder()
      .add(OutOfRangeRemovalPolicy.of(userConfig.range, location, () -> wheel.location())).build();

    user.addCooldown(description(), userConfig.cooldown);
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = user.game().configProcessor().calculate(this, Config.class);
  }

  @Override
  public UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    return wheel.update();
  }

  @Override
  public Collection<Collider> colliders() {
    return List.of(wheel.collider());
  }

  private class Wheel extends AbstractWheel {
    public Wheel(Ray ray) {
      super(user, ray, userConfig.radius, userConfig.speed);
    }

    @Override
    public void render(Vector3d location) {
      Vector3d rotateAxis = Vector3d.PLUS_J.cross(this.ray.direction());
      VectorUtil.circle(this.ray.direction().multiply(this.radius), rotateAxis, 36).forEach(v ->
        ParticleBuilder.fire(user, location.add(v)).extra(0.01).spawn(user.world())
      );
    }

    @Override
    public void postRender(Vector3d location) {
      if (ThreadLocalRandom.current().nextInt(6) == 0) {
        SoundEffect.FIRE.play(user.world(), location);
      }
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      BendingEffect.FIRE_TICK.apply(user, entity, userConfig.fireTicks);
      entity.damage(userConfig.damage, user, description());
      return true;
    }

    @Override
    public boolean onBlockHit(Block block) {
      if (userConfig.fireTrail && MaterialUtil.isIgnitable(block) && user.canBuild(block)) {
        TempBlock.fire().duration(BendingProperties.instance().fireRevertTime()).ability(FireWheel.this).build(block);
      }
      return true;
    }
  }

  private static final class Config implements Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 8000;
    @Modifiable(Attribute.RADIUS)
    private double radius = 1;
    @Modifiable(Attribute.DAMAGE)
    private double damage = 3;
    @Modifiable(Attribute.FIRE_TICKS)
    private int fireTicks = 25;
    @Modifiable(Attribute.RANGE)
    private double range = 25;
    @Comment("How many blocks the wheel advances every tick")
    @Modifiable(Attribute.SPEED)
    private double speed = 0.75;
    private boolean fireTrail = true;

    @Override
    public List<String> path() {
      return List.of("abilities", "fire", "sequences", "firewheel");
    }
  }
}
