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

package me.moros.bending.common.ability.air;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.collision.CollisionUtil;
import me.moros.bending.api.collision.geometry.AABB;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.Direction;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.particle.ParticleBuilder;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.ColorPalette;
import me.moros.bending.api.util.KeyUtil;
import me.moros.bending.api.util.data.DataKey;
import me.moros.bending.api.util.functional.ExpireRemovalPolicy;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.functional.SwappedSlotsRemovalPolicy;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.math.Vector3d;
import net.kyori.adventure.text.Component;

public class Tornado extends AbilityInstance {
  private enum Mode {PUSH, PULL}

  private static final DataKey<Mode> KEY = KeyUtil.data("tornado-mode", Mode.class);

  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Mode mode;

  private double yOffset = 0;
  private double currentAngle = 0;
  private long startTime;

  public Tornado(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    loadConfig();
    removalPolicy = Policies.builder()
      .add(SwappedSlotsRemovalPolicy.of(description()))
      .add(ExpireRemovalPolicy.of(userConfig.duration))
      .add(Policies.NOT_SNEAKING)
      .add(Policies.UNDER_WATER)
      .add(Policies.UNDER_LAVA)
      .build();
    mode = user.store().get(KEY).orElse(Mode.PUSH);
    startTime = System.currentTimeMillis();
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
    Vector3d base = user.rayTrace(userConfig.range).ignoreLiquids(false).blocks(user.world()).position();
    Block baseBlock = user.world().blockAt(base);
    if (MaterialUtil.isTransparent(baseBlock.offset(Direction.DOWN))) {
      return UpdateResult.CONTINUE;
    }
    if (!user.canBuild(baseBlock)) {
      return UpdateResult.REMOVE;
    }

    long time = System.currentTimeMillis();
    double factor = Math.min(1, (time - startTime) / (double) userConfig.growthTime);
    double height = 2 + factor * (userConfig.height - 2);
    double radius = 2 + factor * (userConfig.radius - 2);
    double rBox = 0.6 * radius;
    AABB box = AABB.of(Vector3d.of(-rBox, 0, -rBox), Vector3d.of(rBox, height, rBox)).at(base);
    CollisionUtil.handle(user, box, entity -> {
      double dy = entity.location().y() - base.y();
      double r = 0.5 + (radius - 0.5) * dy;
      Vector3d delta = entity.center().subtract(base);
      double distSq = delta.x() * delta.x() + delta.z() * delta.z();
      if (distSq > r * r) {
        return false;
      }
      Vector3d velocity;
      if (entity.uuid().equals(user.uuid())) {
        double velY;
        if (dy >= height * .95) {
          velY = 0;
        } else if (dy >= height * .85) {
          velY = 6.0 * (.95 - dy / height);
        } else {
          velY = 0.6;
        }
        velocity = user.direction().withY(velY).multiply(factor);
      } else {
        if (mode == Mode.PULL) {
          velocity = delta.add(0, 0.75 * height, 0).normalize().multiply(factor);
        } else {
          Vector3d normal = delta.withY(0).normalize();
          Vector3d ortho = normal.cross(Vector3d.PLUS_J).normalize();
          velocity = ortho.add(normal).normalize().add(0, 0.5, 0).multiply(factor);
        }
      }
      entity.applyVelocity(this, velocity);
      return false;
    }, false, true);

    render(base, factor, height, radius);
    return UpdateResult.CONTINUE;
  }

  private void render(Vector3d base, double factor, double height, double radius) {
    double amount = Math.clamp(factor * 30, 4, 30);
    yOffset += 0.1;
    if (yOffset >= 1) {
      yOffset = 0;
    }
    currentAngle += 4.5;
    if (currentAngle >= 360) {
      currentAngle = 0;
    }
    for (int i = 0; i < 3; i++) {
      double offset = currentAngle + i * 2 * Math.PI / 3.0;
      for (double y = yOffset; y < height; y += (height / amount)) {
        double r = 0.5 + (radius - 0.5) * y / height;
        double x = r * Math.cos(y + offset);
        double z = r * Math.sin(y + offset);
        Vector3d loc = base.add(x, y, z);
        ParticleBuilder.air(loc).spawn(user.world());
        if (ThreadLocalRandom.current().nextInt(28) == 0) {
          SoundEffect.AIR.play(user.world(), loc);
        }
      }
    }
  }

  @Override
  public void onDestroy() {
    user.addCooldown(description(), userConfig.cooldown);
  }

  public static void switchMode(User user) {
    if (user.hasAbilitySelected("tornado")) {
      if (user.store().canEdit(KEY)) {
        Mode mode = user.store().toggle(KEY, Mode.PUSH);
        user.sendActionBar(Component.text("Mode: " + mode.name(), ColorPalette.TEXT_COLOR));
        user.game().abilityManager(user.worldKey()).firstInstance(user, Tornado.class).ifPresent(t -> t.mode = mode);
      }
    }
  }

  private static final class Config implements Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 4000;
    @Modifiable(Attribute.DURATION)
    private long duration = 8000;
    @Modifiable(Attribute.RADIUS)
    private double radius = 8;
    @Modifiable(Attribute.HEIGHT)
    private double height = 12;
    @Modifiable(Attribute.RANGE)
    private double range = 16;
    private long growthTime = 3000;

    @Override
    public List<String> path() {
      return List.of("abilities", "air", "tornado");
    }
  }
}
