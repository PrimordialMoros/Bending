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
import me.moros.bending.api.temporal.Cooldown;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.BendingBar;
import me.moros.bending.api.util.ColorPalette;
import me.moros.bending.api.util.KeyUtil;
import me.moros.bending.api.util.collect.ElementSet;
import me.moros.bending.api.util.functional.ExpireRemovalPolicy;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.functional.SwappedSlotsRemovalPolicy;
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

  private Config userConfig;
  private RemovalPolicy removalPolicy;


  private AvatarModifyPolicy avatarPolicy;
  private ChakraFocus chakraFocus;
  private BendingBar durationBar;
  private boolean active;
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

    cooldown = userConfig.singleUseCooldown;
    chakraFocus = new ChakraFocus(user);
    removalPolicy = Policies.builder().add(SwappedSlotsRemovalPolicy.of(description())).build();
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
    }
    if (!active && chakraFocus.update() == UpdateResult.REMOVE) {
      tryActivate();
    }
    return UpdateResult.CONTINUE;
  }

  @Override
  public void onDestroy() {
    if (chakraFocus != null) {
      chakraFocus.onRemove();
    }
    if (durationBar != null) {
      durationBar.onRemove();
    }
    cleanupModifiers();
    user.removePotion(PotionEffect.GLOWING);
    user.addCooldown(description(), cooldown);
  }

  private void tryActivate() {
    if (active) {
      return;
    }
    Set<Chakra> openedChakras = chakraFocus.getOpenChakras();
    chakraFocus.onRemove();
    chakraFocus = null;

    if (openedChakras.isEmpty()) {
      removalPolicy = (u, d) -> true;
      return;
    }

    active = true;
    long duration;
    boolean fullAvatarState = openedChakras.size() == Chakra.VALUES.size();
    if (fullAvatarState) {
      cooldown = userConfig.cooldown;
      duration = userConfig.duration;
    } else {
      // TODO change to only affect next activation
      duration = userConfig.singleUseDuration;
    }

    int durationTicks = Platform.instance().toTicks(duration, TimeUnit.MILLISECONDS);
    int glowTicks = fullAvatarState ? durationTicks : 8;
    user.addPotion(PotionEffect.GLOWING.builder().duration(glowTicks).amplifier(0).build());
    removalPolicy = Policies.builder().add(ExpireRemovalPolicy.of(duration)).build();

    Set<Element> elementsWithOpenChakras = ElementSet.copyOf(
      openedChakras.stream().map(Chakra::element).filter(Objects::nonNull).toList()
    );

    setupAvatarStateBar(elementsWithOpenChakras, durationTicks);
    resetCooldowns(elementsWithOpenChakras);
    addModifiers(elementsWithOpenChakras);
  }

  private void setupAvatarStateBar(Set<Element> elements, int durationTicks) {
    if (user instanceof Player player) {
      Component suffix = Component.empty();
      if (elements.size() < Element.VALUES.size()) {
        suffix = Component.space().append(Component.join(JOINER, elements.stream().map(Element::displayName).toList()))
          .color(ColorPalette.NEUTRAL);
      }
      Component name = description().displayName().append(suffix);
      BossBar bossBar = BossBar.bossBar(name, 1, Color.WHITE, Overlay.PROGRESS);
      durationBar = BendingBar.of(bossBar, player, durationTicks);
    }
  }

  private void resetCooldowns(Set<Element> elements) {
    if (elements.isEmpty()) {
      return;
    }
    Set<AbilityDescription> abilities = new HashSet<>();
    user.slots().forEach((desc, idx) -> {
      if (elements.containsAll(desc.elements())) {
        abilities.add(desc);
      }
    });
    abilities.forEach(desc -> Cooldown.MANAGER.removeEntry(Cooldown.of(user, desc)));
  }

  private static final JoinConfiguration JOINER = JoinConfiguration.builder()
    .separator(Component.text(", "))
    .prefix(Component.text("("))
    .suffix(Component.text(")"))
    .build();

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

  private void addModifiers(Set<Element> elements) {
    if (elements.isEmpty()) {
      return;
    }
    cleanupModifiers();
    if (avatarPolicy == null) {
      avatarPolicy = new AvatarModifyPolicy(user, elements);
    }
    AVATAR_MODIFIERS.forEach((attribute, modifier) -> user.attributeModifiers().add(avatarPolicy, attribute, modifier));
  }

  private void cleanupModifiers() {
    if (avatarPolicy != null) {
      avatarPolicy.active().set(false);
    }
    user.attributeModifiers().remove(modifier -> modifier.policy().key().equals(AVATAR_MODIFIER_KEY));
  }

  private void onClick() {
    if (active || chakraFocus == null) {
      return;
    }
    if (!chakraFocus.tryFocus()) {
      removalPolicy = (u, d) -> true;
    }
  }

  private record AvatarModifyPolicy(Key key, User user, Set<Element> elementFilter,
                                    AtomicBoolean active) implements ModifyPolicy {
    private AvatarModifyPolicy(User user, Set<Element> elementFilter) {
      this(AVATAR_MODIFIER_KEY, user, elementFilter, new AtomicBoolean(true));
    }

    @Override
    public boolean shouldModify(AbilityDescription desc) {
      return active.get() && elementFilter.containsAll(desc.elements()) && !isPassive(desc);
    }

    private boolean isPassive(AbilityDescription desc) {
      return desc.isActivatedBy(Activation.PASSIVE) && !desc.canBind();
    }
  }

  @ConfigSerializable
  private static final class Config implements Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long singleUseCooldown = 30_000;
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 120_000;
    @Modifiable(Attribute.DURATION)
    private long singleUseDuration = 5_000;
    @Modifiable(Attribute.DURATION)
    private long duration = 60_000;

    @Override
    public List<String> path() {
      return List.of("abilities", "avatar", "avatarstate");
    }
  }
}
