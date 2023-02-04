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

package me.moros.bending.common.ability.earth;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.common.FragileStructure;
import me.moros.bending.api.ability.common.SelectedSource;
import me.moros.bending.api.ability.common.basic.BlockShot;
import me.moros.bending.api.ability.state.State;
import me.moros.bending.api.ability.state.StateChain;
import me.moros.bending.api.collision.Collision;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.collision.geometry.Ray;
import me.moros.bending.api.config.BendingProperties;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.particle.Particle;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.functional.SwappedSlotsRemovalPolicy;
import me.moros.bending.api.util.material.EarthMaterials;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.bending.common.ability.fire.Lightning;
import me.moros.bending.common.config.ConfigManager;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
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
      Collection<EarthBlast> eblasts = user.game().abilityManager(user.worldKey()).userInstances(user, EarthBlast.class)
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

    Predicate<Block> predicate = b -> EarthMaterials.isEarthbendable(user, b) && !b.type().isLiquid();
    Block source = user.find(userConfig.selectRange, predicate);
    if (source == null) {
      return false;
    }
    BlockState fakeData = MaterialUtil.focusedType(source.type()).defaultState();

    Collection<EarthBlast> eblasts = user.game().abilityManager(user.worldKey()).userInstances(user, EarthBlast.class)
      .filter(eb -> eb.blast == null).toList();
    for (EarthBlast eblast : eblasts) {
      State state = eblast.states.current();
      if (state instanceof SelectedSource.WithState selectedSource) {
        selectedSource.reselect(source, fakeData);
        return false;
      }
    }

    states = new StateChain()
      .addState(SelectedSource.create(user, source, userConfig.selectRange, fakeData))
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
    return blast == null ? states.update() : blast.update();
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
      if (EarthMaterials.isEarthbendable(user, source) && !source.type().isLiquid()) {
        blast = new Blast(source);
        SoundEffect.EARTH.play(source);
        removalPolicy = Policies.builder().build();
        user.addCooldown(description(), userConfig.cooldown);
        TempBlock.air().duration(BendingProperties.instance().earthRevertTime()).build(source);
      }
    }
  }

  private static boolean tryDestroy(User user) {
    Collection<EarthBlast> blasts = user.game().abilityManager(user.worldKey()).instances(EarthBlast.class)
      .filter(eb -> eb.blast != null && !user.equals(eb.user)).toList();
    Ray ray = user.ray(config.shatterRange + 2);
    double distSq = config.shatterRange * config.shatterRange;
    for (EarthBlast eb : blasts) {
      Vector3d center = eb.blast.center();
      if (center.distanceSq(user.eyeLocation()) <= distSq && eb.blast.collider().intersects(ray)) {
        Vector3d direction = center.subtract(user.eyeLocation());
        double range = Math.min(1, direction.length());
        Block rayTraced = user.rayTrace(range).direction(direction).ignoreLiquids(false).blocks(user.world()).block();
        if (user.world().blockAt(center).equals(rayTraced)) {
          user.game().abilityManager(user.worldKey()).destroyInstance(eb);
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

  @Override
  public void onCollision(Collision collision) {
    if (collision.collidedAbility() instanceof Lightning) {
      blast.electrify();
    }
  }

  private class Blast extends BlockShot {
    private final double damage;
    private double electrified = 1;

    public Blast(Block block) {
      super(user, block, MaterialUtil.solidType(block.type()), userConfig.range, 20);
      if (EarthMaterials.isMetalBendable(block)) {
        damage = BendingProperties.instance().metalModifier(userConfig.damage);
      } else if (EarthMaterials.isLavaBendable(block)) {
        damage = BendingProperties.instance().magmaModifier(userConfig.damage);
      } else {
        damage = userConfig.damage;
      }
    }

    @Override
    public void postRender() {
      electrified = Math.max(1, electrified - 0.05);
      if (electrified > 1) {
        Vector3d center = center();
        if (ThreadLocalRandom.current().nextInt(5) == 0) {
          SoundEffect.LIGHTNING.play(user.world(), center);
        }
        int particles = FastMath.ceil(24 * (electrified - 1));
        Particle.ELECTRIC_SPARK.builder(center).offset(0.5).count(particles).spawn(user.world());
      }
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      entity.damage(electrified * damage, user, description());
      entity.applyVelocity(EarthBlast.this, direction.multiply(0.6));
      return true;
    }

    @Override
    public boolean onBlockHit(Block block) {
      FragileStructure.tryDamageStructure(block, 4, new Ray(center(), direction));
      return true;
    }

    private void electrify() {
      if (electrified <= 1 && EarthMaterials.METAL_BENDABLE.isTagged(type)) {
        electrified = 2;
      }
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
