/*
 * Copyright 2020-2022 Moros
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

package me.moros.bending.ability.water;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.MultiUpdatable;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.ability.common.FragileStructure;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.Policies;
import me.moros.bending.model.predicate.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.temporal.TempBlock;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class IceWall extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final MultiUpdatable<IceWallColumn> pillars = MultiUpdatable.empty();
  private final Collection<Block> wallBlocks = new ArrayList<>();

  private Block origin;

  public IceWall(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    Block targetBlock = user.rayTrace(config.selectRange).blocks(user.world()).block();
    if (targetBlock != null && WaterMaterials.isIceBendable(targetBlock) && FragileStructure.tryDamageStructure(targetBlock, 0, Ray.ZERO)) {
      return false;
    }
    if (user.onCooldown(description())) {
      return false;
    }
    this.user = user;
    loadConfig();

    origin = user.find(userConfig.selectRange, WaterMaterials::isWaterOrIceBendable);
    if (origin == null) {
      return false;
    }

    raiseWall(userConfig.maxHeight, userConfig.width);
    if (!pillars.isEmpty()) {
      user.addCooldown(description(), userConfig.cooldown);
      removalPolicy = Policies.builder().build();
      return true;
    }
    return false;
  }

  @Override
  public void loadConfig() {
    userConfig = user.game().configProcessor().calculate(this, config);
  }

  @Override
  public UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    return pillars.update();
  }

  private void createPillar(Block block, int height) {
    int h = validate(block, height);
    if (h > 0) {
      pillars.add(new IceWallColumn(block, h));
    }
  }

  private int validate(Block block, int height) {
    if (!WaterMaterials.isWaterOrIceBendable(block) || !TempBlock.isBendable(block)) {
      return 0;
    }
    if (!user.canBuild(block)) {
      return 0;
    }
    for (int i = 0; i < height; i++) {
      Block forwardBlock = block.getRelative(BlockFace.UP, i + 1);
      if (!user.canBuild(forwardBlock)) {
        return i;
      }
      if (!MaterialUtil.isTransparent(forwardBlock) && !WaterMaterials.isWaterOrIceBendable(forwardBlock)) {
        return i;
      }
    }
    return height;
  }

  private void raiseWall(int height, int width) {
    double w = (width - 1) / 2.0;
    Vector3d side = user.direction().cross(Vector3d.PLUS_J).normalize();
    Vector3d center = Vector3d.center(origin);
    int min = -FastMath.ceil(w);
    int max = FastMath.floor(w);
    for (int i = min; i <= max; i++) {
      Block check = center.add(side.multiply(i)).toBlock(user.world());
      int h = height - Math.min(FastMath.ceil(height / 3.0), Math.abs(i));
      if (WaterMaterials.isWaterOrIceBendable(check)) {
        createPillar(check, h);
      } else {
        WorldUtil.findTopBlock(check, h, WaterMaterials::isWaterOrIceBendable).ifPresent(b -> createPillar(b, h));
      }
    }
  }

  @Override
  public void onDestroy() {
    FragileStructure.builder().health(userConfig.wallHealth).predicate(WaterMaterials::isIceBendable).add(wallBlocks).build();
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  private final class IceWallColumn implements Updatable {
    private final Block origin;

    private final int length;

    private int currentLength = 0;
    private long nextUpdateTime = 0;

    private IceWallColumn(Block origin, int length) {
      this.origin = origin;
      this.length = length;
    }

    @Override
    public UpdateResult update() {
      if (currentLength >= length) {
        return UpdateResult.REMOVE;
      }
      long time = System.currentTimeMillis();
      if (time < nextUpdateTime) {
        return UpdateResult.CONTINUE;
      }
      nextUpdateTime = time + 70;
      Block currentIndex = origin.getRelative(BlockFace.UP, currentLength);
      currentLength++;
      if (isValidBlock(currentIndex)) {
        wallBlocks.add(currentIndex);
        SoundUtil.ICE.play(currentIndex);
        TempBlock.ice().build(currentIndex);
        return UpdateResult.CONTINUE;
      }
      return UpdateResult.REMOVE;
    }

    private boolean isValidBlock(Block block) {
      if (MaterialUtil.isLava(block) || !TempBlock.isBendable(block) || !user.canBuild(block)) {
        return false;
      }
      WorldUtil.tryBreakPlant(block);
      return MaterialUtil.isTransparent(block) || WaterMaterials.isWaterOrIceBendable(block);
    }
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 6000;
    @Modifiable(Attribute.SELECTION)
    private double selectRange = 6;
    @Modifiable(Attribute.HEIGHT)
    private int maxHeight = 6;
    @Modifiable(Attribute.RADIUS)
    private int width = 5;
    @Modifiable(Attribute.STRENGTH)
    private int wallHealth = 12;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "water", "icewall");
    }
  }
}
