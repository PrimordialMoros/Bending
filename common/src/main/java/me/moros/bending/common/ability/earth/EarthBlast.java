/*
 * Copyright 2020-2024 Moros
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
import me.moros.bending.api.util.FeaturePermissions;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.functional.SwappedSlotsRemovalPolicy;
import me.moros.bending.api.util.material.EarthMaterials;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.bending.common.ability.fire.Lightning;
import me.moros.bending.common.config.ConfigManager;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class EarthBlast extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

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
        removalPolicy = Policies.defaults();
        user.addCooldown(description(), userConfig.cooldown);
        TempBlock.air().duration(BendingProperties.instance().earthRevertTime()).build(source);
      }
    }
  }

  private static boolean tryDestroy(User user) {
    Collection<EarthBlast> blasts = user.game().abilityManager(user.worldKey()).instances(EarthBlast.class)
      .filter(eb -> eb.blast != null && !user.equals(eb.user) && eb.blast.blastType.canBend(user)).toList();
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
  public Collection<Collider> colliders() {
    return blast == null ? List.of() : List.of(blast.collider());
  }

  @Override
  public void onCollision(Collision collision) {
    if (collision.collidedAbility() instanceof Lightning && blast.electrify()) {
      collision.removeSelf(false);
      collision.removeOther(true);
    }
  }

  private enum BlastType {
    EARTH,
    METAL,
    LAVA;

    private boolean canBend(User user) {
      return switch (this) {
        case EARTH -> true;
        case METAL -> user.hasPermission(FeaturePermissions.METAL);
        case LAVA -> user.hasPermission(FeaturePermissions.LAVA);
      };
    }

    private double calculateDamage(double damage) {
      return switch (this) {
        case EARTH -> damage;
        case METAL -> BendingProperties.instance().metalModifier(damage);
        case LAVA -> BendingProperties.instance().magmaModifier(damage);
      };
    }
  }

  private class Blast extends BlockShot {
    private final BlastType blastType;
    private final double damage;
    private double electrified = 1;

    public Blast(Block block) {
      super(user, block, MaterialUtil.solidType(block.type()), userConfig.range, 20);
      if (EarthMaterials.isMetalBendable(block)) {
        this.blastType = BlastType.METAL;
      } else if (EarthMaterials.isLavaBendable(block)) {
        this.blastType = BlastType.LAVA;
      } else {
        this.blastType = BlastType.EARTH;
      }
      this.damage = blastType.calculateDamage(userConfig.damage);
    }

    @Override
    public void postRender() {
      if (electrified > 1) {
        electrified = Math.max(1, electrified - 0.05);
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
      FragileStructure.tryDamageStructure(block, 4, Ray.of(center(), direction));
      return true;
    }

    private boolean electrify() {
      if (blastType == BlastType.METAL && electrified <= 1) {
        electrified = 2;
        return true;
      }
      return false;
    }
  }

  @ConfigSerializable
  private static final class Config implements Configurable {
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
    public List<String> path() {
      return List.of("abilities", "earth", "earthblast");
    }
  }
}
