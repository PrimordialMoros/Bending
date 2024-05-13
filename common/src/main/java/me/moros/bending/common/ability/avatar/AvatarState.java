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

package me.moros.bending.common.ability.avatar;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.config.attribute.Modifier;
import me.moros.bending.api.config.attribute.ModifierOperation;
import me.moros.bending.api.config.attribute.ModifyPolicy;
import me.moros.bending.api.platform.Platform;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.platform.potion.PotionEffect;
import me.moros.bending.api.platform.sound.Sound;
import me.moros.bending.api.temporal.ActionLimiter;
import me.moros.bending.api.temporal.Cooldown;
import me.moros.bending.api.temporal.Temporary;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.BendingBar;
import me.moros.bending.api.util.ColorPalette;
import me.moros.bending.api.util.KeyUtil;
import me.moros.bending.api.util.collect.ElementSet;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.functional.SwappedSlotsRemovalPolicy;
import me.moros.bending.common.ability.avatar.ChakraFocus.FocusResult;
import me.moros.bending.common.config.ConfigManager;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import static java.util.Map.entry;

public class AvatarState extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private static final Key AVATAR_MODIFIER_KEY = KeyUtil.simple("avatar-modifier");
  private static final Map<Attribute, Modifier> AVATAR_MODIFIERS = Map.ofEntries(
    entry(Attribute.RANGE, Modifier.of(ModifierOperation.SUMMED_MULTIPLICATIVE, 0.25)),
    entry(Attribute.SELECTION, Modifier.of(ModifierOperation.SUMMED_MULTIPLICATIVE, 0.25)),
    entry(Attribute.STRENGTH, Modifier.of(ModifierOperation.SUMMED_MULTIPLICATIVE, 0.25)),
    entry(Attribute.COOLDOWN, Modifier.of(ModifierOperation.MULTIPLICATIVE, 0.5)),
    entry(Attribute.CHARGE_TIME, Modifier.of(ModifierOperation.MULTIPLICATIVE, 0.5)),
    entry(Attribute.DAMAGE, Modifier.of(ModifierOperation.MULTIPLICATIVE, 2)),
    entry(Attribute.RADIUS, Modifier.of(ModifierOperation.MULTIPLICATIVE, 2)),
    entry(Attribute.HEIGHT, Modifier.of(ModifierOperation.MULTIPLICATIVE, 2)),
    entry(Attribute.AMOUNT, Modifier.of(ModifierOperation.MULTIPLICATIVE, 3)),
    entry(Attribute.FIRE_TICKS, Modifier.of(ModifierOperation.MULTIPLICATIVE, 2)),
    entry(Attribute.FREEZE_TICKS, Modifier.of(ModifierOperation.MULTIPLICATIVE, 2))
  );

  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private ChakraFocus chakraFocus;
  private BendingBar durationBar;
  private AvatarModifyPolicy avatarPolicy;

  private long cooldown;

  public AvatarState(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    AvatarState instance = user.game().abilityManager(user.worldKey())
      .firstInstance(user, AvatarState.class).orElse(null);
    if (method == Activation.ATTACK) {
      if (instance != null) {
        instance.onClick();
      }
      return false;
    } else if (instance != null) {
      return false;
    }

    this.user = user;
    loadConfig();

    removalPolicy = Policies.builder().add(SwappedSlotsRemovalPolicy.of(description())).build();
    cooldown = userConfig.transientCooldown;
    chakraFocus = new ChakraFocus(user);
    return true;
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
    if (durationBar != null && durationBar.update() == UpdateResult.REMOVE) {
      durationBar.onRemove();
      durationBar = null;
      return UpdateResult.REMOVE;
    }
    if (chakraFocus != null && chakraFocus.update() == UpdateResult.REMOVE) {
      tryActivate();
    }
    return UpdateResult.CONTINUE;
  }

  private void onClick() {
    if (chakraFocus != null) {
      FocusResult result = chakraFocus.tryFocus();
      if (result == FocusResult.FAIL) {
        user.playSound(Sound.ENTITY_VILLAGER_NO.asEffect(1, 0.9F).sound());
        removalPolicy = (u, d) -> true;
      } else if (result == FocusResult.SUCCESS) {
        tryActivate();
      }
    }
  }

  private void tryActivate() {
    if (chakraFocus == null) {
      return;
    }
    Set<Chakra> openedChakras = chakraFocus.getOpenChakras();
    chakraFocus.onRemove();
    chakraFocus = null;

    Set<Element> elementsWithOpenChakras = ElementSet.copyOf(
      openedChakras.stream().map(Chakra::element).filter(Objects::nonNull).toList()
    );

    if (elementsWithOpenChakras.isEmpty()) {
      removalPolicy = (u, d) -> true;
      return;
    }

    long duration;
    boolean fullAvatarState = openedChakras.size() == Chakra.VALUES.size();
    if (fullAvatarState) {
      cooldown = userConfig.cooldown;
      duration = userConfig.duration;
    } else {
      duration = userConfig.transientDuration;
    }

    int durationTicks = Platform.instance().toTicks(duration, TimeUnit.MILLISECONDS);
    int glowTicks = fullAvatarState ? durationTicks : 8;
    user.addPotion(PotionEffect.GLOWING.builder().duration(glowTicks).amplifier(0).build());
    removalPolicy = Policies.defaults();

    setupAvatarStateBar(elementsWithOpenChakras, fullAvatarState, durationTicks);
    resetCooldownsAndEffects(elementsWithOpenChakras, fullAvatarState);
    addModifiers(elementsWithOpenChakras);
  }

  private void setupAvatarStateBar(Set<Element> elements, boolean fullAvatarState, int durationTicks) {
    if (user instanceof Player player) {
      Component name = description().displayName();
      if (!fullAvatarState) {
        JoinConfiguration joiner = JoinConfiguration.builder()
          .separator(Component.text(", "))
          .prefix(Component.text("("))
          .suffix(Component.text(")"))
          .build();
        Component elementNames = Component.join(joiner, elements.stream().map(Element::displayName).toList());
        name = name.append(Component.space()).append(elementNames.color(ColorPalette.NEUTRAL));
      }
      BossBar bossBar = BossBar.bossBar(name, 1, Color.WHITE, Overlay.PROGRESS);
      durationBar = BendingBar.of(bossBar, player, durationTicks);
    }
  }

  private void resetCooldownsAndEffects(Set<Element> elements, boolean fullAvatarState) {
    ActionLimiter.MANAGER.get(user.uuid()).ifPresent(Temporary::revert);
    if (fullAvatarState) {
      Set<AbilityDescription> abilities = new HashSet<>();
      user.slots().forEach((desc, idx) -> {
        if (elements.containsAll(desc.elements())) {
          abilities.add(desc);
        }
      });
      abilities.forEach(desc -> Cooldown.MANAGER.get(Cooldown.of(user, desc)).ifPresent(Temporary::revert));
    }
  }

  private void addModifiers(Set<Element> elements) {
    cleanupModifiers();
    if (avatarPolicy == null) {
      avatarPolicy = new AvatarModifyPolicy(user, elements);
      Sound.BLOCK_BEACON_ACTIVATE.asEffect(2, 2).play(user.world(), user.eyeLocation());
    }
    AVATAR_MODIFIERS.forEach((attribute, modifier) -> user.attributeModifiers().add(avatarPolicy, attribute, modifier));
  }

  private void cleanupModifiers() {
    if (avatarPolicy != null) {
      avatarPolicy.active().set(false);
      avatarPolicy = null;
      Sound.BLOCK_BEACON_DEACTIVATE.asEffect(2, 1.5F).play(user.world(), user.eyeLocation());
    }
    user.attributeModifiers().remove(modifier -> modifier.policy().key().equals(AVATAR_MODIFIER_KEY));
  }

  @Override
  public void onDestroy() {
    if (durationBar != null) {
      durationBar.onRemove();
    }
    if (chakraFocus != null) {
      chakraFocus.onRemove();
    }
    cleanupModifiers();
    user.removePotion(PotionEffect.GLOWING);
    user.addCooldown(description(), cooldown);
  }

  private record AvatarModifyPolicy(Key key, User user, Set<Element> elementFilter,
                                    AtomicBoolean active) implements ModifyPolicy {
    private AvatarModifyPolicy(User user, Set<Element> elementFilter) {
      this(AVATAR_MODIFIER_KEY, user, elementFilter, new AtomicBoolean(true));
    }

    @Override
    public boolean shouldModify(AbilityDescription desc) {
      return active.get() && isValidAbility(desc) && !isPassive(desc);
    }

    // Ensure we can't modify avatar abilities by checking required elements
    private boolean isValidAbility(AbilityDescription desc) {
      Set<Element> abilityElements = desc.elements();
      return abilityElements.size() < Element.VALUES.size() && elementFilter.containsAll(abilityElements);
    }

    private boolean isPassive(AbilityDescription desc) {
      return desc.isActivatedBy(Activation.PASSIVE) && !desc.canBind();
    }
  }

  @ConfigSerializable
  private static final class Config implements Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 120_000;
    @Modifiable(Attribute.DURATION)
    private long duration = 60_000;
    @Modifiable(Attribute.COOLDOWN)
    private long transientCooldown = 30_000;
    @Modifiable(Attribute.DURATION)
    private long transientDuration = 3_000;

    @Override
    public List<String> path() {
      return List.of("abilities", "avatar", "avatarstate");
    }
  }
}
