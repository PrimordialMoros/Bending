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

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.basic.ParticleStream;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Burstable;
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
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.collision.AABBUtils;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

public class AirBlast extends AbilityInstance implements Ability, Burstable {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private AirStream stream;
  private Vector3 origin;
  private Vector3 direction;

  private boolean launched;
  private boolean selectedOrigin;
  private int particleCount = 6;
  private long renderInterval = 0;

  public AirBlast(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
    this.user = user;
    recalculateConfig();

    if (Policies.IN_LIQUID.test(user, getDescription())) {
      return false;
    }

    removalPolicy = Policies.builder()
      .add(OutOfRangeRemovalPolicy.of(userConfig.selectRange * 2, () -> origin))
      .add(Policies.IN_LIQUID)
      .build();

    for (AirBlast blast : Bending.getGame().getAbilityManager(user.getWorld()).getUserInstances(user, AirBlast.class).collect(Collectors.toList())) {
      if (!blast.launched) {
        if (method == ActivationMethod.SNEAK_RELEASE) {
          if (!blast.selectOrigin()) {
            Bending.getGame().getAbilityManager(user.getWorld()).destroyInstance(blast);
          }
        } else {
          blast.launch();
        }
        return false;
      }
    }

    if (method == ActivationMethod.SNEAK_RELEASE) {
      return selectOrigin();
    } else {
      origin = user.getEyeLocation();
      launch();
    }
    return true;
  }

  @Override
  public void recalculateConfig() {
    userConfig = Bending.getGame().getAttributeSystem().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, getDescription())) {
      return UpdateResult.REMOVE;
    }

    if (!launched) {
      if (!getDescription().equals(user.getSelectedAbility().orElse(null))) {
        return UpdateResult.REMOVE;
      }
      ParticleUtil.createAir(origin.toLocation(user.getWorld())).count(4).offset(0.5, 0.5, 0.5).spawn();
    }

    return (!launched || stream.update() == UpdateResult.CONTINUE) ? UpdateResult.CONTINUE : UpdateResult.REMOVE;
  }

  private boolean selectOrigin() {
    origin = user.getTarget(userConfig.selectRange)
      .subtract(user.getDirection().scalarMultiply(0.5));
    selectedOrigin = true;
    return Bending.getGame().getProtectionSystem().canBuild(user, origin.toBlock(user.getWorld()));
  }

  private void launch() {
    launched = true;
    Vector3 target = user.getTarget(userConfig.range);
    if (user.isSneaking()) {
      Vector3 temp = new Vector3(origin.toArray());
      origin = new Vector3(target.toArray());
      target = temp;
    }
    direction = target.subtract(origin).normalize();
    user.setCooldown(getDescription(), userConfig.cooldown);
    stream = new AirStream(new Ray(origin, direction.scalarMultiply(userConfig.range)));
  }

  @Override
  public @NonNull Collection<@NonNull Collider> getColliders() {
    if (stream == null) {
      return Collections.emptyList();
    }
    return Collections.singletonList(stream.getCollider());
  }

  @Override
  public @NonNull User getUser() {
    return user;
  }

  // Used to initialize the blast for bursts
  @Override
  public void initialize(@NonNull User user, @NonNull Vector3 location, @NonNull Vector3 direction) {
    this.user = user;
    recalculateConfig();
    selectedOrigin = false;
    launched = true;
    origin = location;
    this.direction = direction;
    removalPolicy = Policies.builder().build();
    particleCount = 1;
    renderInterval = 75;
    stream = new AirStream(new Ray(location, direction));
  }

  private class AirStream extends ParticleStream {
    private long nextRenderTime;

    public AirStream(Ray ray) {
      super(user, ray, userConfig.speed, 1.3);
      canCollide = b -> b.isLiquid() || MaterialUtil.isFire(b);
      livingOnly = false;
    }

    @Override
    public void render() {
      long time = System.currentTimeMillis();
      if (renderInterval == 0 || time >= nextRenderTime) {
        ParticleUtil.createAir(getBukkitLocation()).count(particleCount)
          .offset(0.275, 0.275, 0.275)
          .spawn();
        nextRenderTime = time + renderInterval;
      }
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(6) == 0) {
        SoundUtil.AIR_SOUND.play(getBukkitLocation());
      }

      // Handle user separately from the general entity collision.
      if (selectedOrigin) {
        if (AABBUtils.getEntityBounds(user.getEntity()).intersects(collider)) {
          onEntityHit(user.getEntity());
        }
      }
    }

    @Override
    public boolean onEntityHit(@NonNull Entity entity) {
      boolean isUser = entity.equals(user.getEntity());
      double factor = isUser ? userConfig.selfPush : userConfig.otherPush;
      FireTick.extinguish(entity);
      if (factor == 0) {
        return false;
      }
      factor *= 1 - (location.distance(origin) / (2 * userConfig.range));
      // Reduce the push if the player is on the ground.
      if (isUser && user.isOnGround()) {
        factor *= 0.5;
      }
      Vector3 velocity = new Vector3(entity.getVelocity());
      // The strength of the entity's velocity in the direction of the blast.
      double strength = velocity.dotProduct(direction);
      if (strength > factor) {
        double f = velocity.normalize().dotProduct(direction);
        velocity = velocity.scalarMultiply(0.5).add(direction.scalarMultiply(f));
      } else if (strength + factor * 0.5 > factor) {
        velocity = velocity.add(direction.scalarMultiply(factor - strength));
      } else {
        velocity = velocity.add(direction.scalarMultiply(factor * 0.5));
      }
      entity.setVelocity(velocity.clampVelocity());
      entity.setFallDistance(0);
      return false;
    }

    @Override
    public boolean onBlockHit(@NonNull Block block) {
      if (BlockMethods.tryExtinguishFire(user, block)) {
        return false;
      }
      BlockMethods.tryCoolLava(user, block);
      return true;
    }
  }

  private static class Config extends Configurable {
    @Attribute(Attribute.COOLDOWN)
    public long cooldown;
    @Attribute(Attribute.RANGE)
    public double range;
    @Attribute(Attribute.SPEED)
    public double speed;
    @Attribute(Attribute.STRENGTH)
    public double selfPush;
    @Attribute(Attribute.STRENGTH)
    public double otherPush;
    @Attribute(Attribute.SELECTION)
    public double selectRange;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "air", "airblast");

      cooldown = abilityNode.node("cooldown").getLong(1250);
      range = abilityNode.node("range").getDouble(20.0);
      speed = abilityNode.node("speed").getDouble(1.2);

      selfPush = abilityNode.node("push").node("self").getDouble(2.1);
      otherPush = abilityNode.node("push").node("other").getDouble(2.1);

      selectRange = abilityNode.node("select-range").getDouble(8.0);
    }
  }
}
