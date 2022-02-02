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

package me.moros.bending.ability.water.sequence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.Bending;
import me.moros.bending.ability.common.FragileStructure;
import me.moros.bending.ability.common.TravellingSource;
import me.moros.bending.ability.common.basic.BlockStream;
import me.moros.bending.ability.water.Torrent;
import me.moros.bending.ability.water.WaterRing;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.state.State;
import me.moros.bending.model.ability.state.StateChain;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.VectorUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class WaterGimbal extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private StateChain states;

  private boolean launched = false;

  public WaterGimbal(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    if (Bending.game().abilityManager(user.world()).hasAbility(user, WaterGimbal.class)) {
      return false;
    }

    this.user = user;
    loadConfig();

    WaterRing ring = Bending.game().abilityManager(user.world()).firstInstance(user, WaterRing.class).orElse(null);
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
        .addState(new TravellingSource(user, Material.WATER.createBlockData(), 3.5, userConfig.selectRange + 5));
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

    Bending.game().abilityManager(user.world()).destroyInstanceType(user, List.of(Torrent.class, WaterRing.class));
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
    return states.update();
  }

  public static void launch(@NonNull User user) {
    if (user.selectedAbilityName().equals("Torrent")) {
      Bending.game().abilityManager(user.world()).firstInstance(user, WaterGimbal.class).ifPresent(WaterGimbal::launch);
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
  public @NonNull Collection<@NonNull Collider> colliders() {
    return states.current() instanceof GimbalStream gimbalStream ? gimbalStream.colliders() : List.of();
  }

  private static final class Gimbal implements State {
    private final User user;

    private StateChain chain;
    private Block center;

    private boolean started = false;
    private int angle = 0;

    private Gimbal(@NonNull User user) {
      this.user = user;
    }

    @Override
    public void start(@NonNull StateChain chain) {
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
        center = user.location().add(Vector3d.PLUS_J).add(user.direction().setY(0).multiply(2)).toBlock(user.world());
        TempBlock.water().duration(50).build(center);
      }
      chain.chainStore().clear();
      chain.chainStore().addAll(Collections.nCopies(10, center));
      chain.nextState();
    }

    @Override
    public @NonNull UpdateResult update() {
      if (!started) {
        return UpdateResult.REMOVE;
      }
      if (!user.canBuild(user.headBlock())) {
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
        Vector3d v1 = new Vector3d(Math.cos(theta), Math.sin(theta), 0).multiply(3.4);
        Vector3d v2 = new Vector3d(v1.toArray());
        v1 = VectorUtil.rotateAroundAxisX(v1, 0.7, 0.7);
        v1 = VectorUtil.rotateAroundAxisY(v1, cos, sin);
        v2 = VectorUtil.rotateAroundAxisX(v2, 0.7, -0.7);
        v2 = VectorUtil.rotateAroundAxisY(v2, cos, sin);
        Block block1 = center.add(v1).toBlock(user.world());
        Block block2 = center.add(v2).toBlock(user.world());
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
        ParticleUtil.bubble(block).spawn(user.world());
      } else if (MaterialUtil.isTransparent(block)) {
        TempBlock.water().duration(150).build(block);
      }
      if (ThreadLocalRandom.current().nextInt(10) == 0) {
        SoundUtil.WATER.play(block);
      }
    }

    private void addValidBlock(Block start, Collection<Block> ring) {
      for (int i = 0; i < 4; i++) {
        Block block = start.getRelative(BlockFace.UP, i);
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
      super(user, Material.WATER, userConfig.range, 16);
    }

    @Override
    public boolean onEntityHit(@NonNull Entity entity) {
      if (entity instanceof LivingEntity && !affectedEntities.contains(entity)) {
        DamageUtil.damageEntity(entity, user, userConfig.damage, description());
        Vector3d velocity = direction.setY(Math.min(direction.getY(), userConfig.verticalPush));
        EntityUtil.applyVelocity(WaterGimbal.this, entity, velocity);
        affectedEntities.add(entity);
      }
      return false;
    }

    @Override
    protected void renderHead(@NonNull Block block) {
      ParticleUtil.of(Particle.SNOW_SHOVEL, Vector3d.center(block)).count(6).offset(0.25).extra(0.05).spawn(user.world());
      if (!MaterialUtil.isWater(block)) {
        TempBlock.water().build(block);
      }
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(5) == 0) {
        Block head = stream.peekFirst();
        if (head != null) {
          SoundUtil.WATER.play(head);
        }
      }
    }

    @Override
    public void onBlockHit(@NonNull Block block) {
      FragileStructure.tryDamageStructure(List.of(block), 3);
    }
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.SELECTION)
    public double selectRange;
    @Modifiable(Attribute.RANGE)
    public double range;
    @Modifiable(Attribute.DAMAGE)
    public double damage;
    @Modifiable(Attribute.STRENGTH)
    public double knockback;
    @Modifiable(Attribute.STRENGTH)
    public double verticalPush;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "water", "sequences", "watergimbal");

      cooldown = abilityNode.node("cooldown").getLong(10000);
      selectRange = abilityNode.node("select-range").getDouble(8.0);
      range = abilityNode.node("range").getDouble(24.0);
      damage = abilityNode.node("damage").getDouble(6.0);
      knockback = abilityNode.node("knockback").getDouble(1.2);
      verticalPush = abilityNode.node("vertical-push").getDouble(0.25);
    }
  }
}
