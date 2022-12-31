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

package me.moros.bending.ability.water.sequence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.ability.water.Torrent;
import me.moros.bending.ability.water.WaterRing;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.common.FragileStructure;
import me.moros.bending.model.ability.common.TravellingSource;
import me.moros.bending.model.ability.common.basic.BlockStream;
import me.moros.bending.model.ability.state.State;
import me.moros.bending.model.ability.state.StateChain;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.predicate.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.Policies;
import me.moros.bending.model.predicate.RemovalPolicy;
import me.moros.bending.model.predicate.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.platform.Direction;
import me.moros.bending.platform.block.Block;
import me.moros.bending.platform.block.BlockType;
import me.moros.bending.platform.entity.Entity;
import me.moros.bending.platform.entity.LivingEntity;
import me.moros.bending.platform.particle.Particle;
import me.moros.bending.platform.particle.ParticleBuilder;
import me.moros.bending.platform.sound.SoundEffect;
import me.moros.bending.temporal.TempBlock;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import me.moros.math.Vector3d;
import me.moros.math.VectorUtil;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class WaterGimbal extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private StateChain states;

  private boolean launched = false;

  public WaterGimbal(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (user.game().abilityManager(user.worldUid()).hasAbility(user, WaterGimbal.class)) {
      return false;
    }

    this.user = user;
    loadConfig();

    WaterRing ring = user.game().abilityManager(user.worldUid()).firstInstance(user, WaterRing.class).orElse(null);
    List<Block> sources = new ArrayList<>();
    if (ring != null && ring.isReady()) {
      sources.addAll(ring.complete());
    }

    if (sources.isEmpty()) {
      Block source = user.find(userConfig.selectRange, WaterMaterials::isFullWaterSource);
      if (source == null) {
        return false;
      }
      sources.add(source);
      states = new StateChain(sources)
        .addState(new TravellingSource(user, BlockType.WATER.defaultState(), 3.5, userConfig.selectRange + 5));
    } else {
      states = new StateChain(sources);
    }

    states.addState(new Gimbal(user))
      .addState(new GimbalStream())
      .start();

    AbilityDescription torrentDesc = user.selectedAbility();
    if (torrentDesc == null) {
      return false;
    }

    removalPolicy = Policies.builder()
      .add(Policies.NOT_SNEAKING)
      .add(ExpireRemovalPolicy.of(30000))
      .add(SwappedSlotsRemovalPolicy.of(torrentDesc))
      .build();

    user.game().abilityManager(user.worldUid()).destroyUserInstances(user, List.of(Torrent.class, WaterRing.class));
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
    return states.update();
  }

  public static void launch(User user) {
    if (user.selectedAbilityName().equals("Torrent")) {
      user.game().abilityManager(user.worldUid()).firstInstance(user, WaterGimbal.class).ifPresent(WaterGimbal::launch);
    }
  }

  private void launch() {
    if (launched) {
      return;
    }
    State state = states.current();
    if (state instanceof Gimbal) {
      launched = true;
      removalPolicy = Policies.builder().build();
      state.complete();
      user.addCooldown(description(), userConfig.cooldown);
    }
  }

  @Override
  public void onDestroy() {
    State current = states.current();
    if (current instanceof GimbalStream gimbalStream) {
      gimbalStream.cleanAll();
    }
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @Override
  public Collection<Collider> colliders() {
    return states.current() instanceof GimbalStream gimbalStream ? gimbalStream.colliders() : List.of();
  }

  private static final class Gimbal implements State {
    private final User user;

    private StateChain chain;
    private Block center;

    private boolean started = false;
    private int angle = 0;

    private Gimbal(User user) {
      this.user = user;
    }

    @Override
    public void start(StateChain chain) {
      if (started) {
        return;
      }
      this.chain = chain;
      started = !chain.chainStore().isEmpty();
    }

    @Override
    public void complete() {
      if (!started) {
        return;
      }
      if (center == null) {
        center = user.world().blockAt(user.location().add(Vector3d.PLUS_J).add(user.direction().withY(0).multiply(2)));
        TempBlock.water().duration(50).build(center);
      }
      chain.chainStore().clear();
      chain.chainStore().addAll(Collections.nCopies(10, center));
      chain.nextState();
    }

    @Override
    public UpdateResult update() {
      if (!started) {
        return UpdateResult.REMOVE;
      }
      if (!user.canBuild(user.eyeBlock())) {
        return UpdateResult.REMOVE;
      }

      Set<Block> ring = new HashSet<>();
      double yaw = Math.toRadians(-user.yaw()) - Math.PI / 2;
      double cos = Math.cos(yaw);
      double sin = Math.sin(yaw);
      Vector3d center = user.location().add(Vector3d.PLUS_J);
      for (int i = 0; i < 2; i++) {
        double theta = Math.toRadians(angle);
        angle += 18;
        if (angle >= 360) {
          angle = 0;
        }
        Vector3d v1 = Vector3d.of(Math.cos(theta), Math.sin(theta), 0).multiply(3.4);
        Vector3d v2 = v1;
        v1 = VectorUtil.rotateAroundAxisX(v1, 0.7, 0.7);
        v1 = VectorUtil.rotateAroundAxisY(v1, cos, sin);
        v2 = VectorUtil.rotateAroundAxisX(v2, 0.7, -0.7);
        v2 = VectorUtil.rotateAroundAxisY(v2, cos, sin);
        Block block1 = user.world().blockAt(center.add(v1));
        Block block2 = user.world().blockAt(center.add(v2));
        if (!ring.contains(block1)) {
          addValidBlock(block1, ring);
        }
        if (!ring.contains(block2)) {
          addValidBlock(block2, ring);
        }
      }
      ring.forEach(this::render);
      return UpdateResult.CONTINUE;
    }

    private void render(Block block) {
      if (MaterialUtil.isWater(block)) {
        ParticleBuilder.bubble(block).spawn(user.world());
      } else if (MaterialUtil.isTransparent(block)) {
        TempBlock.water().duration(150).build(block);
      }
      if (ThreadLocalRandom.current().nextInt(10) == 0) {
        SoundEffect.WATER.play(block);
      }
    }

    private void addValidBlock(Block start, Collection<Block> ring) {
      for (int i = 0; i < 4; i++) {
        Block block = start.offset(Direction.UP, i);
        if (MaterialUtil.isTransparentOrWater(block)) {
          center = block;
          ring.add(block);
          return;
        }
      }
    }
  }

  private class GimbalStream extends BlockStream {
    private final Set<Entity> affectedEntities = new HashSet<>();

    public GimbalStream() {
      super(user, BlockType.WATER, userConfig.range, 16);
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      if (entity instanceof LivingEntity && !affectedEntities.contains(entity)) {
        entity.damage(userConfig.damage, user, description());
        Vector3d velocity = direction.withY(Math.min(direction.y(), userConfig.knockup)).multiply(userConfig.knockback);
        entity.applyVelocity(WaterGimbal.this, velocity);
        affectedEntities.add(entity);
      }
      return false;
    }

    @Override
    protected void renderHead(Block block) {
      Particle.ITEM_SNOWBALL.builder(block.center()).count(6).offset(0.25).extra(0.05).spawn(user.world());
      if (!MaterialUtil.isWater(block)) {
        TempBlock.water().build(block);
      }
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(5) == 0) {
        Block head = stream.peekFirst();
        if (head != null) {
          SoundEffect.WATER.play(head);
        }
      }
    }

    @Override
    public void onBlockHit(Block block) {
      FragileStructure.tryDamageStructure(block, 3, new Ray(block.center(), direction));
    }
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 10000;
    @Modifiable(Attribute.SELECTION)
    private double selectRange = 8;
    @Modifiable(Attribute.RANGE)
    private double range = 24;
    @Modifiable(Attribute.DAMAGE)
    private double damage = 6;
    @Modifiable(Attribute.STRENGTH)
    private double knockback = 1.2;
    @Modifiable(Attribute.STRENGTH)
    private double knockup = 0.25;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "water", "sequences", "watergimbal");
    }
  }
}
