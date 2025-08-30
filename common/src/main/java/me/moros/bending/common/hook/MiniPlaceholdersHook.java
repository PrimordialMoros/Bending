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

package me.moros.bending.common.hook;

import java.util.function.Function;

import io.github.miniplaceholders.api.Expansion;
import io.github.miniplaceholders.api.resolver.AudienceTagResolver;
import io.github.miniplaceholders.api.utils.Tags;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.User;
import me.moros.bending.common.placeholder.DynamicPlaceholder;
import me.moros.bending.common.placeholder.PlaceholderProvider;
import me.moros.bending.common.placeholder.StaticPlaceholder;
import me.moros.bending.common.util.Initializer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import org.checkerframework.checker.nullness.qual.NonNull;

public record MiniPlaceholdersHook(PlaceholderProvider provider) implements Initializer {
  public MiniPlaceholdersHook() {
    this(PlaceholderProvider.defaultBuilder().build());
  }

  @Override
  public void init() {
    var builder = Expansion.builder("bending");
    for (var keyed : provider) {
      if (keyed.value() instanceof StaticPlaceholder staticPlaceholder) {
        builder.audiencePlaceholder(keyed.key().value(), staticParser(staticPlaceholder));
      } else if (keyed.value() instanceof DynamicPlaceholder dynamicPlaceholder) {
        builder.audiencePlaceholder(keyed.key().value(), dynamicParser(dynamicPlaceholder));
      }
    }
    builder.build().register();
  }

  private AudienceTagResolver<@NonNull Audience> staticParser(StaticPlaceholder placeholder) {
    return (audience, queue, ctx) -> parse(audience, placeholder);
  }

  private AudienceTagResolver<@NonNull Audience> dynamicParser(DynamicPlaceholder placeholder) {
    return (audience, queue, ctx) -> !queue.hasNext() ? Tags.EMPTY_TAG : parse(audience, u -> placeholder.handle(u, queue.pop().value()));
  }

  private Tag parse(Audience audience, Function<User, Component> placeholder) {
    return audience.pointers().get(Identity.UUID)
      .flatMap(Registries.BENDERS::getIfExists)
      .map(placeholder)
      .map(Tag::selfClosingInserting).orElse(Tags.EMPTY_TAG);
  }
}
