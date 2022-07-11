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

package me.moros.bending.ability.earth;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import me.moros.bending.ability.common.FragileStructure;
import me.moros.bending.ability.common.SelectedSource;
import me.moros.bending.ability.common.basic.BlockShot;
import me.moros.bending.config.ConfigManager;
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
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.properties.BendingProperties;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class EarthBlast extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private StateChain states;
  private Blast blast;

  public EarthBlast(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (method == Activation.SNEAK && tryDestroy(user)) {
      return false;
    } else if (method == Activation.ATTACK) {
      Collection<EarthBlast> eblasts = user.game().abilityManager(user.world()).userInstances(user, EarthBlast.class)
        .toList();
      for (EarthBlast eblast : eblasts) {
        if (eblast.blast == null) {
          eblast.launch();
        } else {
          eblast.blast.redirect();
        }
      }
      return false;
    }

    this.user = user;
    loadConfig();

    Predicate<Block> predicate = b -> EarthMaterials.isEarthbendable(user, b) && !b.isLiquid();
    Block source = user.find(userConfig.selectRange, predicate);
    if (source == null) {
      return false;
    }
    BlockData fakeData = MaterialUtil.focusedType(source.getBlockData());

    Collection<EarthBlast> eblasts = user.game().abilityManager(user.world()).userInstances(user, EarthBlast.class)
      .filter(eb -> eb.blast == null).toList();
    for (EarthBlast eblast : eblasts) {
      State state = eblast.states.current();
      if (state instanceof SelectedSource selectedSource) {
        selectedSource.reselect(source, fakeData);
        return false;
      }
    }

    states = new StateChain()
      .addState(new SelectedSource(user, source, userConfig.selectRange, fakeData))
      .start();
    removalPolicy = Policies.builder().add(SwappedSlotsRemovalPolicy.of(description())).build();
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = ConfigManager.calculate(this, config);
  }

  @Override
  public UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    if (blast != null) {
      return blast.update();
    } else {
      return states.update();
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
      if (source == null) {
        return;
      }
      if (EarthMaterials.isEarthbendable(user, source) && !source.isLiquid()) {
        blast = new Blast(source);
        SoundUtil.EARTH.play(source);
        removalPolicy = Policies.builder().build();
        user.addCooldown(description(), userConfig.cooldown);
        TempBlock.air().duration(BendingProperties.instance().earthRevertTime()).build(source);
      }
    }
  }

  private static boolean tryDestroy(User user) {
    Collection<EarthBlast> blasts = user.game().abilityManager(user.world()).instances(EarthBlast.class)
      .filter(eb -> eb.blast != null && !user.equals(eb.user)).toList();
    Ray ray = user.ray(config.shatterRange + 2);
    for (EarthBlast eb : blasts) {
      Vector3d center = eb.blast.center();
      double dist = center.distanceSq(user.eyeLocation());
      if (dist > config.shatterRange * config.shatterRange) {
        continue;
      }
      if (eb.blast.collider().intersects(ray)) {
        Vector3d direction = center.subtract(user.eyeLocation());
        double range = Math.min(1, direction.length());
        Block block = center.toBlock(user.world());
        Block rayTraced = user.rayTrace(range).direction(direction).ignoreLiquids(false).blocks(user.world()).block();
        if (block.equals(rayTraced)) {
          user.game().abilityManager(user.world()).destroyInstance(eb);
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public void onDestroy() {
    State state = states.current();
    if (state instanceof SelectedSource selectedSource) {
      selectedSource.onDestroy();
    }
    if (blast != null) {
      blast.clean();
    }
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @Override
  public Collection<Collider> colliders() {
    return blast == null ? List.of() : List.of(blast.collider());
  }

  private class Blast extends BlockShot {
    private final double damage;

    public Blast(Block block) {
      super(user, block, MaterialUtil.solidType(block.getBlockData()).getMaterial(), userConfig.range, 20);
      if (EarthMaterials.isMetalBendable(block)) {
        damage = BendingProperties.instance().metalModifier(userConfig.damage);
      } else if (EarthMaterials.isLavaBendable(block)) {
        damage = BendingProperties.instance().magmaModifier(userConfig.damage);
      } else {
        damage = userConfig.damage;
      }
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      DamageUtil.damageEntity(entity, user, damage, description());
      EntityUtil.applyVelocity(EarthBlast.this, entity, direction.multiply(0.6));
      return true;
    }

    @Override
    public boolean onBlockHit(Block block) {
      FragileStructure.tryDamageStructure(List.of(block), 4);
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
    private double selectRange = 10;
    @Modifiable(Attribute.DAMAGE)
    private double damage = 2.25;
    private double shatterRange = 14;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "earth", "earthblast");
    }
  }
}
