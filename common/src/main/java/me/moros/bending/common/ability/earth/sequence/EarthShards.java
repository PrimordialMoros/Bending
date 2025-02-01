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

package me.moros.bending.common.ability.earth.sequence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.common.basic.ParticleStream;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.collision.geometry.Ray;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.particle.ParticleBuilder;
import me.moros.bending.api.platform.sound.Sound;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
import me.moros.math.VectorUtil;

// TODO restrictions based on earthglove cooldown, add bleed effect
public class EarthShards extends AbilityInstance {
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Collection<ShardStream> streams = new ArrayList<>();

  private int firedShots = 0;
  private long nextFireTime;

  public EarthShards(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    loadConfig();
    removalPolicy = Policies.defaults();
    user.addCooldown(description(), userConfig.cooldown);
    return true;
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
    if (firedShots < userConfig.maxShots) {
      long time = System.currentTimeMillis();
      if (time >= nextFireTime) {
        nextFireTime = time + userConfig.interval;
        launch(user.handSide(false), user.handSide(true));
      }
    }
    streams.removeIf(stream -> stream.update() == UpdateResult.REMOVE);
    return (streams.isEmpty() && firedShots >= userConfig.maxShots) ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
  }

  private void launch(Vector3d left, Vector3d right) {
    Vector3d target = user.rayTrace(userConfig.range).cast(user.world()).position();
    double distance = target.distance(user.eyeLocation());
    for (int i = 0; i < 2; i++) {
      if (firedShots >= userConfig.maxShots) {
        break;
      }
      firedShots++;
      Vector3d origin = (i == 0) ? right : left;
      Vector3d dir = VectorUtil.gaussianOffset(target, distance * userConfig.spread).subtract(origin);
      streams.add(new ShardStream(Ray.of(origin, dir)));
    }
  }

  @Override
  public Collection<Collider> colliders() {
    return streams.stream().map(ParticleStream::collider).toList();
  }

  private class ShardStream extends ParticleStream {
    private final Vector3d smallDir;
    private final int renderSteps;

    public ShardStream(Ray ray) {
      super(user, ray, userConfig.speed, 0.75);
      canCollide = BlockType::isLiquid;
      renderSteps = FastMath.ceil(userConfig.speed / 0.05);
      smallDir = ray.direction().normalize().multiply(0.05);
      Sound.BLOCK_STONE_BREAK.asEffect(1, 2).play(user.world(), ray.position());
    }

    @Override
    public void render() {
      int max = distanceTravelled <= 0 ? 1 : renderSteps;
      for (int i = 0; i < max; i++) {
        ParticleBuilder.rgb(location.subtract(smallDir.multiply(i)), "#555555", 0.15F).spawn(user.world());
      }
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      entity.damage(userConfig.damage, user, description());
      return true;
    }

    @Override
    public boolean onBlockHit(Block block) {
      return true;
    }
  }

  private static final class Config implements Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 10000;
    @Modifiable(Attribute.DAMAGE)
    private double damage = 0.5;
    @Modifiable(Attribute.RANGE)
    private double range = 16;
    @Modifiable(Attribute.SPEED)
    private double speed = 0.8;
    @Modifiable(Attribute.AMOUNT)
    private int maxShots = 10;
    private double spread = 0.02;
    private long interval = 100;

    @Override
    public List<String> path() {
      return List.of("abilities", "earth", "sequences", "earthshards");
    }
  }
}
