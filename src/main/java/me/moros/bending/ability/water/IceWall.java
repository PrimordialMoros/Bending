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
import java.util.Optional;

import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.FragileStructure;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.SourceUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.NumberConversions;
import org.checkerframework.checker.nullness.qual.NonNull;

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
  public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
    Block targetBlock = WorldMethods.blockCast(user.world(), user.ray(), config.selectRange).orElse(null);
    if (targetBlock != null && FragileStructure.tryDamageStructure(List.of(targetBlock), 0)) {
      return false;
    }
    if (user.onCooldown(description())) {
      return false;
    }
    this.user = user;
    recalculateConfig();

    Optional<Block> source = SourceUtil.find(user, userConfig.selectRange, WaterMaterials::isWaterOrIceBendable);
    if (source.isEmpty()) {
      return false;
    }
    origin = source.get();

    raiseWall(userConfig.maxHeight, userConfig.width);
    if (!pillars.isEmpty()) {
      user.addCooldown(description(), userConfig.cooldown);
      removalPolicy = Policies.builder().build();
      return true;
    }
    return false;
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
    if (!Bending.game().protectionSystem().canBuild(user, block)) {
      return 0;
    }
    for (int i = 0; i < height; i++) {
      Block forwardBlock = block.getRelative(BlockFace.UP, i + 1);
      if (!Bending.game().protectionSystem().canBuild(user, forwardBlock)) {
        return i;
      }
      if (!MaterialUtil.isTransparent(forwardBlock) && forwardBlock.getType() != Material.WATER) {
        return i;
      }
    }
    return height;
  }

  private void raiseWall(int height, int width) {
    double w = (width - 1) / 2.0;
    Vector3 side = user.direction().crossProduct(Vector3.PLUS_J).normalize();
    Vector3 center = Vector3.center(origin);
    for (int i = -NumberConversions.ceil(w); i <= NumberConversions.floor(w); i++) {
      Block check = center.add(side.multiply(i)).toBlock(user.world());
      int h = height - Math.min(NumberConversions.ceil(height / 3.0), Math.abs(i));
      if (MaterialUtil.isTransparentOrWater(check)) {
        for (int j = 1; j < h; j++) {
          Block block = check.getRelative(BlockFace.DOWN, j);
          if (WaterMaterials.isWaterOrIceBendable(block)) {
            createPillar(block, h);
            break;
          } else if (!MaterialUtil.isTransparentOrWater(block)) {
            break;
          }
        }
      } else {
        BlockMethods.getTopValid(check, h, WaterMaterials::isWaterOrIceBendable).ifPresent(b -> createPillar(b, h));
      }
    }
  }

  @Override
  public void onDestroy() {
    FragileStructure.create(wallBlocks, userConfig.wallHealth, WaterMaterials::isIceBendable);
  }

  @Override
  public @NonNull User user() {
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

      Block currentIndex = origin.getRelative(BlockFace.UP, ++currentLength);
      if (canMove(currentIndex)) {
        wallBlocks.add(currentIndex);
        SoundUtil.ICE_SOUND.play(currentIndex.getLocation());
        TempBlock.create(currentIndex, Material.ICE.createBlockData());
      }
      return UpdateResult.CONTINUE;
    }

    private boolean canMove(Block block) {
      if (MaterialUtil.isLava(block) || !TempBlock.isBendable(block)) {
        return false;
      }
      if (!Bending.game().protectionSystem().canBuild(user, block)) {
        return false;
      }
      if (!MaterialUtil.isTransparent(block) && block.getType() != Material.WATER) {
        return false;
      }
      BlockMethods.tryBreakPlant(block);
      return true;
    }
  }

  private static class Config extends Configurable {
    @Attribute(Attribute.COOLDOWN)
    public long cooldown;
    @Attribute(Attribute.SELECTION)
    public double selectRange;
    @Attribute(Attribute.HEIGHT)
    public int maxHeight;
    @Attribute(Attribute.RADIUS)
    public int width;
    @Attribute(Attribute.STRENGTH)
    public int wallHealth;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "water", "icewall");

      cooldown = abilityNode.node("cooldown").getLong(6000);
      selectRange = abilityNode.node("select-range").getDouble(6.0);
      maxHeight = abilityNode.node("max-height").getInt(5);
      width = abilityNode.node("width").getInt(4);
      wallHealth = abilityNode.node("wall-durability").getInt(12);
    }
  }
}
