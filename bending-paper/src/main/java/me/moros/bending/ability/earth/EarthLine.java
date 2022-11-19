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
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.MultiUpdatable;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.ability.common.EarthSpike;
import me.moros.bending.model.ability.common.FragileStructure;
import me.moros.bending.model.ability.common.SelectedSource;
import me.moros.bending.model.ability.common.basic.AbstractLine;
import me.moros.bending.model.ability.state.State;
import me.moros.bending.model.ability.state.StateChain;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.key.RegistryKey;
import me.moros.bending.model.predicate.Policies;
import me.moros.bending.model.predicate.RemovalPolicy;
import me.moros.bending.model.predicate.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.temporal.ActionLimiter;
import me.moros.bending.temporal.TempEntity;
import me.moros.bending.temporal.TempEntity.Builder;
import me.moros.bending.temporal.TempEntity.TempEntityType;
import me.moros.bending.util.ColorPalette;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.math.Vector3d;
import me.moros.math.VectorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
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
      user.game().abilityManager(user.world()).firstInstance(user, EarthLine.class).ifPresent(EarthLine::launch);
      return false;
    }
    if (user.onCooldown(description())) {
      return false;
    }

    this.user = user;
    loadConfig();

    Block source = user.find(userConfig.selectRange, b -> EarthMaterials.isEarthNotLava(user, b));
    if (source == null || !MaterialUtil.isTransparent(source.getRelative(BlockFace.UP))) {
      return false;
    }
    BlockData fakeData = MaterialUtil.focusedType(source.getBlockData());
    Optional<EarthLine> line = user.game().abilityManager(user.world()).firstInstance(user, EarthLine.class);
    if (line.isPresent()) {
      State state = line.get().states.current();
      if (state instanceof SelectedSource selectedSource) {
        selectedSource.reselect(source, fakeData);
      }
      return false;
    }
    states = new StateChain()
      .addState(new SelectedSource(user, source, userConfig.selectRange, fakeData))
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
        Mode mode = user.store().getOrDefault(RegistryKey.create("earthline-mode", Mode.class), Mode.NORMAL);
        earthLine = new Line(source, mode);
        removalPolicy = Policies.builder().add(SwappedSlotsRemovalPolicy.of(description())).build();
        user.addCooldown(description(), userConfig.cooldown);
      }
    }
  }

  public static void switchMode(User user) {
    if (user.selectedAbilityName().equals("EarthLine")) {
      var key = RegistryKey.create("earthline-mode", Mode.class);
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
      BlockData data = location.toBlock(user.world()).getRelative(BlockFace.DOWN).getBlockData();
      TempEntity.builder(data).gravity(false).particles(true).duration(700)
        .build(TempEntityType.ARMOR_STAND, user.world(), location.subtract(x, 2, z));
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(5) == 0) {
        SoundUtil.EARTH.play(user.world(), location);
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
      DamageUtil.damageEntity(entity, user, damage, description());
      Vector3d velocity = direction.withY(userConfig.knockup).normalize().multiply(userConfig.knockback);
      EntityUtil.applyVelocity(EarthLine.this, entity, velocity);
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
      Block below = block.getRelative(BlockFace.DOWN);
      if (EarthMaterials.isLavaBendable(below) || EarthMaterials.isMetalBendable(below)) {
        return false;
      }
      return EarthMaterials.isEarthbendable(user, below);
    }

    @Override
    protected void onCollision(Vector3d point) {
      Block projected = point.toBlock(user.world());
      FragileStructure.tryDamageStructure(projected, 5, new Ray(location, direction));
    }

    public void raiseSpikes() {
      if (mode != Mode.NORMAL || raisedSpikes) {
        return;
      }
      raisedSpikes = true;
      Vector3d loc = location.add(Vector3d.MINUS_J);
      Updatable spikes = MultiUpdatable.builder()
        .add(new EarthSpike(loc.toBlock(user.world()), 1, false))
        .add(new EarthSpike(loc.add(direction).toBlock(user.world()), 2, true))
        .build();
      user.game().abilityManager(user.world()).addUpdatable(spikes);
    }

    private void imprisonTarget(LivingEntity entity) {
      if (imprisoned || !entity.isValid() || EntityUtil.distanceAboveGround(entity, 2) > 1.2) {
        return;
      }
      Material material = null;
      Block blockToCheck = entity.getLocation().getBlock().getRelative(BlockFace.DOWN);
      if (EarthMaterials.isEarthbendable(user, blockToCheck)) { // Prefer to use the block under the entity first
        material = blockToCheck.getType() == Material.GRASS_BLOCK ? Material.DIRT : blockToCheck.getType();
      } else {
        for (Block block : WorldUtil.nearbyBlocks(user.world(), Vector3d.fromCenter(blockToCheck), 1, b -> EarthMaterials.isEarthbendable(user, b), 1)) {
          material = block.getType() == Material.GRASS_BLOCK ? Material.DIRT : block.getType();
        }
      }

      if (material == null) {
        return;
      }

      imprisoned = true;
      EntityUtil.applyVelocity(EarthLine.this, entity, Vector3d.MINUS_J);
      BlockData data = material.createBlockData();
      Vector3d center = Vector3d.from(entity.getLocation()).add(Vector3d.MINUS_J);
      Vector3d offset = Vector3d.of(0, -0.6, 0);
      Builder builder = TempEntity.builder(data).gravity(false).duration(userConfig.prisonDuration);
      VectorUtil.circle(Vector3d.PLUS_I.multiply(0.8), Vector3d.PLUS_J, 8).forEach(v -> {
        builder.build(TempEntityType.ARMOR_STAND, user.world(), center.add(v));
        builder.build(TempEntityType.ARMOR_STAND, user.world(), center.add(offset).add(v));
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
