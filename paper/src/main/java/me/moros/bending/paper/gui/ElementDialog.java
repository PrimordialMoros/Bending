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

package me.moros.bending.paper.gui;

import java.util.List;
import java.util.Locale;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.ability.element.ElementHandler;
import me.moros.bending.api.gui.ElementGui;
import me.moros.bending.api.locale.Translation;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.common.gui.ElementDialogCallback;
import me.moros.bending.common.locale.Message;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickCallback.Options;
import net.kyori.adventure.translation.GlobalTranslator;

public final class ElementDialog implements ElementGui {
  private final Dialog dialog;
  private final Locale locale;

  private ElementDialog(Locale locale) {
    this.locale = locale;
    this.dialog = createDialog();
  }

  @Override
  public boolean show(Player player) {
    player.showDialog(dialog);
    return true;
  }

  private Component asRendered(Component source) {
    return GlobalTranslator.render(source, locale);
  }

  public static ElementGui createMenu(ElementHandler handler, Player player) {
    return new ElementDialog(player.getOrDefault(Identity.LOCALE, Translation.DEFAULT_LOCALE));
  }

  private Dialog createDialog() {
    List<ActionButton> elementButtons = Element.VALUES.stream().map(this::createElementButton).toList();
    return Dialog.create(builder -> builder.empty()
      .base(DialogBase.builder(asRendered(Message.ELEMENTS_GUI_TITLE.build())).build())
      .type(DialogType.multiAction(elementButtons).columns(1).build())
    );
  }

  private ActionButton createElementButton(Element element) {
    Options options = ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).build();
    ElementDialogCallback elementCallback = ElementDialogCallback.of(element);
    return ActionButton
      .builder(asRendered(element.displayName()))
      .tooltip(asRendered(element.description()))
      .action(DialogAction.customClick((view, audience) ->
        audience.get(Identity.UUID).ifPresent(elementCallback::accept), options))
      .build();
  }
}
