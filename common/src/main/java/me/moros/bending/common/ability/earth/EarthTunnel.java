/*
 * Copyright 2020-2023 Moros
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

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
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
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.material.EarthMaterials;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.bending.common.config.ConfigManager;
import me.moros.math.Vector3d;
import me.moros.math.VectorUtil;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class EarthTunnel extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
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
    for (int i = 0; i < 2; i++) {
      if (distance > userConfig.range) {
        return UpdateResult.REMOVE;
      }
      Vector3d offset = VectorUtil.orthogonal(user.direction(), Math.toRadians(angle), radius);
      Block current = user.world().blockAt(center.add(offset));
      if (!user.canBuild(current)) {
        return UpdateResult.REMOVE;
      }
      if (predicate.test(current)) {
        if (userConfig.extractOres) {
          extract(current);
        }
        TempBlock.air().duration(BendingProperties.instance().earthRevertTime()).build(current);
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
          if (++distance > userConfig.range) {
            return UpdateResult.REMOVE;
          }
        }
      } else {
        if (radius <= 0) {
          radius++;
        } else {
          angle += 22.5 / radius;
        }
      }
    }
    return UpdateResult.CONTINUE;
  }

  // TODO tweak drop rates
  private void extract(Block block) {
    if (TempBlock.MANAGER.isTemp(block)) {
      return;
    }
    BlockType type = block.type();
    Item drop = MaterialUtil.ORES.get(type);
    if (drop == null) {
      return;
    }
    int amount = getAmount(drop);
    if (amount <= 0) {
      return;
    }

    BlockType newType;
    if (type.name().contains("NETHER")) {
      newType = BlockType.NETHERRACK;
    } else if (type.name().contains("DEEPSLATE")) {
      newType = BlockType.DEEPSLATE;
    } else {
      newType = BlockType.STONE;
    }
    block.setType(newType);

    int rand = ThreadLocalRandom.current().nextInt(100);
    int factor = rand >= 75 ? 3 : rand >= 50 ? 2 : 1;
    ItemSnapshot item = Platform.instance().factory().itemBuilder(drop).build(factor * amount);
    block.world().dropItem(block.center(), item);
  }

  private int getAmount(Item type) {
    if (type == Item.COAL || type == Item.DIAMOND || type == Item.EMERALD || type == Item.QUARTZ || type == Item.RAW_IRON || type == Item.RAW_GOLD) {
      return 1;
    } else if (type == Item.REDSTONE || type == Item.RAW_COPPER) {
      return 5;
    } else if (type == Item.GOLD_NUGGET) {
      return 6;
    } else if (type == Item.LAPIS_LAZULI) {
      return 9;
    } else {
      return 0;
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

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 2000;
    @Modifiable(Attribute.RANGE)
    private double range = 10;
    @Modifiable(Attribute.RADIUS)
    private double radius = 1;
    private boolean extractOres = true;

    @Override
    public List<String> path() {
      return List.of("abilities", "earth", "earthtunnel");
    }
  }
}
