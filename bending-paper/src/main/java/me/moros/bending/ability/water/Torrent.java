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

package me.moros.bending.ability.water;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.ability.water.sequence.WaterGimbal;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.common.FragileStructure;
import me.moros.bending.model.ability.common.basic.BlockStream;
import me.moros.bending.model.ability.state.State;
import me.moros.bending.model.ability.state.StateChain;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.predicate.Policies;
import me.moros.bending.model.predicate.RemovalPolicy;
import me.moros.bending.model.predicate.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.temporal.TempBlock;
import me.moros.bending.temporal.TempBlock.Builder;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.math.Vector3d;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class Torrent extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private StateChain states;
  private WaterRing ring;

  public Torrent(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (user.game().abilityManager(user.world()).hasAbility(user, WaterGimbal.class)) {
      return false;
    }

    Optional<Torrent> torrent = user.game().abilityManager(user.world()).firstInstance(user, Torrent.class);
    if (torrent.isPresent()) {
      torrent.get().launch();
      return false;
    }

    this.user = user;
    loadConfig();

    ring = WaterRing.getOrCreateInstance(user);
    if (ring == null) {
      return false;
    }

    if (ring.isReady()) {
      List<Block> sources = ring.complete();
      if (sources.isEmpty()) {
        return false;
      }
      states = new StateChain(sources).addState(new TorrentStream()).start();
    }

    removalPolicy = Policies.builder().add(SwappedSlotsRemovalPolicy.of(description())).build();
    return true;
  }

  private void launch() {
    if (states == null) {
      if (ring.isReady() && !user.onCooldown(description())) {
        List<Block> sources = ring.complete();
        if (!sources.isEmpty()) {
          states = new StateChain(sources).addState(new TorrentStream()).start();
          user.addCooldown(description(), userConfig.cooldown);
        }
      }
    } else {
      State current = states.current();
      if (current instanceof TorrentStream torrentStream) {
        torrentStream.clicked = true;
      }
    }
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
    if (states != null) {
      return states.update();
    } else {
      if (ring.isDestroyed()) {
        return UpdateResult.REMOVE;
      }
    }
    return UpdateResult.CONTINUE;
  }

  @Override
  public void onDestroy() {
    if (states != null) {
      State current = states.current();
      if (current instanceof TorrentStream torrentStream) {
        torrentStream.cleanAll();
      }
    }
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @Override
  public Collection<Collider> colliders() {
    if (states != null && states.current() instanceof TorrentStream torrentStream) {
      return torrentStream.colliders();
    }
    return List.of();
  }

  private class TorrentStream extends BlockStream {
    private final Set<Entity> affectedEntities = new HashSet<>();
    private boolean shouldFreeze = false;
    private boolean clicked = false;

    public TorrentStream() {
      super(user, Material.WATER, userConfig.range, 18);
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      if (entity instanceof LivingEntity) {
        if (clicked && !shouldFreeze) {
          shouldFreeze = true;
        }
        if (!affectedEntities.contains(entity)) {
          double damage = shouldFreeze ? userConfig.damage : userConfig.damage + userConfig.freezeBonusDamage;  // apply bonus damage on freeze
          DamageUtil.damageEntity(entity, user, damage, description());
          affectedEntities.add(entity);
        }
      }
      Vector3d velocity = direction.withY(Math.min(direction.y(), userConfig.knockup)).multiply(userConfig.knockback);
      EntityUtil.applyVelocity(Torrent.this, entity, velocity);
      return false;
    }

    @Override
    public void postRender() {
      if (shouldFreeze) {
        freeze();
        return;
      }
      if (ThreadLocalRandom.current().nextInt(5) == 0) {
        Block head = stream.peekFirst();
        if (head != null) {
          SoundUtil.WATER.play(head);
        }
      }
    }

    public boolean freeze() {
      if (!clicked || stream.isEmpty()) {
        return false;
      }
      Block head = stream.getFirst();
      if (head == null) {
        return false;
      }
      cleanAll();
      Builder builder = TempBlock.ice().duration(userConfig.freezeDuration);
      stream.forEach(builder::build);
      stream.clear();
      return true;
    }

    @Override
    public void onBlockHit(Block block) {
      Ray ray = new Ray(Vector3d.fromCenter(block), direction);
      if (clicked) {
        if (freeze()) {
          FragileStructure.tryDamageStructure(block, 8, ray);
        }
        return;
      }
      FragileStructure.tryDamageStructure(block, 1, ray);
    }
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 5000;
    @Modifiable(Attribute.RANGE)
    private double range = 32;
    @Modifiable(Attribute.DAMAGE)
    private double damage = 3;
    @Modifiable(Attribute.DAMAGE)
    private double freezeBonusDamage = 2;
    @Modifiable(Attribute.STRENGTH)
    private double knockback = 1;
    @Modifiable(Attribute.STRENGTH)
    private double knockup = 0.2;
    @Modifiable(Attribute.DURATION)
    private long freezeDuration = 12500;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "water", "torrent");
    }
  }
}
