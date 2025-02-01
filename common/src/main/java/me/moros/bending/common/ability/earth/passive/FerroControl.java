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

package me.moros.bending.common.ability.earth.passive;

import java.util.List;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.block.BlockStateProperties;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.EntityType;
import me.moros.bending.api.platform.item.Inventory;
import me.moros.bending.api.platform.sound.Sound;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.math.Vector3d;

public class FerroControl extends AbilityInstance {
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Entity controlledEntity;

  private long nextInteractTime;

  public FerroControl(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    loadConfig();
    removalPolicy = Policies.builder().add(Policies.NOT_SNEAKING).build();
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = user.game().configProcessor().calculate(this, Config.class);
  }

  @Override
  public UpdateResult update() {
    if (removalPolicy.test(user, description()) || !user.canBend(description())) {
      controlledEntity = null;
      return UpdateResult.CONTINUE;
    }

    if (controlledEntity == null || !controlledEntity.valid()) {
      var result = user.rayTrace(userConfig.entitySelectRange).cast(user.world()).entity();
      controlledEntity = (result != null && result.type() == EntityType.MINECART) ? result : null;
    }

    if (controlledEntity != null) {
      if (!user.canBuild(controlledEntity.block())) {
        controlledEntity = null;
        return UpdateResult.CONTINUE;
      }
      Vector3d targetLocation = user.eyeLocation().add(user.direction().multiply(userConfig.entityRange));
      Vector3d dir = targetLocation.subtract(controlledEntity.location());
      Vector3d velocity = dir.lengthSq() < 1 ? Vector3d.ZERO : dir.normalize().multiply(userConfig.controlSpeed);
      controlledEntity.applyVelocity(this, velocity);
    }

    return UpdateResult.CONTINUE;
  }

  private void act(Block block) {
    if (!user.canBend(description())) {
      return;
    }
    long time = System.currentTimeMillis();
    if (time < nextInteractTime) {
      return;
    }
    nextInteractTime = time + userConfig.cooldown;
    if (!user.canBuild(block)) {
      return;
    }
    BlockState state = block.state();
    var open = state.property(BlockStateProperties.OPEN);
    if (open != null) {
      block.setState(state.withProperty(BlockStateProperties.OPEN, !open));
      Sound sound;
      if (state.type() == BlockType.IRON_DOOR) {
        sound = !open ? Sound.BLOCK_IRON_DOOR_OPEN : Sound.BLOCK_IRON_DOOR_CLOSE;
      } else {
        sound = !open ? Sound.BLOCK_IRON_TRAPDOOR_OPEN : Sound.BLOCK_IRON_TRAPDOOR_CLOSE;
      }
      sound.asEffect(0.5F, 0).play(block);
    }
  }

  public static void act(User user, Block block) {
    if (block.type() == BlockType.IRON_DOOR || block.type() == BlockType.IRON_TRAPDOOR) {
      if (user.sneaking() && mayPlaceBlock(user)) {
        return;
      }
      user.game().abilityManager(user.worldKey()).firstInstance(user, FerroControl.class)
        .ifPresent(ability -> ability.act(block));
    }
  }

  private static boolean mayPlaceBlock(User user) {
    Inventory inv = user.inventory();
    return inv != null && inv.canPlaceBlock();
  }

  private static final class Config implements Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 500;
    @Modifiable(Attribute.SELECTION)
    private double entitySelectRange = 14;
    @Modifiable(Attribute.RANGE)
    private double entityRange = 8;
    @Modifiable(Attribute.SPEED)
    private double controlSpeed = 0.8;

    @Override
    public List<String> path() {
      return List.of("abilities", "earth", "passives", "ferrocontrol");
    }
  }
}

