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

package me.moros.bending.ability.water;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.FragileStructure;
import me.moros.bending.ability.common.basic.BlockStream;
import me.moros.bending.ability.water.sequences.WaterGimbal;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.state.State;
import me.moros.bending.model.ability.state.StateChain;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.checkerframework.checker.nullness.qual.NonNull;

public class Torrent extends AbilityInstance {
  private static final Config config = new Config();
  private static AbilityDescription ringDesc;

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private StateChain states;
  private WaterRing ring;

  public Torrent(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
    if (Bending.game().abilityManager(user.world()).hasAbility(user, WaterGimbal.class)) {
      return false;
    }

    Optional<Torrent> torrent = Bending.game().abilityManager(user.world()).firstInstance(user, Torrent.class);
    if (torrent.isPresent()) {
      torrent.get().launch();
      return false;
    }

    this.user = user;
    recalculateConfig();

    if (ringDesc == null) {
      ringDesc = Bending.game().abilityRegistry().abilityDescription("WaterRing").orElseThrow(RuntimeException::new);
    }

    ring = Bending.game().abilityManager(user.world()).firstInstance(user, WaterRing.class).orElse(null);
    if (ring == null) {
      ring = new WaterRing(ringDesc);
      if (ring.activate(user, method)) {
        Bending.game().abilityManager(user.world()).addAbility(user, ring);
      } else {
        return false;
      }
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
      if (ring.isReady() && !user.isOnCooldown(description())) {
        states = new StateChain(ring.complete()).addState(new TorrentStream()).start();
        user.addCooldown(description(), userConfig.cooldown);
      }
    } else {
      State current = states.current();
      if (current instanceof TorrentStream) {
        ((TorrentStream) current).clicked = true;
      }
    }
  }

  @Override
  public void recalculateConfig() {
    userConfig = Bending.game().attributeSystem().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    if (states != null) {
      return states.update();
    } else {
      if (!Bending.game().abilityManager(user.world()).hasAbility(user, WaterRing.class)) {
        return UpdateResult.REMOVE;
      }
    }
    return UpdateResult.CONTINUE;
  }

  @Override
  public void onDestroy() {
    if (states != null) {
      State current = states.current();
      if (current instanceof TorrentStream) {
        ((TorrentStream) current).cleanAll();
      }
    }
  }

  @Override
  public @NonNull User user() {
    return user;
  }

  @Override
  public @NonNull Collection<@NonNull Collider> colliders() {
    if (states != null) {
      State current = states.current();
      if (current instanceof TorrentStream) {
        return ((TorrentStream) current).colliders();
      }
    }
    return Collections.emptyList();
  }

  private class TorrentStream extends BlockStream {
    private final Set<Entity> affectedEntities = new HashSet<>();
    private boolean shouldFreeze = false;
    private boolean clicked = false;

    public TorrentStream() {
      super(user, Material.WATER, userConfig.range, 90);
    }

    @Override
    public boolean onEntityHit(@NonNull Entity entity) {
      Vector3 velocity = direction.setY(FastMath.min(direction.getY(), userConfig.verticalPush));
      entity.setVelocity(velocity.scalarMultiply(userConfig.knockback).clampVelocity());
      if (entity instanceof LivingEntity) {
        if (clicked && !shouldFreeze) {
          shouldFreeze = true;
        }
        if (!affectedEntities.contains(entity)) {
          double damage = shouldFreeze ? userConfig.damage : userConfig.damage + userConfig.freezeDamage;  // apply bonus damage on freeze
          DamageUtil.damageEntity(entity, user, damage, description());
          affectedEntities.add(entity);
        }
      }
      return false;
    }

    @Override
    public void postRender() {
      if (shouldFreeze) {
        freeze();
      }
    }

    public void freeze() {
      if (!clicked || stream.isEmpty()) {
        return;
      }
      Block head = stream.getFirst();
      if (head == null) {
        return;
      }
      cleanAll();
      for (Block block : stream) {
        TempBlock.create(block, Material.ICE.createBlockData(), userConfig.freezeDuration, true);
      }
      FragileStructure.tryDamageStructure(Collections.singletonList(head), 8);
      stream.clear();
    }

    @Override
    public void onBlockHit(@NonNull Block block) {
      if (clicked) {
        freeze();
        return;
      }
      FragileStructure.tryDamageStructure(Collections.singletonList(block), 1);
    }
  }

  private static class Config extends Configurable {
    @Attribute(Attribute.COOLDOWN)
    public long cooldown;
    @Attribute(Attribute.RANGE)
    public double range;
    @Attribute(Attribute.DAMAGE)
    public double damage;
    @Attribute(Attribute.DAMAGE)
    public double freezeDamage;
    @Attribute(Attribute.STRENGTH)
    public double knockback;
    @Attribute(Attribute.STRENGTH)
    public double verticalPush;
    @Attribute(Attribute.DURATION)
    public long freezeDuration;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "water", "torrent");

      cooldown = abilityNode.node("cooldown").getLong(5000);
      range = abilityNode.node("range").getDouble(32.0);
      damage = abilityNode.node("damage").getDouble(3.0);
      freezeDamage = abilityNode.node("freeze-bonus-damage").getDouble(2.0);
      knockback = abilityNode.node("knockback").getDouble(1.0);
      verticalPush = abilityNode.node("vertical-push").getDouble(0.2);
      freezeDuration = abilityNode.node("freeze-duration").getLong(12500);
    }
  }
}
