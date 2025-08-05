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

package me.moros.bending.api.temporal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import me.moros.bending.api.ability.ActionType;
import me.moros.bending.api.ability.Updatable.UpdateResult;
import me.moros.bending.api.event.ActionLimitEvent;
import me.moros.bending.api.platform.entity.EntityProperties;
import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.BendingBar;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ActionLimiter extends Temporary {
  public static final TemporalManager<UUID, ActionLimiter> MANAGER = new TemporalManager<>(600) {
    @Override
    public void tick() {
      super.tick();
      BARS.entrySet().removeIf(e -> e.getValue().update() == UpdateResult.REMOVE);
    }
  };

  private static final Map<UUID, BendingBar> BARS = new HashMap<>();

  private final UUID uuid;
  private final LivingEntity entity;
  private final TriState hadAI;
  private final Set<ActionType> limitedActions;

  private boolean reverted = false;

  private ActionLimiter(LivingEntity entity, Collection<ActionType> limitedActions, boolean showBar, int ticks) {
    this.uuid = entity.uuid();
    this.entity = entity;
    this.limitedActions = EnumSet.copyOf(limitedActions);
    if (limitedActions.contains(ActionType.MOVE)) {
      this.hadAI = entity.checkProperty(EntityProperties.AI);
      entity.setProperty(EntityProperties.AI, false);
    } else {
      this.hadAI = TriState.NOT_SET;
    }
    if (showBar && ticks > 2 && entity instanceof Player player) {
      BossBar bar = BossBar.bossBar(Component.text("Restricted"), 1, Color.YELLOW, Overlay.PROGRESS);
      BARS.putIfAbsent(uuid, BendingBar.of(bar, player, ticks));
    }
    MANAGER.addEntry(uuid, this, ticks);
  }

  @Override
  public boolean revert() {
    if (reverted) {
      return false;
    }
    reverted = true;
    MANAGER.removeEntry(uuid);
    BendingBar bar = BARS.remove(uuid);
    if (bar != null) {
      bar.onRemove();
    }
    entity.setProperty(EntityProperties.AI, hadAI);
    return true;
  }

  public static boolean isLimited(UUID uuid) {
    return isLimited(uuid, null);
  }

  public static boolean isLimited(UUID uuid, @Nullable ActionType method) {
    ActionLimiter limiter = MANAGER.get(uuid).orElse(null);
    if (limiter == null) {
      return false;
    }
    return method == null || limiter.limitedActions.contains(method);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private Set<ActionType> limitedActions = EnumSet.allOf(ActionType.class);
    private boolean showBar = true;
    private long duration = 5000;

    private Builder() {
    }

    public Builder limit(Collection<ActionType> methods) {
      limitedActions = EnumSet.copyOf(methods);
      return this;
    }

    public Builder limit(ActionType method, ActionType @Nullable ... methods) {
      Collection<ActionType> c = new ArrayList<>();
      c.add(method);
      if (methods != null) {
        c.addAll(List.of(methods));
      }
      return limit(c);
    }

    public Builder showBar(boolean showBar) {
      this.showBar = showBar;
      return this;
    }

    public Builder duration(long duration) {
      this.duration = duration;
      return this;
    }

    public Optional<ActionLimiter> build(User source, LivingEntity target) {
      Objects.requireNonNull(source);
      Objects.requireNonNull(target);
      if (isLimited(target.uuid())) {
        return Optional.empty();
      }
      ActionLimitEvent event = source.game().eventBus().postActionLimitEvent(source, target, duration);
      if (event.cancelled() || event.duration() <= 0) {
        return Optional.empty();
      }
      return Optional.of(new ActionLimiter(target, limitedActions, showBar, MANAGER.fromMillis(event.duration())));
    }
  }
}
