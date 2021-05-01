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

package me.moros.bending.ability.air;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.air.sequences.AirWheel;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.collision.AABBUtils;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.EntityMethods;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.block.Block;

public class AirScooter extends AbilityInstance implements Ability {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private HeightSmoother heightSmoother;

  public boolean canRender = true;
  private double verticalPosition = 0;
  private int stuckCount = 0;

  public AirScooter(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
    if (Bending.game().abilityManager(user.world()).hasAbility(user, AirScooter.class)) {
      return false;
    }
    if (Bending.game().abilityManager(user.world()).hasAbility(user, AirWheel.class)) {
      return false;
    }
    this.user = user;
    recalculateConfig();

    if (Policies.IN_LIQUID.test(user, description())) {
      return false;
    }

    heightSmoother = new HeightSmoother();

    double dist = EntityMethods.distanceAboveGround(user.entity());
    if ((dist < 0.5 || dist > 3)) {
      return false;
    }

    removalPolicy = Policies.builder()
      .add(Policies.SNEAKING)
      .add(ExpireRemovalPolicy.of(userConfig.duration))
      .build();
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
    if (!Bending.game().protectionSystem().canBuild(user, user.locBlock(), description())) {
      return UpdateResult.REMOVE;
    }

    stuckCount = user.velocity().getNormSq() < 0.1 ? stuckCount + 1 : 0;
    if (stuckCount > 10 || !move()) {
      return UpdateResult.REMOVE;
    }

    if (canRender) {
      render();
    }
    if (ThreadLocalRandom.current().nextInt(4) == 0) {
      SoundUtil.AIR_SOUND.play(user.entity().getLocation());
    }
    return UpdateResult.CONTINUE;
  }

  @Override
  public void onDestroy() {
    user.addCooldown(description(), userConfig.cooldown);
  }

  private void render() {
    verticalPosition += 0.25 * FastMath.PI;
    for (double theta = 0; theta < 2 * FastMath.PI * 2; theta += FastMath.PI / 5) {
      double sin = FastMath.sin(verticalPosition);
      double x = 0.6 * FastMath.cos(theta) * sin;
      double y = 0.6 * FastMath.cos(verticalPosition);
      double z = 0.6 * FastMath.sin(theta) * sin;
      ParticleUtil.createAir(user.entity().getLocation().add(x, y - 0.25, z)).spawn();
    }
  }

  @Override
  public @NonNull User user() {
    return user;
  }

  private boolean move() {
    if (isColliding()) {
      return false;
    }
    double height = EntityMethods.distanceAboveGround(user.entity());
    double smoothedHeight = heightSmoother.add(height);
    if (user.locBlock().isLiquid()) {
      height = 0.5;
    } else if (smoothedHeight > 3.25) {
      return false;
    }
    double delta = getPrediction() - height;
    double force = FastMath.max(-0.5, FastMath.min(0.5, 0.3 * delta));
    Vector3 velocity = user.direction().setY(0).normalize().scalarMultiply(userConfig.speed).setY(force);
    user.entity().setVelocity(velocity.clampVelocity());
    user.entity().setFallDistance(0);
    return true;
  }

  private boolean isColliding() {
    double speed = user.velocity().setY(0).getNorm();
    Vector3 direction = user.direction().setY(0).normalize(Vector3.ZERO);
    Vector3 front = user.eyeLocation().subtract(new Vector3(0, 0.5, 0))
      .add(direction.scalarMultiply(FastMath.max(userConfig.speed, speed)));
    Block block = front.toBlock(user.world());
    return !MaterialUtil.isTransparentOrWater(block) || !block.isPassable();
  }

  private double getPrediction() {
    double playerSpeed = user.velocity().setY(0).getNorm();
    double speed = FastMath.max(userConfig.speed, playerSpeed) * 3;
    Vector3 offset = user.direction().setY(0).normalize().scalarMultiply(speed);
    Vector3 location = user.location().add(offset);
    AABB userBounds = AABBUtils.entityBounds(user.entity()).at(offset);
    for (Block block : BlockMethods.combineFaces(location.toBlock(user.world()))) {
      if (AABBUtils.blockBounds(block).intersects(userBounds)) {
        return 2.25;
      }
    }
    return 1.25;
  }

  private static class HeightSmoother {
    private final double[] values;
    private int index;

    private HeightSmoother() {
      index = 0;
      values = new double[10];
    }

    private double add(double value) {
      values[index] = value;
      index = (index + 1) % values.length;
      return get();
    }

    private double get() {
      return Arrays.stream(values).sum() / values.length;
    }
  }

  private static class Config extends Configurable {
    @Attribute(Attribute.SPEED)
    public double speed;
    @Attribute(Attribute.COOLDOWN)
    public long cooldown;
    @Attribute(Attribute.DURATION)
    public long duration;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "air", "airscooter");

      speed = abilityNode.node("speed").getDouble(0.7);
      cooldown = abilityNode.node("cooldown").getLong(2000);
      duration = abilityNode.node("duration").getLong(15000);
    }
  }
}
