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

package me.moros.bending.ability.earth;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

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
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.RayTrace;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.EntityMethods;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class EarthBlast extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private StateChain states;
  private Blast blast;

  public EarthBlast(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    if (method == Activation.SNEAK && tryDestroy(user)) {
      return false;
    } else if (method == Activation.ATTACK) {
      Collection<EarthBlast> eblasts = Bending.game().abilityManager(user.world()).userInstances(user, EarthBlast.class)
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

    Collection<EarthBlast> eblasts = Bending.game().abilityManager(user.world()).userInstances(user, EarthBlast.class)
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
    userConfig = Bending.configManager().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
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
        SoundUtil.EARTH.play(source.getLocation());
        removalPolicy = Policies.builder().build();
        user.addCooldown(description(), userConfig.cooldown);
        TempBlock.createAir(source, BendingProperties.EARTHBENDING_REVERT_TIME);
      }
    }
  }

  private static boolean tryDestroy(User user) {
    Collection<EarthBlast> blasts = Bending.game().abilityManager(user.world()).instances(EarthBlast.class)
      .filter(eb -> eb.blast != null && !user.equals(eb.user)).toList();
    for (EarthBlast eb : blasts) {
      Vector3d center = eb.blast.center();
      double dist = center.distanceSq(user.eyeLocation());
      if (dist > config.shatterRange * config.shatterRange) {
        continue;
      }
      if (eb.blast.collider().intersects(user.ray(dist))) {
        Vector3d direction = center.subtract(user.eyeLocation());
        double range = Math.min(1, direction.length());
        Block block = center.toBlock(user.world());
        Block rayTraced = RayTrace.of(user.eyeLocation(), direction).range(range).ignoreLiquids(false).result(user.world()).block();
        if (block.equals(rayTraced)) {
          Bending.game().abilityManager(user.world()).destroyInstance(eb);
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
  public @NonNull Collection<@NonNull Collider> colliders() {
    return blast == null ? List.of() : List.of(blast.collider());
  }

  private class Blast extends BlockShot {
    private final double damage;

    public Blast(Block block) {
      super(user, block, MaterialUtil.solidType(block.getBlockData()).getMaterial(), userConfig.range, 20);
      if (EarthMaterials.isMetalBendable(block)) {
        damage = userConfig.damage * BendingProperties.METAL_MODIFIER;
      } else if (EarthMaterials.isLavaBendable(block)) {
        damage = userConfig.damage * BendingProperties.MAGMA_MODIFIER;
      } else {
        damage = userConfig.damage;
      }
    }

    @Override
    public boolean onEntityHit(@NonNull Entity entity) {
      DamageUtil.damageEntity(entity, user, damage, description());
      EntityMethods.applyVelocity(EarthBlast.this, entity, direction.multiply(0.6));
      return true;
    }

    @Override
    public boolean onBlockHit(@NonNull Block block) {
      FragileStructure.tryDamageStructure(List.of(block), 4);
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

    public double shatterRange;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "earth", "earthblast");

      cooldown = abilityNode.node("cooldown").getLong(1000);
      range = abilityNode.node("range").getDouble(24.0);
      selectRange = abilityNode.node("select-range").getDouble(10.0);
      damage = abilityNode.node("damage").getDouble(2.25);
      shatterRange = abilityNode.node("max-shatter-range").getDouble(14.0);
    }
  }
}
