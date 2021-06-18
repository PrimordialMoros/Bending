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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.FragileStructure;
import me.moros.bending.ability.common.Pillar;
import me.moros.bending.ability.common.SelectedSource;
import me.moros.bending.ability.common.basic.AbstractLine;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempFallingBlock;
import me.moros.bending.game.temporal.TempArmorStand;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.ActionType;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.state.State;
import me.moros.bending.model.ability.state.StateChain;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingExplosion;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.FireTick;
import me.moros.bending.util.MovementHandler;
import me.moros.bending.util.SoundEffect;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.EntityMethods;
import me.moros.bending.util.methods.VectorMethods;
import me.moros.bending.util.methods.WorldMethods;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.NumberConversions;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;

public class EarthLine extends AbilityInstance {
  private enum Mode {NORMAL, PRISON, MAGMA}

  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Collection<Pillar> spikes = new ArrayList<>();

  private StateChain states;
  private Line earthLine;
  private Mode mode;

  public EarthLine(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    if (method == Activation.ATTACK) {
      Bending.game().abilityManager(user.world()).firstInstance(user, EarthLine.class).ifPresent(EarthLine::launch);
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
      if (state instanceof SelectedSource) {
        ((SelectedSource) state).reselect(source, fakeData);
      }
      return false;
    }
    mode = Mode.NORMAL;
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
      if (earthLine.raisedSpikes) {
        spikes.removeIf(p -> p.update() == UpdateResult.REMOVE);
        return spikes.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
      }
      earthLine.controllable(user.sneaking());
      UpdateResult result = earthLine.update();
      // Handle case where spikes are raised on entity collision and line is removed
      if (result == UpdateResult.REMOVE && earthLine.raisedSpikes) {
        return UpdateResult.CONTINUE;
      }
      return result;
    } else {
      return states.update();
    }
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
      if (EarthMaterials.isLavaBendable(source)) {
        mode = Mode.MAGMA;
      }
      earthLine = new Line(source);
      removalPolicy = Policies.builder().add(SwappedSlotsRemovalPolicy.of(description())).build();
      user.addCooldown(description(), userConfig.cooldown);
    }
  }

  public static void prisonMode(@NonNull User user) {
    if (user.selectedAbilityName().equals("EarthLine")) {
      Bending.game().abilityManager(user.world()).firstInstance(user, EarthLine.class).ifPresent(EarthLine::prisonMode);
    }
  }

  private void prisonMode() {
    if (mode == Mode.NORMAL) {
      mode = Mode.PRISON;
      user.sendActionBar(Component.text("*Prison Mode*", NamedTextColor.GRAY));
    }
  }

  @Override
  public void onDestroy() {
    State state = states.current();
    if (state instanceof SelectedSource) {
      ((SelectedSource) state).onDestroy();
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
    private boolean raisedSpikes = false;
    private boolean imprisoned = false;

    public Line(Block source) {
      super(user, source, userConfig.range, mode == Mode.MAGMA ? 0.6 : 0.8, false);
    }

    @Override
    public void render() {
      double x = ThreadLocalRandom.current().nextDouble(-0.125, 0.125);
      double z = ThreadLocalRandom.current().nextDouble(-0.125, 0.125);
      Location spawnLoc = location.subtract(new Vector3(x, 2, z)).toLocation(user.world());
      Material type = mode == Mode.MAGMA ? Material.MAGMA_BLOCK : location.toBlock(user.world()).getRelative(BlockFace.DOWN).getType();
      new TempArmorStand(spawnLoc, type, 700);
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(5) == 0) {
        SoundUtil.EARTH.play(location.toLocation(user.world()));
      }
    }

    @Override
    public boolean onEntityHit(@NonNull Entity entity) {
      double damage = userConfig.damage;
      switch (mode) {
        case NORMAL:
          raiseSpikes();
          break;
        case PRISON:
          imprisonTarget((LivingEntity) entity);
          return true;
        case MAGMA:
          damage = userConfig.damage * BendingProperties.MAGMA_MODIFIER;
          FireTick.ignite(user, entity);
          break;
      }
      DamageUtil.damageEntity(entity, user, damage, description());
      return true;
    }

    @Override
    public boolean onBlockHit(@NonNull Block block) {
      if (MaterialUtil.isWater(block)) {
        if (mode == Mode.MAGMA) {
          BlockMethods.playLavaExtinguishEffect(block);
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
      Location center = location.toLocation(user.world());
      FragileStructure.tryDamageStructure(List.of(center.getBlock()), mode == Mode.MAGMA ? 0 : 5);
      if (mode != Mode.MAGMA) {
        return;
      }
      BendingExplosion.builder()
        .size(userConfig.explosionRadius)
        .damage(userConfig.explosionDamage)
        .fireTicks(40)
        .soundEffect(new SoundEffect(Sound.ENTITY_GENERIC_EXPLODE, 3, 0.5F))
        .buildAndExplode(user, description(), location);

      Predicate<Block> predicate = b -> b.getY() >= NumberConversions.floor(location.y) && EarthMaterials.isEarthOrSand(b);
      List<Block> wall = WorldMethods.nearbyBlocks(center, userConfig.explosionRadius, predicate);
      wall.removeIf(b -> !user.canBuild(b));
      Collections.shuffle(wall);
      for (Block block : wall) {
        Vector3 velocity = VectorMethods.gaussianOffset(Vector3.ZERO, 0.2, 0.1, 0.2);
        TempBlock.createAir(block, BendingProperties.EXPLOSION_REVERT_TIME);
        new TempFallingBlock(block, Material.MAGMA_BLOCK.createBlockData(), velocity, true, 10000);
      }
    }

    public void raiseSpikes() {
      if (mode != Mode.NORMAL || raisedSpikes) {
        return;
      }
      raisedSpikes = true;
      Vector3 loc = location.add(Vector3.MINUS_J);
      Predicate<Block> predicate = b -> EarthMaterials.isEarthNotLava(user, b);

      Pillar.builder(user, loc.toBlock(user.world())).predicate(predicate).build(1).ifPresent(spikes::add);
      Pillar.builder(user, loc.add(direction).toBlock(user.world())).predicate(predicate).build(2).ifPresent(spikes::add);
    }

    private void imprisonTarget(LivingEntity entity) {
      if (imprisoned || !entity.isValid() || EntityMethods.distanceAboveGround(entity) > 1.2) {
        return;
      }
      Material material = null;
      Block blockToCheck = entity.getLocation().getBlock().getRelative(BlockFace.DOWN);
      if (EarthMaterials.isEarthbendable(user, blockToCheck)) { // Prefer to use the block under the entity first
        material = blockToCheck.getType() == Material.GRASS_BLOCK ? Material.DIRT : blockToCheck.getType();
      } else {
        Location center = blockToCheck.getLocation().add(0.5, 0.5, 0.5);
        for (Block block : WorldMethods.nearbyBlocks(center, 1, b -> EarthMaterials.isEarthbendable(user, b), 1)) {
          material = block.getType() == Material.GRASS_BLOCK ? Material.DIRT : block.getType();
        }
      }

      if (material == null) {
        return;
      }

      imprisoned = true;
      entity.setVelocity(Vector3.MINUS_J.toBukkitVector());
      Material mat = material;
      VectorMethods.circle(Vector3.PLUS_I.multiply(0.8), Vector3.PLUS_J, 8).forEach(v -> {
        Location loc = entity.getLocation().add(0, -1.1, 0);
        new TempArmorStand(loc.add(v.toBukkitVector()), mat, userConfig.prisonDuration);
        new TempArmorStand(loc.add(0, -0.7, 0), mat, userConfig.prisonDuration);
      });
      MovementHandler.restrictEntity(user, entity, userConfig.prisonDuration).disableActions(EnumSet.allOf(ActionType.class));
    }

    public void controllable(boolean value) {
      if (mode != Mode.MAGMA) {
        controllable = value;
      }
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
      explosionRadius = abilityNode.node("explosion-radius").getDouble(3.5);
      explosionDamage = abilityNode.node("explosion-damage").getDouble(2.5);
      prisonDuration = abilityNode.node("prison-duration").getLong(1500);
    }
  }
}
