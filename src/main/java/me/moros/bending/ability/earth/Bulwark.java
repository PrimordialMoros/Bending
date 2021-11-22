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

package me.moros.bending.ability.earth;

import java.util.ArrayList;
import java.util.Collection;

import me.moros.bending.Bending;
import me.moros.bending.ability.common.Pillar;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.material.EarthMaterials;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class Bulwark extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Updatable wall;
  private final Collection<Block> bases = new ArrayList<>();

  private boolean collapsing = false;
  private long startTime;

  public Bulwark(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
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
      raiseWall.pillars().stream().map(Pillar::origin).map(b -> b.getRelative(BlockFace.UP, 2)).forEach(bases::add);
      wall = raiseWall;
      startTime = System.currentTimeMillis();
      return true;
    }
    return false;
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

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long wallCooldown;
    @Modifiable(Attribute.DURATION)
    public long wallDuration;
    @Modifiable(Attribute.RANGE)
    public double wallRange;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "earth", "eartharmor", "wall");

      wallCooldown = abilityNode.node("cooldown").getLong(3000);
      wallDuration = abilityNode.node("duration").getLong(2000);
      wallRange = abilityNode.node("range").getDouble(4.5);
    }
  }
}
