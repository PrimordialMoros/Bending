/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.moros.bending.Bending;
import me.moros.bending.ability.air.AirBlade;
import me.moros.bending.ability.air.AirBlast;
import me.moros.bending.ability.air.AirBreath;
import me.moros.bending.ability.air.AirBurst;
import me.moros.bending.ability.air.AirPunch;
import me.moros.bending.ability.air.AirScooter;
import me.moros.bending.ability.air.AirShield;
import me.moros.bending.ability.air.AirSpout;
import me.moros.bending.ability.air.AirSwipe;
import me.moros.bending.ability.air.Tornado;
import me.moros.bending.ability.air.passives.AirAgility;
import me.moros.bending.ability.air.passives.GracefulDescent;
import me.moros.bending.ability.air.sequences.AirWheel;
import me.moros.bending.ability.earth.Bulwark;
import me.moros.bending.ability.earth.Catapult;
import me.moros.bending.ability.earth.Collapse;
import me.moros.bending.ability.earth.EarthArmor;
import me.moros.bending.ability.earth.EarthBlast;
import me.moros.bending.ability.earth.EarthGlove;
import me.moros.bending.ability.earth.EarthLine;
import me.moros.bending.ability.earth.EarthShot;
import me.moros.bending.ability.earth.EarthSmash;
import me.moros.bending.ability.earth.EarthTunnel;
import me.moros.bending.ability.earth.LavaDisk;
import me.moros.bending.ability.earth.MetalCable;
import me.moros.bending.ability.earth.RaiseEarth;
import me.moros.bending.ability.earth.Shockwave;
import me.moros.bending.ability.earth.passives.DensityShift;
import me.moros.bending.ability.earth.passives.EarthCling;
import me.moros.bending.ability.earth.passives.FerroControl;
import me.moros.bending.ability.earth.sequences.EarthPillars;
import me.moros.bending.ability.earth.sequences.EarthShards;
import me.moros.bending.ability.fire.Blaze;
import me.moros.bending.ability.fire.Bolt;
import me.moros.bending.ability.fire.Combustion;
import me.moros.bending.ability.fire.FireBlast;
import me.moros.bending.ability.fire.FireBreath;
import me.moros.bending.ability.fire.FireBurst;
import me.moros.bending.ability.fire.FireJet;
import me.moros.bending.ability.fire.FireShield;
import me.moros.bending.ability.fire.FireWall;
import me.moros.bending.ability.fire.HeatControl;
import me.moros.bending.ability.fire.sequences.FireKick;
import me.moros.bending.ability.fire.sequences.FireSpin;
import me.moros.bending.ability.fire.sequences.FireWave;
import me.moros.bending.ability.fire.sequences.FireWheel;
import me.moros.bending.ability.water.FrostBreath;
import me.moros.bending.ability.water.HealingWaters;
import me.moros.bending.ability.water.IceCrawl;
import me.moros.bending.ability.water.IceSpike;
import me.moros.bending.ability.water.IceWall;
import me.moros.bending.ability.water.OctopusForm;
import me.moros.bending.ability.water.PhaseChange;
import me.moros.bending.ability.water.Torrent;
import me.moros.bending.ability.water.WaterBubble;
import me.moros.bending.ability.water.WaterManipulation;
import me.moros.bending.ability.water.WaterRing;
import me.moros.bending.ability.water.WaterSpout;
import me.moros.bending.ability.water.WaterWave;
import me.moros.bending.ability.water.passives.FastSwim;
import me.moros.bending.ability.water.passives.HydroSink;
import me.moros.bending.ability.water.sequences.Iceberg;
import me.moros.bending.ability.water.sequences.WaterGimbal;
import me.moros.bending.game.manager.CollisionManager;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.sequence.AbilityAction;
import me.moros.bending.model.ability.sequence.Sequence;
import me.moros.bending.model.collision.CollisionBuilder;
import me.moros.bending.model.collision.RegisteredCollision;
import org.checkerframework.checker.nullness.qual.NonNull;

import static me.moros.bending.model.Element.*;
import static me.moros.bending.model.ability.util.ActivationMethod.*;

/**
 * Used to initialize all ability descriptions, sequences and collisions
 */
public final class AbilityInitializer {
  public static final List<String> spoutLayer = List.of("AirSpout", "WaterSpout");
  public static final List<String> shieldLayer = List.of("AirShield", "FireShield", "WallOfFire");
  public static final List<String> breathCollisions = List.of("EarthBlast", "FireBlast", "WaterManipulation");
  public static final List<String> layer0 = List.of("EarthGlove", "MetalCable");
  public static final List<String> layer1 = List.of("AirSwipe", "EarthBlast", "FireBlast", "WaterManipulation");
  public static final List<String> layer2 = List.of("AirWheel", "AirPunch", "AirBlade", "FireKick", "FireSpin", "FireWheel");
  public static final List<String> layer3 = List.of("LavaDisk", "Combustion", "EarthSmash");

  private final Collection<AbilityDescription> abilities = new ArrayList<>(64);
  private final Map<AbilityDescription, Sequence> sequences = new HashMap<>();
  private final AbilityRegistry registry;

  protected AbilityInitializer(@NonNull Game game) {
    registry = game.abilityRegistry();
    initAir();
    initWater();
    initEarth();
    initFire();

    int abilityAmount = registry.registerAbilities(abilities);
    int addonAmount = registry.registerAbilities(AbilityRegistry.AddonRegistry.addonAbilities());
    int sequenceAmount = game.sequenceManager().registerSequences(sequences);
    int addonSequences = game.sequenceManager().registerSequences(AbilityRegistry.AddonRegistry.addonSequences());
    int collisionAmount = CollisionManager.registerCollisions(buildCollisions());

    Bending.logger().info("Registered " + abilityAmount + " core abilities and " + addonAmount + " addon abilities!");
    Bending.logger().info("Registered " + sequenceAmount + " core sequences and " + addonSequences + " addon sequences!");
    Bending.logger().info("Registered " + collisionAmount + " collisions!");
  }

  private Collection<RegisteredCollision> buildCollisions() {
    return new CollisionBuilder(registry)
      .addLayer(layer0)
      .addSpecialLayer(spoutLayer)
      .addLayer(layer1)
      .addLayer(layer2)
      .addSpecialLayer(shieldLayer)
      .addLayer(layer3)
      .addSimpleCollision("FrostBreath", breathCollisions, false, true)
      .addSimpleCollision("FireBreath", breathCollisions, false, true)
      .addSimpleCollision("FireBreath", "EarthSmash", false, false)
      .addSimpleCollision("FrostBreath", "EarthSmash", false, false)
      .addSimpleCollision("FrostBreath", "FireBreath", true, true)
      .addSimpleCollision("FrostBreath", "AirShield", true, true)
      .addSimpleCollision("EarthShot", "AirShield", true, false)
      .addSimpleCollision("IceCrawl", "EarthLine", true, false)
      .build();
  }

  private void initAir() {
    abilities.add(AbilityDescription.builder("AirAgility", AirAgility::new)
      .element(AIR).activation(PASSIVE)
      .canBind(false).harmless(true).build());

    abilities.add(AbilityDescription.builder("GracefulDescent", GracefulDescent::new)
      .element(AIR).activation(PASSIVE)
      .canBind(false).harmless(true).build());

    abilities.add(AbilityDescription.builder("AirSwipe", AirSwipe::new)
      .element(AIR).activation(ATTACK, SNEAK).build());

    abilities.add(AbilityDescription.builder("AirBlast", AirBlast::new)
      .element(AIR).activation(ATTACK, SNEAK_RELEASE).build());

    abilities.add(AbilityDescription.builder("AirBurst", AirBurst::new)
      .element(AIR).activation(ATTACK, SNEAK, FALL).build());

    abilities.add(AbilityDescription.builder("AirShield", AirShield::new)
      .element(AIR).activation(SNEAK).build());

    abilities.add(AbilityDescription.builder("AirSpout", AirSpout::new)
      .element(AIR).activation(ATTACK).harmless(true).build());

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
      .element(AIR).activation(ATTACK).harmless(true).build();
    abilities.add(airScooter);

    AbilityDescription airWheel = AbilityDescription.builder("AirWheel", AirWheel::new)
      .element(AIR).activation(SEQUENCE).build();
    abilities.add(airWheel);

    sequences.put(airWheel, new Sequence(
      new AbilityAction(airScooter, SNEAK),
      new AbilityAction(airScooter, SNEAK_RELEASE),
      new AbilityAction(airScooter, SNEAK),
      new AbilityAction(airScooter, SNEAK_RELEASE),
      new AbilityAction(airBlade, ATTACK)
    ));
  }

  private void initWater() {
    abilities.add(AbilityDescription.builder("FastSwim", FastSwim::new)
      .element(WATER).activation(PASSIVE).canBind(false).harmless(true).build());

    abilities.add(AbilityDescription.builder("HydroSink", HydroSink::new)
      .element(WATER).activation(PASSIVE).canBind(false).harmless(true).build());

    AbilityDescription waterManipulation = AbilityDescription.builder("WaterManipulation", WaterManipulation::new)
      .element(WATER).activation(SNEAK, ATTACK).sourcePlant(true).bypassCooldown(true).build();
    abilities.add(waterManipulation);

    abilities.add(AbilityDescription.builder("WaterSpout", WaterSpout::new)
      .element(WATER).activation(ATTACK).harmless(true).build());

    abilities.add(AbilityDescription.builder("HealingWaters", HealingWaters::new)
      .element(WATER).activation(SNEAK).harmless(true).build());

    abilities.add(AbilityDescription.builder("WaterBubble", WaterBubble::new)
      .element(WATER).activation(SNEAK).build());

    abilities.add(AbilityDescription.builder("OctopusForm", OctopusForm::new)
      .element(WATER).activation(ATTACK).sourcePlant(true).build());

    AbilityDescription waterRing = AbilityDescription.builder("WaterRing", WaterRing::new)
      .element(WATER).activation(ATTACK).sourcePlant(true).build();
    abilities.add(waterRing);

    AbilityDescription waterWave = AbilityDescription.builder("WaterWave", WaterWave::new)
      .element(WATER).activation(SNEAK).canBind(false).build();
    abilities.add(waterWave);

    AbilityDescription torrent = AbilityDescription.builder("Torrent", Torrent::new)
      .element(WATER).activation(ATTACK).sourcePlant(true).bypassCooldown(true).build();
    abilities.add(torrent);

    AbilityDescription phaseChange = AbilityDescription.builder("PhaseChange", PhaseChange::new)
      .element(WATER).activation(PASSIVE).bypassCooldown(true).build();
    abilities.add(phaseChange);

    AbilityDescription iceCrawl = AbilityDescription.builder("IceCrawl", IceCrawl::new)
      .element(WATER).activation(ATTACK, SNEAK).build();
    abilities.add(iceCrawl);

    AbilityDescription iceSpike = AbilityDescription.builder("IceSpike", IceSpike::new)
      .element(WATER).activation(ATTACK, SNEAK).build();
    abilities.add(iceSpike);

    abilities.add(AbilityDescription.builder("IceWall", IceWall::new)
      .element(WATER).activation(SNEAK).build());

    AbilityDescription waterGimbal = AbilityDescription.builder("WaterGimbal", WaterGimbal::new)
      .element(WATER).activation(SEQUENCE).build();
    abilities.add(waterGimbal);

    abilities.add(AbilityDescription.builder("FrostBreath", FrostBreath::new)
      .element(WATER).activation(SNEAK).build());

    AbilityDescription iceberg = AbilityDescription.builder("Iceberg", Iceberg::new)
      .element(WATER).activation(SEQUENCE).build();
    abilities.add(iceberg);

    sequences.put(waterGimbal, new Sequence(
      new AbilityAction(waterRing, SNEAK),
      new AbilityAction(waterRing, SNEAK_RELEASE),
      new AbilityAction(waterRing, SNEAK),
      new AbilityAction(waterRing, SNEAK_RELEASE),
      new AbilityAction(torrent, SNEAK)
    ));

    sequences.put(iceberg, new Sequence(
      new AbilityAction(phaseChange, SNEAK),
      new AbilityAction(iceSpike, SNEAK_RELEASE),
      new AbilityAction(phaseChange, SNEAK),
      new AbilityAction(iceSpike, SNEAK_RELEASE),
      new AbilityAction(iceSpike, SNEAK)
    ));
  }

  private void initEarth() {
    abilities.add(AbilityDescription.builder("DensityShift", DensityShift::new)
      .element(EARTH).activation(PASSIVE).canBind(false).harmless(true).build());

    abilities.add(AbilityDescription.builder("EarthCling", EarthCling::new)
      .element(EARTH).activation(PASSIVE).canBind(false).harmless(true).build());

    abilities.add(AbilityDescription.builder("FerroControl", FerroControl::new)
      .element(EARTH).activation(PASSIVE).canBind(false).harmless(true).build());

    abilities.add(AbilityDescription.builder("EarthBlast", EarthBlast::new)
      .element(EARTH).activation(SNEAK, ATTACK).bypassCooldown(true).build());

    abilities.add(AbilityDescription.builder("EarthSmash", EarthSmash::new)
      .element(EARTH).activation(ATTACK, SNEAK).bypassCooldown(true).build());

    abilities.add(AbilityDescription.builder("EarthShot", EarthShot::new)
      .element(EARTH).activation(ATTACK, SNEAK).build());

    abilities.add(AbilityDescription.builder("EarthLine", EarthLine::new)
      .element(EARTH).activation(ATTACK, SNEAK).build());

    abilities.add(AbilityDescription.builder("EarthTunnel", EarthTunnel::new)
      .element(EARTH).activation(SNEAK).build());

    AbilityDescription earthArmor = AbilityDescription.builder("EarthArmor", EarthArmor::new)
      .element(EARTH).activation(ATTACK).build();
    abilities.add(earthArmor);

    AbilityDescription bulwark = AbilityDescription.builder("Bulwark", Bulwark::new)
      .element(EARTH).activation(SEQUENCE).hidden(true).build();
    abilities.add(bulwark);

    AbilityDescription earthGlove = AbilityDescription.builder("EarthGlove", EarthGlove::new)
      .element(EARTH).activation(ATTACK, SNEAK).bypassCooldown(true).build();
    abilities.add(earthGlove);

    abilities.add(AbilityDescription.builder("RaiseEarth", RaiseEarth::new)
      .element(EARTH).activation(ATTACK, SNEAK).build());

    AbilityDescription collapse = AbilityDescription.builder("Collapse", Collapse::new)
      .element(EARTH).activation(ATTACK, SNEAK).build();
    abilities.add(collapse);

    AbilityDescription catapult = AbilityDescription.builder("Catapult", Catapult::new)
      .element(EARTH).activation(ATTACK, SNEAK).harmless(true).build();
    abilities.add(catapult);

    AbilityDescription shockwave = AbilityDescription.builder("Shockwave", Shockwave::new)
      .element(EARTH).activation(ATTACK, SNEAK, FALL).build();
    abilities.add(shockwave);

    AbilityDescription earthPillars = AbilityDescription.builder("EarthPillars", EarthPillars::new)
      .element(EARTH).activation(SEQUENCE, FALL).build();
    abilities.add(earthPillars);

    AbilityDescription earthShards = AbilityDescription.builder("EarthShards", EarthShards::new)
      .element(EARTH).activation(SEQUENCE).build();
    abilities.add(earthShards);

    abilities.add(AbilityDescription.builder("MetalCable", MetalCable::new)
      .element(EARTH).activation(ATTACK, SNEAK).bypassCooldown(true).build());

    abilities.add(AbilityDescription.builder("LavaDisk", LavaDisk::new)
      .element(EARTH).activation(SNEAK).build());

    sequences.put(bulwark, new Sequence(
      new AbilityAction(earthArmor, SNEAK),
      new AbilityAction(earthArmor, SNEAK_RELEASE)
    ));

    sequences.put(earthPillars, new Sequence(
      new AbilityAction(shockwave, SNEAK),
      new AbilityAction(catapult, SNEAK_RELEASE)
    ));

    sequences.put(earthShards, new Sequence(
      new AbilityAction(earthGlove, SNEAK),
      new AbilityAction(collapse, SNEAK_RELEASE),
      new AbilityAction(collapse, SNEAK),
      new AbilityAction(earthGlove, SNEAK_RELEASE)
    ));
  }

  private void initFire() {
    AbilityDescription fireBlast = AbilityDescription.builder("FireBlast", FireBlast::new)
      .element(FIRE).activation(ATTACK, SNEAK).build();
    abilities.add(fireBlast);

    abilities.add(AbilityDescription.builder("FireBurst", FireBurst::new)
      .element(FIRE).activation(ATTACK, SNEAK).build());

    AbilityDescription heatControl = AbilityDescription.builder("HeatControl", HeatControl::new)
      .element(FIRE).activation(PASSIVE).bypassCooldown(true).build();
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

    AbilityDescription fireWave = AbilityDescription.builder("FireWave", FireWave::new)
      .element(FIRE).activation(SEQUENCE).build();
    abilities.add(fireWave);

    AbilityDescription fireKick = AbilityDescription.builder("FireKick", FireKick::new)
      .element(FIRE).activation(SEQUENCE).build();
    abilities.add(fireKick);

    AbilityDescription fireSpin = AbilityDescription.builder("FireSpin", FireSpin::new)
      .element(FIRE).activation(SEQUENCE).build();
    abilities.add(fireSpin);

    AbilityDescription fireWheel = AbilityDescription.builder("FireWheel", FireWheel::new)
      .element(FIRE).activation(SEQUENCE).build();
    abilities.add(fireWheel);

    abilities.add(AbilityDescription.builder("Bolt", Bolt::new)
      .element(FIRE).activation(SNEAK).build());

    abilities.add(AbilityDescription.builder("Combustion", Combustion::new)
      .element(FIRE).activation(ATTACK, SNEAK).bypassCooldown(true).build());

    sequences.put(fireWave, new Sequence(
      new AbilityAction(heatControl, SNEAK),
      new AbilityAction(heatControl, SNEAK_RELEASE),
      new AbilityAction(heatControl, SNEAK),
      new AbilityAction(heatControl, SNEAK_RELEASE),
      new AbilityAction(fireWall, ATTACK)
    ));

    sequences.put(fireKick, new Sequence(
      new AbilityAction(fireBlast, ATTACK),
      new AbilityAction(fireBlast, ATTACK),
      new AbilityAction(fireBlast, SNEAK),
      new AbilityAction(fireBlast, ATTACK)
    ));

    sequences.put(fireSpin, new Sequence(
      new AbilityAction(fireBlast, ATTACK),
      new AbilityAction(fireBlast, ATTACK),
      new AbilityAction(fireShield, ATTACK),
      new AbilityAction(fireShield, SNEAK),
      new AbilityAction(fireShield, SNEAK_RELEASE)
    ));

    sequences.put(fireWheel, new Sequence(
      new AbilityAction(fireShield, SNEAK),
      new AbilityAction(fireShield, INTERACT_BLOCK),
      new AbilityAction(fireShield, INTERACT_BLOCK),
      new AbilityAction(fireShield, SNEAK_RELEASE)
    ));
  }
}
