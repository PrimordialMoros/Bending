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

package me.moros.bending.common.ability.fire;

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
import me.moros.bending.api.ability.Updatable;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.Platform;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockTag;
import me.moros.bending.api.platform.entity.EntityProperties;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.platform.item.PlayerInventory;
import me.moros.bending.api.platform.particle.Particle;
import me.moros.bending.api.platform.particle.ParticleBuilder;
import me.moros.bending.api.platform.world.WorldUtil;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.temporal.TempLight;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.ColorPalette;
import me.moros.bending.api.util.KeyUtil;
import me.moros.bending.api.util.data.DataKey;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.functional.SwappedSlotsRemovalPolicy;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.bending.common.util.BatchQueue;
import me.moros.math.Vector3d;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

public class HeatControl extends AbilityInstance {
  private enum Mode {COOLING, HEATING}

  private static final DataKey<Mode> KEY = KeyUtil.data("heatcontrol-mode", Mode.class);

  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private HeatControlState state;
  private boolean sneakActivation;

  public HeatControl(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    sneakActivation = method == Activation.SNEAK;

    if (user.game().abilityManager(user.worldKey()).userInstances(user, HeatControl.class)
      .anyMatch(h -> sneakActivation == h.sneakActivation)) {
      return false;
    }

    this.user = user;
    loadConfig();

    if (sneakActivation) {
      removalPolicy = Policies.builder()
        .add(Policies.NOT_SNEAKING)
        .add(SwappedSlotsRemovalPolicy.of(description()))
        .build();
    } else {
      removalPolicy = Policies.defaults();
    }

    if (user.store().get(KEY).orElse(Mode.COOLING) == Mode.COOLING) {
      state = sneakActivation ? coolLava() : extinguish();
    } else {
      state = sneakActivation ? cooking() : melt();
    }

    if (state != null && state.shouldAddCooldown()) {
      user.addCooldown(description(), userConfig.cooldown);
    }
    return state != null;
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
    return state.update();
  }

  @Override
  public void onDestroy() {
    state.onDestroy();
  }

  private @Nullable HeatControlState extinguish() {
    Collection<Block> blocks = getShuffledBlocks(userConfig.range, userConfig.radius, HeatControl::isExtinguishable);
    return tryCreateBatchProcessor(16, WorldUtil::tryExtinguishFire, blocks);
  }

  private @Nullable HeatControlState coolLava() {
    Collection<Block> blocks = getShuffledBlocks(userConfig.solidifyRange, userConfig.solidifyRadius, MaterialUtil::isLava);
    return tryCreateBatchProcessor(1, WorldUtil::tryCoolLava, blocks);
  }

  private @Nullable HeatControlState melt() {
    Collection<Block> blocks = getShuffledBlocks(userConfig.range, userConfig.radius, MaterialUtil::isMeltable);
    return tryCreateBatchProcessor(4, WorldUtil::tryMelt, blocks);
  }

  private HeatControlState cooking() {
    HeatControlState result = new Heating(user, userConfig.cookInterval, userConfig.chargeTime);
    // Overwrite removal policy
    removalPolicy = Policies.builder().add(SwappedSlotsRemovalPolicy.of(description())).build();
    return result;
  }

  private Collection<Block> getShuffledBlocks(double range, double radius, Predicate<Block> predicate) {
    Vector3d center = user.rayTrace(range).blocks(user.world()).position();
    List<Block> blocks = user.world().nearbyBlocks(center, radius, predicate.and(user::canBuild));
    Collections.shuffle(blocks);
    return blocks;
  }

  private @Nullable BatchProcessor tryCreateBatchProcessor(int batchSize, BlockProcessor processor, Collection<Block> blocks) {
    BatchProcessor result = new BatchProcessor(user, new ArrayDeque<>(), batchSize, processor);
    return result.fillQueue(blocks) ? result : null;
  }

  private interface HeatControlState extends Updatable {
    void onDestroy();

    boolean shouldAddCooldown();
  }

  @FunctionalInterface
  private interface BlockProcessor {
    boolean accept(User user, Block block);
  }

  private record BatchProcessor(User user, Queue<Block> queue, int batchSize,
                                BlockProcessor blockProcessor) implements BatchQueue<Block>, HeatControlState {
    @Override
    public boolean process(Block block) {
      if (!TempBlock.isBendable(block)) {
        return false;
      }
      return blockProcessor.accept(user, block);
    }

    @Override
    public UpdateResult update() {
      processQueue(batchSize);
      return isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
    }

    @Override
    public void onDestroy() {
      queue.clear();
    }

    @Override
    public boolean shouldAddCooldown() {
      return !isEmpty();
    }
  }

  private static final class Heating implements HeatControlState {
    private final User user;
    private final long cookInterval;
    private final long fullyChargedTime;

    private long lastCookTime;
    private boolean requireSneak;
    private boolean charged;

    private TempLight light;
    private int ticks = 3;

    private Heating(User user, long cookInterval, long chargeTime) {
      this.user = user;
      this.cookInterval = cookInterval;
      this.lastCookTime = System.currentTimeMillis();
      this.fullyChargedTime = this.lastCookTime + chargeTime;
      this.requireSneak = true;
      this.charged = false;
    }

    @Override
    public UpdateResult update() {
      long time = System.currentTimeMillis();
      boolean sneaking = user.sneaking();
      if (!charged && time >= fullyChargedTime) {
        charged = true;
      }
      if (charged && requireSneak && !sneaking) {
        requireSneak = false;
      }
      if (sneaking != requireSneak || user.underWater()) {
        return UpdateResult.REMOVE;
      }
      if (time > lastCookTime + cookInterval && cook()) {
        lastCookTime = time;
      }
      Vector3d handLoc = user.mainHandSide();
      if (charged) {
        ParticleBuilder.fire(user, handLoc).spawn(user.world());
        createLight(user.eyeBlock());
      } else {
        Particle particle = ThreadLocalRandom.current().nextInt(5) == 0 ? Particle.SMALL_FLAME : Particle.SMOKE;
        particle.builder(handLoc).spawn(user.world());
      }
      user.editProperty(EntityProperties.FREEZE_TICKS, freezeTicks -> freezeTicks - 2);
      return UpdateResult.CONTINUE;
    }

    @Override
    public void onDestroy() {
      if (light != null) {
        light.unlock();
        light = null;
      }
    }

    @Override
    public boolean shouldAddCooldown() {
      return false;
    }

    private boolean cook() {
      if (user.inventory() instanceof PlayerInventory inv) {
        ItemSnapshot uncooked = inv.itemInMainHand();
        ItemSnapshot cooked = Platform.instance().factory().campfireRecipeCooked(uncooked.type()).orElse(null);
        if (cooked != null && inv.remove(uncooked.type())) {
          inv.offer(cooked);
          return true;
        }
      }
      return false;
    }

    private void createLight(Block block) {
      if (light != null && !light.block().equals(block)) {
        light.unlock();
      }
      light = TempLight.builder(++ticks).rate(2).duration(0).build(block).map(TempLight::lock).orElse(null);
    }
  }

  private static boolean isExtinguishable(Block block) {
    return MaterialUtil.isFire(block) || MaterialUtil.isCampfire(block) || BlockTag.CANDLES.isTagged(block);
  }

  public static boolean canBurn(User user) {
    AbilityDescription selected = user.selectedAbility();
    if (selected == null) {
      return true;
    }
    return !user.hasAbilitySelected("HeatControl") || !user.canBend(selected);
  }

  public static void toggleMode(User user) {
    if (user.hasAbilitySelected("heatcontrol")) {
      if (user.store().canEdit(KEY)) {
        Mode mode = user.store().toggle(KEY, Mode.COOLING);
        user.sendActionBar(Component.text("Mode: " + mode.name(), ColorPalette.TEXT_COLOR));
      }
    }
  }

  private static final class Config implements Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 2000;
    @Modifiable(Attribute.RANGE)
    private double range = 10;
    @Modifiable(Attribute.RADIUS)
    private double radius = 5;
    @Modifiable(Attribute.CHARGE_TIME)
    private long chargeTime = 1500;
    @Modifiable(Attribute.RANGE)
    private double solidifyRange = 5;
    @Modifiable(Attribute.RADIUS)
    private double solidifyRadius = 6;
    @Modifiable(Attribute.CHARGE_TIME)
    private long cookInterval = 2000;

    @Override
    public List<String> path() {
      return List.of("abilities", "fire", "heatcontrol");
    }
  }
}
