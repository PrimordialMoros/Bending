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

package me.moros.bending.ability.air.passives;

import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.PotionUtil;
import org.bukkit.potion.PotionEffectType;
import org.checkerframework.checker.nullness.qual.NonNull;

public class AirAgility extends AbilityInstance implements Ability {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  public AirAgility(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
    this.user = user;
    recalculateConfig();
    removalPolicy = Policies.builder().build();
    return true;
  }

  @Override
  public void recalculateConfig() {
    userConfig = Bending.game().attributeSystem().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, description()) || !user.canBend(description())) {
      return UpdateResult.CONTINUE;
    }
    handlePotionEffect(PotionEffectType.JUMP, userConfig.jumpAmplifier);
    handlePotionEffect(PotionEffectType.SPEED, userConfig.speedAmplifier);
    return UpdateResult.CONTINUE;
  }

  private void handlePotionEffect(PotionEffectType type, int amplifier) {
    if (amplifier < 0) {
      return;
    }
    PotionUtil.tryAddPotion(user.entity(), type, 100, amplifier);
  }

  @Override
  public void onDestroy() {
    user.entity().removePotionEffect(PotionEffectType.JUMP);
    user.entity().removePotionEffect(PotionEffectType.SPEED);
  }

  @Override
  public @NonNull User user() {
    return user;
  }

  private static class Config extends Configurable {
    @Attribute(Attribute.STRENGTH)
    public int speedAmplifier;
    @Attribute(Attribute.STRENGTH)
    public int jumpAmplifier;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "air", "passives", "airagility");

      speedAmplifier = abilityNode.node("speed-amplifier").getInt(2) - 1;
      jumpAmplifier = abilityNode.node("jump-amplifier").getInt(3) - 1;
    }
  }
}
