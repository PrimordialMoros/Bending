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

package me.moros.bending.fabric.platform.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import me.moros.bending.api.functional.Suppliers;
import me.moros.bending.api.util.data.DataKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public class ItemUtil {
  private static final Supplier<GsonComponentSerializer> gcs = Suppliers.lazy(GsonComponentSerializer::gson);
  private static final byte TAG_STRING = 8;
  private static final String ITEM_DISPLAY = "display";
  private static final String ITEM_LORE = "Lore";
  private static final String ITEM_UNBREAKABLE = "Unbreakable";

  private static void deleteLore(ItemStack stack) {
    final CompoundTag tag = stack.getTag();
    if (tag != null && tag.contains(ITEM_DISPLAY)) {
      tag.getCompound(ITEM_DISPLAY).remove(ITEM_LORE);
    }
  }

  public static void setUnbreakable(ItemStack stack, boolean value) {
    setBooleanTag(stack, ITEM_UNBREAKABLE, value);
  }

  public static boolean hasKey(ItemStack stack, DataKey<?> key) {
    var tag = stack.getTag();
    return tag != null && tag.getBoolean(key.asString());
  }

  public static void addKey(ItemStack stack, DataKey<?> key, boolean value) {
    setBooleanTag(stack, key.asString(), value);
  }

  public static void removeKey(ItemStack stack, DataKey<?> key) {
    var tag = stack.getTag();
    if (tag != null) {
      tag.remove(key.asString());
    }
  }

  public static List<Component> getLore(ItemStack stack) {
    CompoundTag tag = stack.getTag();
    if (tag == null || !tag.contains(ITEM_DISPLAY)) {
      return List.of();
    }
    CompoundTag displayCompound = tag.getCompound(ITEM_DISPLAY);
    ListTag list = displayCompound.getList(ITEM_LORE, TAG_STRING);
    return list.isEmpty() ? List.of() : json(list.stream().collect(TO_STRING_LIST));
  }

  public static void setLore(ItemStack stack, List<Component> lore) {
    if (lore.isEmpty()) {
      deleteLore(stack);
      return;
    }
    stack.getOrCreateTagElement(ITEM_DISPLAY).put(ITEM_LORE, listTagJson(lore));
  }

  public static void setBooleanTag(ItemStack stack, String tagName, boolean value) {
    if (value || stack.hasTag()) {
      CompoundTag tag = stack.getOrCreateTag();
      if (value) {
        tag.putBoolean(tagName, true);
      } else {
        tag.remove(tagName);
      }
    }
  }

  private static List<Component> json(final List<String> strings) {
    List<Component> components = new ArrayList<>();
    for (String string : strings) {
      components.add(gcs.get().deserialize(string));
    }
    return components;
  }

  private static ListTag listTagJson(List<Component> components) {
    ListTag nbt = new ListTag();
    for (Component component : components) {
      nbt.add(StringTag.valueOf(gcs.get().serialize(component)));
    }
    return nbt;
  }

  private static final Collector<Tag, ?, List<String>> TO_STRING_LIST = toList(Tag::getAsString);

  private static <E> Collector<Tag, List<E>, List<E>> toList(Function<Tag, E> toValueFunction) {
    return Collector.of(ArrayList::new,
      (list, value) -> list.add(toValueFunction.apply(value)),
      (first, second) -> {
        first.addAll(second);
        return first;
      },
      list -> list);
  }
}
