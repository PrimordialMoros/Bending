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

package me.moros.bending.common.ability.water;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.ActionType;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.Updatable;
import me.moros.bending.api.collision.CollisionUtil;
import me.moros.bending.api.collision.geometry.Sphere;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.EntityTypeTag;
import me.moros.bending.api.platform.entity.EntityUtil;
import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.bending.api.platform.item.EquipmentSlot;
import me.moros.bending.api.platform.item.PlayerInventory;
import me.moros.bending.api.platform.potion.PotionEffect;
import me.moros.bending.api.platform.sound.Sound;
import me.moros.bending.api.temporal.ActionLimiter;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.ColorPalette;
import me.moros.bending.api.util.ExpiringSet;
import me.moros.bending.api.util.KeyUtil;
import me.moros.bending.api.util.data.DataKey;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.functional.SwappedSlotsRemovalPolicy;
import me.moros.bending.api.util.qte.QuickTimeEvent;
import me.moros.bending.common.util.stamina.StaminaBar;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
import me.moros.math.VectorUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

public class BloodBending extends AbilityInstance {
  private enum Mode {SINGLE, MULTI}

  private static final DataKey<Mode> KEY = KeyUtil.data("bloodbending-mode", Mode.class);

  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Map<UUID, BloodBendingEffect> targets = new HashMap<>();
  private final ExpiringSet<UUID> disarmedTargets = new ExpiringSet<>(20_000);

  private StaminaBar stamina;

  public BloodBending(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;

    boolean attackActivation = method == Activation.ATTACK;
    BloodBending instance = user.game().abilityManager(user.worldKey()).firstInstance(user, BloodBending.class)
      .orElse(null);
    if (instance != null) {
      if (attackActivation) {
        instance.tryDisarm();
      } else {
        instance.acquireTargets();
      }
      return false;
    }

    if (attackActivation) {
      return false;
    }

    loadConfig();
    Component title = Component.text("Stamina", ColorPalette.WATER);
    stamina = StaminaBar.builder()
      .bar(BossBar.bossBar(title, 1, Color.BLUE, Overlay.PROGRESS))
      .maxStamina(userConfig.stamina.capacity)
      .staminaRegen(userConfig.stamina.regen)
      .build(this.user);
    if (acquireTargets()) {
      removalPolicy = Policies.builder().add(SwappedSlotsRemovalPolicy.of(description())).build();
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
    if (!user.sneaking()) {
      reset();
    } else {
      targets.entrySet().removeIf(e -> e.getValue().update() == UpdateResult.REMOVE);
    }
    return stamina.update();
  }

  private void tryDisarm() {
    for (BloodBendingEffect effect : targets.values()) {
      if (disarmedTargets.add(effect.target.uuid())) {
        effect.attemptDisarm();
      }
    }
  }

  private boolean acquireTargets() {
    Mode mode = user.store().get(KEY).orElse(Mode.SINGLE);
    List<Entity> collectedEntities = new ArrayList<>();
    if (mode == Mode.SINGLE) {
      Entity direct = user.rayTrace(userConfig.selectRange + 1).raySize(0.6).cast(user.world()).entity();
      if (direct != null) {
        collectedEntities.add(direct);
      }
    } else {
      Vector3d center = user.center();
      CollisionUtil.handle(user, Sphere.of(center, userConfig.radius), collectedEntities::add);
      collectedEntities.sort(Comparator.comparingDouble(e -> e.center().distanceSq(center)));
    }
    for (Entity entity : collectedEntities) {
      if (entity instanceof LivingEntity living) {
        tryTrackEntity(living).ifPresent(effect -> targets.put(living.uuid(), effect));
      }
    }
    return !targets.isEmpty();
  }

  @Override
  public void onDestroy() {
    reset();
    user.addCooldown(description(), userConfig.cooldown);
  }

  private void reset() {
    stamina.reset();
    targets.values().forEach(BloodBendingEffect::onRemove);
    targets.clear();
  }

  private Optional<BloodBendingEffect> tryTrackEntity(LivingEntity target) {
    if (!targets.containsKey(target.uuid()) && !EntityTypeTag.SENSITIVE_TO_SMITE.containsValue(target.type())) {
      if (targets.size() < userConfig.maxTargets && stamina.drainCost(userConfig.stamina.initialCost)) {
        return Optional.of(new BloodBendingEffect(this, target, userConfig.stamina.drainPerTarget));
      }
    }
    return Optional.empty();
  }

  public static void switchMode(User user) {
    if (user.hasAbilitySelected("bloodbending")) {
      if (user.store().canEdit(KEY)) {
        Mode mode = user.store().toggle(KEY, Mode.SINGLE);
        user.sendActionBar(Component.text("Target: " + mode.name(), ColorPalette.TEXT_COLOR));
      }
    }
  }

  private static final class BloodBendingEffect implements Updatable {
    private static final float INITIAL_SPEED_FACTOR = 1;
    private static final float MAX_SPEED_FACTOR = 1.6F;
    private static final float MAX_PANIC_SPEED_FACTOR = 2.4F;

    private final BloodBending parent;
    private final LivingEntity target;
    private final Vector3d targetLocation;
    private final double distanceSquared;
    private final int drain;

    private boolean limited;

    private int ticks;
    private int nextHeartbeatTick;
    private boolean heartbeatAlt;
    private float speedFactor;
    private float maxSpeedFactor;
    private ResistDisarm disarm;
    private TriState sneaking;

    private BloodBendingEffect(BloodBending parent, LivingEntity target, int drain) {
      this.parent = parent;
      this.target = target;
      this.targetLocation = target.location().withY(0.5 + parent.user().eyeLocation().y());
      this.distanceSquared = Math.pow(parent.userConfig.radius + 2, 2);
      this.drain = drain;
      this.limited = false;
      this.ticks = 0;
      this.nextHeartbeatTick = ThreadLocalRandom.current().nextInt(3);
      this.heartbeatAlt = false;
      this.speedFactor = INITIAL_SPEED_FACTOR;
      this.maxSpeedFactor = MAX_SPEED_FACTOR;
      this.sneaking = TriState.NOT_SET;
    }

    @Override
    public UpdateResult update() {
      if (!isValidTarget() || !parent.stamina.tickingDrain(drain)) {
        onRemove();
        return UpdateResult.REMOVE;
      }
      heartbeat();
      limitTarget();
      handleDisarmUpdate();
      EntityUtil.tryAddPotion(target, PotionEffect.DARKNESS, 100, 0);
      Vector3d randomizedNearbyLocation = VectorUtil.gaussianOffset(targetLocation, 0.25);
      Vector3d direction = randomizedNearbyLocation.subtract(target.location());
      double factor = 0.2 * direction.length();
      target.applyVelocity(parent, direction.normalize().multiply(Math.clamp(factor, -0.5, 0.5)));
      return UpdateResult.CONTINUE;
    }

    private void panic() {
      maxSpeedFactor = MAX_PANIC_SPEED_FACTOR;
    }

    private void resetPanic() {
      maxSpeedFactor = MAX_SPEED_FACTOR;
    }

    private void handleHeartbeatSpeed() {
      float signum = Math.signum(maxSpeedFactor - speedFactor);
      if (signum != 0) {
        float delta = (maxSpeedFactor - INITIAL_SPEED_FACTOR) / 40;
        speedFactor = Math.clamp(speedFactor + (signum * delta), INITIAL_SPEED_FACTOR, maxSpeedFactor);
      }
    }

    private void heartbeat() {
      handleHeartbeatSpeed();
      if (++ticks < nextHeartbeatTick) {
        return;
      }
      if (disarm != null && disarm.isFocused()) {
        return;
      }
      int ticksPerCycle = FastMath.ceil(20 / speedFactor);
      nextHeartbeatTick = ticks + FastMath.ceil(ticksPerCycle * (heartbeatAlt ? 0.65F : 0.35F));
      float pitch = heartbeatAlt ? 1.3F : 1.8F;
      target.playSound(Sound.ENTITY_WARDEN_HEARTBEAT.asEffect(0.4F * speedFactor, pitch).sound());
      heartbeatAlt = !heartbeatAlt;
    }

    private void limitTarget() {
      if (limited) {
        return;
      }
      limited = ActionLimiter.builder()
        .limit(EnumSet.complementOf(EnumSet.of(ActionType.MOVE)))
        .showBar(false)
        .duration(30000)
        .build(parent.user(), target)
        .isPresent();
    }

    private void handleDisarmUpdate() {
      if (disarm == null) {
        return;
      }
      boolean targetSneaking = target.sneaking();
      if (sneaking == TriState.NOT_SET) {
        if (!targetSneaking) {
          sneaking = TriState.FALSE;
        }
      } else if (sneaking == TriState.FALSE) {
        if (targetSneaking) {
          sneaking = TriState.TRUE;
          disarm.attempt();
        }
      }
      if (disarm.update() == UpdateResult.REMOVE) {
        disarm = null;
      }
    }

    private void onRemove() {
      if (disarm != null) {
        disarm.onRemove();
      }
      ActionLimiter.MANAGER.get(target.uuid()).ifPresent(ActionLimiter::revert);
    }

    private boolean isValidTarget() {
      if (!target.valid()) {
        return false;
      }
      User user = parent.user();
      return target.world().equals(user.world()) && target.location().distanceSq(user.eyeLocation()) <= distanceSquared;
    }

    private void attemptDisarm() {
      if (disarm == null) {
        int cost = 5 * parent.userConfig.stamina.drainPerTarget;
        if (parent.stamina.hasAtLeast(cost)) {
          disarm = new ResistDisarm(this, cost);
          panic();
        }
      }
    }
  }

  private static final class ResistDisarm extends QuickTimeEvent {
    private final BloodBendingEffect parent;
    private final int staminaCost;

    private ResistDisarm(BloodBendingEffect parent, int staminaCost) {
      super(parent.target, Component.text("Resist Disarm"), 50); // 2.5s
      this.parent = parent;
      this.staminaCost = staminaCost;
    }

    @Override
    protected void onSuccess() {
      Sound.ENTITY_PLAYER_ATTACK_WEAK.asEffect(1, 0.4F).play(parent.target.world(), parent.target.center());
      parent.parent.stamina.drainCost(staminaCost);
      parent.resetPanic();
    }

    @Override
    protected void onFailure() {
      Vector3d center = parent.target.center();
      Sound.ENTITY_PLAYER_ATTACK_CRIT.asEffect(1, 1.8F).play(parent.target.world(), center);
      Sound.BLOCK_BONE_BLOCK_BREAK.asEffect(1, 1.2F).play(parent.target.world(), center);
      if (parent.target.inventory() instanceof PlayerInventory inv) {
        inv.dropItem(EquipmentSlot.MAINHAND);
        inv.dropItem(EquipmentSlot.OFFHAND);
      }
      parent.parent.stamina.fill(staminaCost);
    }
  }

  private static final class Config implements Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 20000;
    @Modifiable(Attribute.SELECTION)
    private double selectRange = 8;
    @Modifiable(Attribute.RADIUS)
    private double radius = 5;
    @Modifiable(Attribute.AMOUNT)
    private int maxTargets = 5;

    private StaminaConfig stamina = new StaminaConfig();

    @Override
    public List<String> path() {
      return List.of("abilities", "water", "bloodbending");
    }
  }

  @ConfigSerializable
  private static final class StaminaConfig {
    @Comment("Total stamina capacity")
    private int capacity = 200;
    @Comment("Initial stamina cost when starting to bloodbend a target")
    private int initialCost = 20;
    @Comment("Base stamina drain per second")
    private int drainPerTarget = 15;
    @Comment("Stamina regeneration per second")
    private int regen = 25;
  }
}
