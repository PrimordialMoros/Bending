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

package me.moros.bending.common.ability.earth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.Updatable;
import me.moros.bending.api.ability.common.Pillar;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.Direction;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.functional.ExpireRemovalPolicy;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.material.EarthMaterials;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class Bulwark extends AbilityInstance {
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Updatable wall;
  private final Collection<Block> bases = new ArrayList<>();

  private boolean collapsing = false;
  private long startTime;

  public Bulwark(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    loadConfig();

    Block source = user.find(userConfig.wallRange, b -> EarthMaterials.isEarthNotLava(user, b));
    if (source == null) {
      return false;
    }

    RaiseEarth raiseWall = new RaiseEarth(description());
    if (raiseWall.activate(user, source, 2, 3, 75)) {
      removalPolicy = Policies.builder()
        .add(ExpireRemovalPolicy.of(5000 + userConfig.wallDuration))
        .build();
      user.addCooldown(description(), userConfig.wallCooldown);
      raiseWall.pillars().map(Pillar::origin).map(b -> b.offset(Direction.UP, 2)).forEach(bases::add);
      wall = raiseWall;
      startTime = System.currentTimeMillis();
      return true;
    }
    return false;
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

    if (System.currentTimeMillis() > startTime + userConfig.wallDuration) {
      collapse();
    }

    UpdateResult result = wall.update();
    return collapsing ? result : UpdateResult.CONTINUE;
  }

  private void collapse() {
    if (collapsing) {
      return;
    }
    collapsing = true;
    Collapse collapseWall = new Collapse(description());
    if (collapseWall.activate(user, bases, 2)) {
      wall = collapseWall;
    }
  }

  @ConfigSerializable
  private static final class Config implements Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long wallCooldown = 3000;
    @Modifiable(Attribute.DURATION)
    private long wallDuration = 2000;
    @Modifiable(Attribute.RANGE)
    private double wallRange = 4.5;

    @Override
    public List<String> path() {
      return List.of("abilities", "earth", "eartharmor", "wall");
    }
  }
}
