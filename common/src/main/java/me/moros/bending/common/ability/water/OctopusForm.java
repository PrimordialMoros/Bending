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

package me.moros.bending.common.ability.water;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.collision.CollisionUtil;
import me.moros.bending.api.collision.geometry.AABB;
import me.moros.bending.api.config.BendingProperties;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.Direction;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.particle.ParticleBuilder;
import me.moros.bending.api.platform.world.WorldUtil;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.ExpiringSet;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.functional.SwappedSlotsRemovalPolicy;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.math.Vector3d;

// TODO make tentacle extension animation
public class OctopusForm extends AbilityInstance {
  private static final double RADIUS = 3.0;
  private static final AABB TENTACLE_BOX = AABB.of(Vector3d.of(-1, 0.0, -1), Vector3d.of(1, 2.5, 1));

  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Collection<Block> base = new ArrayList<>();
  private final List<Tentacle> tentacles = new ArrayList<>();

  private final Set<TempBlock> affectedBlocks = new HashSet<>();
  private final ExpiringSet<UUID> affectedEntities = new ExpiringSet<>(500);

  private WaterRing ring;
  private Block lastBlock;

  private boolean formed = false;
  private long nextTentacleFormTime = 0;

  public OctopusForm(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    OctopusForm octopusForm = user.game().abilityManager(user.worldKey()).firstInstance(user, OctopusForm.class).orElse(null);
    if (octopusForm != null) {
      octopusForm.punch();
      return false;
    }

    this.user = user;
    loadConfig();
    removalPolicy = Policies.defaults();
    ring = WaterRing.getOrCreateInstance(user);
    return ring != null;
  }

  @Override
  public void loadConfig() {
    userConfig = user.game().configProcessor().calculate(this, Config.class);
  }

  @Override
  public UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    if (formed) {
      cleanAll();
      if (!user.canBuild()) {
        return UpdateResult.REMOVE;
      }
      boolean forceUpdate = false;
      Block current = user.block();
      if (!current.equals(lastBlock)) {
        base.clear();
        base.addAll(WorldUtil.createBlockRing(current, RADIUS));
        lastBlock = current;
        forceUpdate = true;
      }
      if (base.stream().noneMatch(user::canBuild)) {
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
    if (!user.hasAbilitySelected("octopusform")) {
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
      Block below = block.offset(Direction.DOWN);
      if (MaterialUtil.isWater(below) && TempBlock.isBendable(below)) {
        TempBlock.ice().duration(BendingProperties.instance().iceRevertTime()).build(below);
      }
      renderWaterBlock(block);
    }
  }

  private void renderTentacles(boolean forceUpdate) {
    Vector3d center = user.location().center();
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
      ParticleBuilder.bubble(block).spawn(user.world());
    } else if (MaterialUtil.isTransparent(block)) {
      TempBlock.water().duration(250).build(block).ifPresent(affectedBlocks::add);
    }
  }

  private void punch() {
    if (!formed) {
      return;
    }
    Vector3d center = user.location().floor().add(0.5, 0, 0.5);
    double r = RADIUS + 0.5;
    for (double phi = 0; phi < Math.PI * 2; phi += Math.PI / 4) {
      Vector3d tentacleBase = center.add(Math.cos(phi) * r, 0, Math.sin(phi) * r);
      CollisionUtil.handle(user, TENTACLE_BOX.at(tentacleBase), this::onEntityHit, true);
    }
  }

  private boolean onEntityHit(Entity entity) {
    if (affectedEntities.add(entity.uuid())) {
      entity.damage(userConfig.damage, user, description());
      Vector3d dir = entity.center().subtract(user.location()).normalize().multiply(userConfig.knockback);
      entity.applyVelocity(this, dir);
    }
    return false;
  }

  private void clean(Block block) {
    if (MaterialUtil.isWater(block)) {
      TempBlock.air().build(block);
    }
  }

  private void cleanAll() {
    for (TempBlock tb : affectedBlocks) {
      TempBlock.air().fixWater(false).build(tb.block());
    }
    affectedBlocks.clear();
  }

  @Override
  public void onDestroy() {
    if (formed) {
      user.addCooldown(description(), userConfig.cooldown);
      cleanAll();
    }
  }

  private final class Tentacle {
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
      updateBlocks(user.location().center());
    }

    private void updateBlocks(Vector3d center) {
      blocks.clear();
      long time = System.currentTimeMillis();
      nextUpdateTime = time + ThreadLocalRandom.current().nextLong(250, 550);
      double bottomOffset = ThreadLocalRandom.current().nextDouble(1);
      double xBottom = cos * (RADIUS + bottomOffset);
      double zBottom = sin * (RADIUS + bottomOffset);
      blocks.add(user.world().blockAt(center.add(xBottom, 1, zBottom)));
      if (time > topFormTime) {
        double topOffset = ThreadLocalRandom.current().nextDouble(1);
        double xTop = cos * (RADIUS + topOffset);
        double zTop = sin * (RADIUS + topOffset);
        blocks.add(user.world().blockAt(center.add(xTop, 2, zTop)));
      }
    }
  }

  private static final class Config implements Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 1000;
    @Modifiable(Attribute.DAMAGE)
    private double damage = 2;
    @Modifiable(Attribute.STRENGTH)
    private double knockback = 1.75;

    @Override
    public List<String> path() {
      return List.of("abilities", "water", "octopusform");
    }
  }
}
