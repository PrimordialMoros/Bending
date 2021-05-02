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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;

import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.fire.FireWall;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.OBB;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.WorldMethods;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.NonNull;

public class FireWave extends AbilityInstance {
  private static final Config config = new Config();
  private static AbilityDescription wallDesc;

  private Config userConfig;

  private final Queue<WallInfo> walls = new ArrayDeque<>();
  private FireWall wall;

  private long nextTime;

  public FireWave(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
    if (wallDesc == null) {
      wallDesc = Bending.game().abilityRegistry().abilityDescription("FireWall").orElseThrow(RuntimeException::new);
    }
    wall = new FireWall(wallDesc);
    if (user.isOnCooldown(wall.description()) || !wall.activate(user, ActivationMethod.ATTACK)) {
      return false;
    }

    recalculateConfig();

    Vector3 origin = user.eyeLocation().add(user.direction().scalarMultiply(wall.range()));
    Vector3 direction = user.direction();
    double yaw = user.yaw();
    double hw = wall.width() / 2.0;
    double hh = wall.height() / 2.0;
    for (double i = 0.5; i <= 2 * userConfig.steps; i += 0.5) {
      Vector3 currentPosition = origin.add(direction.scalarMultiply(i));
      if (!Bending.game().protectionSystem().canBuild(user, currentPosition.toBlock(user.world()))) {
        break;
      }
      hh += 0.2;
      AABB aabb = new AABB(new Vector3(-hw, -hh, -0.5), new Vector3(hw, hh, 0.5));
      OBB collider = new OBB(aabb, Vector3.PLUS_J, FastMath.toRadians(yaw)).addPosition(currentPosition);
      double radius = collider.halfExtents().maxComponent() + 1;
      Collection<Block> blocks = WorldMethods.nearbyBlocks(currentPosition.toLocation(user.world()), radius, b -> collider.contains(new Vector3(b)) && MaterialUtil.isTransparent(b));
      if (blocks.isEmpty()) {
        break;
      }
      walls.offer(new WallInfo(blocks, collider));

    }
    if (walls.isEmpty()) {
      return false;
    }
    wall.updateDuration(userConfig.duration);
    nextTime = 0;
    user.addCooldown(description(), userConfig.cooldown);
    return true;
  }

  @Override
  public void recalculateConfig() {
    userConfig = Bending.game().attributeSystem().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    long time = System.currentTimeMillis();
    if (time >= nextTime) {
      nextTime = time + 250;
      WallInfo info = walls.poll();
      if (info != null) {
        wall.wall(info.blocks(), info.collider());
      }
    }
    return wall.update();
  }

  @Override
  public void onDestroy() {
    wall.onDestroy();
  }

  @Override
  public @NonNull User user() {
    return wall.user();
  }

  private static class WallInfo {
    private final Collection<Block> blocks;
    private final OBB collider;

    private WallInfo(Collection<Block> blocks, OBB collider) {
      this.blocks = blocks;
      this.collider = collider;
    }

    private Collection<Block> blocks() {
      return blocks;
    }

    private OBB collider() {
      return collider;
    }
  }

  private static class Config extends Configurable {
    @Attribute(Attribute.COOLDOWN)
    public long cooldown;
    @Attribute(Attribute.HEIGHT)
    public double maxHeight;
    @Attribute(Attribute.DURATION)
    public long duration;
    @Attribute(Attribute.RANGE)
    public long steps;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "fire", "sequences", "firewave");

      cooldown = abilityNode.node("cooldown").getLong(16000);
      maxHeight = abilityNode.node("max-height").getDouble(10.0);
      duration = abilityNode.node("duration").getLong(8000);
      steps = abilityNode.node("steps").getInt(8);

      abilityNode.node("steps").comment("The amount of blocks the FireWave will advance.");
    }
  }
}
