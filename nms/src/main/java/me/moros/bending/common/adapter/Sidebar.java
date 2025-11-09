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

package me.moros.bending.common.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ObjIntConsumer;

import io.netty.buffer.Unpooled;
import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.gui.Board;
import me.moros.bending.api.user.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.chat.numbers.NumberFormatTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Team;
import net.minecraft.world.scores.Team.CollisionRule;
import net.minecraft.world.scores.Team.Visibility;
import net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType;
import org.jspecify.annotations.Nullable;

import static net.kyori.adventure.text.format.TextDecoration.STRIKETHROUGH;

public abstract class Sidebar implements Board {
  private static final Component ACTIVE = Component.text("> ");
  private static final Component INACTIVE = Component.text("> ", NamedTextColor.DARK_GRAY);
  private static final Component SEP = Component.text(" -------------- ");
  private static final String OBJECTIVE_ID = "bending-board";
  private static final String[] CHAT_CODES;

  static {
    CHAT_CODES = new String[16];
    char prefix = ChatFormatting.PREFIX_CODE;
    for (int i = 0; i < CHAT_CODES.length; i++) {
      CHAT_CODES[i] = prefix + Integer.toHexString(i);
    }
  }

  private static String generateInvisibleLegacyString(int slot) {
    String hidden = CHAT_CODES[slot % CHAT_CODES.length];
    int delta = slot - CHAT_CODES.length;
    return delta <= 0 ? (hidden + ChatFormatting.RESET) : hidden + generateInvisibleLegacyString(delta);
  }

  private final MinecraftServer server;
  private final User user;
  private final Map<AbilityDescription, IndexedScore> misc;
  private int selectedSlot;
  private boolean closed;

  protected Sidebar(MinecraftServer server, User user) {
    this.server = server;
    this.user = user;
    this.misc = new ConcurrentHashMap<>();
    this.selectedSlot = user.currentSlot();
  }

  protected void init(Component title) {
    trySendPackets(createInitialPacket(title));
  }

  private List<Packet<? super ClientGamePacketListener>> createInitialPacket(Component title) {
    List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>(11);
    packets.addAll(buildInitialPackets(title));
    forEachSlotScore((score, slot) -> packets.addAll(createScore(slot, score)));
    return packets;
  }

  protected abstract Component emptySlot(int slot);

  protected abstract net.minecraft.network.chat.Component toNative(Component component);

  @Override
  public boolean isEnabled() {
    return !closed;
  }

  @Override
  public void disableScoreboard() {
    if (!closed) {
      List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
      for (int slot = 1; slot <= 9; slot++) {
        packets.add(buildTeamPacket(slot, TeamAction.REMOVE, Score.EMPTY));
      }
      misc.values().forEach(indexed -> packets.add(buildTeamPacket(indexed.index(), TeamAction.REMOVE, Score.EMPTY)));
      packets.add(buildRemoveObjectivePacket());
      trySendPackets(packets);
      closed = true;
    }
  }

  @Override
  public void updateAll() {
    List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
    forEachSlotScore((score, slot) -> packets.add(updateScore(slot, score)));
    trySendPackets(packets);
  }

  private Score getScoreForSlot(int slot, @Nullable AbilityDescription desc) {
    Component prefix = slot == selectedSlot ? ACTIVE : INACTIVE;
    Component suffix;
    if (desc == null) {
      suffix = emptySlot(slot);
    } else {
      suffix = !user.onCooldown(desc) ? desc.displayName() : desc.displayName().decorate(STRIKETHROUGH);
    }
    return new Score(prefix, suffix);
  }

  private void forEachSlotScore(ObjIntConsumer<Score> consumer) {
    var snapshot = user.slots().abilities();
    for (int slot = 1; slot <= 9; slot++) {
      consumer.accept(getScoreForSlot(slot, snapshot.get(slot - 1)), slot);
    }
  }

  @Override
  public void activeSlot(int oldSlot, int newSlot) {
    if (validSlot(oldSlot) && validSlot(newSlot)) {
      if (selectedSlot != oldSlot) {
        oldSlot = selectedSlot; // Fixes bug when slot is set using setHeldItemSlot
      }
      selectedSlot = newSlot;
      List<Packet<? super ClientGamePacketListener>> packets = List.of(
        updateScore(oldSlot, getScoreForSlot(oldSlot, user.boundAbility(oldSlot))),
        updateScore(newSlot, getScoreForSlot(newSlot, user.boundAbility(newSlot)))
      );
      trySendPackets(packets);
    }
  }

  private boolean validSlot(int slot) {
    return 1 <= slot && slot <= 9;
  }

  @Override
  public void updateMisc(AbilityDescription desc, boolean show) {
    List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
    if (show) {
      if (misc.isEmpty()) {
        packets.addAll(createScore(10, new Score(SEP)));
      }
      Component miscName = INACTIVE.append(desc.displayName().decorate(STRIKETHROUGH));
      IndexedScore indexed = new IndexedScore(pickAvailableSlot(), new Score(miscName));
      if (misc.putIfAbsent(desc, indexed) == null) {
        packets.addAll(createScore(indexed.index(), indexed.scoreEntry()));
      }
    } else {
      IndexedScore old = misc.remove(desc);
      if (old != null) {
        packets.addAll(removeScore(old.index()));
        if (misc.isEmpty()) {
          packets.addAll(removeScore(10));
        }
      }
    }
    trySendPackets(packets);
  }

  private int pickAvailableSlot() {
    int idx = 11;
    for (var indexedTeam : misc.values()) {
      idx = Math.max(indexedTeam.index() + 1, idx);
    }
    return idx;
  }

  private FriendlyByteBuf createByteBuf() {
    return new FriendlyByteBuf(Unpooled.buffer());
  }

  private RegistryFriendlyByteBuf createRegistryFriendlyByteBuf() {
    return new RegistryFriendlyByteBuf(createByteBuf(), server.registryAccess());
  }

  private List<Packet<? super ClientGamePacketListener>> buildInitialPackets(Component title) {
    return List.of(buildCreateObjectivePacket(title), buildSetDisplayObjectivePacket());
  }

  private ClientboundSetObjectivePacket buildCreateObjectivePacket(Component title) {
    var buf = createRegistryFriendlyByteBuf();
    buf.writeUtf(OBJECTIVE_ID);
    buf.writeByte(ClientboundSetObjectivePacket.METHOD_ADD);
    ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, toNative(title));
    buf.writeEnum(RenderType.INTEGER);
    NumberFormatTypes.OPTIONAL_STREAM_CODEC.encode(buf, Optional.of(BlankFormat.INSTANCE));
    return ClientboundSetObjectivePacket.STREAM_CODEC.decode(buf);
  }

  private ClientboundSetObjectivePacket buildRemoveObjectivePacket() {
    var buf = createRegistryFriendlyByteBuf();
    buf.writeUtf(OBJECTIVE_ID);
    buf.writeByte(ClientboundSetObjectivePacket.METHOD_REMOVE);
    return ClientboundSetObjectivePacket.STREAM_CODEC.decode(buf);
  }

  private ClientboundSetDisplayObjectivePacket buildSetDisplayObjectivePacket() {
    var buf = createByteBuf();
    buf.writeById(DisplaySlot::id, DisplaySlot.SIDEBAR);
    buf.writeUtf(OBJECTIVE_ID);
    return ClientboundSetDisplayObjectivePacket.STREAM_CODEC.decode(buf);
  }

  private List<Packet<? super ClientGamePacketListener>> createScore(int score, Score scoreEntry) {
    String objName = generateInvisibleLegacyString(score);
    return List.of(
      new ClientboundSetScorePacket(objName, OBJECTIVE_ID, -score, Optional.empty(), Optional.empty()),
      buildTeamPacket(score, TeamAction.CREATE, scoreEntry)
    );
  }

  private List<Packet<? super ClientGamePacketListener>> removeScore(int score) {
    String objName = generateInvisibleLegacyString(score);
    return List.of(
      buildTeamPacket(score, TeamAction.REMOVE, Score.EMPTY),
      new ClientboundResetScorePacket(objName, OBJECTIVE_ID)
    );
  }

  private ClientboundSetPlayerTeamPacket updateScore(int score, Score scoreEntry) {
    return buildTeamPacket(score, TeamAction.UPDATE, scoreEntry);
  }

  private ClientboundSetPlayerTeamPacket buildTeamPacket(int score, TeamAction action, Score scoreEntry) {
    var buf = createRegistryFriendlyByteBuf();
    buf.writeUtf(String.valueOf(score));
    buf.writeByte(action.method);

    if (action != TeamAction.REMOVE) {
      ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, net.minecraft.network.chat.Component.literal(String.valueOf(score)));
      buf.writeByte(0x00); // flags
      Team.Visibility.STREAM_CODEC.encode(buf, Visibility.ALWAYS);
      Team.CollisionRule.STREAM_CODEC.encode(buf, CollisionRule.ALWAYS);
      buf.writeEnum(ChatFormatting.RESET);
      ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, toNative(scoreEntry.prefix()));
      ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, toNative(scoreEntry.suffix()));
      if (action == TeamAction.CREATE) {
        buf.writeCollection(List.of(generateInvisibleLegacyString(score)), FriendlyByteBuf::writeUtf);
      }
    }
    return ClientboundSetPlayerTeamPacket.STREAM_CODEC.decode(buf);
  }

  private void trySendPackets(SequencedCollection<Packet<? super ClientGamePacketListener>> packets) {
    if (!closed && !packets.isEmpty()) {
      Packet<?> packet = packets.size() == 1 ? packets.getFirst() : new ClientboundBundlePacket(packets);
      ServerPlayer player = server.getPlayerList().getPlayer(user.uuid());
      if (player != null) {
        player.connection.send(packet);
      }
    }
  }

  private enum TeamAction {
    CREATE(0),
    REMOVE(1),
    UPDATE(2);

    private final int method;

    TeamAction(int method) {
      this.method = method;
    }
  }

  private record IndexedScore(int index, Score scoreEntry) {
  }

  private record Score(Component prefix, Component suffix) {
    public Score(Component prefix) {
      this(prefix, Component.empty());
    }

    private static final Score EMPTY = new Score(Component.empty(), Component.empty());
  }
}

