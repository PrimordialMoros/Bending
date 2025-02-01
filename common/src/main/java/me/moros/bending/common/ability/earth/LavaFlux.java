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

package me.moros.bending.common.ability.earth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.common.Fracture;
import me.moros.bending.api.ability.common.FragileStructure;
import me.moros.bending.api.ability.common.basic.BlockLine;
import me.moros.bending.api.collision.geometry.Ray;
import me.moros.bending.api.config.BendingProperties;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.Direction;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.platform.world.WorldUtil;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.material.EarthMaterials;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
import me.moros.math.VectorUtil;

public class LavaFlux extends AbilityInstance {
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Collection<Block> collisionBases = new HashSet<>();

  private Line line;

  public LavaFlux(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    loadConfig();
    return release();
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
    if (line.update() == UpdateResult.REMOVE) {
      checkWallCollisions();
      return UpdateResult.REMOVE;
    }
    return UpdateResult.CONTINUE;
  }

  private void checkWallCollisions() {
    if (!line.collided) {
      return;
    }
    Collection<Block> blocks = new ArrayList<>();
    for (Block block : collisionBases) {
      for (int i = 0; i <= userConfig.wallRadius; i++) {
        Block temp = block.offset(Direction.UP, i);
        if (canDestroyBlock(temp)) {
          blocks.add(temp);
        } else {
          break;
        }
      }
    }
    Fracture fracture = Fracture.builder().fragile(
      FragileStructure.builder().fallingBlocks(true).health(1).predicate(b -> b.type() == BlockType.MAGMA_BLOCK)
    ).interval(70).add(blocks).build();
    if (fracture != null) {
      user.game().abilityManager(user.worldKey()).addUpdatable(fracture);
    }
  }

  private boolean canDestroyBlock(Block block) {
    if (EarthMaterials.isMetalBendable(block) || !user.canBuild(block)) {
      return false;
    }
    return line.checkDistance(block) && EarthMaterials.isEarthbendable(user, block);
  }

  private boolean release() {
    if (!user.isOnGround()) {
      return false;
    }
    Vector3d center = user.location().center();
    Vector3d dir = user.direction().withY(0).normalize().multiply(userConfig.range + 2);

    line = new Line(Ray.of(center, dir), 70);
    // First update in same tick to only apply cooldown if line is valid
    if (line.update() == UpdateResult.REMOVE) {
      return false;
    }
    removalPolicy = Policies.builder().add(Policies.NOT_SNEAKING).build();
    user.addCooldown(description(), userConfig.cooldown);
    return true;
  }

  private class Line extends BlockLine {
    private final Collection<Vector2i> affectedBlocks = new HashSet<>();
    private final Vector3d face;
    private final int offset;
    private final int wallOffset;

    private boolean center = true;
    private boolean collided;

    public Line(Ray ray, long interval) {
      super(user, ray);
      this.interval = interval;
      face = VectorUtil.nearestFace(user.direction().cross(Vector3d.PLUS_J));
      int w = Math.max(3, userConfig.width);
      offset = w % 2 == 0 ? w / 2 : (w - 1) / 2;
      w = Math.max(w, FastMath.ceil(userConfig.wallRadius));
      wallOffset = w % 2 == 0 ? w / 2 : (w - 1) / 2;
      diagonalMovement = false;
    }

    @Override
    public boolean isValidBlock(Block block) {
      if (!MaterialUtil.isTransparent(block) && !EarthMaterials.isLavaBendable(block)) {
        return false;
      }
      Block below = block.offset(Direction.DOWN);
      if (EarthMaterials.isMetalBendable(below)) {
        return false;
      }
      return EarthMaterials.isEarthbendable(user, below);
    }

    @Override
    public void render(Block block) {
      render(block, offset, offset);
      center = true;
    }

    private void render(Block block, int left, int right) {
      center = false;
      if (!affectedBlocks.add(Vector2i.at(block.blockX(), block.blockZ()))) {
        return;
      }
      WorldUtil.tryBreakPlant(block);
      if (MaterialUtil.isFire(block)) {
        block.setType(BlockType.AIR);
      }
      Block below = block.offset(Direction.DOWN);
      TempBlock.builder(MaterialUtil.lavaData(1)).duration(150).ability(LavaFlux.this).build(block);
      long delay = ThreadLocalRandom.current().nextLong(1500);
      TempBlock.builder(BlockType.STONE).duration(userConfig.duration + delay).build(below);
      TempBlock.builder(BlockType.MAGMA_BLOCK).duration(userConfig.duration).ability(LavaFlux.this).build(below);
      if (ThreadLocalRandom.current().nextInt(6) == 0) {
        SoundEffect.LAVA.play(block);
      }
      if (left > 0) {
        expand(block, face, left - 1, 0);
      }
      if (right > 0) {
        expand(block, face.negate(), 0, right - 1);
      }
    }

    private void expand(Block base, Vector3d side, int left, int right) {
      Resolved resolved = resolve(base.center(), side);
      Block block = user.world().blockAt(resolved.point());
      if (resolved.success()) {
        render(block, left, right);
      } else {
        collisionBases.add(block);
      }
    }

    private void resolveOnly(Vector3d point, Vector3d side, int amount) {
      if (amount > 0) {
        Resolved resolved = resolve(point, side);
        if (!resolved.success()) {
          resolveOnly(resolved.point(), side, amount - 1);
        }
      }
    }

    @Override
    public void onCollision(Vector3d collided) {
      Block block = user.world().blockAt(collided);
      if (!checkDistance(block)) {
        return;
      }
      this.collided = true;
      if (center) {
        center = false;
        resolveOnly(collided, face, wallOffset);
        resolveOnly(collided, face.negate(), wallOffset);
      }
      if (MaterialUtil.isWater(block)) {
        WorldUtil.playLavaExtinguishEffect(block);
        TempBlock.builder(BlockType.OBSIDIAN)
          .duration(BendingProperties.instance().earthRevertTime(1000)).build(block);
        return;
      }
      collisionBases.add(block);
    }

    private boolean checkDistance(Block block) {
      return block.center().distanceSq(location) < userConfig.wallRadius * userConfig.wallRadius;
    }
  }

  private static final class Config implements Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 10000;
    @Modifiable(Attribute.RANGE)
    private double range = 9;
    @Modifiable(Attribute.RADIUS)
    private int width = 3;
    @Modifiable(Attribute.STRENGTH)
    private double wallRadius = 5;
    @Modifiable(Attribute.DURATION)
    private long duration = 8000;

    @Override
    public List<String> path() {
      return List.of("abilities", "earth", "lavaflux");
    }
  }
}
