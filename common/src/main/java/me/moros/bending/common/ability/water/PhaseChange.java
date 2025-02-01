/*
 * Copyright 2020-2025 Moros
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
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.platform.world.WorldUtil;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.functional.SwappedSlotsRemovalPolicy;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.bending.common.util.BatchQueue;
import me.moros.math.Vector3d;
import org.spongepowered.configurate.objectmapping.meta.Comment;

public class PhaseChange extends AbilityInstance {
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private PhaseTransformer phaseTransformer;

  public PhaseChange(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    loadConfig();

    if (method == Activation.ATTACK) {
      freeze();
    } else if (method == Activation.SNEAK) {
      melt();
    }
    return phaseTransformer != null && !phaseTransformer.isEmpty();
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
    return phaseTransformer.processQueue() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
  }

  @Override
  public void onDestroy() {
    if (phaseTransformer != null) {
      phaseTransformer.clear();
    }
  }

  private void freeze() {
    phaseTransformer = new Freeze(user, new ArrayDeque<>(), userConfig.freezeSpeed);
    Vector3d center = user.rayTrace(userConfig.freezeRange).ignoreLiquids(false).blocks(user.world()).position();
    if (phaseTransformer.fillQueue(getShuffledBlocks(center, userConfig.freezeRadius, MaterialUtil::isWater))) {
      user.addCooldown(description(), userConfig.freezeCooldown);
    }
    removalPolicy = Policies.builder().build();
  }

  private void melt() {
    phaseTransformer = new Melt(user, new ArrayDeque<>(), userConfig.meltSpeed);
    Vector3d center = user.rayTrace(userConfig.meltRange).blocks(user.world()).position();
    if (phaseTransformer.fillQueue(getShuffledBlocks(center, userConfig.meltRadius, MaterialUtil::isMeltable))) {
      user.addCooldown(description(), 500);
    }
    removalPolicy = Policies.builder()
      .add(Policies.NOT_SNEAKING)
      .add(SwappedSlotsRemovalPolicy.of(description()))
      .build();
  }

  private Collection<Block> getShuffledBlocks(Vector3d center, double radius, Predicate<Block> predicate) {
    List<Block> newBlocks = user.world().nearbyBlocks(center, radius, predicate);
    newBlocks.removeIf(b -> !user.canBuild(b));
    Collections.shuffle(newBlocks);
    return newBlocks;
  }

  private interface PhaseTransformer extends BatchQueue<Block> {
    int batchSize();

    default boolean processQueue() {
      processQueue(batchSize());
      return queue().isEmpty();
    }
  }

  private record Freeze(User user, Queue<Block> queue, int batchSize) implements PhaseTransformer {
    @Override
    public boolean process(Block block) {
      if (!TempBlock.isBendable(block) || !MaterialUtil.isWater(block) || !user.canBuild(block)) {
        return false;
      }
      TempBlock.ice().build(block);
      if (ThreadLocalRandom.current().nextInt(12) == 0) {
        SoundEffect.ICE.play(block);
      }
      return true;
    }
  }

  private record Melt(User user, Queue<Block> queue, int batchSize) implements PhaseTransformer {
    @Override
    public boolean process(Block block) {
      return TempBlock.isBendable(block) && WorldUtil.tryMelt(user, block);
    }
  }

  private static final class Config implements Configurable {
    @Modifiable(Attribute.SELECTION)
    private double freezeRange = 7;
    @Modifiable(Attribute.RADIUS)
    private double freezeRadius = 3.5;
    @Comment("How many blocks can be affected per tick")
    @Modifiable(Attribute.SPEED)
    private int freezeSpeed = 8;
    @Modifiable(Attribute.COOLDOWN)
    private long freezeCooldown = 2000;
    @Modifiable(Attribute.SELECTION)
    private double meltRange = 7;
    @Modifiable(Attribute.RADIUS)
    private double meltRadius = 4.5;
    @Comment("How many blocks can be affected per tick")
    @Modifiable(Attribute.SPEED)
    private int meltSpeed = 8;

    @Override
    public List<String> path() {
      return List.of("abilities", "water", "phasechange");
    }
  }
}

