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

package me.moros.bending.ability.earth.passive;

import java.util.function.Predicate;

import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.user.User;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class DensityShift extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;

  public DensityShift(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    this.user = user;
    loadConfig();
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = Bending.configManager().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    return UpdateResult.CONTINUE;
  }

  private boolean isSoftened() {
    if (!user.canBend(description())) {
      return false;
    }
    Block block = user.locBlock().getRelative(BlockFace.DOWN);
    if (EarthMaterials.isEarthbendable(user, block)) {
      softenArea();
      return true;
    }
    return MaterialUtil.isTransparent(block);
  }

  public static boolean isSoftened(@NonNull User user) {
    return Bending.game().abilityManager(user.world()).firstInstance(user, DensityShift.class)
      .map(DensityShift::isSoftened).orElse(false);
  }

  private void softenArea() {
    Vector3d center = Vector3d.center(user.locBlock().getRelative(BlockFace.DOWN));
    Predicate<Block> predicate = b -> EarthMaterials.isEarthOrSand(b) && b.getRelative(BlockFace.UP).isPassable();
    for (Block b : WorldUtil.nearbyBlocks(user.world(), center, userConfig.radius, predicate)) {
      if (MaterialUtil.isAir(b.getRelative(BlockFace.DOWN)) || !TempBlock.isBendable(b)) {
        continue;
      }
      TempBlock.builder(MaterialUtil.softType(b.getBlockData())).bendable(true)
        .duration(userConfig.duration).build(b).ifPresent(TempBlock::forceWeak);
    }
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.DURATION)
    public long duration;
    @Modifiable(Attribute.RADIUS)
    public double radius;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "earth", "passives", "densityshift");

      duration = abilityNode.node("duration").getLong(6000);
      radius = abilityNode.node("radius").getDouble(2.0);
    }
  }
}

