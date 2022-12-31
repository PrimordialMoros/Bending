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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.common.FragileStructure;
import me.moros.bending.model.ability.common.SelectedSource;
import me.moros.bending.model.ability.common.basic.BlockShot;
import me.moros.bending.model.ability.state.State;
import me.moros.bending.model.ability.state.StateChain;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.predicate.Policies;
import me.moros.bending.model.predicate.RemovalPolicy;
import me.moros.bending.model.predicate.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.platform.block.Block;
import me.moros.bending.platform.block.BlockType;
import me.moros.bending.platform.entity.Entity;
import me.moros.bending.platform.particle.Particle;
import me.moros.bending.platform.sound.SoundEffect;
import me.moros.bending.temporal.TempBlock;
import me.moros.bending.util.BendingEffect;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import me.moros.math.Vector3d;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

public class WaterManipulation extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private StateChain states;
  private Manip manip;
  private final Deque<Block> trail = new ArrayDeque<>(2);

  private boolean isIce;

  public WaterManipulation(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (method == Activation.ATTACK) {
      Collection<WaterManipulation> manips = user.game().abilityManager(user.worldUid()).userInstances(user, WaterManipulation.class)
        .toList();
      redirectAny(user);
      for (WaterManipulation manip : manips) {
        if (manip.manip == null) {
          manip.launch();
        } else {
          manip.manip.redirect();
        }
      }
      return false;
    }

    this.user = user;
    loadConfig();

    Block source = user.find(userConfig.selectRange, WaterMaterials::isWaterBendable);
    if (source == null) {
      return false;
    }

    Collection<WaterManipulation> manips = user.game().abilityManager(user.worldUid()).userInstances(user, WaterManipulation.class)
      .filter(m -> m.manip == null).toList();
    for (WaterManipulation manip : manips) {
      State state = manip.states.current();
      if (state instanceof SelectedSource selectedSource) {
        selectedSource.reselect(source);
        return false;
      }
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
    return manip == null ? states.update() : manip.update();
  }

  private void renderTrail(@Nullable Block block, int level) {
    if (block == null) {
      return;
    }
    if (MaterialUtil.isTransparentOrWater(block)) {
      WorldUtil.tryBreakPlant(block);
      if (!MaterialUtil.isWater(block)) {
        TempBlock.builder(MaterialUtil.waterData(level)).build(block);
      }
    }
  }

  private void launch() {
    if (user.onCooldown(description())) {
      return;
    }
    State state = states.current();
    if (state instanceof SelectedSource) {
      state.complete();
      Block source = states.chainStore().stream().findAny().orElse(null);
      if (source == null || !TempBlock.isBendable(source)) {
        return;
      }
      if (WaterMaterials.isWaterBendable(source)) {
        isIce = WaterMaterials.isIceBendable(source);
        manip = new Manip(source);
        removalPolicy = Policies.builder().build();
        user.addCooldown(description(), userConfig.cooldown);
        TempBlock.air().build(source);
      }
    }
  }

  private static void redirectAny(User user) {
    Collection<WaterManipulation> manips = user.game().abilityManager(user.worldUid()).instances(WaterManipulation.class)
      .filter(m -> m.manip != null && !user.equals(m.user)).toList();
    Ray ray = user.ray(config.maxRedirectRange + 2);
    double minSq = config.noRedirectRange * config.noRedirectRange;
    double maxSq = config.maxRedirectRange * config.maxRedirectRange;
    for (WaterManipulation manip : manips) {
      Vector3d center = manip.manip.center();
      double dist = center.distance(manip.user().eyeLocation());
      if (dist * dist < minSq || center.distanceSq(user.eyeLocation()) > maxSq) {
        continue;
      }
      Collider selectSphere = new Sphere(center, config.redirectGrabRadius);
      if (selectSphere.intersects(ray)) {
        Vector3d direction = center.subtract(user.eyeLocation());
        double range = Math.min(1, direction.length());
        Block rayTraced = user.rayTrace(range).direction(direction).ignoreLiquids(false).blocks(user.world()).block();
        if (user.world().blockAt(center).equals(rayTraced)) {
          user.game().abilityManager(user.worldUid()).changeOwner(manip, user);
          manip.manip.redirect();
        }
      }
    }
  }

  @Override
  public void onDestroy() {
    if (manip != null) {
      trail.forEach(manip::clean);
      manip.clean();
    }
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @Override
  public void onUserChange(User newUser) {
    this.user = newUser;
    manip.user(newUser);
  }

  @Override
  public Collection<Collider> colliders() {
    return manip == null ? List.of() : List.of(manip.collider());
  }

  private class Manip extends BlockShot {
    public Manip(Block block) {
      super(user, block, isIce ? block.type() : BlockType.WATER, userConfig.range, isIce ? 16 : 20);
      allowUnderWater = true;
    }

    @Override
    public void postRender() {
      Vector3d center = center();
      if (ThreadLocalRandom.current().nextInt(5) == 0) {
        SoundEffect effect = isIce ? SoundEffect.ICE : SoundEffect.WATER;
        effect.play(user.world(), center);
      }
      if (isIce) {
        Particle.ITEM_SNOWBALL.builder(center).count(8).offset(0.4).spawn(user.world());
      } else {
        Block trail1 = previousBlock();
        if (trail1 != null) {
          if (!trail.isEmpty()) {
            clean(trail.peekFirst());
          }
          if (trail.size() == 2) {
            clean(trail.removeLast());
          }
          trail.addFirst(trail1);
          renderTrail(trail1, 7);
          renderTrail(trail.peekLast(), 6);
        }
      }
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      if (isIce) {
        BendingEffect.FROST_TICK.apply(user, entity, userConfig.freezeTicks);
      }
      entity.damage(userConfig.damage, user, description());
      entity.applyVelocity(WaterManipulation.this, direction.multiply(0.5));
      return true;
    }

    @Override
    public boolean onBlockHit(Block block) {
      FragileStructure.tryDamageStructure(block, 3, new Ray(center(), direction));
      return true;
    }
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 1000;
    @Modifiable(Attribute.RANGE)
    private double range = 24;
    @Modifiable(Attribute.SELECTION)
    private double selectRange = 12;
    @Modifiable(Attribute.DAMAGE)
    private double damage = 2;
    @Modifiable(Attribute.FREEZE_TICKS)
    private int freezeTicks = 60;
    private double redirectGrabRadius = 2;
    @Comment("Manips within that distance from the bender who controls them cannot be redirected")
    private double noRedirectRange = 5;
    private double maxRedirectRange = 20;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "water", "watermanipulation");
    }
  }
}
