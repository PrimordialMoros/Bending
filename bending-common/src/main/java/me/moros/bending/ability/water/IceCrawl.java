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

package me.moros.bending.ability.water;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.BendingProperties;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.ActionType;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.common.FragileStructure;
import me.moros.bending.model.ability.common.SelectedSource;
import me.moros.bending.model.ability.common.basic.AbstractLine;
import me.moros.bending.model.ability.state.State;
import me.moros.bending.model.ability.state.StateChain;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.predicate.Policies;
import me.moros.bending.model.predicate.RemovalPolicy;
import me.moros.bending.model.predicate.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.platform.Direction;
import me.moros.bending.platform.block.Block;
import me.moros.bending.platform.block.BlockType;
import me.moros.bending.platform.entity.Entity;
import me.moros.bending.platform.entity.LivingEntity;
import me.moros.bending.platform.item.Item;
import me.moros.bending.platform.sound.SoundEffect;
import me.moros.bending.temporal.ActionLimiter;
import me.moros.bending.temporal.TempBlock;
import me.moros.bending.temporal.TempEntity;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import me.moros.math.Vector3d;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class IceCrawl extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private StateChain states;
  private Line iceLine;

  public IceCrawl(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (method == Activation.ATTACK) {
      user.game().abilityManager(user.worldUid()).firstInstance(user, IceCrawl.class).ifPresent(IceCrawl::launch);
      return false;
    }

    this.user = user;
    loadConfig();

    Block source = user.find(userConfig.selectRange, WaterMaterials::isWaterOrIceBendable);
    if (source == null) {
      return false;
    }

    Optional<IceCrawl> line = user.game().abilityManager(user.worldUid()).firstInstance(user, IceCrawl.class);
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
    userConfig = user.game().configProcessor().calculate(this, config);
  }

  @Override
  public UpdateResult update() {
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
  public Collection<Collider> colliders() {
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
      TempEntity.armorStand(Item.PACKED_ICE).gravity(false).particles(true).duration(1400)
        .build(user.world(), location.subtract(x, 2, z));
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(5) == 0) {
        SoundEffect.ICE.play(user.world(), location);
      }
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      entity.damage(userConfig.damage, user, description());
      if (entity.valid() && entity instanceof LivingEntity livingEntity) {
        Vector3d spawnLoc = entity.location().subtract(0, 0.2, 0);
        TempEntity.fallingBlock(BlockType.PACKED_ICE.defaultState()).gravity(false)
          .duration(userConfig.freezeDuration).build(user.world(), spawnLoc);
        ActionLimiter.builder().limit(ActionType.MOVE).duration(userConfig.freezeDuration).build(user, livingEntity);
      }
      return true;
    }

    @Override
    public boolean onBlockHit(Block block) {
      if (MaterialUtil.isWater(block)) {
        TempBlock.ice().duration(BendingProperties.instance().iceRevertTime()).build(block);
      }
      return WorldUtil.tryCoolLava(user, block);
    }

    @Override
    protected boolean isValidBlock(Block block) {
      if (!MaterialUtil.isTransparentOrWater(block)) {
        return false;
      }
      Block below = block.offset(Direction.DOWN);
      return WaterMaterials.isWaterOrIceBendable(below) || below.type().isCollidable();
    }

    @Override
    protected void onCollision(Vector3d point) {
      FragileStructure.tryDamageStructure(user.world().blockAt(point), 5, new Ray(location, direction));
    }
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 6000;
    @Modifiable(Attribute.DURATION)
    private long freezeDuration = 1500;
    @Modifiable(Attribute.RANGE)
    private double range = 22;
    @Modifiable(Attribute.SELECTION)
    private double selectRange = 8;
    @Modifiable(Attribute.DAMAGE)
    private double damage = 2;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "water", "icecrawl");
    }
  }
}
