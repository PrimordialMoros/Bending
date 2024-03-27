/*
 * Copyright 2020-2024 Moros
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

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.config.BendingProperties;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.Platform;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.material.EarthMaterials;
import me.moros.bending.common.config.ConfigManager;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
import me.moros.math.VectorUtil;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

public class EarthTunnel extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Predicate<Block> predicate;
  private Vector3d center;

  private double distance = 0;
  private int radius = 0;
  private int angle = 0;

  public EarthTunnel(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (user.game().abilityManager(user.worldKey()).hasAbility(user, EarthTunnel.class)) {
      return false;
    }

    this.user = user;
    loadConfig();

    predicate = b -> EarthMaterials.isEarthNotLava(user, b);
    Block block = user.find(userConfig.range, predicate);
    if (block == null) {
      return false;
    }

    center = block.center();
    removalPolicy = Policies.builder().add(Policies.NOT_SNEAKING).build();

    return true;
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
    int count = 0;
    while (++count <= userConfig.speed) {
      if (distance > userConfig.range) {
        return UpdateResult.REMOVE;
      }
      Vector3d offset = VectorUtil.orthogonal(user.direction(), Math.toRadians(angle), radius);
      Block current = user.world().blockAt(center.add(offset));
      if (!user.canBuild(current)) {
        return UpdateResult.REMOVE;
      }
      if (predicate.test(current)) {
        tryMineBlock(current);
      }
      if (angle >= 360) {
        angle = 0;
        Block block = user.find(userConfig.range, predicate);
        if (block == null) {
          return UpdateResult.REMOVE;
        }
        center = block.center();

        if (++radius > userConfig.radius) {
          radius = 0;
          distance++;
        }
      } else {
        if (radius <= 0) {
          radius++;
        } else {
          angle += FastMath.ceil(22.5 / radius);
        }
      }
    }
    return UpdateResult.CONTINUE;
  }

  private void tryMineBlock(Block block) {
    Collection<ItemSnapshot> capturedDrops;
    if (userConfig.extractOres && !TempBlock.MANAGER.isTemp(block)) {
      capturedDrops = Platform.instance().factory().calculateOptimalOreDrops(block);
      if (!capturedDrops.isEmpty() && !userConfig.persistent) {
        BlockType type = block.type();
        BlockType newType;
        if (type.name().contains("nether")) {
          newType = BlockType.NETHERRACK;
        } else if (type.name().contains("deepslate")) {
          newType = BlockType.DEEPSLATE;
        } else {
          newType = BlockType.STONE;
        }
        block.setType(newType);
      }
    } else {
      capturedDrops = List.of();
    }
    if (userConfig.persistent) {
      block.setType(BlockType.AIR);
    } else {
      TempBlock.air().duration(BendingProperties.instance().earthRevertTime()).build(block);
    }
    Vector3d pos = block.center();
    capturedDrops.forEach(drop -> block.world().dropItem(pos, drop));
  }

  @Override
  public void onDestroy() {
    user.addCooldown(description(), userConfig.cooldown);
  }

  @ConfigSerializable
  private static final class Config implements Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 2000;
    @Modifiable(Attribute.RANGE)
    private double range = 10;
    @Modifiable(Attribute.RADIUS)
    private double radius = 1;
    @Comment("How many blocks to excavate every tick")
    @Modifiable(Attribute.SPEED)
    private int speed = 2;
    private boolean extractOres = true;
    @Comment("Persistent earth tunnel will NOT revert blocks, use at your own risk")
    private boolean persistent = false;

    @Override
    public List<String> path() {
      return List.of("abilities", "earth", "earthtunnel");
    }
  }
}
