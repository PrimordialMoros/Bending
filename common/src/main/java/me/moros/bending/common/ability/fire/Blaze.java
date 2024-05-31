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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.MultiUpdatable;
import me.moros.bending.api.ability.common.basic.BlockLine;
import me.moros.bending.api.collision.geometry.Ray;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
import me.moros.math.VectorUtil;

public class Blaze extends AbilityInstance {
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final MultiUpdatable<FireStream> streams = MultiUpdatable.empty();
  private final Set<Block> affectedBlocks = new HashSet<>();

  public Blaze(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (user.game().abilityManager(user.worldKey()).hasAbility(user, Blaze.class)) {
      return false;
    }

    this.user = user;
    loadConfig();
    return release(method == Activation.ATTACK);
  }

  @Override
  public void loadConfig() {
    userConfig = user.game().configProcessor().calculate(this, Config.class);
  }

  private boolean release(boolean cone) {
    double range = cone ? userConfig.coneRange : userConfig.ringRange;

    Vector3d origin = user.location().center();
    Vector3d dir = user.direction().withY(0).normalize();
    if (cone) {
      double deltaAngle = Math.PI / (3 * range);
      VectorUtil.createArc(dir, Vector3d.PLUS_J, deltaAngle, FastMath.ceil(range / 1.5)).forEach(v ->
        streams.add(new FireStream(Ray.of(origin, v.multiply(range))))
      );
    } else {
      VectorUtil.circle(dir, Vector3d.PLUS_J, FastMath.ceil(6 * range)).forEach(v ->
        streams.add(new FireStream(Ray.of(origin, v.multiply(range))))
      );
    }

    // First update in same tick to only activate if there are valid fire streams
    if (streams.update() == UpdateResult.REMOVE) {
      return false;
    }
    removalPolicy = Policies.defaults();
    user.addCooldown(description(), userConfig.cooldown);
    return true;
  }

  @Override
  public UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    return streams.update();
  }

  @Override
  public void onDestroy() {
    if (!affectedBlocks.isEmpty()) {
      user.addCooldown(description(), userConfig.cooldown);
    }
  }

  private class FireStream extends BlockLine {
    public FireStream(Ray ray) {
      super(user, ray);
      this.interval = 70;
    }

    @Override
    public boolean isValidBlock(Block block) {
      return MaterialUtil.isFire(block) || MaterialUtil.isIgnitable(block);
    }

    @Override
    public void render(Block block) {
      if (!affectedBlocks.add(block)) {
        return;
      }
      TempBlock.fire().duration(500).ability(Blaze.this).build(block);
      if (ThreadLocalRandom.current().nextInt(6) == 0) {
        SoundEffect.FIRE.play(block);
      }
    }
  }

  private static final class Config implements Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 1000;
    @Modifiable(Attribute.RANGE)
    private double coneRange = 10;
    @Modifiable(Attribute.RANGE)
    private double ringRange = 7;

    @Override
    public List<String> path() {
      return List.of("abilities", "fire", "blaze");
    }
  }
}

