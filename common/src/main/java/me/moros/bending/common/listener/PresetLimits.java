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

package me.moros.bending.common.listener;

import java.util.function.ToIntFunction;

import me.moros.bending.api.config.BendingProperties;
import me.moros.bending.api.event.EventBus;
import me.moros.bending.api.event.PresetRegisterEvent;
import me.moros.bending.api.user.User;
import me.moros.bending.common.locale.Message;
import org.checkerframework.checker.nullness.qual.Nullable;

public record PresetLimits(ToIntFunction<User> maxPresetsFn) {
  private void onPresetRegister(PresetRegisterEvent event) {
    int maxPresets = maxPresetsFn.applyAsInt(event.user());
    if (maxPresets > 0 && event.user().presetSize() >= maxPresets) {
      Message.PRESET_LIMIT.send(event.user(), maxPresets);
      event.cancelled(true);
    }
  }

  public static void register(EventBus eventBus, @Nullable ToIntFunction<User> limiter) {
    var listener = new PresetLimits(limiter == null ? u -> BendingProperties.instance().maxPresets() : limiter);
    eventBus.subscribe(PresetRegisterEvent.class, listener::onPresetRegister);
  }
}
