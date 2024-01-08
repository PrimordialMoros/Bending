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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.MultiUpdatable;
import me.moros.bending.api.ability.Updatable;
import me.moros.bending.api.collision.CollisionUtil;
import me.moros.bending.api.collision.geometry.AABB;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.collision.geometry.Sphere;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.Direction;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.platform.world.WorldUtil;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.BendingEffect;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.bending.api.util.material.WaterMaterials;
import me.moros.bending.common.config.ConfigManager;
import me.moros.math.Vector3d;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class IceSpike extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final MultiUpdatable<IcePillar> pillars = MultiUpdatable.empty();
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
      Collider collider = Sphere.of(user.location(), userConfig.fieldRadius);
      CollisionUtil.handle(user, collider, this::createPillar, true);
    } else {
      Block source = null;
      Entity entity = user.rayTrace(userConfig.selectRange).cast(user.world()).entity();
      if (entity != null) {
        Block base = entity.block().offset(Direction.DOWN);
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
    return pillars.update();
  }

  private boolean createPillar(Entity entity) {
    Block base = entity.block().offset(Direction.DOWN);
    boolean unique = pillars.stream()
      .noneMatch(p -> p.origin.blockX() == base.blockX() && p.origin.blockZ() == base.blockZ());
    if (WaterMaterials.isIceBendable(base)) {
      if (unique) {
        base.state().asParticle(entity.center()).count(8).offset(1, 0.1, 1).spawn(user.world());
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
      Block forwardBlock = block.offset(Direction.UP, i + 1);
      if (!user.canBuild(forwardBlock)) {
        return i;
      }
      if (!MaterialUtil.isTransparent(forwardBlock) && forwardBlock.type() != BlockType.WATER) {
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

  private final class IcePillar implements Updatable {
    private final Block origin;
    private final BlockType material;
    private final Deque<Block> pillarBlocks;

    private final int length;

    private boolean reverting = false;
    private int currentLength = 0;
    private long nextUpdateTime = 0;

    private IcePillar(Block origin, int length) {
      this.origin = origin;
      this.material = origin.type();
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
        SoundEffect.ICE.play(block);
        return UpdateResult.CONTINUE;
      }

      Block currentIndex = origin.offset(Direction.UP, ++currentLength);
      AABB collider = AABB.BLOCK_BOUNDS.at(currentIndex);
      CollisionUtil.handle(user, collider, this::onEntityHit, true, true);

      if (canMove(currentIndex)) {
        pillarBlocks.offerFirst(currentIndex);
        TempBlock.builder(material).build(currentIndex);
        SoundEffect.ICE.play(currentIndex);
      } else {
        reverting = true;
      }

      return UpdateResult.CONTINUE;
    }

    private boolean canMove(Block newBlock) {
      if (MaterialUtil.isLava(newBlock)) {
        return false;
      }
      if (!MaterialUtil.isTransparent(newBlock) && newBlock.type() != BlockType.WATER) {
        return false;
      }
      WorldUtil.tryBreakPlant(newBlock);
      return true;
    }

    private boolean onEntityHit(Entity entity) {
      if (!affectedEntities.contains(entity) && !entity.uuid().equals(user.uuid())) {
        affectedEntities.add(entity);
        BendingEffect.FROST_TICK.apply(user, entity, userConfig.freezeTicks);
        entity.damage(userConfig.damage, user, description());
        entity.applyVelocity(IceSpike.this, Vector3d.PLUS_J.multiply(userConfig.knockup));
        return true;
      }
      return false;
    }
  }

  @ConfigSerializable
  private static final class Config implements Configurable {
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
    private long fieldCooldown = 5000;
    @Modifiable(Attribute.RADIUS)
    private double fieldRadius = 10;

    @Override
    public List<String> path() {
      return List.of("abilities", "water", "icespike");
    }
  }
}
