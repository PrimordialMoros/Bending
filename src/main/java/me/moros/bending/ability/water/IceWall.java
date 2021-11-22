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

package me.moros.bending.ability.water;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import me.moros.bending.Bending;
import me.moros.bending.ability.common.FragileStructure;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.RayTrace;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import me.moros.bending.util.methods.BlockMethods;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class IceWall extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Collection<IceWallColumn> pillars = new ArrayList<>();
  private final Collection<Block> wallBlocks = new ArrayList<>();

  private Block origin;

  public IceWall(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    Block targetBlock = RayTrace.of(user).range(config.selectRange).result(user.world()).block();
    if (targetBlock != null && FragileStructure.tryDamageStructure(List.of(targetBlock), 0)) {
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
    userConfig = Bending.configManager().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }

    pillars.removeIf(pillar -> pillar.update() == UpdateResult.REMOVE);
    return pillars.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
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
    for (int i = -FastMath.ceil(w); i <= FastMath.floor(w); i++) {
      Block check = center.add(side.multiply(i)).toBlock(user.world());
      int h = height - Math.min(FastMath.ceil(height / 3.0), Math.abs(i));
      if (WaterMaterials.isWaterOrIceBendable(check)) {
        createPillar(check, h);
      } else {
        BlockMethods.findTopBlock(check, h, WaterMaterials::isWaterOrIceBendable).ifPresent(b -> createPillar(b, h));
      }
    }
  }

  @Override
  public void onDestroy() {
    FragileStructure.create(wallBlocks, userConfig.wallHealth, WaterMaterials::isIceBendable);
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  private class IceWallColumn implements Updatable {
    protected final Block origin;

    protected final int length;

    protected int currentLength = 0;
    protected long nextUpdateTime = 0;

    private IceWallColumn(@NonNull Block origin, int length) {
      this.origin = origin;
      this.length = length;
    }

    @Override
    public @NonNull UpdateResult update() {
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
        SoundUtil.ICE.play(currentIndex.getLocation());
        TempBlock.create(currentIndex, Material.ICE.createBlockData());
        return UpdateResult.CONTINUE;
      }
      return UpdateResult.REMOVE;
    }

    private boolean isValidBlock(Block block) {
      if (MaterialUtil.isLava(block) || !TempBlock.isBendable(block) || !user.canBuild(block)) {
        return false;
      }
      BlockMethods.tryBreakPlant(block);
      return MaterialUtil.isTransparent(block) || WaterMaterials.isWaterOrIceBendable(block);
    }
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.SELECTION)
    public double selectRange;
    @Modifiable(Attribute.HEIGHT)
    public int maxHeight;
    @Modifiable(Attribute.RADIUS)
    public int width;
    @Modifiable(Attribute.STRENGTH)
    public int wallHealth;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "water", "icewall");

      cooldown = abilityNode.node("cooldown").getLong(6000);
      selectRange = abilityNode.node("select-range").getDouble(6.0);
      maxHeight = abilityNode.node("max-height").getInt(6);
      width = abilityNode.node("width").getInt(5);
      wallHealth = abilityNode.node("wall-durability").getInt(12);
    }
  }
}
