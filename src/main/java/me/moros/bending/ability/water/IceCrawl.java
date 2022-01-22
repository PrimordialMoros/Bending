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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.Bending;
import me.moros.bending.ability.common.SelectedSource;
import me.moros.bending.ability.common.basic.AbstractLine;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.ActionLimiter;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.game.temporal.TempPacketEntity;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.ActionType;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.state.State;
import me.moros.bending.model.ability.state.StateChain;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class IceCrawl extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private StateChain states;
  private Line iceLine;

  public IceCrawl(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    if (method == Activation.ATTACK) {
      Bending.game().abilityManager(user.world()).firstInstance(user, IceCrawl.class).ifPresent(IceCrawl::launch);
      return false;
    }

    this.user = user;
    loadConfig();

    Block source = user.find(userConfig.selectRange, WaterMaterials::isWaterOrIceBendable);
    if (source == null) {
      return false;
    }

    Optional<IceCrawl> line = Bending.game().abilityManager(user.world()).firstInstance(user, IceCrawl.class);
    if (method == Activation.SNEAK && line.isPresent()) {
      State state = line.get().states.current();
      if (state instanceof SelectedSource selectedSource) {
        selectedSource.reselect(source);
      }
      return false;
    }

    states = new StateChain()
      .addState(new SelectedSource(user, source, userConfig.selectRange))
      .start();

    removalPolicy = Policies.builder().add(SwappedSlotsRemovalPolicy.of(description())).build();
    return true;
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
    if (iceLine != null) {
      return iceLine.update();
    } else {
      return states.update();
    }
  }

  private void launch() {
    if (iceLine != null) {
      return;
    }
    State state = states.current();
    if (state instanceof SelectedSource) {
      state.complete();
      Optional<Block> src = states.chainStore().stream().findAny();
      if (src.isPresent()) {
        iceLine = new Line(src.get());
        removalPolicy = Policies.builder().build();
        user.addCooldown(description(), userConfig.cooldown);
      }
    }
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @Override
  public @NonNull Collection<@NonNull Collider> colliders() {
    return iceLine == null ? List.of() : List.of(iceLine.collider());
  }

  private class Line extends AbstractLine {
    public Line(Block source) {
      super(user, source, userConfig.range, 0.7, true);
      skipVertical = true;
      diagonalsPredicate = MaterialUtil::isTransparentOrWater;
    }

    @Override
    public void render() {
      double x = ThreadLocalRandom.current().nextDouble(-0.125, 0.125);
      double z = ThreadLocalRandom.current().nextDouble(-0.125, 0.125);
      TempPacketEntity.builder(Material.PACKED_ICE.createBlockData()).gravity(false).particles(true).duration(1400)
        .buildArmorStand(user.world(), location.subtract(new Vector3d(x, 2, z)));
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(5) == 0) {
        SoundUtil.ICE.play(user.world(), location);
      }
    }

    @Override
    public boolean onEntityHit(@NonNull Entity entity) {
      DamageUtil.damageEntity(entity, user, userConfig.damage, description());
      if (entity.isValid() && entity instanceof LivingEntity livingEntity) {
        Vector3d spawnLoc = new Vector3d(entity.getLocation()).subtract(new Vector3d(0, 0.2, 0));
        TempPacketEntity.builder(Material.PACKED_ICE.createBlockData()).gravity(false)
          .duration(userConfig.freezeDuration).buildFallingBlock(user.world(), spawnLoc);
        ActionLimiter.builder().limit(ActionType.MOVE).duration(userConfig.freezeDuration).build(user, livingEntity);
      }
      return true;
    }

    @Override
    public boolean onBlockHit(@NonNull Block block) {
      if (MaterialUtil.isWater(block)) {
        TempBlock.ice().duration(BendingProperties.ICE_DURATION).build(block);
      }
      return WorldUtil.tryCoolLava(user, block);
    }

    @Override
    protected boolean isValidBlock(@NonNull Block block) {
      if (!MaterialUtil.isTransparentOrWater(block)) {
        return false;
      }
      Block below = block.getRelative(BlockFace.DOWN);
      return WaterMaterials.isWaterOrIceBendable(below) || !below.isPassable();
    }
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.DURATION)
    public long freezeDuration;
    @Modifiable(Attribute.RANGE)
    public double range;
    @Modifiable(Attribute.SELECTION)
    public double selectRange;
    @Modifiable(Attribute.DAMAGE)
    public double damage;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "water", "icecrawl");

      cooldown = abilityNode.node("cooldown").getLong(6000);
      freezeDuration = abilityNode.node("freeze-duration").getLong(1500);
      range = abilityNode.node("range").getDouble(22.0);
      selectRange = abilityNode.node("select-range").getDouble(8.0);
      damage = abilityNode.node("damage").getDouble(2.0);
    }
  }
}
