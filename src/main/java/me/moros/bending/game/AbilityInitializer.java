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
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.sequence.AbilityAction;
import me.moros.bending.model.ability.sequence.Sequence;
import me.moros.bending.model.collision.CollisionBuilder;
import me.moros.bending.model.collision.RegisteredCollision;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.moros.bending.model.Element.*;
import static me.moros.bending.model.ability.util.ActivationMethod.*;

/**
 * Used to initialize all ability descriptions, sequences and collisions
 */
public final class AbilityInitializer {
	public static final List<String> spoutLayer = Arrays.asList("AirSpout", "WaterSpout");
	public static final List<String> shieldLayer = Arrays.asList("AirShield", "FireShield", "WallOfFire");
	public static final List<String> layer0 = Arrays.asList("EarthGlove", "MetalCable");
	public static final List<String> layer1 = Arrays.asList("AirSwipe", "EarthBlast", "FireBlast", "WaterManipulation");
	public static final List<String> layer2 = Arrays.asList("AirWheel", "AirPunch", "AirBlade", "FireKick", "FireSpin", "FireWheel");
	public static final List<String> layer3 = Arrays.asList("LavaDisk", "Combustion", "EarthSmash");

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
		int addonAmount = registry.registerAbilities(AbilityRegistry.AddonRegistry.getAddonAbilities());
		int sequenceAmount = game.getSequenceManager().registerSequences(sequences);
		int addonSequences = game.getSequenceManager().registerSequences(AbilityRegistry.AddonRegistry.getAddonSequences());
		int collisionAmount = CollisionManager.registerCollisions(buildCollisions());

		Bending.getLog().info("Registered " + abilityAmount + " core abilities and " + addonAmount + " addon abilities!");
		Bending.getLog().info("Registered " + sequenceAmount + " core sequences and " + addonSequences + " addon sequences!");
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

		abilities.add(AbilityDescription.builder("Tornado", Tornado::new)
			.element(AIR).activation(SNEAK).build());

		AbilityDescription airBlade = AbilityDescription.builder("AirBlade", AirBlade::new)
			.element(AIR).activation(SNEAK, SNEAK_RELEASE).build();
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
			.element(WATER).activation(SNEAK, ATTACK).sourcesPlants(true).bypassCooldown(true).build();
		abilities.add(waterManipulation);

		abilities.add(AbilityDescription.builder("WaterSpout", WaterSpout::new)
			.element(WATER).activation(ATTACK).harmless(true).build());

		abilities.add(AbilityDescription.builder("HealingWaters", HealingWaters::new)
			.element(WATER).activation(SNEAK).harmless(true).build());

		abilities.add(AbilityDescription.builder("WaterBubble", WaterBubble::new)
			.element(WATER).activation(SNEAK).build());

		abilities.add(AbilityDescription.builder("OctopusForm", OctopusForm::new)
			.element(WATER).activation(ATTACK).sourcesPlants(true).build());

		AbilityDescription waterRing = AbilityDescription.builder("WaterRing", WaterRing::new)
			.element(WATER).activation(ATTACK).sourcesPlants(true).build();
		abilities.add(waterRing);

		AbilityDescription waterWave = AbilityDescription.builder("WaterWave", WaterWave::new)
			.element(WATER).activation(SNEAK).canBind(false).build();
		abilities.add(waterWave);

		AbilityDescription torrent = AbilityDescription.builder("Torrent", Torrent::new)
			.element(WATER).activation(ATTACK).sourcesPlants(true).bypassCooldown(true).build();
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

		AbilityDescription frostBreath = AbilityDescription.builder("FrostBreath", FrostBreath::new)
			.element(WATER).activation(SEQUENCE).build();
		abilities.add(frostBreath);

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

		sequences.put(frostBreath, new Sequence(
			new AbilityAction(iceCrawl, SNEAK),
			new AbilityAction(iceCrawl, SNEAK_RELEASE),
			new AbilityAction(phaseChange, SNEAK),
			new AbilityAction(phaseChange, SNEAK_RELEASE),
			new AbilityAction(iceCrawl, SNEAK)
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

		AbilityDescription fireShield = AbilityDescription.builder("FireShield", FireShield::new)
			.element(FIRE).activation(ATTACK, SNEAK).build();
		abilities.add(fireShield);

		AbilityDescription fireJet = AbilityDescription.builder("FireJet", FireJet::new)
			.element(FIRE).activation(ATTACK).harmless(true).build();
		abilities.add(fireJet);

		AbilityDescription fireWall = AbilityDescription.builder("FireWall", FireWall::new)
			.element(FIRE).activation(ATTACK).build();
		abilities.add(fireWall);

		AbilityDescription fireWave = AbilityDescription.builder("FireWave", FireWave::new)
			.element(FIRE).activation(SEQUENCE).build();
		abilities.add(fireWave);

		AbilityDescription jetBlast = AbilityDescription.builder("JetBlast", JetBlast::new)
			.element(FIRE).activation(SEQUENCE).harmless(true).build();
		abilities.add(jetBlast);

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

		sequences.put(jetBlast, new Sequence(
			new AbilityAction(fireJet, SNEAK),
			new AbilityAction(fireJet, SNEAK_RELEASE),
			new AbilityAction(fireJet, SNEAK),
			new AbilityAction(fireJet, SNEAK_RELEASE),
			new AbilityAction(fireShield, SNEAK),
			new AbilityAction(fireShield, SNEAK_RELEASE),
			new AbilityAction(fireJet, ATTACK)
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
