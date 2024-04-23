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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import me.moros.bending.api.ability.Updatable;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.platform.sound.Sound;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.ColorPalette;
import me.moros.bending.api.util.collect.ElementSet;
import me.moros.math.FastMath;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.TextColor;

public class ChakraFocus implements Updatable {
  private static final int TOTAL_TICKS = 60; // 3s total time
  private static final int VALID_TICK_RANGE = 10; // 500ms

  private final User user;
  private final Map<Chakra, ChakraData> chakras;
  private final BossBar bar;
  private final int duration;

  private int ticks;

  public ChakraFocus(User user) {
    this.user = user;
    this.duration = TOTAL_TICKS;
    this.chakras = new EnumMap<>(Chakra.class);
    initChakras();
    this.bar = BossBar.bossBar(buildName(), 0, Color.PURPLE, Overlay.NOTCHED_10);
  }

  private void initChakras() {
    for (var chakra : Chakra.VALUES) {
      chakras.put(chakra, new ChakraData(duration, false));
    }
    Set<Element> boundElements = ElementSet.mutable();
    user.slots().forEach((desc, idx) -> {
      if (desc.elements().size() == 1) {
        boundElements.add(desc.elements().iterator().next());
      }
    });
    boundElements.forEach(e -> chakras.get(Chakra.elementalChakra(e)).open());
  }

  private Component buildName() {
    List<Component> names = chakras.entrySet().stream().map(e -> formatChakraName(e.getKey(), e.getValue())).toList();
    Component joinedNames = Component.join(JoinConfiguration.separator(Component.text(" - ")), names);
    return Component.text().color(ColorPalette.NEUTRAL).append(joinedNames).build();
  }

  private Component formatChakraName(Chakra chakra, ChakraData focus) {
    TextColor color = ColorPalette.NEUTRAL;
    if (focus.isOpen()) {
      color = chakra.displayName().color();
    } else if (focus.isFocused(ticks)) {
      color = ColorPalette.ACCENT;
    }
    return chakra.displayName().color(color);
  }

  @Override
  public UpdateResult update() {
    if (!user.sneaking() || ticks >= duration) {
      return UpdateResult.REMOVE;
    }
    float factor = FastMath.clamp(ticks / (float) duration, 0, 1);
    bar.name(buildName()).progress(factor);
    if (ticks++ == 0) {
      user.showBossBar(bar);
    }
    notifyChakraFocus();
    return UpdateResult.CONTINUE;
  }

  private void notifyChakraFocus() {
    for (var chakra : chakras.values()) {
      if (!chakra.isOpen() && chakra.isTargetTick(ticks)) {
        user.playSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP.asEffect().sound());
        return;
      }
    }
  }

  public boolean tryFocus() {
    boolean focused = false;
    for (var chakra : chakras.values()) {
      if (!chakra.isOpen() && chakra.isFocused(ticks)) {
        chakra.open();
        focused = true;
      }
    }
    return focused;
  }

  public Set<Chakra> getOpenChakras() {
    return chakras.entrySet().stream()
      .filter(e -> e.getValue().isOpen())
      .map(Entry::getKey)
      .collect(Collectors.toSet());
  }

  public void onRemove() {
    user.hideBossBar(bar);
  }

  private static final class ChakraData {
    private final int targetTick;
    private boolean open;

    private ChakraData(int duration, boolean open) {
      this.targetTick = FastMath.round(duration * 0.1 * ThreadLocalRandom.current().nextInt(1, 9));
      this.open = open;
    }

    public boolean isFocused(int tick) {
      return tick >= targetTick && tick <= targetTick + VALID_TICK_RANGE;
    }

    public boolean isTargetTick(int tick) {
      return tick == targetTick;
    }

    public boolean isOpen() {
      return open;
    }

    public void open() {
      open = true;
    }
  }
}
