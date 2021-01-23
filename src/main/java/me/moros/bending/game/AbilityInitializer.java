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

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.Bending;
import me.moros.bending.ability.air.*;
import me.moros.bending.ability.air.passives.*;
import me.moros.bending.ability.air.sequences.*;
import me.moros.bending.ability.earth.*;
import me.moros.bending.ability.earth.passives.*;
import me.moros.bending.ability.earth.sequences.*;
import me.moros.bending.ability.fire.*;
import me.moros.bending.ability.fire.sequences.*;
import me.moros.bending.ability.water.*;
import me.moros.bending.ability.water.passives.*;
import me.moros.bending.ability.water.sequences.*;
import me.moros.bending.game.manager.CollisionManager;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.sequence.AbilityAction;
import me.moros.bending.model.ability.sequence.Sequence;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.collision.CollisionBuilder;
import me.moros.bending.model.collision.RegisteredCollision;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Used to initialize all ability descriptions, sequences and collisions
 */
public final class AbilityInitializer {
	public static final List<String> spoutLayer = Arrays.asList("AirSpout", "WaterSpout");
	public static final List<String> shieldLayer = Arrays.asList("AirShield", "FireShield", "WallOfFire");
	public static final List<String> layer0 = Arrays.asList("EarthGlove", "MetalCable");
	public static final List<String> layer1 = Arrays.asList("AirSwipe", "EarthBlast", "FireBlast", "WaterManipulation");
	public static final List<String> layer2 = Arrays.asList("AirWheel", "AirPunch", "AirBlade", "FireKick", "FireSpin", "FireWheel");
	public static final List<String> layer3 = Arrays.asList("LavaDisk", "Combustion");

	private final Collection<AbilityDescription> abilities = new ArrayList<>(64);
	private final Map<AbilityDescription, Sequence> sequences = new HashMap<>();
	private final AbilityRegistry registry;

	protected AbilityInitializer(@NonNull Game game) {
		registry = game.getAbilityRegistry();
		initAir();
		initWater();
		initEarth();
		initFire();

		int abilityAmount = registry.registerAbilities(abilities);
		int sequenceAmount = game.getSequenceManager().registerSequences(sequences);
		int collisionAmount = CollisionManager.registerCollisions(buildCollisions());

		Bending.getLog().info("Registered " + abilityAmount + " abilities!");
		Bending.getLog().info("Registered " + sequenceAmount + " sequences!");
		Bending.getLog().info("Registered " + collisionAmount + " collisions!");
	}

	private Collection<RegisteredCollision> buildCollisions() {
		return new CollisionBuilder(registry)
			.addLayer(layer0)
			.addSpecialLayer(spoutLayer)
			.addLayer(layer1)
			.addLayer(layer2)
			.addSpecialLayer(shieldLayer)
			.addLayer(layer3)
			.addSimpleCollision("FrostBreath", "AirShield", true, true)
			.addSimpleCollision("IceCrawl", "EarthLine", true, false)
			.build();
	}

	private void initAir() {
		abilities.add(AbilityDescription.builder("AirAgility", AirAgility::new)
			.setElement(Element.AIR).setActivation(ActivationMethod.PASSIVE)
			.setCanBind(false).setHarmless(true).build());

		abilities.add(AbilityDescription.builder("GracefulDescent", GracefulDescent::new)
			.setElement(Element.AIR).setActivation(ActivationMethod.PASSIVE)
			.setCanBind(false).setHarmless(true).build());

		abilities.add(AbilityDescription.builder("AirSwipe", AirSwipe::new)
			.setElement(Element.AIR).setActivation(ActivationMethod.ATTACK, ActivationMethod.SNEAK).build());

		abilities.add(AbilityDescription.builder("AirBlast", AirBlast::new)
			.setElement(Element.AIR).setActivation(ActivationMethod.ATTACK, ActivationMethod.SNEAK_RELEASE).build());

		abilities.add(AbilityDescription.builder("AirBurst", AirBurst::new)
			.setElement(Element.AIR).setActivation(ActivationMethod.SNEAK, ActivationMethod.FALL).build());

		abilities.add(AbilityDescription.builder("AirShield", AirShield::new)
			.setElement(Element.AIR).setActivation(ActivationMethod.SNEAK).build());

		abilities.add(AbilityDescription.builder("AirSpout", AirSpout::new)
			.setElement(Element.AIR).setActivation(ActivationMethod.ATTACK).setHarmless(true).build());

		abilities.add(AbilityDescription.builder("AirPunch", AirPunch::new)
			.setElement(Element.AIR).setActivation(ActivationMethod.ATTACK).build());

		AbilityDescription airBlade = AbilityDescription.builder("AirBlade", AirBlade::new)
			.setElement(Element.AIR).setActivation(ActivationMethod.SNEAK, ActivationMethod.SNEAK_RELEASE).build();
		abilities.add(airBlade);

		AbilityDescription airScooter = AbilityDescription.builder("AirScooter", AirScooter::new)
			.setElement(Element.AIR).setActivation(ActivationMethod.ATTACK).setHarmless(true).build();
		abilities.add(airScooter);

		AbilityDescription airWheel = AbilityDescription.builder("AirWheel", AirWheel::new)
			.setElement(Element.AIR).setActivation(ActivationMethod.SEQUENCE).build();
		abilities.add(airWheel);

		sequences.put(airWheel, new Sequence(
			new AbilityAction(airScooter, ActivationMethod.SNEAK),
			new AbilityAction(airScooter, ActivationMethod.SNEAK_RELEASE),
			new AbilityAction(airScooter, ActivationMethod.SNEAK),
			new AbilityAction(airScooter, ActivationMethod.SNEAK_RELEASE),
			new AbilityAction(airBlade, ActivationMethod.ATTACK)
		));
	}

	private void initWater() {
		abilities.add(AbilityDescription.builder("FastSwim", FastSwim::new)
			.setElement(Element.WATER).setActivation(ActivationMethod.PASSIVE).setCanBind(false).setHarmless(true).build());

		abilities.add(AbilityDescription.builder("HydroSink", HydroSink::new)
			.setElement(Element.WATER).setActivation(ActivationMethod.PASSIVE).setCanBind(false).setHarmless(true).build());

		AbilityDescription waterManipulation = AbilityDescription.builder("WaterManipulation", WaterManipulation::new)
			.setElement(Element.WATER).setActivation(ActivationMethod.SNEAK).setSourcesPlants(true).build();
		abilities.add(waterManipulation);

		abilities.add(AbilityDescription.builder("WaterSpout", WaterSpout::new)
			.setElement(Element.WATER).setActivation(ActivationMethod.ATTACK).setHarmless(true).build());

		abilities.add(AbilityDescription.builder("HealingWaters", HealingWaters::new)
			.setElement(Element.WATER).setActivation(ActivationMethod.SNEAK).setHarmless(true).build());

		abilities.add(AbilityDescription.builder("WaterBubble", WaterBubble::new)
			.setElement(Element.WATER).setActivation(ActivationMethod.SNEAK).build());

		abilities.add(AbilityDescription.builder("OctopusForm", OctopusForm::new)
			.setElement(Element.WATER).setActivation(ActivationMethod.ATTACK).setSourcesPlants(true).build());

		AbilityDescription waterRing = AbilityDescription.builder("WaterRing", WaterRing::new)
			.setElement(Element.WATER).setActivation(ActivationMethod.ATTACK).setSourcesPlants(true).build();
		abilities.add(waterRing);

		AbilityDescription waterWave = AbilityDescription.builder("WaterWave", WaterWave::new)
			.setElement(Element.WATER).setActivation(ActivationMethod.SNEAK).setCanBind(false).build();
		abilities.add(waterWave);

		AbilityDescription torrent = AbilityDescription.builder("Torrent", Torrent::new)
			.setElement(Element.WATER).setActivation(ActivationMethod.ATTACK).setSourcesPlants(true).build();
		abilities.add(torrent);

		AbilityDescription phaseChange = AbilityDescription.builder("PhaseChange", PhaseChange::new)
			.setElement(Element.WATER).setActivation(ActivationMethod.PASSIVE).build();
		abilities.add(phaseChange);

		AbilityDescription iceCrawl = AbilityDescription.builder("IceCrawl", IceCrawl::new)
			.setElement(Element.WATER).setActivation(ActivationMethod.SNEAK).build();
		abilities.add(iceCrawl);

		AbilityDescription iceSpike = AbilityDescription.builder("IceSpike", IceSpike::new)
			.setElement(Element.WATER).setActivation(ActivationMethod.ATTACK, ActivationMethod.SNEAK).build();
		abilities.add(iceSpike);

		abilities.add(AbilityDescription.builder("IceWall", IceWall::new)
			.setElement(Element.WATER).setActivation(ActivationMethod.SNEAK).build());

		AbilityDescription waterGimbal = AbilityDescription.builder("WaterGimbal", WaterGimbal::new)
			.setElement(Element.WATER).setActivation(ActivationMethod.SEQUENCE).build();
		abilities.add(waterGimbal);

		AbilityDescription frostBreath = AbilityDescription.builder("FrostBreath", FrostBreath::new)
			.setElement(Element.WATER).setActivation(ActivationMethod.SEQUENCE).build();
		abilities.add(frostBreath);

		AbilityDescription iceDrill = AbilityDescription.builder("Iceberg", Iceberg::new)
			.setElement(Element.WATER).setActivation(ActivationMethod.SEQUENCE).build();
		abilities.add(iceDrill);

		sequences.put(waterGimbal, new Sequence(
			new AbilityAction(waterRing, ActivationMethod.SNEAK),
			new AbilityAction(waterRing, ActivationMethod.SNEAK_RELEASE),
			new AbilityAction(waterRing, ActivationMethod.SNEAK),
			new AbilityAction(waterRing, ActivationMethod.SNEAK_RELEASE),
			new AbilityAction(torrent, ActivationMethod.SNEAK)
		));

		sequences.put(frostBreath, new Sequence(
			new AbilityAction(iceCrawl, ActivationMethod.SNEAK),
			new AbilityAction(iceCrawl, ActivationMethod.SNEAK_RELEASE),
			new AbilityAction(phaseChange, ActivationMethod.SNEAK),
			new AbilityAction(phaseChange, ActivationMethod.SNEAK_RELEASE),
			new AbilityAction(iceCrawl, ActivationMethod.SNEAK)
		));

		sequences.put(iceDrill, new Sequence(
			new AbilityAction(phaseChange, ActivationMethod.SNEAK),
			new AbilityAction(iceSpike, ActivationMethod.SNEAK_RELEASE),
			new AbilityAction(phaseChange, ActivationMethod.SNEAK),
			new AbilityAction(iceSpike, ActivationMethod.SNEAK_RELEASE),
			new AbilityAction(iceSpike, ActivationMethod.SNEAK)
		));
	}

	private void initEarth() {
		abilities.add(AbilityDescription.builder("DensityShift", DensityShift::new)
			.setElement(Element.EARTH).setActivation(ActivationMethod.PASSIVE).setCanBind(false).setHarmless(true).build());

		abilities.add(AbilityDescription.builder("EarthCling", EarthCling::new)
			.setElement(Element.EARTH).setActivation(ActivationMethod.PASSIVE).setCanBind(false).setHarmless(true).build());

		abilities.add(AbilityDescription.builder("FerroControl", FerroControl::new)
			.setElement(Element.EARTH).setActivation(ActivationMethod.PASSIVE).setCanBind(false).setHarmless(true).build());

		abilities.add(AbilityDescription.builder("EarthBlast", EarthBlast::new)
			.setElement(Element.EARTH).setActivation(ActivationMethod.SNEAK).build());

		abilities.add(AbilityDescription.builder("EarthSmash", EarthSmash::new)
			.setElement(Element.EARTH).setActivation(ActivationMethod.SNEAK).build());

		abilities.add(AbilityDescription.builder("EarthShot", EarthShot::new)
			.setElement(Element.EARTH).setActivation(ActivationMethod.SNEAK).build());

		abilities.add(AbilityDescription.builder("EarthLine", EarthLine::new)
			.setElement(Element.EARTH).setActivation(ActivationMethod.SNEAK).build());

		abilities.add(AbilityDescription.builder("EarthTunnel", EarthTunnel::new)
			.setElement(Element.EARTH).setActivation(ActivationMethod.SNEAK).build());

		AbilityDescription earthArmor = AbilityDescription.builder("EarthArmor", EarthArmor::new)
			.setElement(Element.EARTH).setActivation(ActivationMethod.ATTACK).build();
		abilities.add(earthArmor);

		AbilityDescription earthArmorWall = AbilityDescription.builder("EarthArmorWall", EarthArmorWall::new)
			.setElement(Element.EARTH).setActivation(ActivationMethod.SEQUENCE).setHidden(true).build();
		abilities.add(earthArmorWall);

		AbilityDescription earthGlove = AbilityDescription.builder("EarthGlove", EarthGlove::new)
			.setElement(Element.EARTH).setActivation(ActivationMethod.ATTACK).build();
		abilities.add(earthGlove);

		abilities.add(AbilityDescription.builder("RaiseEarth", RaiseEarth::new)
			.setElement(Element.EARTH).setActivation(ActivationMethod.ATTACK, ActivationMethod.SNEAK).build());

		AbilityDescription collapse = AbilityDescription.builder("Collapse", Collapse::new)
			.setElement(Element.EARTH).setActivation(ActivationMethod.ATTACK, ActivationMethod.SNEAK).build();
		abilities.add(collapse);

		AbilityDescription catapult = AbilityDescription.builder("Catapult", Catapult::new)
			.setElement(Element.EARTH).setActivation(ActivationMethod.ATTACK).setHarmless(true).build();
		abilities.add(catapult);

		AbilityDescription shockwave = AbilityDescription.builder("Shockwave", Shockwave::new)
			.setElement(Element.EARTH).setActivation(ActivationMethod.SNEAK, ActivationMethod.FALL).build();
		abilities.add(shockwave);

		AbilityDescription earthPillars = AbilityDescription.builder("EarthPillars", EarthPillars::new)
			.setElement(Element.EARTH).setActivation(ActivationMethod.SEQUENCE, ActivationMethod.FALL).build();
		abilities.add(earthPillars);

		AbilityDescription earthShards = AbilityDescription.builder("EarthShards", EarthShards::new)
			.setElement(Element.EARTH).setActivation(ActivationMethod.SEQUENCE).build();
		abilities.add(earthShards);

		abilities.add(AbilityDescription.builder("MetalCable", MetalCable::new)
			.setElement(Element.EARTH).setActivation(ActivationMethod.ATTACK).build());

		abilities.add(AbilityDescription.builder("LavaDisk", LavaDisk::new)
			.setElement(Element.EARTH).setActivation(ActivationMethod.SNEAK).build());

		sequences.put(earthArmorWall, new Sequence(
			new AbilityAction(earthArmor, ActivationMethod.SNEAK),
			new AbilityAction(earthArmor, ActivationMethod.SNEAK_RELEASE)
		));

		sequences.put(earthPillars, new Sequence(
			new AbilityAction(shockwave, ActivationMethod.SNEAK),
			new AbilityAction(catapult, ActivationMethod.SNEAK_RELEASE)
		));

		sequences.put(earthShards, new Sequence(
			new AbilityAction(earthGlove, ActivationMethod.SNEAK),
			new AbilityAction(collapse, ActivationMethod.SNEAK_RELEASE),
			new AbilityAction(collapse, ActivationMethod.SNEAK),
			new AbilityAction(earthGlove, ActivationMethod.SNEAK_RELEASE)
		));
	}

	private void initFire() {
		AbilityDescription fireBlast = AbilityDescription.builder("FireBlast", FireBlast::new)
			.setElement(Element.FIRE).setActivation(ActivationMethod.ATTACK, ActivationMethod.SNEAK).build();
		abilities.add(fireBlast);

		abilities.add(AbilityDescription.builder("FireBurst", FireBurst::new)
			.setElement(Element.FIRE).setActivation(ActivationMethod.SNEAK).build());

		AbilityDescription heatControl = AbilityDescription.builder("HeatControl", HeatControl::new)
			.setElement(Element.FIRE).setActivation(ActivationMethod.PASSIVE).build();
		abilities.add(heatControl);

		abilities.add(AbilityDescription.builder("Blaze", Blaze::new)
			.setElement(Element.FIRE).setActivation(ActivationMethod.ATTACK, ActivationMethod.SNEAK).build());

		AbilityDescription fireShield = AbilityDescription.builder("FireShield", FireShield::new)
			.setElement(Element.FIRE).setActivation(ActivationMethod.ATTACK, ActivationMethod.SNEAK).build();
		abilities.add(fireShield);

		AbilityDescription fireJet = AbilityDescription.builder("FireJet", FireJet::new)
			.setElement(Element.FIRE).setActivation(ActivationMethod.ATTACK).setHarmless(true).build();
		abilities.add(fireJet);

		AbilityDescription fireWall = AbilityDescription.builder("FireWall", FireWall::new)
			.setElement(Element.FIRE).setActivation(ActivationMethod.ATTACK).build();
		abilities.add(fireWall);

		AbilityDescription fireWave = AbilityDescription.builder("FireWave", FireWave::new)
			.setElement(Element.FIRE).setActivation(ActivationMethod.SEQUENCE).build();
		abilities.add(fireWave);

		AbilityDescription jetBlast = AbilityDescription.builder("JetBlast", JetBlast::new)
			.setElement(Element.FIRE).setActivation(ActivationMethod.SEQUENCE).setHarmless(true).build();
		abilities.add(jetBlast);

		AbilityDescription fireKick = AbilityDescription.builder("FireKick", FireKick::new)
			.setElement(Element.FIRE).setActivation(ActivationMethod.SEQUENCE).build();
		abilities.add(fireKick);

		AbilityDescription fireSpin = AbilityDescription.builder("FireSpin", FireSpin::new)
			.setElement(Element.FIRE).setActivation(ActivationMethod.SEQUENCE).build();
		abilities.add(fireSpin);

		AbilityDescription fireWheel = AbilityDescription.builder("FireWheel", FireWheel::new)
			.setElement(Element.FIRE).setActivation(ActivationMethod.SEQUENCE).build();
		abilities.add(fireWheel);

		abilities.add(AbilityDescription.builder("Bolt", Bolt::new)
			.setElement(Element.FIRE).setActivation(ActivationMethod.SNEAK).build());

		abilities.add(AbilityDescription.builder("Combustion", Combustion::new)
			.setElement(Element.FIRE).setActivation(ActivationMethod.SNEAK).build());

		sequences.put(fireWave, new Sequence(
			new AbilityAction(heatControl, ActivationMethod.SNEAK),
			new AbilityAction(heatControl, ActivationMethod.SNEAK_RELEASE),
			new AbilityAction(heatControl, ActivationMethod.SNEAK),
			new AbilityAction(heatControl, ActivationMethod.SNEAK_RELEASE),
			new AbilityAction(fireWall, ActivationMethod.ATTACK)
		));

		sequences.put(jetBlast, new Sequence(
			new AbilityAction(fireJet, ActivationMethod.SNEAK),
			new AbilityAction(fireJet, ActivationMethod.SNEAK_RELEASE),
			new AbilityAction(fireJet, ActivationMethod.SNEAK),
			new AbilityAction(fireJet, ActivationMethod.SNEAK_RELEASE),
			new AbilityAction(fireShield, ActivationMethod.SNEAK),
			new AbilityAction(fireShield, ActivationMethod.SNEAK_RELEASE),
			new AbilityAction(fireJet, ActivationMethod.ATTACK)
		));

		sequences.put(fireKick, new Sequence(
			new AbilityAction(fireBlast, ActivationMethod.ATTACK),
			new AbilityAction(fireBlast, ActivationMethod.ATTACK),
			new AbilityAction(fireBlast, ActivationMethod.SNEAK),
			new AbilityAction(fireBlast, ActivationMethod.ATTACK)
		));

		sequences.put(fireSpin, new Sequence(
			new AbilityAction(fireBlast, ActivationMethod.ATTACK),
			new AbilityAction(fireBlast, ActivationMethod.ATTACK),
			new AbilityAction(fireShield, ActivationMethod.ATTACK),
			new AbilityAction(fireShield, ActivationMethod.SNEAK),
			new AbilityAction(fireShield, ActivationMethod.SNEAK_RELEASE)
		));

		sequences.put(fireWheel, new Sequence(
			new AbilityAction(fireShield, ActivationMethod.SNEAK),
			new AbilityAction(fireShield, ActivationMethod.INTERACT_BLOCK),
			new AbilityAction(fireShield, ActivationMethod.INTERACT_BLOCK),
			new AbilityAction(fireShield, ActivationMethod.SNEAK_RELEASE)
		));
	}
}
