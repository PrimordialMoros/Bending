/*
 * Copyright 2020-2021 Moros
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

package me.moros.bending.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
import me.moros.bending.ability.air.passive.AirAgility;
import me.moros.bending.ability.air.passive.GracefulDescent;
import me.moros.bending.ability.air.sequence.AirWheel;
import me.moros.bending.ability.earth.Bulwark;
import me.moros.bending.ability.earth.Catapult;
import me.moros.bending.ability.earth.Collapse;
import me.moros.bending.ability.earth.EarthArmor;
import me.moros.bending.ability.earth.EarthBlast;
import me.moros.bending.ability.earth.EarthGlove;
import me.moros.bending.ability.earth.EarthLine;
import me.moros.bending.ability.earth.EarthShot;
import me.moros.bending.ability.earth.EarthSmash;
import me.moros.bending.ability.earth.EarthSurf;
import me.moros.bending.ability.earth.EarthTunnel;
import me.moros.bending.ability.earth.LavaDisk;
import me.moros.bending.ability.earth.MetalCable;
import me.moros.bending.ability.earth.RaiseEarth;
import me.moros.bending.ability.earth.Shockwave;
import me.moros.bending.ability.earth.passive.DensityShift;
import me.moros.bending.ability.earth.passive.EarthCling;
import me.moros.bending.ability.earth.passive.FerroControl;
import me.moros.bending.ability.earth.sequence.EarthPillars;
import me.moros.bending.ability.earth.sequence.EarthShards;
import me.moros.bending.ability.fire.Blaze;
import me.moros.bending.ability.fire.Combustion;
import me.moros.bending.ability.fire.FireBlast;
import me.moros.bending.ability.fire.FireBreath;
import me.moros.bending.ability.fire.FireBurst;
import me.moros.bending.ability.fire.FireJet;
import me.moros.bending.ability.fire.FireShield;
import me.moros.bending.ability.fire.FireWall;
import me.moros.bending.ability.fire.FlameRush;
import me.moros.bending.ability.fire.HeatControl;
import me.moros.bending.ability.fire.Lightning;
import me.moros.bending.ability.fire.sequence.FireKick;
import me.moros.bending.ability.fire.sequence.FireSpin;
import me.moros.bending.ability.fire.sequence.FireWheel;
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
import me.moros.bending.ability.water.passive.FastSwim;
import me.moros.bending.ability.water.passive.HydroSink;
import me.moros.bending.ability.water.sequence.Iceberg;
import me.moros.bending.ability.water.sequence.WaterGimbal;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.description.AbilityDescription.Sequence;
import me.moros.bending.model.ability.description.SequenceStep;
import me.moros.bending.model.collision.RegisteredCollision;
import me.moros.bending.registry.Registries;

import static me.moros.bending.model.Element.*;
import static me.moros.bending.model.ability.Activation.*;

/**
 * Used to initialize all ability descriptions, sequences and collisions
 */
public final class AbilityInitializer {
  public static final List<String> spouts = List.of("AirSpout", "WaterSpout");
  public static final List<String> blasts = List.of("EarthBlast", "FireBlast", "WaterManipulation");
  public static final List<String> layer0 = List.of("EarthGlove", "MetalCable");
  public static final List<String> layer1 = List.of("AirSwipe", "AirBurst", "EarthBlast", "FireBlast", "FireBurst", "WaterManipulation");
  public static final List<String> layer2 = List.of("AirWheel", "AirPunch", "AirBlade", "FireKick", "FireSpin", "FireWheel", "FlameRush");
  public static final List<String> layer3 = List.of("LavaDisk", "EarthSmash", "Combustion");

  private final Collection<AbilityDescription> abilities = new ArrayList<>(64);
  private final Collection<Sequence> sequences = new ArrayList<>();

  AbilityInitializer() {
    initAir();
    initWater();
    initEarth();
    initFire();

    int abilityAmount = Registries.ABILITIES.register(abilities);
    int sequenceAmount = Registries.SEQUENCES.register(sequences);
    int collisionAmount = Registries.COLLISIONS.register(buildCollisions());

    Bending.logger().info("Registered " + (abilityAmount + sequenceAmount) + " abilities!");
    Bending.logger().info("Registered " + collisionAmount + " collisions!");
  }

  private Collection<RegisteredCollision> buildCollisions() {
    Collection<String> shieldCollisions = new ArrayList<>();
    shieldCollisions.addAll(layer0);
    shieldCollisions.addAll(layer1);
    shieldCollisions.add("EarthShot");
    return RegisteredCollision.builder()
      .layer(layer0)
      .layer(layer1)
      .layer(layer2)
      .layer(layer3)
      .add(spouts, layer1, true, false)
      .add(spouts, List.of("LavaDisk", "EarthSmash", "FlameRush"), true, false)
      .add(shieldCollisions, List.of("AirShield", "WallOfFire"), true, false)
      .add("FireShield", blasts, false, true)
      .add("FireShield", "Combustion", true, true)
      .add("FrostBreath", blasts, false, true)
      .add("FireBreath", blasts, false, true)
      .add("FireBreath", "EarthSmash", false, false)
      .add("FrostBreath", "EarthSmash", false, false)
      .add("FrostBreath", "FireBreath", true, true)
      .add("FrostBreath", "AirShield", true, true)
      .add("IceCrawl", "EarthLine", true, false)
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
      .element(AIR).activation(SEQUENCE).buildSequence(new SequenceStep(airScooter, SNEAK),
        new SequenceStep(airScooter, SNEAK_RELEASE),
        new SequenceStep(airScooter, SNEAK),
        new SequenceStep(airScooter, SNEAK_RELEASE),
        new SequenceStep(airBlade, ATTACK)
      );
    sequences.add(airWheel);
  }

  private void initWater() {
    abilities.add(AbilityDescription.builder("FastSwim", FastSwim::new)
      .element(WATER).activation(PASSIVE).canBind(false).build());

    abilities.add(AbilityDescription.builder("HydroSink", HydroSink::new)
      .element(WATER).activation(PASSIVE).canBind(false).build());

    AbilityDescription waterManipulation = AbilityDescription.builder("WaterManipulation", WaterManipulation::new)
      .element(WATER).activation(SNEAK, ATTACK).sourcePlant(true).bypassCooldown(true).build();
    abilities.add(waterManipulation);

    abilities.add(AbilityDescription.builder("WaterSpout", WaterSpout::new)
      .element(WATER).activation(ATTACK).build());

    abilities.add(AbilityDescription.builder("HealingWaters", HealingWaters::new)
      .element(WATER).activation(SNEAK).build());

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
      .element(WATER).activation(SNEAK).bypassCooldown(true).build());

    abilities.add(AbilityDescription.builder("FrostBreath", FrostBreath::new)
      .element(WATER).activation(SNEAK).build());

    Sequence waterGimbal = AbilityDescription.builder("WaterGimbal", WaterGimbal::new)
      .element(WATER).activation(SEQUENCE).buildSequence(
        new SequenceStep(waterRing, SNEAK),
        new SequenceStep(waterRing, SNEAK_RELEASE),
        new SequenceStep(waterRing, SNEAK),
        new SequenceStep(waterRing, SNEAK_RELEASE),
        new SequenceStep(torrent, SNEAK)
      );
    sequences.add(waterGimbal);

    Sequence iceberg = AbilityDescription.builder("Iceberg", Iceberg::new)
      .element(WATER).activation(SEQUENCE).buildSequence(
        new SequenceStep(phaseChange, SNEAK),
        new SequenceStep(iceSpike, SNEAK_RELEASE),
        new SequenceStep(phaseChange, SNEAK),
        new SequenceStep(iceSpike, SNEAK_RELEASE),
        new SequenceStep(iceSpike, SNEAK)
      );
    sequences.add(iceberg);
  }

  private void initEarth() {
    abilities.add(AbilityDescription.builder("DensityShift", DensityShift::new)
      .element(EARTH).activation(PASSIVE).canBind(false).build());

    abilities.add(AbilityDescription.builder("EarthCling", EarthCling::new)
      .element(EARTH).activation(PASSIVE).canBind(false).build());

    abilities.add(AbilityDescription.builder("FerroControl", FerroControl::new)
      .element(EARTH).activation(PASSIVE).canBind(false).build());

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
      .element(EARTH).activation(SNEAK).build());

    abilities.add(AbilityDescription.builder("MetalCable", MetalCable::new)
      .element(EARTH).activation(ATTACK, SNEAK).bypassCooldown(true).build());

    abilities.add(AbilityDescription.builder("LavaDisk", LavaDisk::new)
      .element(EARTH).activation(SNEAK).build());

    Sequence bulwark = AbilityDescription.builder("Bulwark", Bulwark::new)
      .element(EARTH).activation(SEQUENCE).hidden(true).buildSequence(
        new SequenceStep(earthArmor, SNEAK),
        new SequenceStep(earthArmor, SNEAK_RELEASE)
      );
    sequences.add(bulwark);

    Sequence earthPillars = AbilityDescription.builder("EarthPillars", EarthPillars::new)
      .element(EARTH).activation(SEQUENCE, FALL).buildSequence(
        new SequenceStep(shockwave, SNEAK),
        new SequenceStep(catapult, SNEAK_RELEASE)
      );
    sequences.add(earthPillars);

    Sequence earthShards = AbilityDescription.builder("EarthShards", EarthShards::new)
      .element(EARTH).activation(SEQUENCE).buildSequence(
        new SequenceStep(earthGlove, SNEAK),
        new SequenceStep(collapse, SNEAK_RELEASE),
        new SequenceStep(collapse, SNEAK),
        new SequenceStep(earthGlove, SNEAK_RELEASE)
      );
    sequences.add(earthShards);
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

    abilities.add(AbilityDescription.builder("Lightning", Lightning::new)
      .element(FIRE).activation(SNEAK).build());

    abilities.add(AbilityDescription.builder("Combustion", Combustion::new)
      .element(FIRE).activation(ATTACK, SNEAK).bypassCooldown(true).build());

    Sequence fireKick = AbilityDescription.builder("FireKick", FireKick::new)
      .element(FIRE).activation(SEQUENCE).buildSequence(
        new SequenceStep(fireBlast, ATTACK),
        new SequenceStep(fireBlast, ATTACK),
        new SequenceStep(fireBlast, SNEAK),
        new SequenceStep(fireBlast, ATTACK)
      );
    sequences.add(fireKick);

    Sequence fireSpin = AbilityDescription.builder("FireSpin", FireSpin::new)
      .element(FIRE).activation(SEQUENCE).buildSequence(
        new SequenceStep(fireBlast, ATTACK),
        new SequenceStep(fireBlast, ATTACK),
        new SequenceStep(fireShield, ATTACK),
        new SequenceStep(fireShield, SNEAK),
        new SequenceStep(fireShield, SNEAK_RELEASE)
      );
    sequences.add(fireSpin);

    Sequence fireWheel = AbilityDescription.builder("FireWheel", FireWheel::new)
      .element(FIRE).activation(SEQUENCE).buildSequence(
        new SequenceStep(fireShield, SNEAK),
        new SequenceStep(fireShield, INTERACT_BLOCK),
        new SequenceStep(fireShield, INTERACT_BLOCK),
        new SequenceStep(fireShield, SNEAK_RELEASE)
      );
    sequences.add(fireWheel);
  }
}
