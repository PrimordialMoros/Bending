/*
 * Copyright 2020-2023 Moros
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

package me.moros.bending.api.ability.preset;

import me.moros.bending.api.locale.Message;
import me.moros.bending.api.locale.Message.Args1;
import net.kyori.adventure.text.Component;

/**
 * Represents the result of a preset creation query.
 */
public enum PresetCreateResult implements Args1<String> {
  /**
   * Preset has been successfully created.
   */
  SUCCESS(Message.PRESET_SUCCESS),
  /**
   * Preset creation was unable to complete because it already exists.
   */
  EXISTS(Message.PRESET_EXISTS),
  /**
   * Preset creation was cancelled by an event listener.
   */
  CANCELLED(Message.PRESET_CANCELLED),
  /**
   * Preset creation failed to a storage error.
   */
  FAIL(Message.PRESET_FAIL);

  private final Args1<String> message;

  PresetCreateResult(Args1<String> message) {
    this.message = message;
  }

  @Override
  public Component build(String arg0) {
    return message.build(arg0);
  }
}
