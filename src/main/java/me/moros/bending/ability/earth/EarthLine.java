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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import me.moros.bending.Bending;
import me.moros.bending.ability.common.FragileStructure;
import me.moros.bending.ability.common.SelectedSource;
import me.moros.bending.ability.common.basic.AbstractLine;
import me.moros.bending.ability.earth.util.EarthSpike;
import me.moros.bending.ability.earth.util.Fracture;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.ActionLimiter;
import me.moros.bending.game.temporal.TempPacketEntity;
import me.moros.bending.game.temporal.TempPacketEntity.Builder;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.state.State;
import me.moros.bending.model.ability.state.StateChain;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.DataKey;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingEffect;
import me.moros.bending.util.BendingExplosion;
import me.moros.bending.util.ColorPalette;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.VectorUtil;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class EarthLine extends AbilityInstance {
  private enum Mode {NORMAL, PRISON, MAGMA}

  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private StateChain states;
  private Line earthLine;

  public EarthLine(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    if (method == Activation.ATTACK) {
      Bending.game().abilityManager(user.world()).firstInstance(user, EarthLine.class).ifPresent(EarthLine::launch);
      return false;
    }
    if (user.onCooldown(description())) {
      return false;
    }

    this.user = user;
    loadConfig();

    Block source = user.find(userConfig.selectRange, b -> EarthMaterials.isEarthbendable(user, b));
    if (source == null || !MaterialUtil.isTransparent(source.getRelative(BlockFace.UP))) {
      return false;
    }
    BlockData fakeData = MaterialUtil.focusedType(source.getBlockData());
    Optional<EarthLine> line = Bending.game().abilityManager(user.world()).firstInstance(user, EarthLine.class);
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
    userConfig = Bending.configManager().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    if (earthLine != null) {
      earthLine.controllable(user.sneaking());
      if (earthLine.update() == UpdateResult.CONTINUE) {
        return UpdateResult.CONTINUE;
      }
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
      if (source == null) {
        return;
      }
      var key = DataKey.of("earthline-mode", Mode.class);
      Mode mode = EarthMaterials.isLavaBendable(source) ? Mode.MAGMA : user.store().getOrDefault(key, Mode.NORMAL);
      earthLine = new Line(source, mode);
      removalPolicy = Policies.builder().add(SwappedSlotsRemovalPolicy.of(description())).build();
      user.addCooldown(description(), userConfig.cooldown);
    }
  }

  public static void switchMode(@NonNull User user) {
    if (user.selectedAbilityName().equals("EarthLine")) {
      var key = DataKey.of("earthline-mode", Mode.class);
      if (user.store().canEdit(key)) {
        Mode mode = user.store().merge(key, Mode.PRISON, (m1, m2) -> m1 == Mode.PRISON ? Mode.NORMAL : Mode.PRISON);
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
  public @NonNull Collection<@NonNull Collider> colliders() {
    return earthLine == null ? List.of() : List.of(earthLine.collider());
  }

  private class Line extends AbstractLine {
    private final Mode mode;
    private boolean raisedSpikes = false;
    private boolean imprisoned = false;

    public Line(Block source, Mode mode) {
      super(user, source, userConfig.range, mode == Mode.MAGMA ? 0.6 : 0.8, false);
      this.mode = mode;
    }

    @Override
    public void render() {
      double x = ThreadLocalRandom.current().nextDouble(-0.125, 0.125);
      double z = ThreadLocalRandom.current().nextDouble(-0.125, 0.125);
      BlockData data = mode == Mode.MAGMA ? Material.MAGMA_BLOCK.createBlockData() : location.toBlock(user.world()).getRelative(BlockFace.DOWN).getBlockData();
      TempPacketEntity.builder(data).gravity(false).particles(true).duration(700)
        .buildArmorStand(user.world(), location.subtract(new Vector3d(x, 2, z)));
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(5) == 0) {
        SoundUtil.EARTH.play(user.world(), location);
      }
    }

    @Override
    public boolean onEntityHit(@NonNull Entity entity) {
      double damage = userConfig.damage;
      switch (mode) {
        case NORMAL -> raiseSpikes();
        case PRISON -> {
          imprisonTarget((LivingEntity) entity);
          return true;
        }
        case MAGMA -> {
          damage = Bending.properties().magmaModifier(userConfig.damage);
          BendingEffect.FIRE_TICK.apply(user, entity);
        }
      }
      DamageUtil.damageEntity(entity, user, damage, description());
      Vector3d velocity = direction.setY(userConfig.knockup).normalize().multiply(userConfig.knockback);
      EntityUtil.applyVelocity(EarthLine.this, entity, velocity);
      return true;
    }

    @Override
    public boolean onBlockHit(@NonNull Block block) {
      if (MaterialUtil.isWater(block)) {
        if (mode == Mode.MAGMA) {
          WorldUtil.playLavaExtinguishEffect(block);
          return true;
        }
      }
      return false;
    }

    @Override
    protected boolean isValidBlock(@NonNull Block block) {
      if (!MaterialUtil.isTransparent(block)) {
        return false;
      }
      Block below = block.getRelative(BlockFace.DOWN);
      if (mode != Mode.MAGMA && MaterialUtil.isLava(below)) {
        return false;
      }
      if (mode == Mode.MAGMA && EarthMaterials.isMetalBendable(below)) {
        return false;
      }
      return EarthMaterials.isEarthbendable(user, below);
    }

    @Override
    protected void onCollision() {
      FragileStructure.tryDamageStructure(List.of(location.toBlock(user.world())), mode == Mode.MAGMA ? 0 : 5);
      if (mode != Mode.MAGMA) {
        return;
      }
      BendingExplosion.builder()
        .size(userConfig.explosionRadius)
        .damage(userConfig.explosionDamage)
        .fireTicks(40)
        .sound(3, 0.5F)
        .buildAndExplode(EarthLine.this, location);

      Predicate<Block> predicate = b -> b.getY() >= FastMath.floor(location.getY()) && EarthMaterials.isEarthOrSand(b);
      List<Block> wall = WorldUtil.nearbyBlocks(user.world(), location, userConfig.explosionRadius, predicate);
      wall.removeIf(b -> !user.canBuild(b));
      Collections.shuffle(wall);
      Updatable fracture = Fracture.of(wall);
      if (fracture != null) {
        states = new StateChain().addState(new StateWrapper(fracture)).start();
        earthLine = null;
      }
    }

    public void raiseSpikes() {
      if (mode != Mode.NORMAL || raisedSpikes) {
        return;
      }
      raisedSpikes = true;
      Vector3d loc = location.add(Vector3d.MINUS_J);
      StateWrapper state = new StateWrapper(
        new EarthSpike(loc.toBlock(user.world()), 1, false),
        new EarthSpike(loc.add(direction).toBlock(user.world()), 2, true)
      );
      states = new StateChain().addState(state).start();
      earthLine = null;
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
        for (Block block : WorldUtil.nearbyBlocks(user.world(), Vector3d.center(blockToCheck), 1, b -> EarthMaterials.isEarthbendable(user, b), 1)) {
          material = block.getType() == Material.GRASS_BLOCK ? Material.DIRT : block.getType();
        }
      }

      if (material == null) {
        return;
      }

      imprisoned = true;
      EntityUtil.applyVelocity(EarthLine.this, entity, Vector3d.MINUS_J);
      BlockData data = material.createBlockData();
      Vector3d center = new Vector3d(entity.getLocation()).add(Vector3d.MINUS_J);
      Vector3d offset = new Vector3d(0, -0.6, 0);
      Builder builder = TempPacketEntity.builder(data).gravity(false).duration(userConfig.prisonDuration);
      VectorUtil.circle(Vector3d.PLUS_I.multiply(0.8), Vector3d.PLUS_J, 8).forEach(v -> {
        builder.buildArmorStand(user.world(), center.add(v));
        builder.buildArmorStand(user.world(), center.add(offset).add(v));
      });
      ActionLimiter.builder().duration(userConfig.prisonDuration).build(user, entity);
    }

    public void controllable(boolean value) {
      if (mode != Mode.MAGMA) {
        controllable = value;
      }
    }
  }

  private static final class StateWrapper implements State {
    private final Collection<Updatable> actions;
    private StateChain chain;
    private boolean started = false;

    private StateWrapper(Updatable first, Updatable... rest) {
      actions = new ArrayList<>();
      this.actions.add(first);
      if (rest != null) {
        this.actions.addAll(List.of(rest));
      }
    }

    @Override
    public @NonNull UpdateResult update() {
      actions.removeIf(action -> action.update() == UpdateResult.REMOVE);
      return actions.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
    }

    @Override
    public void start(@NonNull StateChain chain) {
      if (started) {
        return;
      }
      this.chain = chain;
      started = !actions.isEmpty();
    }

    @Override
    public void complete() {
      if (!started) {
        return;
      }
      chain.chainStore().clear();
      chain.nextState();
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
    @Modifiable(Attribute.STRENGTH)
    public double knockback;
    @Modifiable(Attribute.STRENGTH)
    public double knockup;
    @Modifiable(Attribute.RADIUS)
    public double explosionRadius;
    @Modifiable(Attribute.DAMAGE)
    public double explosionDamage;
    @Modifiable(Attribute.DURATION)
    public long prisonDuration;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "earth", "earthline");

      cooldown = abilityNode.node("cooldown").getLong(5000);
      range = abilityNode.node("range").getDouble(20.0);
      selectRange = abilityNode.node("select-range").getDouble(6.0);
      damage = abilityNode.node("damage").getDouble(3.0);
      knockback = abilityNode.node("knockback").getDouble(1.1);
      knockup = abilityNode.node("knockup").getDouble(0.55);
      explosionRadius = abilityNode.node("explosion-radius").getDouble(3.5);
      explosionDamage = abilityNode.node("explosion-damage").getDouble(2.5);
      prisonDuration = abilityNode.node("prison-duration").getLong(1500);
    }
  }
}
