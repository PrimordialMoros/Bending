/*
 * Copyright 2020-2026 Moros
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

package me.moros.bending.common.ability.air.passive;

import java.util.List;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.config.attribute.ModifierOperation;
import me.moros.bending.api.platform.entity.AttributeType;
import me.moros.bending.api.platform.entity.EntityProperties;
import me.moros.bending.api.user.User;
import org.spongepowered.configurate.objectmapping.meta.Comment;

public class GracefulDescent extends AbilityInstance {
  private Config userConfig;

  private boolean modifiedFall = false;

  public GracefulDescent(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    loadConfig();
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = user.game().configProcessor().calculate(this, Config.class);
  }

  @Override
  public UpdateResult update() {
    if (!modifiedFall) {
      user.propertyValue(EntityProperties.ATTRIBUTES)
        .addModifier(
          AttributeType.SAFE_FALL_DISTANCE, description().key(),
          ModifierOperation.ADDITIVE, userConfig.safeFallDistanceBonus
        );
      modifiedFall = true;
    }
    return UpdateResult.CONTINUE;
  }

  @Override
  public void onDestroy() {
    user.propertyValue(EntityProperties.ATTRIBUTES)
      .removeModifier(AttributeType.SAFE_FALL_DISTANCE, description().key());
    modifiedFall = false;
  }

  private boolean isGraceful() {
    if (!user.canBend(description())) {
      return false;
    }
    return user.propertyValue(EntityProperties.ATTRIBUTES)
      .value(AttributeType.ARMOR).orElse(0) < userConfig.heavyArmorThreshold;
  }

  public static boolean isGraceful(User user) {
    return user.game().abilityManager(user.worldKey()).firstInstance(user, GracefulDescent.class)
      .map(GracefulDescent::isGraceful).orElse(false);
  }

  private static final class Config implements Configurable {
    @Modifiable(Attribute.HEIGHT)
    private double safeFallDistanceBonus = 8;

    @Comment("Armor points greater than or equal to this value are considered heavy and the passive will no longer function.")
    @Modifiable(Attribute.STRENGTH)
    private int heavyArmorThreshold = 8; // full leather armor provides 7 armor

    @Override
    public List<String> path() {
      return List.of("abilities", "air", "passives", "gracefuldescent");
    }
  }
}
