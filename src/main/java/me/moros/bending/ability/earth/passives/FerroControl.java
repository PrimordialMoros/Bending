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

package me.moros.bending.ability.earth.passives;

import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.methods.EntityMethods;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.Minecart;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class FerroControl extends AbilityInstance implements Ability {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Minecart controlledEntity;

  private long nextInteractTime;

  public FerroControl(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    this.user = user;
    loadConfig();
    removalPolicy = Policies.builder().add(Policies.NOT_SNEAKING).build();
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = Bending.configManager().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, description()) || !user.canBend(description()) || !user.hasPermission("bending.metal")) {
      controlledEntity = null;
      return UpdateResult.CONTINUE;
    }

    if (controlledEntity == null || !controlledEntity.isValid()) {
      controlledEntity = (Minecart) user.compositeRayTrace(userConfig.entitySelectRange, Minecart.class).result(user.world()).entity();
    }

    if (controlledEntity != null) {
      if (!user.canBuild(controlledEntity.getLocation().getBlock())) {
        controlledEntity = null;
        return UpdateResult.CONTINUE;
      }
      Vector3d targetLocation = user.eyeLocation().add(user.direction().multiply(userConfig.entityRange));
      Vector3d dir = targetLocation.subtract(new Vector3d(controlledEntity.getLocation()));
      Vector3d velocity = dir.lengthSq() < 1 ? Vector3d.ZERO : dir.normalize().multiply(userConfig.controlSpeed);
      EntityMethods.applyVelocity(this, controlledEntity, velocity);
    }

    return UpdateResult.CONTINUE;
  }

  private void act(Block block) {
    if (!user.canBend(description()) || !user.hasPermission("bending.metal")) {
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
    Openable openable = (Openable) block.getBlockData();
    openable.setOpen(!openable.isOpen());
    block.setBlockData(openable);
    Sound sound;
    if (block.getType() == Material.IRON_DOOR) {
      sound = openable.isOpen() ? Sound.BLOCK_IRON_DOOR_OPEN : Sound.BLOCK_IRON_DOOR_CLOSE;
    } else {
      sound = openable.isOpen() ? Sound.BLOCK_IRON_TRAPDOOR_OPEN : Sound.BLOCK_IRON_TRAPDOOR_CLOSE;
    }
    SoundUtil.playSound(block.getLocation(), sound, 0.5F, 0);
  }

  public static void act(@NonNull User user, @NonNull Block block) {
    if (block.getType() == Material.IRON_DOOR || block.getType() == Material.IRON_TRAPDOOR) {
      Bending.game().abilityManager(user.world()).firstInstance(user, FerroControl.class)
        .ifPresent(ability -> ability.act(block));
    }
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.SELECTION)
    public double blockRange;
    @Modifiable(Attribute.SELECTION)
    public double entitySelectRange;
    @Modifiable(Attribute.RANGE)
    public double entityRange;
    @Modifiable(Attribute.SPEED)
    public double controlSpeed;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "earth", "passives", "ferrocontrol");

      cooldown = abilityNode.node("cooldown").getLong(500);
      blockRange = abilityNode.node("block-range").getDouble(6.0);
      entitySelectRange = abilityNode.node("entity-select-range").getDouble(14.0);
      entityRange = abilityNode.node("entity-control-range").getDouble(8.0);
      controlSpeed = abilityNode.node("entity-control-speed").getDouble(0.8);
    }
  }
}

