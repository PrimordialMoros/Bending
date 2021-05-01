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

package me.moros.bending.ability.fire.sequences;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.basic.AbstractWheel;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.FireTick;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.OutOfRangeRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.VectorMethods;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

public class FireWheel extends AbilityInstance implements Ability {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Wheel wheel;

  public FireWheel(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
    this.user = user;
    recalculateConfig();

    Vector3 direction = user.direction().setY(0).normalize();
    Vector3 location = user.location().add(direction);
    location = location.add(new Vector3(0, userConfig.radius, 0));
    if (location.toBlock(user.world()).isLiquid()) {
      return false;
    }

    wheel = new Wheel(new Ray(location, direction));
    if (!wheel.resolveMovement(userConfig.radius)) {
      return false;
    }

    removalPolicy = Policies.builder()
      .add(OutOfRangeRemovalPolicy.of(userConfig.range, location, () -> wheel.location())).build();

    user.addCooldown(description(), userConfig.cooldown);
    return true;
  }

  @Override
  public void recalculateConfig() {
    userConfig = Bending.game().attributeSystem().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    return wheel.update();
  }

  @Override
  public @NonNull User user() {
    return user;
  }

  @Override
  public @NonNull Collection<@NonNull Collider> colliders() {
    return Collections.singletonList(wheel.collider());
  }

  private class Wheel extends AbstractWheel {
    public Wheel(Ray ray) {
      super(user, ray, userConfig.radius, userConfig.speed);
    }

    @Override
    public void render() {
      Vector3 rotateAxis = Vector3.PLUS_J.crossProduct(this.ray.direction);
      VectorMethods.circle(this.ray.direction.scalarMultiply(this.radius), rotateAxis, 36).forEach(v ->
        ParticleUtil.createFire(user, location.add(v).toLocation(user.world())).extra(0.01).spawn()
      );
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(6) == 0) {
        SoundUtil.FIRE_SOUND.play(location.toLocation(user.world()));
      }
    }

    @Override
    public boolean onEntityHit(@NonNull Entity entity) {
      DamageUtil.damageEntity(entity, user, userConfig.damage, description());
      FireTick.LARGER.apply(user, entity, userConfig.fireTicks);
      return true;
    }

    @Override
    public boolean onBlockHit(@NonNull Block block) {
      if (MaterialUtil.isIgnitable(block) && Bending.game().protectionSystem().canBuild(user, block)) {
        TempBlock.create(block, Material.FIRE.createBlockData(), BendingProperties.FIRE_REVERT_TIME, true);
      }
      return true;
    }
  }

  private static class Config extends Configurable {
    @Attribute(Attribute.COOLDOWN)
    public long cooldown;
    @Attribute(Attribute.RADIUS)
    public double radius;
    @Attribute(Attribute.DAMAGE)
    public double damage;
    @Attribute(Attribute.FIRE_TICKS)
    public int fireTicks;
    @Attribute(Attribute.RANGE)
    public double range;
    @Attribute(Attribute.SPEED)
    public double speed;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "fire", "sequences", "firewheel");

      cooldown = abilityNode.node("cooldown").getLong(8000);
      radius = abilityNode.node("radius").getDouble(1.0);
      damage = abilityNode.node("damage").getDouble(3.5);
      fireTicks = abilityNode.node("fire-ticks").getInt(25);
      range = abilityNode.node("range").getDouble(20.0);
      speed = abilityNode.node("speed").getDouble(0.75);

      abilityNode.node("speed").comment("How many blocks the wheel advances every tick.");
    }
  }
}
