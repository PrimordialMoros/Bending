/*
 * Copyright 2020-2023 Moros
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

import java.util.List;

import me.moros.bending.BendingProperties;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.predicate.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.Policies;
import me.moros.bending.model.predicate.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.platform.block.Block;
import me.moros.bending.platform.block.BlockState;
import me.moros.bending.platform.block.BlockType;
import me.moros.bending.platform.potion.PotionEffect;
import me.moros.bending.platform.sound.SoundEffect;
import me.moros.bending.temporal.TempArmor;
import me.moros.bending.temporal.TempBlock;
import me.moros.bending.temporal.TempEntity;
import me.moros.bending.temporal.TempEntity.TempFallingBlock;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class EarthArmor extends AbilityInstance {
  private enum Mode {ROCK, IRON, GOLD}

  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Mode mode;
  private TempFallingBlock temp;
  private BlockState data;

  private boolean formed = false;
  private int resistance;

  public EarthArmor(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (user.game().abilityManager(user.worldKey()).hasAbility(user, EarthArmor.class)) {
      return false;
    }

    this.user = user;
    loadConfig();

    Block source = user.find(userConfig.selectRange, b -> EarthMaterials.isEarthNotLava(user, b));
    if (source == null) {
      return false;
    }

    if (EarthMaterials.isMetalBendable(source)) {
      mode = source.type() == BlockType.GOLD_BLOCK ? Mode.GOLD : Mode.IRON;
      resistance = userConfig.metalPower;
      SoundEffect.METAL.play(source);
    } else {
      mode = Mode.ROCK;
      resistance = userConfig.power;
      SoundEffect.EARTH.play(source);
    }
    data = source.state();
    TempBlock.air().duration(BendingProperties.instance().earthRevertTime()).build(source);
    temp = TempEntity.fallingBlock(data).velocity(Vector3d.of(0, 0.2, 0))
      .gravity(false).duration(10000).buildReal(source);
    removalPolicy = Policies.builder().add(ExpireRemovalPolicy.of(5000)).build();
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
    if (formed) {
      return UpdateResult.CONTINUE;
    }
    return moveBlock() ? UpdateResult.CONTINUE : UpdateResult.REMOVE;
  }

  private void formArmor() {
    if (formed) {
      return;
    }
    var armorBuilder = switch (mode) {
      case ROCK -> TempArmor.leather();
      case IRON -> TempArmor.iron();
      case GOLD -> TempArmor.gold();
    };
    armorBuilder.duration(userConfig.duration).build(user);
    int duration = FastMath.round(userConfig.duration / 50.0);
    EntityUtil.tryAddPotion(user, PotionEffect.RESISTANCE, duration, resistance - 1);
    removalPolicy = Policies.builder().add(ExpireRemovalPolicy.of(userConfig.duration)).build();
    user.addCooldown(description(), userConfig.cooldown);
    formed = true;
  }

  private boolean moveBlock() {
    if (!temp.isValid()) {
      return false;
    }
    Vector3d center = temp.center();

    Block currentBlock = user.world().blockAt(center);
    WorldUtil.tryBreakPlant(currentBlock);
    if (!(currentBlock.type().isLiquid() || MaterialUtil.isAir(currentBlock) || EarthMaterials.isEarthbendable(user, currentBlock))) {
      return false;
    }

    final double distanceSquared = user.eyeLocation().distanceSq(center);
    final double speedFactor = (distanceSquared > userConfig.selectRange * userConfig.selectRange) ? 1.5 : 0.8;
    if (distanceSquared < 0.5) {
      temp.revert();
      formArmor();
      return true;
    }

    Vector3d dir = user.eyeLocation().subtract(center).normalize().multiply(speedFactor);
    temp.entity().applyVelocity(this, dir);
    return true;
  }

  @Override
  public void onDestroy() {
    Vector3d center;
    if (!formed && temp != null) {
      center = temp.center();
      temp.revert();
    } else {
      center = user.eyeLocation();
    }
    user.removePotion(PotionEffect.RESISTANCE);
    data.type().soundGroup().breakSound().asEffect(2, 1).play(user.world(), center);
    data.asParticle(center).count(8).offset(0.5).spawn(user.world());
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  public static boolean hasArmor(User user) {
    return user.game().abilityManager(user.worldKey()).firstInstance(user, EarthArmor.class)
      .map(e -> e.formed).orElse(false);
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 20_000;
    @Modifiable(Attribute.DURATION)
    private long duration = 12_000;
    @Modifiable(Attribute.SELECTION)
    private double selectRange = 8;
    @Modifiable(Attribute.STRENGTH)
    private int power = 2;
    @Modifiable(Attribute.STRENGTH)
    private int metalPower = 3;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "earth", "eartharmor");
    }
  }
}