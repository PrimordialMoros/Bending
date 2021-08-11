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

package me.moros.bending.ability.water;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.FragileStructure;
import me.moros.bending.ability.common.SelectedSource;
import me.moros.bending.ability.common.basic.BlockShot;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.state.State;
import me.moros.bending.model.ability.state.StateChain;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingEffect;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.RayTrace;
import me.moros.bending.util.SoundEffect;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.EntityMethods;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;

public class WaterManipulation extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private StateChain states;
  private Manip manip;
  private final Deque<Block> trail = new ArrayDeque<>(2);

  private boolean isIce;

  public WaterManipulation(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    if (method == Activation.ATTACK) {
      Collection<WaterManipulation> manips = Bending.game().abilityManager(user.world()).userInstances(user, WaterManipulation.class)
        .collect(Collectors.toList());
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

    Collection<WaterManipulation> manips = Bending.game().abilityManager(user.world()).userInstances(user, WaterManipulation.class)
      .filter(m -> m.manip == null).collect(Collectors.toList());
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
    userConfig = Bending.configManager().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    if (manip != null) {
      UpdateResult result = manip.update();
      if (result == UpdateResult.CONTINUE) {
        SoundEffect effect = isIce ? SoundUtil.ICE : SoundUtil.WATER;
        if (ThreadLocalRandom.current().nextInt(5) == 0) {
          effect.play(manip.center().toLocation(user.world()));
        }

        if (isIce) {
          Location center = manip.center().toLocation(user.world());
          ParticleUtil.create(Particle.ITEM_CRACK, center).count(4)
            .offset(0.4, 0.4, 0.4).data(new ItemStack(Material.ICE)).spawn();
          ParticleUtil.create(Particle.SNOW_SHOVEL, center).count(6)
            .offset(0.4, 0.4, 0.4).spawn();
        } else {
          Block trail1 = manip.previousBlock();
          if (trail1 != null) {
            if (!trail.isEmpty()) {
              manip.clean(trail.peekFirst());
            }
            if (trail.size() == 2) {
              manip.clean(trail.removeLast());
            }
            trail.addFirst(trail1);
            renderTrail(trail1, 7);
            renderTrail(trail.peekLast(), 6);
          }
        }
      }
      return result;
    } else {
      return states.update();
    }
  }

  private void renderTrail(Block block, int level) {
    if (block == null) {
      return;
    }
    if (MaterialUtil.isTransparentOrWater(block)) {
      BlockMethods.tryBreakPlant(block);
      if (!MaterialUtil.isWater(block)) {
        TempBlock.create(block, MaterialUtil.waterData(level));
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
        TempBlock.createAir(source);
      }
    }
  }

  private static void redirectAny(User user) {
    Collection<WaterManipulation> manips = Bending.game().abilityManager(user.world()).instances(WaterManipulation.class)
      .filter(m -> m.manip != null && !user.equals(m.user)).collect(Collectors.toList());
    for (WaterManipulation manip : manips) {
      Vector3d center = manip.manip.center();
      double dist = center.distanceSq(manip.user().eyeLocation());
      double dist2 = center.distanceSq(user.eyeLocation());
      if (dist < config.rMin * config.rMin || dist2 > config.rMax * config.rMax) {
        continue;
      }
      Sphere selectSphere = new Sphere(center, config.redirectGrabRadius);
      if (selectSphere.intersects(user.ray(dist))) {
        Vector3d direction = center.subtract(user.eyeLocation());
        double range = Math.min(1, direction.length());
        Block rayTraced = RayTrace.of(user.eyeLocation(), direction).range(range).ignoreLiquids(false).result(user.world()).block();
        if (center.toBlock(user.world()).equals(rayTraced)) {
          Bending.game().abilityManager(user.world()).changeOwner(manip, user);
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
  public void onUserChange(@NonNull User newUser) {
    this.user = newUser;
    manip.user(newUser);
  }

  @Override
  public @NonNull Collection<@NonNull Collider> colliders() {
    return manip == null ? List.of() : List.of(manip.collider());
  }

  private class Manip extends BlockShot {
    public Manip(Block block) {
      super(user, block, isIce ? block.getType() : Material.WATER, userConfig.range, isIce ? 16 : 20);
      allowUnderWater = true;
    }

    @Override
    public boolean onEntityHit(@NonNull Entity entity) {
      if (isIce) {
        BendingEffect.FROST_TICK.apply(user, entity, userConfig.freezeTicks);
      }
      DamageUtil.damageEntity(entity, user, userConfig.damage, description());
      EntityMethods.applyVelocity(WaterManipulation.this, entity, direction.multiply(0.5));
      return true;
    }

    @Override
    public boolean onBlockHit(@NonNull Block block) {
      FragileStructure.tryDamageStructure(List.of(block), 3);
      return true;
    }
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.RANGE)
    public double range;
    @Modifiable(Attribute.SELECTION)
    public double selectRange;
    @Modifiable(Attribute.DAMAGE)
    public double damage;
    @Modifiable(Attribute.FREEZE_TICKS)
    public int freezeTicks;

    public double redirectGrabRadius;
    public double rMin;
    public double rMax;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "water", "watermanipulation");

      cooldown = abilityNode.node("cooldown").getLong(1000);
      range = abilityNode.node("range").getDouble(24.0);
      selectRange = abilityNode.node("select-range").getDouble(12.0);
      damage = abilityNode.node("damage").getDouble(2.0);
      freezeTicks = abilityNode.node("iceblast-freeze-ticks").getInt(60);

      redirectGrabRadius = abilityNode.node("redirect-grab-radius").getDouble(2.0);
      rMin = abilityNode.node("no-redirect-range").getDouble(5.0);
      rMax = abilityNode.node("max-redirect-range").getDouble(20.0);

      abilityNode.node("no-redirect-range").comment("Manips within that distance from the bender who controls them cannot be redirected.");
    }
  }
}
