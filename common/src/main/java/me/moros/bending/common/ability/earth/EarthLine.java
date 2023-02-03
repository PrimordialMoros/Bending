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
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.MultiUpdatable;
import me.moros.bending.api.ability.Updatable;
import me.moros.bending.api.ability.common.EarthSpike;
import me.moros.bending.api.ability.common.FragileStructure;
import me.moros.bending.api.ability.common.SelectedSource;
import me.moros.bending.api.ability.common.basic.AbstractLine;
import me.moros.bending.api.ability.state.State;
import me.moros.bending.api.ability.state.StateChain;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.collision.geometry.Ray;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.functional.Policies;
import me.moros.bending.api.functional.RemovalPolicy;
import me.moros.bending.api.functional.SwappedSlotsRemovalPolicy;
import me.moros.bending.api.platform.Direction;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.temporal.ActionLimiter;
import me.moros.bending.api.temporal.TempEntity;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.ColorPalette;
import me.moros.bending.api.util.KeyUtil;
import me.moros.bending.api.util.material.EarthMaterials;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.bending.common.config.ConfigManager;
import me.moros.math.Vector3d;
import me.moros.math.VectorUtil;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class EarthLine extends AbilityInstance {
  private enum Mode {NORMAL, PRISON}

  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private StateChain states;
  private Line earthLine;

  public EarthLine(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (method == Activation.ATTACK) {
      user.game().abilityManager(user.worldKey()).firstInstance(user, EarthLine.class).ifPresent(EarthLine::launch);
      return false;
    }
    if (user.onCooldown(description())) {
      return false;
    }

    this.user = user;
    loadConfig();

    Block source = user.find(userConfig.selectRange, b -> EarthMaterials.isEarthNotLava(user, b));
    if (source == null || !MaterialUtil.isTransparent(source.offset(Direction.UP))) {
      return false;
    }
    BlockState fakeData = MaterialUtil.focusedType(source.type()).defaultState();
    Optional<EarthLine> line = user.game().abilityManager(user.worldKey()).firstInstance(user, EarthLine.class);
    if (line.isPresent()) {
      State state = line.get().states.current();
      if (state instanceof SelectedSource.WithState selectedSource) {
        selectedSource.reselect(source, fakeData);
      }
      return false;
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
    if (earthLine != null) {
      earthLine.controllable(user.sneaking());
      return earthLine.update();
    }
    return states.update();
  }

  private void launch() {
    if (earthLine != null) {
      earthLine.raiseSpikes();
      return;
    }
    State state = states.current();
    if (state instanceof SelectedSource) {
      state.complete();
      Block source = states.chainStore().stream().findAny().orElse(null);
      if (source != null) {
        Mode mode = user.store().get(KeyUtil.data("earthline-mode", Mode.class)).orElse(Mode.NORMAL);
        earthLine = new Line(source, mode);
        removalPolicy = Policies.builder().add(SwappedSlotsRemovalPolicy.of(description())).build();
        user.addCooldown(description(), userConfig.cooldown);
      }
    }
  }

  public static void switchMode(User user) {
    if (user.selectedAbilityName().equals("EarthLine")) {
      var key = KeyUtil.data("earthline-mode", Mode.class);
      if (user.store().canEdit(key)) {
        Mode mode = user.store().toggle(key, Mode.NORMAL);
        user.sendActionBar(Component.text("Mode: " + mode.name(), ColorPalette.TEXT_COLOR));
      }
    }
  }

  @Override
  public void onDestroy() {
    State state = states.current();
    if (state instanceof SelectedSource selectedSource) {
      selectedSource.onDestroy();
    }
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @Override
  public Collection<Collider> colliders() {
    return earthLine == null ? List.of() : List.of(earthLine.collider());
  }

  private class Line extends AbstractLine {
    private final Mode mode;
    private boolean raisedSpikes = false;
    private boolean imprisoned = false;

    public Line(Block source, Mode mode) {
      super(user, source, userConfig.range, 0.8, false);
      this.mode = mode;
    }

    @Override
    public void render() {
      double x = ThreadLocalRandom.current().nextDouble(-0.125, 0.125);
      double z = ThreadLocalRandom.current().nextDouble(-0.125, 0.125);
      BlockType type = user.world().blockAt(location).offset(Direction.DOWN).type();
      TempEntity.armorStand(type.asItem().orElse(Item.GRASS_BLOCK)).gravity(false).particles(true).duration(700)
        .build(user.world(), location.subtract(x, 2, z));
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(5) == 0) {
        SoundEffect.EARTH.play(user.world(), location);
      }
      if (raisedSpikes || imprisoned) {
        removalPolicy = (u, d) -> true;
      }
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      double damage = userConfig.damage;
      switch (mode) {
        case NORMAL -> raiseSpikes();
        case PRISON -> {
          imprisonTarget((LivingEntity) entity);
          return true;
        }
      }
      entity.damage(damage, user, description());
      Vector3d velocity = direction.withY(userConfig.knockup).normalize().multiply(userConfig.knockback);
      entity.applyVelocity(EarthLine.this, velocity);
      return true;
    }

    @Override
    public boolean onBlockHit(Block block) {
      return false;
    }

    @Override
    protected boolean isValidBlock(Block block) {
      if (!MaterialUtil.isTransparent(block)) {
        return false;
      }
      Block below = block.offset(Direction.DOWN);
      if (EarthMaterials.isLavaBendable(below) || EarthMaterials.isMetalBendable(below)) {
        return false;
      }
      return EarthMaterials.isEarthbendable(user, below);
    }

    @Override
    protected void onCollision(Vector3d point) {
      FragileStructure.tryDamageStructure(user.world().blockAt(point), 5, new Ray(location, direction));
    }

    public void raiseSpikes() {
      if (mode != Mode.NORMAL || raisedSpikes) {
        return;
      }
      raisedSpikes = true;
      Vector3d loc = location.add(Vector3d.MINUS_J);
      Updatable spikes = MultiUpdatable.builder()
        .add(new EarthSpike(user.world().blockAt(loc), 1, false))
        .add(new EarthSpike(user.world().blockAt(loc.add(direction)), 2, true))
        .build();
      user.game().abilityManager(user.worldKey()).addUpdatable(spikes);
    }

    private void imprisonTarget(LivingEntity entity) {
      if (imprisoned || !entity.valid() || entity.distanceAboveGround(2) > 1.2) {
        return;
      }
      BlockType material = null;
      Block blockToCheck = entity.block().offset(Direction.DOWN);
      if (EarthMaterials.isEarthbendable(user, blockToCheck)) { // Prefer to use the block under the entity first
        material = blockToCheck.type() == BlockType.GRASS_BLOCK ? BlockType.DIRT : blockToCheck.type();
      } else {
        for (Block block : user.world().nearbyBlocks(blockToCheck.center(), 1, b -> EarthMaterials.isEarthbendable(user, b), 1)) {
          material = block.type() == BlockType.GRASS_BLOCK ? BlockType.DIRT : block.type();
        }
      }

      if (material == null) {
        return;
      }

      imprisoned = true;
      entity.applyVelocity(EarthLine.this, Vector3d.MINUS_J);
      Vector3d center = entity.location().add(Vector3d.MINUS_J);
      Vector3d offset = Vector3d.of(0, -0.6, 0);
      var builder = TempEntity.armorStand(material.asItem().orElse(Item.GRASS_BLOCK))
        .gravity(false).duration(userConfig.prisonDuration);
      VectorUtil.circle(Vector3d.PLUS_I.multiply(0.8), Vector3d.PLUS_J, 8).forEach(v -> {
        builder.build(user.world(), center.add(v));
        builder.build(user.world(), center.add(offset).add(v));
      });
      ActionLimiter.builder().duration(userConfig.prisonDuration).build(user, entity);
    }

    public void controllable(boolean value) {
      controllable = value;
    }
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 5000;
    @Modifiable(Attribute.RANGE)
    private double range = 20;
    @Modifiable(Attribute.SELECTION)
    private double selectRange = 6;
    @Modifiable(Attribute.DAMAGE)
    private double damage = 3;
    @Modifiable(Attribute.STRENGTH)
    private double knockback = 1.1;
    @Modifiable(Attribute.STRENGTH)
    private double knockup = 0.55;
    @Modifiable(Attribute.DURATION)
    private long prisonDuration = 1500;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "earth", "earthline");
    }
  }
}
