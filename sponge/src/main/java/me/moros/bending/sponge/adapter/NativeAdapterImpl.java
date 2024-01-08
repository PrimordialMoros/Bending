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

package me.moros.bending.sponge.adapter;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.world.World;
import me.moros.bending.common.adapter.AbstractNativeAdapter;
import me.moros.bending.sponge.mixin.accessor.AdvancementProgressAccess;
import me.moros.bending.sponge.mixin.accessor.CriterionProgressAccess;
import me.moros.bending.sponge.mixin.accessor.EntityAccess;
import me.moros.bending.sponge.platform.block.SpongeBlockState;
import me.moros.bending.sponge.platform.world.SpongeWorld;
import net.kyori.adventure.text.Component;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.FrameType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.api.Sponge;
import org.spongepowered.common.adventure.SpongeAdventure;

public final class NativeAdapterImpl extends AbstractNativeAdapter {
  public NativeAdapterImpl() {
    super(((MinecraftServer) Sponge.server()).getPlayerList());
  }

  @Override
  protected ServerLevel adapt(World world) {
    return (ServerLevel) ((SpongeWorld) world).handle();
  }

  @Override
  protected BlockState adapt(me.moros.bending.api.platform.block.BlockState state) {
    return (BlockState) ((SpongeBlockState) state).handle();
  }

  @Override
  protected ItemStack adapt(Item item) {
    return BuiltInRegistries.ITEM.get(new ResourceLocation(item.key().namespace(), item.key().value())).getDefaultInstance();
  }

  @Override
  protected net.minecraft.network.chat.Component adapt(Component component) {
    return SpongeAdventure.asVanilla(component);
  }

  @Override
  protected int nextEntityId() {
    return EntityAccess.idCounter().incrementAndGet();
  }

  @Override
  protected ClientboundUpdateAdvancementsPacket createNotificationPacket(Item item, Component title) {
    String identifier = "bending:notification";
    ResourceLocation id = new ResourceLocation(identifier);
    String criteriaId = "bending:criteria_progress";
    ItemStack icon = adapt(item);
    net.minecraft.network.chat.Component nmsTitle = SpongeAdventure.asVanilla(title);
    net.minecraft.network.chat.Component nmsDesc = net.minecraft.network.chat.Component.empty();
    FrameType type = FrameType.TASK;
    var advancement = Advancement.Builder.advancement()
      .display(icon, nmsTitle, nmsDesc, null, type, true, false, true)
      .addCriterion(criteriaId, new Criterion()).build(id);
    // Bypass sponge mixins
    Map<String, CriterionProgress> map = new HashMap<>();
    var criterion = new CriterionProgress();
    //noinspection DataFlowIssue
    ((CriterionProgressAccess) criterion).setDate(new Date());
    map.put(criteriaId, criterion);
    var progressMap = Map.of(id, AdvancementProgressAccess.bending$create(map));
    return new ClientboundUpdateAdvancementsPacket(false, List.of(advancement), Set.of(), progressMap);
  }
}
