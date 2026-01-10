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

package me.moros.bending.fabric.platform;

import io.leangen.geantyref.TypeToken;
import me.moros.bending.common.command.parser.AbilityParser;
import net.kyori.adventure.audience.Audience;
import net.minecraft.commands.arguments.IdentifierArgument;
import org.incendo.cloud.brigadier.BrigadierManagerHolder;

public final class BrigadierSetup {
  private BrigadierSetup() {
  }

  public static <C extends Audience> void setup(BrigadierManagerHolder<C, ?> holder) {
    holder.brigadierManager().registerMapping(
      new TypeToken<AbilityParser<C>>() {
      },
      builder -> builder.toConstant(IdentifierArgument.id()).cloudSuggestions()
    );
  }
}
