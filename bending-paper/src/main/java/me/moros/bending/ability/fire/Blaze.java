/*
 * Copyright 2020-2022 Moros
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

package me.moros.bending.ability.fire;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.Bending;
import me.moros.bending.ability.common.basic.BlockLine;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.VectorUtil;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class Blaze extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Collection<FireStream> streams = new ArrayList<>();
  private final Set<Block> affectedBlocks = new HashSet<>();

  public Blaze(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    if (Bending.game().abilityManager(user.world()).hasAbility(user, Blaze.class)) {
      return false;
    }

    this.user = user;
    loadConfig();
    return release(method == Activation.ATTACK);
  }

  @Override
  public void loadConfig() {
    userConfig = Bending.configManager().calculate(this, config);
  }

  private boolean release(boolean cone) {
    double range = cone ? userConfig.coneRange : userConfig.ringRange;

    Vector3d origin = user.location().snapToBlockCenter();
    Vector3d dir = user.direction().withY(0).normalize();
    if (cone) {
      double deltaAngle = Math.PI / (3 * range);
      VectorUtil.createArc(dir, Vector3d.PLUS_J, deltaAngle, FastMath.ceil(range / 1.5)).forEach(v ->
        streams.add(new FireStream(new Ray(origin, v.multiply(range))))
      );
    } else {
      VectorUtil.circle(dir, Vector3d.PLUS_J, FastMath.ceil(6 * range)).forEach(v ->
        streams.add(new FireStream(new Ray(origin, v.multiply(range))))
      );
    }

    // First update in same tick to only activate if there are valid fire streams
    streams.removeIf(stream -> stream.update() == UpdateResult.REMOVE);
    if (streams.isEmpty()) {
      return false;
    }
    removalPolicy = Policies.builder().build();
    user.addCooldown(description(), userConfig.cooldown);
    return true;
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    streams.removeIf(stream -> stream.update() == UpdateResult.REMOVE);
    return streams.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
  }

  @Override
  public void onDestroy() {
    if (!affectedBlocks.isEmpty()) {
      user.addCooldown(description(), userConfig.cooldown);
    }
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  private class FireStream extends BlockLine {
    public FireStream(Ray ray) {
      super(user, ray);
      this.interval = 70;
    }

    @Override
    public boolean isValidBlock(@NonNull Block block) {
      return MaterialUtil.isFire(block) || MaterialUtil.isIgnitable(block);
    }

    @Override
    public void render(@NonNull Block block) {
      if (!affectedBlocks.add(block)) {
        return;
      }
      TempBlock.fire().duration(500).build(block);
      if (ThreadLocalRandom.current().nextInt(6) == 0) {
        SoundUtil.FIRE.play(block);
      }
    }
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.RANGE)
    public double coneRange;
    @Modifiable(Attribute.RANGE)
    public double ringRange;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "fire", "blaze");

      cooldown = abilityNode.node("cooldown").getLong(1000);
      coneRange = abilityNode.node("cone-range").getDouble(10.0);
      ringRange = abilityNode.node("ring-range").getDouble(7.0);
    }
  }
}

