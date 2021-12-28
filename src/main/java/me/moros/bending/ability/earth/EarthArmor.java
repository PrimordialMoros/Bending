/*
 * Copyright 2020-2021 Moros
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

import java.util.function.Supplier;

import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempArmor;
import me.moros.bending.game.temporal.TempArmor.Builder;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.game.temporal.TempFallingBlock;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.potion.PotionEffectType;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class EarthArmor extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Mode mode;
  private TempFallingBlock fallingBlock;

  private boolean formed = false;
  private int resistance;

  public EarthArmor(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    if (Bending.game().abilityManager(user.world()).hasAbility(user, EarthArmor.class)) {
      return false;
    }

    this.user = user;
    loadConfig();

    Block source = user.find(userConfig.selectRange, b -> EarthMaterials.isEarthNotLava(user, b));
    if (source == null) {
      return false;
    }

    if (EarthMaterials.isMetalBendable(source)) {
      mode = source.getType() == Material.GOLD_BLOCK ? Mode.GOLD : Mode.IRON;
      resistance = userConfig.metalPower;
      SoundUtil.METAL.play(source.getLocation());
    } else {
      mode = Mode.ROCK;
      resistance = userConfig.power;
      SoundUtil.EARTH.play(source.getLocation());
    }
    BlockData data = source.getBlockData();
    TempBlock.air().duration(BendingProperties.EARTHBENDING_REVERT_TIME).build(source);
    fallingBlock = TempFallingBlock.builder(data).velocity(new Vector3d(0, 0.2, 0))
      .gravity(false).duration(10000).build(source);
    removalPolicy = Policies.builder().add(ExpireRemovalPolicy.of(5000)).build();
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

    if (formed) {
      return UpdateResult.CONTINUE;
    }

    return moveBlock() ? UpdateResult.CONTINUE : UpdateResult.REMOVE;
  }

  private void formArmor() {
    if (formed) {
      return;
    }
    mode.builder.get().duration(userConfig.duration).build(user);
    int duration = FastMath.round(userConfig.duration / 50.0);
    EntityUtil.tryAddPotion(user.entity(), PotionEffectType.DAMAGE_RESISTANCE, duration, resistance);
    removalPolicy = Policies.builder().add(ExpireRemovalPolicy.of(userConfig.duration)).build();
    user.addCooldown(description(), userConfig.cooldown);
    formed = true;
  }

  private boolean moveBlock() {
    if (!fallingBlock.fallingBlock().isValid()) {
      return false;
    }
    Vector3d center = fallingBlock.center();

    Block currentBlock = center.toBlock(user.world());
    WorldUtil.tryBreakPlant(currentBlock);
    if (!(currentBlock.isLiquid() || MaterialUtil.isAir(currentBlock) || EarthMaterials.isEarthbendable(user, currentBlock))) {
      return false;
    }

    final double distanceSquared = user.eyeLocation().distanceSq(center);
    final double speedFactor = (distanceSquared > userConfig.selectRange * userConfig.selectRange) ? 1.5 : 0.8;
    if (distanceSquared < 0.5) {
      fallingBlock.revert();
      formArmor();
      return true;
    }

    Vector3d dir = user.eyeLocation().subtract(center).normalize().multiply(speedFactor);
    EntityUtil.applyVelocity(this, fallingBlock.fallingBlock(), dir);
    return true;
  }

  @Override
  public void onDestroy() {
    Location center;
    if (!formed && fallingBlock != null) {
      center = fallingBlock.center().toLocation(user.world());
      fallingBlock.revert();
    } else {
      center = user.entity().getEyeLocation();
    }
    user.entity().removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
    SoundUtil.playSound(center, fallingBlock.fallingBlock().getBlockData().getSoundGroup().getBreakSound(), 2, 1);
    ParticleUtil.of(Particle.BLOCK_CRACK, center)
      .count(8).offset(0.5, 0.5, 0.5)
      .data(fallingBlock.fallingBlock().getBlockData()).spawn();
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  public static boolean hasArmor(@NonNull User user) {
    return Bending.game().abilityManager(user.world()).firstInstance(user, EarthArmor.class)
      .map(e -> e.formed).orElse(false);
  }

  private enum Mode {
    ROCK(TempArmor::leather),
    IRON(TempArmor::iron),
    GOLD(TempArmor::gold);

    private final Supplier<TempArmor.Builder> builder;

    Mode(Supplier<Builder> builder) {
      this.builder = builder;
    }
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.DURATION)
    public long duration;
    @Modifiable(Attribute.SELECTION)
    public double selectRange;
    @Modifiable(Attribute.STRENGTH)
    public int power;
    @Modifiable(Attribute.STRENGTH)
    public int metalPower;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "earth", "eartharmor");

      cooldown = abilityNode.node("cooldown").getLong(20000);
      duration = abilityNode.node("duration").getLong(12000);
      selectRange = abilityNode.node("select-range").getDouble(8.0);
      power = abilityNode.node("power").getInt(2) - 1;
      metalPower = abilityNode.node("metal-power").getInt(3) - 1;
    }
  }
}
