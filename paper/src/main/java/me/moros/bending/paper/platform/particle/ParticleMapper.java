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

package me.moros.bending.paper.platform.particle;

import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.particle.Particle;
import me.moros.bending.api.platform.particle.ParticleContext;
import me.moros.bending.api.platform.particle.ParticleDustData;
import me.moros.bending.api.platform.particle.ParticleDustData.Transitive;
import me.moros.bending.paper.platform.PlatformAdapter;
import org.bukkit.Color;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Particle.DustTransition;
import org.checkerframework.checker.nullness.qual.Nullable;

import static me.moros.bending.api.platform.particle.Particle.*;

public final class ParticleMapper {
  // TODO stupid bukkit enums
  public static org.bukkit.@Nullable Particle mapParticle(Particle p) {
    if (p == POOF) return org.bukkit.Particle.EXPLOSION_NORMAL;
    if (p == EXPLOSION) return org.bukkit.Particle.EXPLOSION_LARGE;
    if (p == EXPLOSION_EMITTER) return org.bukkit.Particle.EXPLOSION_HUGE;
    if (p == FIREWORK) return org.bukkit.Particle.FIREWORKS_SPARK;
    if (p == BUBBLE) return org.bukkit.Particle.WATER_BUBBLE;
    if (p == SPLASH) return org.bukkit.Particle.WATER_SPLASH;
    if (p == FISHING) return org.bukkit.Particle.WATER_WAKE;
    if (p == UNDERWATER) return org.bukkit.Particle.SUSPENDED;
    if (p == CRIT) return org.bukkit.Particle.CRIT;
    if (p == ENCHANTED_HIT) return org.bukkit.Particle.CRIT_MAGIC;
    if (p == SMOKE) return org.bukkit.Particle.SMOKE_NORMAL;
    if (p == LARGE_SMOKE) return org.bukkit.Particle.SMOKE_LARGE;
    if (p == EFFECT) return org.bukkit.Particle.SPELL;
    if (p == EGG_CRACK) return org.bukkit.Particle.EGG_CRACK;
    if (p == INSTANT_EFFECT) return org.bukkit.Particle.SPELL_INSTANT;
    if (p == ENTITY_EFFECT) return org.bukkit.Particle.SPELL_MOB;
    if (p == AMBIENT_ENTITY_EFFECT) return org.bukkit.Particle.SPELL_MOB_AMBIENT;
    if (p == WITCH) return org.bukkit.Particle.SPELL_WITCH;
    if (p == DRIPPING_WATER) return org.bukkit.Particle.DRIP_WATER;
    if (p == DRIPPING_LAVA) return org.bukkit.Particle.DRIP_LAVA;
    if (p == ANGRY_VILLAGER) return org.bukkit.Particle.VILLAGER_ANGRY;
    if (p == HAPPY_VILLAGER) return org.bukkit.Particle.VILLAGER_HAPPY;
    if (p == MYCELIUM) return org.bukkit.Particle.TOWN_AURA;
    if (p == NOTE) return org.bukkit.Particle.NOTE;
    if (p == PORTAL) return org.bukkit.Particle.PORTAL;
    if (p == ENCHANT) return org.bukkit.Particle.ENCHANTMENT_TABLE;
    if (p == FLAME) return org.bukkit.Particle.FLAME;
    if (p == CHERRY_LEAVES) return org.bukkit.Particle.CHERRY_LEAVES;
    if (p == LAVA) return org.bukkit.Particle.LAVA;
    if (p == CLOUD) return org.bukkit.Particle.CLOUD;
    if (p == DUST) return org.bukkit.Particle.REDSTONE;
    if (p == ITEM_SNOWBALL) return org.bukkit.Particle.SNOWBALL;
    if (p == ITEM_SLIME) return org.bukkit.Particle.SLIME;
    if (p == HEART) return org.bukkit.Particle.HEART;
    if (p == ITEM) return org.bukkit.Particle.ITEM_CRACK;
    if (p == BLOCK) return org.bukkit.Particle.BLOCK_DUST;
    if (p == RAIN) return org.bukkit.Particle.WATER_DROP;
    if (p == ELDER_GUARDIAN) return org.bukkit.Particle.MOB_APPEARANCE;
    if (p == DRAGON_BREATH) return org.bukkit.Particle.DRAGON_BREATH;
    if (p == END_ROD) return org.bukkit.Particle.END_ROD;
    if (p == DAMAGE_INDICATOR) return org.bukkit.Particle.DAMAGE_INDICATOR;
    if (p == SWEEP_ATTACK) return org.bukkit.Particle.SWEEP_ATTACK;
    if (p == FALLING_DUST) return org.bukkit.Particle.FALLING_DUST;
    if (p == TOTEM_OF_UNDYING) return org.bukkit.Particle.TOTEM;
    if (p == SPIT) return org.bukkit.Particle.SPIT;
    if (p == SQUID_INK) return org.bukkit.Particle.SQUID_INK;
    if (p == BUBBLE_POP) return org.bukkit.Particle.BUBBLE_POP;
    if (p == CURRENT_DOWN) return org.bukkit.Particle.CURRENT_DOWN;
    if (p == BUBBLE_COLUMN_UP) return org.bukkit.Particle.BUBBLE_COLUMN_UP;
    if (p == NAUTILUS) return org.bukkit.Particle.NAUTILUS;
    if (p == DOLPHIN) return org.bukkit.Particle.DOLPHIN;
    if (p == SNEEZE) return org.bukkit.Particle.SNEEZE;
    if (p == CAMPFIRE_COSY_SMOKE) return org.bukkit.Particle.CAMPFIRE_COSY_SMOKE;
    if (p == CAMPFIRE_SIGNAL_SMOKE) return org.bukkit.Particle.CAMPFIRE_SIGNAL_SMOKE;
    if (p == COMPOSTER) return org.bukkit.Particle.COMPOSTER;
    if (p == FLASH) return org.bukkit.Particle.FLASH;
    if (p == FALLING_LAVA) return org.bukkit.Particle.FALLING_LAVA;
    if (p == LANDING_LAVA) return org.bukkit.Particle.LANDING_LAVA;
    if (p == FALLING_WATER) return org.bukkit.Particle.FALLING_WATER;
    if (p == DRIPPING_HONEY) return org.bukkit.Particle.DRIPPING_HONEY;
    if (p == FALLING_HONEY) return org.bukkit.Particle.FALLING_HONEY;
    if (p == LANDING_HONEY) return org.bukkit.Particle.LANDING_HONEY;
    if (p == FALLING_NECTAR) return org.bukkit.Particle.FALLING_NECTAR;
    if (p == SOUL_FIRE_FLAME) return org.bukkit.Particle.SOUL_FIRE_FLAME;
    if (p == ASH) return org.bukkit.Particle.ASH;
    if (p == CRIMSON_SPORE) return org.bukkit.Particle.CRIMSON_SPORE;
    if (p == WARPED_SPORE) return org.bukkit.Particle.WARPED_SPORE;
    if (p == SOUL) return org.bukkit.Particle.SOUL;
    if (p == DRIPPING_OBSIDIAN_TEAR) return org.bukkit.Particle.DRIPPING_OBSIDIAN_TEAR;
    if (p == FALLING_OBSIDIAN_TEAR) return org.bukkit.Particle.FALLING_OBSIDIAN_TEAR;
    if (p == LANDING_OBSIDIAN_TEAR) return org.bukkit.Particle.LANDING_OBSIDIAN_TEAR;
    if (p == REVERSE_PORTAL) return org.bukkit.Particle.REVERSE_PORTAL;
    if (p == WHITE_ASH) return org.bukkit.Particle.WHITE_ASH;
    if (p == DUST_COLOR_TRANSITION) return org.bukkit.Particle.DUST_COLOR_TRANSITION;
    if (p == VIBRATION) return org.bukkit.Particle.VIBRATION;
    if (p == FALLING_SPORE_BLOSSOM) return org.bukkit.Particle.FALLING_SPORE_BLOSSOM;
    if (p == SPORE_BLOSSOM_AIR) return org.bukkit.Particle.SPORE_BLOSSOM_AIR;
    if (p == SMALL_FLAME) return org.bukkit.Particle.SMALL_FLAME;
    if (p == SNOWFLAKE) return org.bukkit.Particle.SNOWFLAKE;
    if (p == DRIPPING_DRIPSTONE_LAVA) return org.bukkit.Particle.DRIPPING_DRIPSTONE_LAVA;
    if (p == FALLING_DRIPSTONE_LAVA) return org.bukkit.Particle.FALLING_DRIPSTONE_LAVA;
    if (p == DRIPPING_DRIPSTONE_WATER) return org.bukkit.Particle.DRIPPING_DRIPSTONE_WATER;
    if (p == FALLING_DRIPSTONE_WATER) return org.bukkit.Particle.FALLING_DRIPSTONE_WATER;
    if (p == GLOW_SQUID_INK) return org.bukkit.Particle.GLOW_SQUID_INK;
    if (p == GLOW) return org.bukkit.Particle.GLOW;
    if (p == WAX_ON) return org.bukkit.Particle.WAX_ON;
    if (p == WAX_OFF) return org.bukkit.Particle.WAX_OFF;
    if (p == ELECTRIC_SPARK) return org.bukkit.Particle.ELECTRIC_SPARK;
    if (p == SCRAPE) return org.bukkit.Particle.SCRAPE;
    if (p == BLOCK_MARKER) return org.bukkit.Particle.BLOCK_MARKER;
    if (p == SONIC_BOOM) return org.bukkit.Particle.SONIC_BOOM;
    if (p == SCULK_SOUL) return org.bukkit.Particle.SCULK_SOUL;
    if (p == SCULK_CHARGE) return org.bukkit.Particle.SCULK_CHARGE;
    if (p == SCULK_CHARGE_POP) return org.bukkit.Particle.SCULK_CHARGE_POP;
    if (p == SHRIEK) return org.bukkit.Particle.SHRIEK;
    return null;
  }

  public static <T> @Nullable Object mapParticleData(ParticleContext<T> context) {
    var p = context.particle();
    var data = context.data();
    if ((p == Particle.BLOCK || p == Particle.FALLING_DUST || p == Particle.BLOCK_MARKER) && data instanceof BlockState state) {
      return PlatformAdapter.toBukkitData(state);
    } else if (p == Particle.ITEM && data instanceof Item item) {
      return PlatformAdapter.toBukkitItem(item);
    } else if (p == Particle.DUST && data instanceof ParticleDustData dust) {
      return new DustOptions(Color.fromRGB(dust.red(), dust.green(), dust.blue()), dust.size());
    } else if (p == Particle.DUST_COLOR_TRANSITION && data instanceof Transitive dust) {
      var from = Color.fromRGB(dust.red(), dust.green(), dust.blue());
      var to = Color.fromRGB(dust.toRed(), dust.toGreen(), dust.toBlue());
      return new DustTransition(from, to, dust.size());
    } /*else if (p == Particle.VIBRATION && data instanceof Vibration v) { // TODO Add?
    }*/
    return null;
  }
}
