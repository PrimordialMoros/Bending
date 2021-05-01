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

package me.moros.bending.model.preset;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.locale.Message;
import net.kyori.adventure.text.Component;

public enum PresetCreateResult {
  SUCCESS(Message.PRESET_SUCCESS),
  EXISTS(Message.PRESET_EXISTS),
  FAIL(Message.PRESET_FAIL);

  private final Message.Args1<String> message;

  PresetCreateResult(Message.Args1<String> message) {
    this.message = message;
  }

  public @NonNull Component message(@NonNull String name) {
    return message.build(name);
  }
}
