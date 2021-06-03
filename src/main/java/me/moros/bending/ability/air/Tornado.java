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

import java.util.concurrent.ThreadLocalRandom;

import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.EntityMethods;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;

public class Tornado extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private double yOffset = 0;
  private long startTime;

  public Tornado(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    this.user = user;
    loadConfig();
    removalPolicy = Policies.builder()
      .add(SwappedSlotsRemovalPolicy.of(description()))
      .add(Policies.NOT_SNEAKING)
      .add(ExpireRemovalPolicy.of(userConfig.duration))
      .build();
    startTime = System.currentTimeMillis();
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = Bending.configManager().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, description()) || user.headBlock().isLiquid()) {
      return UpdateResult.REMOVE;
    }

    if (!user.canBuild(user.locBlock())) {
      return UpdateResult.REMOVE;
    }
    Vector3 base = user.rayTrace(userConfig.range, false);
    Block baseBlock = base.toBlock(user.world());
    if (MaterialUtil.isTransparent(baseBlock.getRelative(BlockFace.DOWN))) {
      return UpdateResult.CONTINUE;
    }
    if (!user.canBuild(baseBlock)) {
      return UpdateResult.REMOVE;
    }

    long time = System.currentTimeMillis();
    double factor = Math.min(1, time - startTime / userConfig.growthTime);
    double height = 2 + factor * (userConfig.height - 2);
    double radius = 2 + factor * (userConfig.radius - 2);

    AABB box = new AABB(new Vector3(-radius, 0, -radius), new Vector3(radius, height, radius)).at(base);
    CollisionUtil.handleEntityCollisions(user, box, entity -> {
      double dy = entity.getLocation().getY() - base.y;
      double r = 2 + (radius - 2) * dy;
      Vector3 delta = EntityMethods.entityCenter(entity).subtract(base);
      double distSq = delta.x * delta.x + delta.z * delta.z;
      if (distSq > r * r) {
        return false;
      }

      if (entity.equals(user.entity())) {
        double velY;
        if (dy >= height * .95) {
          velY = 0;
        } else if (dy >= height * .85) {
          velY = 6.0 * (.95 - dy / height);
        } else {
          velY = 0.6;
        }
        Vector3 velocity = user.direction().setY(velY).multiply(factor);
        entity.setVelocity(velocity.clampVelocity());
      } else {
        Vector3 normal = delta.setY(0).normalize();
        Vector3 ortho = normal.crossProduct(Vector3.PLUS_J).normalize();
        Vector3 velocity = ortho.add(normal).normalize().multiply(factor);
        entity.setVelocity(velocity.clampVelocity());
      }
      return false;
    }, true, true);

    render(base, factor, height, radius);
    return UpdateResult.CONTINUE;
  }

  private void render(Vector3 base, double factor, double height, double radius) {
    double amount = Math.min(30, Math.max(4, factor * 30));
    yOffset += 0.1;
    if (yOffset >= 1) {
      yOffset = 0;
    }
    for (int i = 0; i < 3; i++) {
      double offset = i * 2 * Math.PI / 3.0;
      for (double y = yOffset; y < height; y += (height / amount)) {
        double r = 2 + (radius - 2) * y / height;
        double x = r * Math.cos(y + offset);
        double z = r * Math.sin(y + offset);
        Location loc = base.add(new Vector3(x, y, z)).toLocation(user.world());
        ParticleUtil.createAir(loc).spawn();
        if (ThreadLocalRandom.current().nextInt(20) == 0) {
          SoundUtil.AIR.play(loc);
        }
      }
    }
  }

  @Override
  public void onDestroy() {
    user.addCooldown(description(), userConfig.cooldown);
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.DURATION)
    public long duration;
    @Modifiable(Attribute.RADIUS)
    public double radius;
    @Modifiable(Attribute.HEIGHT)
    public double height;
    @Modifiable(Attribute.RANGE)
    public double range;
    public long growthTime;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "air", "tornado");

      cooldown = abilityNode.node("cooldown").getLong(4000);
      duration = abilityNode.node("duration").getLong(8000);
      radius = abilityNode.node("radius").getDouble(10.0);
      height = abilityNode.node("height").getDouble(15.0);
      range = abilityNode.node("range").getDouble(25.0);
      growthTime = abilityNode.node("growth-time").getLong(2000);
    }
  }
}
