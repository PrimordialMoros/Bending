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

package me.moros.bending.ability.water;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;

import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.Policies;
import me.moros.bending.model.predicate.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.temporal.TempBlock;
import me.moros.bending.util.BendingEffect;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class IceSpike extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Collection<IcePillar> pillars = new ArrayList<>();
  private final Collection<Entity> affectedEntities = new HashSet<>();

  public IceSpike(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    loadConfig();

    boolean field = method == Activation.SNEAK;
    if (field) {
      Collider collider = new Sphere(user.location(), userConfig.fieldRadius);
      CollisionUtil.handle(user, collider, this::createPillar, true);
    } else {
      Block source = null;
      Entity entity = user.rayTrace(userConfig.selectRange).entities(user.world()).entity();
      if (entity != null) {
        Block base = entity.getLocation().getBlock().getRelative(BlockFace.DOWN);
        if (user.canBuild(base) && WaterMaterials.isIceBendable(base) && TempBlock.isBendable(base)) {
          source = base;
        }
      }
      if (source == null) {
        source = user.find(userConfig.selectRange, WaterMaterials::isIceBendable);
        if (source == null) {
          return false;
        }
      }
      buildPillar(source);
    }
    if (!pillars.isEmpty()) {
      user.addCooldown(description(), field ? userConfig.fieldCooldown : userConfig.columnCooldown);
      removalPolicy = Policies.builder().build();
      return true;
    }
    return false;
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

    pillars.removeIf(pillar -> pillar.update() == UpdateResult.REMOVE);
    return pillars.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
  }

  private boolean createPillar(Entity entity) {
    Block base = entity.getLocation().getBlock().getRelative(BlockFace.DOWN);
    boolean unique = pillars.stream()
      .noneMatch(p -> p.origin.getX() == base.getX() && p.origin.getZ() == base.getZ());
    if (WaterMaterials.isIceBendable(base)) {
      if (unique) {
        ParticleUtil.of(Particle.BLOCK_DUST, EntityUtil.entityCenter(entity))
          .count(8).offset(1, 0.1, 1).data(base.getBlockData()).spawn(user.world());
        buildPillar(base);
      }
      return true;
    }
    return false;
  }

  private void buildPillar(Block block) {
    int h = validate(block);
    if (h > 0) {
      pillars.add(new IcePillar(block, h));
    }
  }

  private int validate(Block block) {
    int height = userConfig.columnMaxHeight;
    if (!WaterMaterials.isIceBendable(block) || !TempBlock.isBendable(block)) {
      return 0;
    }
    if (!user.canBuild(block)) {
      return 0;
    }
    for (int i = 0; i < height; i++) {
      Block forwardBlock = block.getRelative(BlockFace.UP, i + 1);
      if (!user.canBuild(forwardBlock)) {
        return i;
      }
      if (!MaterialUtil.isTransparent(forwardBlock) && forwardBlock.getType() != Material.WATER) {
        return i;
      }
    }
    return height;
  }

  private void clean(Block block) {
    if (WaterMaterials.isIceBendable(block)) {
      TempBlock.air().build(block);
    }
  }

  @Override
  public void onDestroy() {
    for (IcePillar pillar : pillars) {
      pillar.pillarBlocks.forEach(this::clean);
    }
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  private final class IcePillar implements Updatable {
    private final Block origin;
    private final Material material;
    private final Deque<Block> pillarBlocks;

    private final int length;

    private boolean reverting = false;
    private int currentLength = 0;
    private long nextUpdateTime = 0;

    private IcePillar(Block origin, int length) {
      this.origin = origin;
      this.material = origin.getType();
      this.length = length;
      this.pillarBlocks = new ArrayDeque<>(length);
    }

    @Override
    public UpdateResult update() {
      if (reverting && pillarBlocks.isEmpty()) {
        return UpdateResult.REMOVE;
      }
      if (!reverting && currentLength >= length) {
        reverting = true;
      }

      long time = System.currentTimeMillis();
      if (time < nextUpdateTime) {
        return UpdateResult.CONTINUE;
      }
      nextUpdateTime = time + 70;

      if (reverting) {
        if (pillarBlocks.isEmpty()) {
          return UpdateResult.REMOVE;
        }
        Block block = pillarBlocks.pollFirst();
        clean(block);
        SoundUtil.ICE.play(block);
        return UpdateResult.CONTINUE;
      }

      Block currentIndex = origin.getRelative(BlockFace.UP, ++currentLength);
      AABB collider = AABB.BLOCK_BOUNDS.at(new Vector3d(currentIndex));
      CollisionUtil.handle(user, collider, this::onEntityHit, true, true);

      if (canMove(currentIndex)) {
        pillarBlocks.offerFirst(currentIndex);
        TempBlock.builder(material.createBlockData()).build(currentIndex);
        SoundUtil.ICE.play(currentIndex);
      } else {
        reverting = true;
      }

      return UpdateResult.CONTINUE;
    }

    private boolean canMove(Block newBlock) {
      if (MaterialUtil.isLava(newBlock)) {
        return false;
      }
      if (!MaterialUtil.isTransparent(newBlock) && newBlock.getType() != Material.WATER) {
        return false;
      }
      WorldUtil.tryBreakPlant(newBlock);
      return true;
    }

    private boolean onEntityHit(Entity entity) {
      if (!affectedEntities.contains(entity) && !entity.equals(user.entity())) {
        affectedEntities.add(entity);
        BendingEffect.FROST_TICK.apply(user, entity, userConfig.freezeTicks);
        DamageUtil.damageEntity(entity, user, userConfig.damage, description());
        EntityUtil.applyVelocity(IceSpike.this, entity, Vector3d.PLUS_J.multiply(userConfig.knockup));
        return true;
      }
      return false;
    }
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.SELECTION)
    private double selectRange = 10;
    @Modifiable(Attribute.DAMAGE)
    private double damage = 3;
    @Modifiable(Attribute.STRENGTH)
    private double knockup = 0.8;
    @Modifiable(Attribute.FREEZE_TICKS)
    private int freezeTicks = 80;
    @Modifiable(Attribute.COOLDOWN)
    private long columnCooldown = 1500;
    @Modifiable(Attribute.HEIGHT)
    private int columnMaxHeight = 5;
    @Modifiable(Attribute.COOLDOWN)
    private long fieldCooldown = 5;
    @Modifiable(Attribute.RADIUS)
    private double fieldRadius = 10;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "water", "icespike");
    }
  }
}
