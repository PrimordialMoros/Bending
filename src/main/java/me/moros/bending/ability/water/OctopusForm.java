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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ExpiringSet;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.EntityMethods;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;

// TODO make tentacle extension animation
public class OctopusForm extends AbilityInstance {
  private static final Config config = new Config();
  private static final double RADIUS = 3.0;
  private static final AABB TENTACLE_BOX = new AABB(new Vector3d(-1, 0.0, -1), new Vector3d(1, 2.5, 1));


  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Collection<Block> base = new ArrayList<>();
  private final List<Tentacle> tentacles = new ArrayList<>();

  private final ExpiringSet<Entity> affectedEntities = new ExpiringSet<>(500);

  private WaterRing ring;
  private Block lastBlock;

  private boolean formed = false;
  private long nextTentacleFormTime = 0;

  public OctopusForm(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    OctopusForm octopusForm = Bending.game().abilityManager(user.world()).firstInstance(user, OctopusForm.class).orElse(null);
    if (octopusForm != null) {
      octopusForm.punch();
      return false;
    }

    this.user = user;
    loadConfig();
    removalPolicy = Policies.builder().build();
    ring = WaterRing.getOrCreateInstance(user);
    return ring != null;
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
    if (formed) {
      cleanAll();
      if (!user.canBuild(user.locBlock())) {
        return UpdateResult.REMOVE;
      }
      boolean forceUpdate = false;
      Block current = user.locBlock();
      if (!current.equals(lastBlock)) {
        base.clear();
        base.addAll(BlockMethods.createBlockRing(user.locBlock(), RADIUS));
        lastBlock = current;
        forceUpdate = true;
      }
      if (base.stream().noneMatch(b -> user.canBuild(b))) {
        return UpdateResult.REMOVE;
      }
      renderBase();
      int size = tentacles.size();
      if (size < 8 && System.currentTimeMillis() >= nextTentacleFormTime) {
        tentacles.add(new Tentacle(size));
      }
      renderTentacles(forceUpdate);
    } else {
      if (ring.isDestroyed()) {
        return UpdateResult.REMOVE;
      }
      if (ring.isReady() && user.sneaking()) {
        form();
      }
    }
    return UpdateResult.CONTINUE;
  }

  private void form() {
    if (!user.selectedAbilityName().equals("OctopusForm")) {
      return;
    }
    ring.complete().forEach(this::clean);
    formed = true;
    nextTentacleFormTime = System.currentTimeMillis() + 150;
    removalPolicy = Policies.builder()
      .add(Policies.NOT_SNEAKING)
      .add(SwappedSlotsRemovalPolicy.of(description()))
      .build();
  }

  private void renderBase() {
    for (Block block : base) {
      Block below = block.getRelative(BlockFace.DOWN);
      if (MaterialUtil.isWater(below) && TempBlock.isBendable(below)) {
        TempBlock.create(below, Material.ICE.createBlockData(), BendingProperties.ICE_DURATION, true);
      }
      renderWaterBlock(block);
    }
  }

  private void renderTentacles(boolean forceUpdate) {
    Vector3d center = user.location().snapToBlockCenter();
    long time = System.currentTimeMillis();
    for (Tentacle tentacle : tentacles) {
      if (forceUpdate || time > tentacle.nextUpdateTime) {
        tentacle.updateBlocks(center);
      }
      tentacle.blocks.forEach(this::renderWaterBlock);
    }
  }

  private void renderWaterBlock(Block block) {
    if (!TempBlock.isBendable(block)) {
      return;
    }
    if (MaterialUtil.isWater(block)) {
      ParticleUtil.create(Particle.WATER_BUBBLE, block.getLocation().add(0.5, 0.5, 0.5))
        .count(5).offset(0.25, 0.25, 0.25).spawn();
    } else if (MaterialUtil.isTransparent(block)) {
      TempBlock.create(block, Material.WATER.createBlockData(), 250);
    }
  }

  private void punch() {
    if (!formed) {
      return;
    }
    Vector3d center = user.location().floor().add(new Vector3d(0.5, 0, 0.5));
    double r = RADIUS + 0.5;
    for (double phi = 0; phi < Math.PI * 2; phi += Math.PI / 4) {
      Vector3d tentacleBase = center.add(new Vector3d(Math.cos(phi) * r, 0, Math.sin(phi) * r));
      CollisionUtil.handleEntityCollisions(user, TENTACLE_BOX.at(tentacleBase), this::onEntityHit, true);
    }
  }

  private boolean onEntityHit(Entity entity) {
    if (!affectedEntities.contains(entity)) {
      DamageUtil.damageEntity(entity, user, userConfig.damage, description());
      Vector3d dir = EntityMethods.entityCenter(entity).subtract(user.location());
      entity.setVelocity(dir.normalize().multiply(userConfig.knockback).clampVelocity());
      affectedEntities.add(entity);
    }
    return false;
  }

  private void clean(Block block) {
    if (MaterialUtil.isWater(block)) {
      TempBlock.createAir(block);
    }
  }

  private void cleanAll() {
    for (Tentacle t : tentacles) {
      t.blocks.forEach(this::clean);
    }
    base.forEach(this::clean);
  }

  @Override
  public void onDestroy() {
    if (formed) {
      user.addCooldown(description(), userConfig.cooldown);
      cleanAll();
    }
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  private class Tentacle {
    private final Collection<Block> blocks;
    private final double cos, sin;
    private final long topFormTime;

    private long nextUpdateTime;

    private Tentacle(int index) {
      blocks = new ArrayList<>();
      double phi = index * Math.PI / 4;
      cos = Math.cos(phi);
      sin = Math.sin(phi);
      topFormTime = System.currentTimeMillis() + 150;
      updateBlocks(user.location().snapToBlockCenter());
    }

    private void updateBlocks(Vector3d center) {
      blocks.clear();
      long time = System.currentTimeMillis();
      nextUpdateTime = time + ThreadLocalRandom.current().nextLong(250, 550);
      double bottomOffset = ThreadLocalRandom.current().nextDouble(1);
      double xBottom = cos * (RADIUS + bottomOffset);
      double zBottom = sin * (RADIUS + bottomOffset);
      blocks.add(center.add(new Vector3d(xBottom, 1, zBottom)).toBlock(user.world()));
      if (time > topFormTime) {
        double topOffset = ThreadLocalRandom.current().nextDouble(1);
        double xTop = cos * (RADIUS + topOffset);
        double zTop = sin * (RADIUS + topOffset);
        blocks.add(center.add(new Vector3d(xTop, 2, zTop)).toBlock(user.world()));
      }
    }
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.DAMAGE)
    public double damage;
    @Modifiable(Attribute.STRENGTH)
    public double knockback;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "water", "octopusform");

      cooldown = abilityNode.node("cooldown").getLong(1000);
      damage = abilityNode.node("damage").getDouble(2.0);
      knockback = abilityNode.node("knockback").getDouble(1.75);
    }
  }
}
