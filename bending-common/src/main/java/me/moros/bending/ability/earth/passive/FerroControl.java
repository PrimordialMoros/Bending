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

package me.moros.bending.ability.earth.passive;

import java.util.List;

import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.predicate.Policies;
import me.moros.bending.model.predicate.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.platform.block.Block;
import me.moros.bending.platform.block.BlockState;
import me.moros.bending.platform.block.BlockType;
import me.moros.bending.platform.entity.Entity;
import me.moros.bending.platform.entity.EntityType;
import me.moros.bending.platform.item.Inventory;
import me.moros.bending.platform.property.StateProperty;
import me.moros.bending.platform.sound.Sound;
import me.moros.math.Vector3d;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class FerroControl extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
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
    userConfig = user.game().configProcessor().calculate(this, config);
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
    var open = state.property(StateProperty.OPEN);
    if (open != null) {
      block.setState(state.withProperty(StateProperty.OPEN, !open));
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
      user.game().abilityManager(user.worldUid()).firstInstance(user, FerroControl.class)
        .ifPresent(ability -> ability.act(block));
    }
  }

  private static boolean mayPlaceBlock(User user) {
    Inventory inv = user.inventory();
    return inv != null && inv.canPlaceBlock();
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 500;
    @Modifiable(Attribute.SELECTION)
    private double entitySelectRange = 14;
    @Modifiable(Attribute.RANGE)
    private double entityRange = 8;
    @Modifiable(Attribute.SPEED)
    private double controlSpeed = 0.8;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "earth", "passives", "ferrocontrol");
    }
  }
}

