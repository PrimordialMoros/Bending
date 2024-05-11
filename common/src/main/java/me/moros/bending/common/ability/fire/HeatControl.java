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

package me.moros.bending.common.ability.fire;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.common.basic.PhaseTransformer;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.Platform;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockTag;
import me.moros.bending.api.platform.entity.EntityProperties;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.platform.item.PlayerInventory;
import me.moros.bending.api.platform.particle.ParticleBuilder;
import me.moros.bending.api.platform.world.WorldUtil;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.temporal.TempLight;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.ColorPalette;
import me.moros.bending.api.util.KeyUtil;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.bending.common.config.ConfigManager;
import me.moros.math.Vector3d;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class HeatControl extends AbilityInstance {
  private enum Light {ON, OFF}

  private static final Config config = ConfigManager.load(Config::new);

  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Solidify solidify = new Solidify();
  private final Melt melt = new Melt();

  private boolean canLight = true;
  private TempLight light;
  private int ticks = 3;

  private long startTime;

  public HeatControl(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (method == Activation.ATTACK) {
      user.game().abilityManager(user.worldKey()).firstInstance(user, HeatControl.class).ifPresent(HeatControl::act);
      return false;
    } else if (method == Activation.SNEAK) {
      user.game().abilityManager(user.worldKey()).firstInstance(user, HeatControl.class).ifPresent(HeatControl::solidify);
      return false;
    }
    this.user = user;
    loadConfig();
    removalPolicy = Policies.builder().build();
    startTime = System.currentTimeMillis();
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = user.game().configProcessor().calculate(this, config);
  }

  @Override
  public UpdateResult update() {
    if (removalPolicy.test(user, description()) || !user.canBend(description())) {
      solidify.clear();
      melt.clear();
      resetLight();
      return UpdateResult.CONTINUE;
    }
    melt.processQueue(1);
    long time = System.currentTimeMillis();
    Vector3d handLoc = user.mainHandSide();
    boolean dryHand = !MaterialUtil.isWater(user.world().blockAt(handLoc));
    if (dryHand && description().equals(user.selectedAbility())) {
      boolean sneaking = user.sneaking();
      if (sneaking) {
        if (startTime <= 0) {
          startTime = time;
        } else if (time > startTime + userConfig.cookInterval && cook()) {
          startTime = System.currentTimeMillis();
        }
        int freezeTicks = user.propertyValue(EntityProperties.FREEZE_TICKS);
        if (freezeTicks > 1) {
          user.setProperty(EntityProperties.FREEZE_TICKS, freezeTicks - 2);
        }
        solidify.processQueue(1);
      } else {
        solidify.clear();
        startTime = 0;
      }
      Block head = user.eyeBlock();
      if (sneaking || (canLight && head.world().lightLevel(head) < 7)) {
        ParticleBuilder.fire(user, handLoc).spawn(user.world());
        createLight(head);
      } else {
        resetLight();
      }
    } else {
      startTime = 0;
      resetLight();
    }
    return UpdateResult.CONTINUE;
  }

  private void createLight(Block block) {
    if (light != null && !block.equals(light.block())) {
      light.unlockAndRevert();
    }
    light = TempLight.builder(++ticks).rate(2).duration(0).build(block).map(TempLight::lock).orElse(null);
  }

  private void resetLight() {
    ticks = 3;
    if (light != null) {
      light.unlockAndRevert();
      light = null;
    }
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

  private void act() {
    if (user.onCooldown(description())) {
      return;
    }
    boolean acted = false;
    Vector3d center = user.rayTrace(userConfig.range).blocks(user.world()).position();
    Predicate<Block> predicate = b -> MaterialUtil.isFire(b) || MaterialUtil.isCampfire(b) || BlockTag.CANDLES.isTagged(b) || MaterialUtil.isMeltable(b);
    Predicate<Block> safe = b -> TempBlock.isBendable(b) && user.canBuild(b);
    List<Block> toMelt = new ArrayList<>();
    for (Block block : user.world().nearbyBlocks(center, userConfig.radius, predicate.and(safe))) {
      acted = true;
      if (MaterialUtil.isFire(block) || MaterialUtil.isCampfire(block) || BlockTag.CANDLES.isTagged(block)) {
        WorldUtil.tryExtinguishFire(user, block);
      } else if (MaterialUtil.isMeltable(block)) {
        toMelt.add(block);
      }
    }
    if (!toMelt.isEmpty()) {
      Collections.shuffle(toMelt);
      melt.fillQueue(toMelt);
    }
    if (acted) {
      user.addCooldown(description(), userConfig.cooldown);
    }
  }

  private void solidify() {
    if (user.onCooldown(description())) {
      return;
    }
    Vector3d center = user.rayTrace(userConfig.solidifyRange).ignoreLiquids(false).blocks(user.world()).position();
    if (solidify.fillQueue(getShuffledBlocks(center, userConfig.solidifyRadius, MaterialUtil::isLava))) {
      user.addCooldown(description(), userConfig.cooldown);
    }
  }

  public static void toggleLight(User user) {
    if (user.hasAbilitySelected("heatcontrol")) {
      var key = KeyUtil.data("heatcontrol-light", Light.class);
      if (user.store().canEdit(key)) {
        Light light = user.store().toggle(key, Light.ON);
        user.sendActionBar(Component.text("Light: " + light.name(), ColorPalette.TEXT_COLOR));
        user.game().abilityManager(user.worldKey()).firstInstance(user, HeatControl.class).ifPresent(h -> h.canLight = light == Light.ON);
      }
    }
  }

  private Collection<Block> getShuffledBlocks(Vector3d center, double radius, Predicate<Block> predicate) {
    List<Block> newBlocks = user.world().nearbyBlocks(center, radius, predicate);
    newBlocks.removeIf(b -> !user.canBuild(b));
    Collections.shuffle(newBlocks);
    return newBlocks;
  }

  public static boolean canBurn(User user) {
    AbilityDescription selected = user.selectedAbility();
    if (selected == null) {
      return true;
    }
    return !user.hasAbilitySelected("HeatControl") || !user.canBend(selected);
  }

  @Override
  public void onDestroy() {
    resetLight();
  }

  private class Solidify extends PhaseTransformer {
    @Override
    protected boolean processBlock(Block block) {
      if (MaterialUtil.isLava(block) && TempBlock.isBendable(block)) {
        return WorldUtil.tryCoolLava(user, block);
      }
      return false;
    }
  }

  private class Melt extends PhaseTransformer {
    @Override
    protected boolean processBlock(Block block) {
      if (!TempBlock.isBendable(block)) {
        return false;
      }
      return WorldUtil.tryMelt(user, block);
    }
  }

  @ConfigSerializable
  private static final class Config implements Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 2000;
    @Modifiable(Attribute.RANGE)
    private double range = 10;
    @Modifiable(Attribute.RADIUS)
    private double radius = 5;
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

