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

package me.moros.bending.common.ability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityDescription.Sequence;
import me.moros.bending.api.collision.CollisionPair;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.registry.Container;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.util.FeaturePermissions;
import me.moros.bending.api.util.KeyUtil;
import me.moros.bending.common.ability.air.AirBlade;
import me.moros.bending.common.ability.air.AirBlast;
import me.moros.bending.common.ability.air.AirBreath;
import me.moros.bending.common.ability.air.AirBurst;
import me.moros.bending.common.ability.air.AirPunch;
import me.moros.bending.common.ability.air.AirScooter;
import me.moros.bending.common.ability.air.AirShield;
import me.moros.bending.common.ability.air.AirSpout;
import me.moros.bending.common.ability.air.AirSwipe;
import me.moros.bending.common.ability.air.Tornado;
import me.moros.bending.common.ability.air.passive.AirAgility;
import me.moros.bending.common.ability.air.passive.GracefulDescent;
import me.moros.bending.common.ability.air.sequence.AirWheel;
import me.moros.bending.common.ability.avatar.AvatarState;
import me.moros.bending.common.ability.earth.*;
import me.moros.bending.common.ability.earth.passive.DensityShift;
import me.moros.bending.common.ability.earth.passive.EarthCling;
import me.moros.bending.common.ability.earth.passive.FerroControl;
import me.moros.bending.common.ability.earth.passive.Locksmithing;
import me.moros.bending.common.ability.earth.sequence.EarthPillars;
import me.moros.bending.common.ability.earth.sequence.EarthShards;
import me.moros.bending.common.ability.fire.Blaze;
import me.moros.bending.common.ability.fire.Combustion;
import me.moros.bending.common.ability.fire.FireBlast;
import me.moros.bending.common.ability.fire.FireBreath;
import me.moros.bending.common.ability.fire.FireBurst;
import me.moros.bending.common.ability.fire.FireJet;
import me.moros.bending.common.ability.fire.FireShield;
import me.moros.bending.common.ability.fire.FireWall;
import me.moros.bending.common.ability.fire.FlameRush;
import me.moros.bending.common.ability.fire.HeatControl;
import me.moros.bending.common.ability.fire.Lightning;
import me.moros.bending.common.ability.fire.sequence.FireKick;
import me.moros.bending.common.ability.fire.sequence.FireSpin;
import me.moros.bending.common.ability.fire.sequence.FireWheel;
import me.moros.bending.common.ability.water.FrostBreath;
import me.moros.bending.common.ability.water.HealingWaters;
import me.moros.bending.common.ability.water.IceCrawl;
import me.moros.bending.common.ability.water.IceSpike;
import me.moros.bending.common.ability.water.IceWall;
import me.moros.bending.common.ability.water.OctopusForm;
import me.moros.bending.common.ability.water.PhaseChange;
import me.moros.bending.common.ability.water.Torrent;
import me.moros.bending.common.ability.water.WaterBubble;
import me.moros.bending.common.ability.water.WaterManipulation;
import me.moros.bending.common.ability.water.WaterRing;
import me.moros.bending.common.ability.water.WaterSpout;
import me.moros.bending.common.ability.water.WaterWave;
import me.moros.bending.common.ability.water.passive.FastSwim;
import me.moros.bending.common.ability.water.passive.HydroSink;
import me.moros.bending.common.ability.water.sequence.Iceberg;
import me.moros.bending.common.ability.water.sequence.WaterGimbal;
import me.moros.bending.common.config.ConfigManager;
import me.moros.bending.common.util.Initializer;
import me.moros.bending.common.util.ReflectionUtil;
import net.kyori.adventure.key.Key;

import static me.moros.bending.api.ability.Activation.*;
import static me.moros.bending.api.ability.element.Element.*;

/**
 * Used to initialize all default ability descriptions, sequences and collisions.
 */
public final class AbilityInitializer implements Initializer {
  private static final Key DUMMY = KeyUtil.simple("dummy");

  public static final Container<Key> spouts = wrap("AirSpout", "WaterSpout");
  public static final Container<Key> blasts = wrap("EarthBlast", "FireBlast", "WaterManipulation");
  public static final Container<Key> layer0 = wrap("EarthGlove", "MetalCable");
  public static final Container<Key> layer1 = wrap("AirSwipe", "AirBurst", "EarthBlast", "FireBlast", "FireBurst", "WaterManipulation");
  public static final Container<Key> layer2 = wrap("AirWheel", "AirPunch", "AirBlade", "FireKick", "FireSpin", "FireWheel", "FlameRush");
  public static final Container<Key> layer3 = wrap("LavaDisk", "EarthSmash", "Combustion");

  private static Container<Key> wrap(String... abilities) {
    var keys = Stream.of(abilities).map(AbilityInitializer::key).collect(Collectors.toSet());
    return Container.create(DUMMY, keys);
  }

  private static Key key(String ability) {
    return Objects.requireNonNull(KeyUtil.BENDING_KEY_MAPPER.apply(ability.toLowerCase(Locale.ROOT)));
  }

  private final Collection<AbilityDescription> abilities = new ArrayList<>(64);

  @Override
  public void init() {
    initAir();
    initWater();
    initEarth();
    initFire();
    initAvatar();

    Registries.ABILITIES.register(abilities);
    Registries.COLLISIONS.register(buildCollisions());
  }

  private Collection<CollisionPair> buildCollisions() {
    Collection<Key> shieldCollisions = new ArrayList<>();
    layer0.forEach(shieldCollisions::add);
    layer1.forEach(shieldCollisions::add);
    shieldCollisions.add(key("EarthShot"));
    return CollisionPair.builder()
      .layer(layer0)
      .layer(layer1)
      .layer(layer2)
      .layer(layer3)
      .add(spouts, layer1, true, false)
      .add(spouts, wrap("LavaDisk", "EarthSmash", "FlameRush"), true, false)
      .add(shieldCollisions, wrap("AirShield", "FireWall"), true, false)
      .add(key("Shockwave"), blasts, false, true)
      .add(key("FireShield"), blasts, false, true)
      .add(key("FireShield"), key("Combustion"), true, true)
      .add(key("FrostBreath"), blasts, false, true)
      .add(key("FireBreath"), blasts, false, true)
      .add(key("Lightning"), key("MetalCable"), true, false)
      .add(key("Lightning"), key("EarthSmash"), false, true)
      .add(key("Lightning"), key("EarthBlast"), false, true)
      .add(key("FireBreath"), key("EarthSmash"), false, false)
      .add(key("FrostBreath"), key("EarthSmash"), false, false)
      .add(key("FrostBreath"), key("FireBreath"), true, true)
      .add(key("FrostBreath"), key("AirShield"), true, true)
      .add(key("IceCrawl"), key("EarthLine"), true, false)
      .build();
  }

  private void initAir() {
    abilities.add(AbilityDescription.builder("AirAgility", AirAgility::new)
      .element(AIR).activation(PASSIVE).canBind(false).build());

    abilities.add(AbilityDescription.builder("GracefulDescent", GracefulDescent::new)
      .element(AIR).activation(PASSIVE).canBind(false).build());

    abilities.add(AbilityDescription.builder("AirSwipe", AirSwipe::new)
      .element(AIR).activation(ATTACK, SNEAK).build());

    abilities.add(AbilityDescription.builder("AirBlast", AirBlast::new)
      .element(AIR).activation(ATTACK, SNEAK_RELEASE).build());

    abilities.add(AbilityDescription.builder("AirBurst", AirBurst::new)
      .element(AIR).activation(ATTACK, SNEAK, FALL).build());

    abilities.add(AbilityDescription.builder("AirShield", AirShield::new)
      .element(AIR).activation(SNEAK).build());

    abilities.add(AbilityDescription.builder("AirSpout", AirSpout::new)
      .element(AIR).activation(ATTACK).build());

    abilities.add(AbilityDescription.builder("AirPunch", AirPunch::new)
      .element(AIR).activation(ATTACK).build());

    abilities.add(AbilityDescription.builder("AirBreath", AirBreath::new)
      .element(AIR).activation(SNEAK).build());

    abilities.add(AbilityDescription.builder("Tornado", Tornado::new)
      .element(AIR).activation(SNEAK).build());

    AbilityDescription airBlade = AbilityDescription.builder("AirBlade", AirBlade::new)
      .element(AIR).activation(SNEAK).build();
    abilities.add(airBlade);

    AbilityDescription airScooter = AbilityDescription.builder("AirScooter", AirScooter::new)
      .element(AIR).activation(ATTACK).build();
    abilities.add(airScooter);

    Sequence airWheel = AbilityDescription.builder("AirWheel", AirWheel::new)
      .element(AIR).activation(SEQUENCE).buildSequence(steps -> steps
        .add(airScooter, SNEAK, SNEAK_RELEASE, SNEAK, SNEAK_RELEASE)
        .add(airBlade, ATTACK)
      );
    abilities.add(airWheel);
  }

  private void initWater() {
    abilities.add(AbilityDescription.builder("FastSwim", FastSwim::new)
      .element(WATER).activation(PASSIVE).canBind(false).build());

    abilities.add(AbilityDescription.builder("HydroSink", HydroSink::new)
      .element(WATER).activation(PASSIVE).canBind(false).build());

    AbilityDescription waterManipulation = AbilityDescription.builder("WaterManipulation", WaterManipulation::new)
      .element(WATER).activation(SNEAK, ATTACK).bypassCooldown(true).build();
    abilities.add(waterManipulation);

    abilities.add(AbilityDescription.builder("WaterSpout", WaterSpout::new)
      .element(WATER).activation(ATTACK).build());

    abilities.add(AbilityDescription.builder("HealingWaters", HealingWaters::new)
      .element(WATER).activation(SNEAK).build());

    abilities.add(AbilityDescription.builder("WaterBubble", WaterBubble::new)
      .element(WATER).activation(SNEAK).build());

    abilities.add(AbilityDescription.builder("OctopusForm", OctopusForm::new)
      .element(WATER).activation(ATTACK).build());

    AbilityDescription waterRing = AbilityDescription.builder("WaterRing", WaterRing::new)
      .element(WATER).activation(ATTACK).build();
    abilities.add(waterRing);

    AbilityDescription waterWave = AbilityDescription.builder("WaterWave", WaterWave::new)
      .element(WATER).activation(PASSIVE, SNEAK).canBind(false).build();
    abilities.add(waterWave);

    AbilityDescription torrent = AbilityDescription.builder("Torrent", Torrent::new)
      .element(WATER).activation(ATTACK).bypassCooldown(true).build();
    abilities.add(torrent);

    AbilityDescription phaseChange = AbilityDescription.builder("PhaseChange", PhaseChange::new)
      .element(WATER).activation(ATTACK, SNEAK).build();
    abilities.add(phaseChange);

    AbilityDescription iceCrawl = AbilityDescription.builder("IceCrawl", IceCrawl::new)
      .element(WATER).activation(ATTACK, SNEAK).build();
    abilities.add(iceCrawl);

    AbilityDescription iceSpike = AbilityDescription.builder("IceSpike", IceSpike::new)
      .element(WATER).activation(ATTACK, SNEAK).build();
    abilities.add(iceSpike);

    abilities.add(AbilityDescription.builder("IceWall", IceWall::new)
      .element(WATER).activation(SNEAK).bypassCooldown(true).build());

    abilities.add(AbilityDescription.builder("FrostBreath", FrostBreath::new)
      .element(WATER).activation(SNEAK).build());

    Sequence waterGimbal = AbilityDescription.builder("WaterGimbal", WaterGimbal::new)
      .element(WATER).activation(SEQUENCE).buildSequence(steps -> steps
        .add(waterRing, SNEAK, SNEAK_RELEASE, SNEAK, SNEAK_RELEASE)
        .add(torrent, SNEAK)
      );
    abilities.add(waterGimbal);

    Sequence iceberg = AbilityDescription.builder("Iceberg", Iceberg::new)
      .element(WATER).activation(SEQUENCE).buildSequence(steps -> steps
        .add(phaseChange, SNEAK)
        .add(iceSpike, SNEAK_RELEASE)
        .add(phaseChange, SNEAK)
        .add(iceSpike, SNEAK_RELEASE, SNEAK)
      );
    abilities.add(iceberg);
  }

  private void initEarth() {
    abilities.add(AbilityDescription.builder("DensityShift", DensityShift::new)
      .element(EARTH).activation(PASSIVE).canBind(false).build());

    abilities.add(AbilityDescription.builder("EarthCling", EarthCling::new)
      .element(EARTH).activation(PASSIVE).canBind(false).build());

    abilities.add(AbilityDescription.builder("FerroControl", FerroControl::new)
      .element(EARTH).activation(PASSIVE).canBind(false).require(FeaturePermissions.METAL).build());

    abilities.add(AbilityDescription.builder("Locksmithing", Locksmithing::new)
      .element(EARTH).activation(PASSIVE).canBind(false).require(FeaturePermissions.METAL).build());

    abilities.add(AbilityDescription.builder("EarthBlast", EarthBlast::new)
      .element(EARTH).activation(SNEAK, ATTACK).bypassCooldown(true).build());

    abilities.add(AbilityDescription.builder("EarthSmash", EarthSmash::new)
      .element(EARTH).activation(ATTACK, SNEAK).bypassCooldown(true).build());

    abilities.add(AbilityDescription.builder("EarthShot", EarthShot::new)
      .element(EARTH).activation(ATTACK, SNEAK).build());

    abilities.add(AbilityDescription.builder("EarthLine", EarthLine::new)
      .element(EARTH).activation(ATTACK, SNEAK).bypassCooldown(true).build());

    abilities.add(AbilityDescription.builder("EarthTunnel", EarthTunnel::new)
      .element(EARTH).activation(SNEAK).build());

    AbilityDescription earthArmor = AbilityDescription.builder("EarthArmor", EarthArmor::new)
      .element(EARTH).activation(ATTACK).build();
    abilities.add(earthArmor);

    AbilityDescription earthGlove = AbilityDescription.builder("EarthGlove", EarthGlove::new)
      .element(EARTH).activation(ATTACK, SNEAK).bypassCooldown(true).build();
    abilities.add(earthGlove);

    abilities.add(AbilityDescription.builder("RaiseEarth", RaiseEarth::new)
      .element(EARTH).activation(ATTACK, SNEAK).build());

    AbilityDescription collapse = AbilityDescription.builder("Collapse", Collapse::new)
      .element(EARTH).activation(ATTACK, SNEAK).build();
    abilities.add(collapse);

    AbilityDescription catapult = AbilityDescription.builder("Catapult", Catapult::new)
      .element(EARTH).activation(ATTACK, SNEAK).build();
    abilities.add(catapult);

    AbilityDescription shockwave = AbilityDescription.builder("Shockwave", Shockwave::new)
      .element(EARTH).activation(ATTACK, SNEAK, FALL).build();
    abilities.add(shockwave);

    abilities.add(AbilityDescription.builder("EarthSurf", EarthSurf::new)
      .element(EARTH).activation(SNEAK, FALL).build());

    abilities.add(AbilityDescription.builder("MetalCable", MetalCable::new)
      .element(EARTH).activation(ATTACK, SNEAK).bypassCooldown(true).require(FeaturePermissions.METAL).build());

    abilities.add(AbilityDescription.builder("LavaDisk", LavaDisk::new)
      .element(EARTH).activation(SNEAK).require(FeaturePermissions.LAVA).build());

    abilities.add(AbilityDescription.builder("LavaFlux", LavaFlux::new)
      .element(EARTH).activation(SNEAK).require(FeaturePermissions.LAVA).build());

    Sequence bulwark = AbilityDescription.builder("Bulwark", Bulwark::new)
      .element(EARTH).activation(SEQUENCE).hidden(true).buildSequence(steps -> steps
        .add(earthArmor, SNEAK, SNEAK_RELEASE)
      );
    abilities.add(bulwark);

    Sequence earthPillars = AbilityDescription.builder("EarthPillars", EarthPillars::new)
      .element(EARTH).activation(SEQUENCE, FALL).buildSequence(steps -> steps
        .add(shockwave, SNEAK)
        .add(catapult, SNEAK_RELEASE)
      );
    abilities.add(earthPillars);

    Sequence earthShards = AbilityDescription.builder("EarthShards", EarthShards::new)
      .element(EARTH).activation(SEQUENCE).buildSequence(steps -> steps
        .add(earthGlove, SNEAK)
        .add(collapse, SNEAK_RELEASE, SNEAK)
        .add(earthGlove, SNEAK_RELEASE)
      );
    abilities.add(earthShards);
  }

  private void initFire() {
    AbilityDescription fireBlast = AbilityDescription.builder("FireBlast", FireBlast::new)
      .element(FIRE).activation(ATTACK, SNEAK).build();
    abilities.add(fireBlast);

    abilities.add(AbilityDescription.builder("FireBurst", FireBurst::new)
      .element(FIRE).activation(ATTACK, SNEAK).build());

    abilities.add(AbilityDescription.builder("FlameRush", FlameRush::new)
      .element(FIRE).activation(SNEAK).build());

    AbilityDescription heatControl = AbilityDescription.builder("HeatControl", HeatControl::new)
      .element(FIRE).activation(ATTACK, SNEAK).build();
    abilities.add(heatControl);

    abilities.add(AbilityDescription.builder("Blaze", Blaze::new)
      .element(FIRE).activation(ATTACK, SNEAK).build());

    abilities.add(AbilityDescription.builder("FireBreath", FireBreath::new)
      .element(FIRE).activation(SNEAK).build());

    AbilityDescription fireShield = AbilityDescription.builder("FireShield", FireShield::new)
      .element(FIRE).activation(ATTACK, SNEAK).build();
    abilities.add(fireShield);

    AbilityDescription fireJet = AbilityDescription.builder("FireJet", FireJet::new)
      .element(FIRE).activation(ATTACK).build();
    abilities.add(fireJet);

    AbilityDescription fireWall = AbilityDescription.builder("FireWall", FireWall::new)
      .element(FIRE).activation(ATTACK).build();
    abilities.add(fireWall);

    abilities.add(AbilityDescription.builder("Lightning", Lightning::new)
      .element(FIRE).activation(SNEAK).build());

    abilities.add(AbilityDescription.builder("Combustion", Combustion::new)
      .element(FIRE).activation(ATTACK, SNEAK).bypassCooldown(true).build());

    Sequence fireKick = AbilityDescription.builder("FireKick", FireKick::new)
      .element(FIRE).activation(SEQUENCE).buildSequence(steps -> steps
        .add(fireBlast, ATTACK, ATTACK, SNEAK, ATTACK)
      );
    abilities.add(fireKick);

    Sequence fireSpin = AbilityDescription.builder("FireSpin", FireSpin::new)
      .element(FIRE).activation(SEQUENCE).buildSequence(steps -> steps
        .add(fireBlast, ATTACK, ATTACK)
        .add(fireShield, ATTACK, SNEAK, SNEAK_RELEASE)
      );
    abilities.add(fireSpin);

    Sequence fireWheel = AbilityDescription.builder("FireWheel", FireWheel::new)
      .element(FIRE).activation(SEQUENCE).buildSequence(steps -> steps
        .add(fireShield, SNEAK, INTERACT_BLOCK, INTERACT_BLOCK, SNEAK_RELEASE)
      );
    abilities.add(fireWheel);
  }

  private void initAvatar() {
    abilities.add(AbilityDescription.builder("AvatarState", AvatarState::new)
      .element(AIR, WATER, EARTH, FIRE).activation(ATTACK, SNEAK).build());
  }

  public static void initConfigs() {
    // Init configs
    Registries.ABILITIES.stream()
      .map(AbilityDescription::createAbility)
      .map(a -> ReflectionUtil.findInnerClass(a, Configurable.class)).filter(Objects::nonNull)
      .forEach(ConfigManager::cache);
  }
}
