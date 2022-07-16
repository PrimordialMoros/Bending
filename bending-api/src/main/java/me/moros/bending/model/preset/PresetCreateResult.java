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

package me.moros.bending.model.preset;

import me.moros.bending.locale.Message;
import me.moros.bending.locale.Message.Args1;

public enum PresetCreateResult {
  SUCCESS(Message.PRESET_SUCCESS),
  EXISTS(Message.PRESET_EXISTS),
  CANCELLED(Message.PRESET_CANCELLED),
  FAIL(Message.PRESET_FAIL);

  private final Args1<String> message;

  PresetCreateResult(Args1<String> message) {
    this.message = message;
  }

  public Args1<String> message() {
    return message;
  }
}
