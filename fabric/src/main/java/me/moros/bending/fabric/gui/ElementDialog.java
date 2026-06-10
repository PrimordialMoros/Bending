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

package me.moros.bending.fabric.gui;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.ability.element.ElementHandler;
import me.moros.bending.api.gui.ElementGui;
import me.moros.bending.api.locale.Translation;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.util.KeyUtil;
import me.moros.bending.common.gui.ElementDialogCallback;
import me.moros.bending.common.locale.Message;
import me.moros.bending.fabric.platform.PlatformAdapter;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.DialogAction;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraft.server.dialog.action.Action;
import net.minecraft.server.dialog.action.StaticAction;
import net.minecraft.server.level.ServerPlayer;

public final class ElementDialog implements ElementGui {
  private static final Map<Element, UUID> ELEMENT_CALLBACKS = new EnumMap<>(Element.class);
  private static final Map<UUID, ElementDialogCallback> STORED_CALLBACKS = new ConcurrentHashMap<>();

  private final Locale locale;
  private final MinecraftServerAudiences adapter;
  private final Dialog dialog;

  private ElementDialog(ServerPlayer player) {
    this.locale = player.getOrDefault(Identity.LOCALE, Translation.DEFAULT_LOCALE);
    this.adapter = MinecraftServerAudiences.of(player.level().getServer());
    this.dialog = createDialog();
  }

  @Override
  public boolean show(Player player) {
    PlatformAdapter.toFabricEntity(player).openDialog(Holder.direct(dialog));
    return true;
  }

  public static ElementGui createMenu(ElementHandler handler, Player player) {
    return new ElementDialog(PlatformAdapter.toFabricEntity(player));
  }

  private Dialog createDialog() {
    List<ActionButton> elementButtons = Element.VALUES.stream().map(this::createElementButton).toList();
    ActionButton close = new ActionButton(new CommonButtonData(CommonComponents.GUI_BACK, 200), Optional.empty());
    CommonDialogData data = new CommonDialogData(
      asNativeRendered(Message.ELEMENTS_GUI_TITLE.build()),
      Optional.empty(),
      true,
      true,
      DialogAction.CLOSE,
      List.of(),
      List.of()
    );
    return new MultiActionDialog(data, elementButtons, Optional.of(close), 1);
  }

  private ActionButton createElementButton(Element element) {
    CommonButtonData data = new CommonButtonData(asNativeRendered(element.displayName()),
      Optional.of(asNativeRendered(element.description())),
      CommonButtonData.DEFAULT_WIDTH
    );
    Action action = new StaticAction(createCustomClickEvent(element));
    return new ActionButton(data, Optional.of(action));
  }

  private net.minecraft.network.chat.Component asNativeRendered(Component source) {
    return adapter.asNative(GlobalTranslator.render(source, locale));
  }

  private static final Identifier KEY = PlatformAdapter.identifier(KeyUtil.simple("element"));
  private static final String ID_KEY = "id";

  private static ClickEvent createCustomClickEvent(Element element) {
    CompoundTag tag = new CompoundTag();
    tag.store(ID_KEY, UUIDUtil.CODEC, createCallback(element));
    return new ClickEvent.Custom(KEY, Optional.of(tag));
  }

  private static UUID createCallback(Element element) {
    UUID uuid = ELEMENT_CALLBACKS.computeIfAbsent(element, _ -> UUID.randomUUID());
    STORED_CALLBACKS.putIfAbsent(uuid, ElementDialogCallback.of(element));
    return uuid;
  }

  public static void tryCallback(UUID uuid, Tag tag) {
    tag.asCompound().ifPresent(t -> {
      Optional<UUID> id = t.read(ID_KEY, UUIDUtil.CODEC);
      if (id.isEmpty()) {
        return;
      }
      ElementDialogCallback callback = STORED_CALLBACKS.get(id.get());
      if (callback != null) {
        callback.accept(uuid);
      }
    });
  }
}
