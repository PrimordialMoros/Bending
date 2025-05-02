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

package me.moros.bending.common.ability.water;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.ActionType;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.common.FragileStructure;
import me.moros.bending.api.ability.common.SelectedSource;
import me.moros.bending.api.ability.common.basic.AbstractLine;
import me.moros.bending.api.ability.state.State;
import me.moros.bending.api.ability.state.StateChain;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.collision.geometry.Ray;
import me.moros.bending.api.config.BendingProperties;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.Direction;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.bending.api.platform.entity.display.Transformation;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.platform.world.WorldUtil;
import me.moros.bending.api.temporal.ActionLimiter;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.temporal.TempDisplayEntity;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.functional.SwappedSlotsRemovalPolicy;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.bending.api.util.material.WaterMaterials;
import me.moros.math.Vector3d;

public class IceCrawl extends AbilityInstance {
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
      user.game().abilityManager(user.worldKey()).firstInstance(user, IceCrawl.class).ifPresent(IceCrawl::launch);
      return false;
    }

    this.user = user;
    loadConfig();

    Block source = user.find(userConfig.selectRange, WaterMaterials::isWaterOrIceBendable);
    if (source == null) {
      return false;
    }

    Optional<IceCrawl> line = user.game().abilityManager(user.worldKey()).firstInstance(user, IceCrawl.class);
    if (method == Activation.SNEAK && line.isPresent()) {
      State state = line.get().states.current();
      if (state instanceof SelectedSource selectedSource) {
        selectedSource.reselect(source);
      }
      return false;
    }

    states = new StateChain()
      .addState(SelectedSource.create(user, source, userConfig.selectRange))
      .start();

    removalPolicy = Policies.builder().add(SwappedSlotsRemovalPolicy.of(description())).build();
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = user.game().configProcessor().calculate(this, Config.class);
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
        removalPolicy = Policies.defaults();
        user.addCooldown(description(), userConfig.cooldown);
      }
    }
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
    public void render(Vector3d location) {
      double x = ThreadLocalRandom.current().nextDouble(-0.125, 0.125);
      double z = ThreadLocalRandom.current().nextDouble(-0.125, 0.125);
      Vector3d spawnLoc = location.add(x, -0.75, z);
      TempDisplayEntity.builder(BlockType.PACKED_ICE).gravity(true).duration(1200)
        .velocity(Vector3d.of(0, 0.16, 0))
        .edit(d -> d.transformation(Transformation.scaled(0.75)))
        .build(user.world(), spawnLoc);
      BlockType.PACKED_ICE.asParticle(location).count(6).offset(0.25, 0.125, 0.25).spawn(user.world());
    }

    @Override
    public void postRender(Vector3d location) {
      if (ThreadLocalRandom.current().nextInt(5) == 0) {
        SoundEffect.ICE.play(user.world(), location);
      }
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      entity.damage(userConfig.damage, user, description());
      if (entity.valid() && entity instanceof LivingEntity livingEntity) {
        TempDisplayEntity.builder(BlockType.PACKED_ICE).duration(userConfig.freezeDuration)
          .build(user.world(), entity.location().add(0, -0.2, 0));
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
      FragileStructure.tryDamageStructure(user.world().blockAt(point), 5, Ray.of(collider.position(), direction));
    }
  }

  private static final class Config implements Configurable {
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
    public List<String> path() {
      return List.of("abilities", "water", "icecrawl");
    }
  }
}
