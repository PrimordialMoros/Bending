/*
 *   Copyright 2020 Moros <https://github.com/PrimordialMoros>
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

import me.moros.bending.Bending;
import me.moros.bending.ability.air.*;
import me.moros.bending.ability.air.passives.*;
import me.moros.bending.ability.fire.*;
import me.moros.bending.ability.fire.sequences.*;
import me.moros.bending.ability.water.*;
import me.moros.bending.game.manager.SequenceManager;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.sequence.AbilityAction;
import me.moros.bending.model.ability.sequence.Sequence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Used to initialize all ability descriptions, sequences and collisions
 */
// TODO split into sub categories
public final class AbilityInitializer {
	public static void loadAbilities() {
		AbilityRegistry abilityRegistry = Game.getAbilityRegistry();
		SequenceManager sequenceManager = Game.getSequenceManager();

		List<AbilityDescription> air = new ArrayList<>();
		List<AbilityDescription> water = new ArrayList<>();
		List<AbilityDescription> earth = new ArrayList<>();
		List<AbilityDescription> fire = new ArrayList<>();

		air.add(AbilityDescription.builder("AirSwipe", AirSwipe.class)
			.setElement(Element.AIR).setActivation(ActivationMethod.PUNCH, ActivationMethod.SNEAK).build());

		air.add(AbilityDescription.builder("AirAgility", AirAgility.class)
			.setElement(Element.AIR).setActivation(ActivationMethod.PASSIVE)
			.setHidden(true).setHarmless(true).build());

		air.add(AbilityDescription.builder("GracefulDescent", GracefulDescent.class)
			.setElement(Element.AIR).setActivation(ActivationMethod.PASSIVE)
			.setHidden(true).setHarmless(true).build());

		air.add(AbilityDescription.builder("AirBlast", AirBlast.class)
			.setElement(Element.AIR).setActivation(ActivationMethod.PUNCH, ActivationMethod.SNEAK_RELEASE).build());

		air.add(AbilityDescription.builder("AirBurst", AirBurst.class)
			.setElement(Element.AIR).setActivation(ActivationMethod.SNEAK, ActivationMethod.FALL).build());

		air.add(AbilityDescription.builder("AirShield", AirShield.class)
			.setElement(Element.AIR).setActivation(ActivationMethod.SNEAK).build());

		air.add(AbilityDescription.builder("AirScooter", AirScooter.class)
			.setElement(Element.AIR).setActivation(ActivationMethod.PUNCH).setHarmless(true).build());

		air.add(AbilityDescription.builder("AirSpout", AirSpout.class)
			.setElement(Element.AIR).setActivation(ActivationMethod.PUNCH).setHarmless(true).build());

		air.add(AbilityDescription.builder("AirBlade", AirBlade.class)
			.setElement(Element.AIR).setActivation(ActivationMethod.PUNCH).build());

		air.add(AbilityDescription.builder("AirPunch", AirPunch.class)
			.setElement(Element.AIR).setActivation(ActivationMethod.PUNCH).build());

		water.add(AbilityDescription.builder("WaterSpout", WaterSpout.class)
			.setElement(Element.WATER).setActivation(ActivationMethod.PUNCH).setHarmless(true).build());

		AbilityDescription fireBlast = AbilityDescription.builder("FireBlast", FireBlast.class)
			.setElement(Element.FIRE).setActivation(ActivationMethod.PUNCH, ActivationMethod.SNEAK).build();
		fire.add(fireBlast);

		fire.add(AbilityDescription.builder("FireBurst", FireBurst.class)
			.setElement(Element.FIRE).setActivation(ActivationMethod.SNEAK).build());

		AbilityDescription heatControl = AbilityDescription.builder("HeatControl", HeatControl.class)
			.setElement(Element.FIRE).setActivation(ActivationMethod.PUNCH, ActivationMethod.SNEAK).build();
		fire.add(heatControl);

		fire.add(AbilityDescription.builder("Blaze", Blaze.class)
			.setElement(Element.FIRE).setActivation(ActivationMethod.PUNCH, ActivationMethod.SNEAK).build());

		AbilityDescription fireShield = AbilityDescription.builder("FireShield", FireShield.class)
			.setElement(Element.FIRE).setActivation(ActivationMethod.PUNCH, ActivationMethod.SNEAK).build();
		fire.add(fireShield);

		AbilityDescription fireJet = AbilityDescription.builder("FireJet", FireJet.class)
			.setElement(Element.FIRE).setActivation(ActivationMethod.PUNCH).setHarmless(true).build();
		fire.add(fireJet);

		AbilityDescription fireWall = AbilityDescription.builder("FireWall", FireWall.class)
			.setElement(Element.FIRE).setActivation(ActivationMethod.PUNCH).build();
		fire.add(fireWall);

		AbilityDescription fireWave = AbilityDescription.builder("FireWave", FireWave.class)
			.setElement(Element.FIRE).setActivation(ActivationMethod.SEQUENCE).build();
		fire.add(fireWave);

		AbilityDescription jetBlast = AbilityDescription.builder("JetBlast", JetBlast.class)
			.setElement(Element.FIRE).setActivation(ActivationMethod.SEQUENCE).setHarmless(true).build();
		fire.add(jetBlast);

		AbilityDescription fireKick = AbilityDescription.builder("FireKick", FireKick.class)
			.setElement(Element.FIRE).setActivation(ActivationMethod.SEQUENCE).build();
		fire.add(fireKick);

		AbilityDescription fireSpin = AbilityDescription.builder("FireSpin", FireSpin.class)
			.setElement(Element.FIRE).setActivation(ActivationMethod.SEQUENCE).build();
		fire.add(fireSpin);

		AbilityDescription fireWheel = AbilityDescription.builder("FireWheel", FireWheel.class)
			.setElement(Element.FIRE).setActivation(ActivationMethod.SEQUENCE).build();
		fire.add(fireWheel);

		fire.add(AbilityDescription.builder("Bolt", Bolt.class)
			.setElement(Element.FIRE).setActivation(ActivationMethod.SNEAK).build());

		fire.add(AbilityDescription.builder("Combustion", Combustion.class)
			.setElement(Element.FIRE).setActivation(ActivationMethod.SNEAK).build());

		abilityRegistry.registerAbilities(air);
		abilityRegistry.registerAbilities(water);
		abilityRegistry.registerAbilities(earth);
		abilityRegistry.registerAbilities(fire);

		Map<AbilityDescription, Sequence> sequences = new HashMap<>();
		sequences.put(fireWave, new Sequence(
			new AbilityAction(heatControl, ActivationMethod.SNEAK),
			new AbilityAction(fireWall, ActivationMethod.PUNCH),
			new AbilityAction(fireWall, ActivationMethod.SNEAK_RELEASE)
		));

		sequences.put(jetBlast, new Sequence(
			new AbilityAction(fireJet, ActivationMethod.SNEAK),
			new AbilityAction(fireJet, ActivationMethod.SNEAK_RELEASE),
			new AbilityAction(fireJet, ActivationMethod.SNEAK),
			new AbilityAction(fireJet, ActivationMethod.SNEAK_RELEASE),
			new AbilityAction(fireShield, ActivationMethod.SNEAK),
			new AbilityAction(fireShield, ActivationMethod.SNEAK_RELEASE),
			new AbilityAction(fireJet, ActivationMethod.PUNCH)
		));

		sequences.put(fireKick, new Sequence(
			new AbilityAction(fireBlast, ActivationMethod.PUNCH),
			new AbilityAction(fireBlast, ActivationMethod.PUNCH),
			new AbilityAction(fireBlast, ActivationMethod.SNEAK),
			new AbilityAction(fireBlast, ActivationMethod.PUNCH)
		));

		sequences.put(fireSpin, new Sequence(
			new AbilityAction(fireBlast, ActivationMethod.PUNCH),
			new AbilityAction(fireBlast, ActivationMethod.PUNCH),
			new AbilityAction(fireShield, ActivationMethod.PUNCH),
			new AbilityAction(fireShield, ActivationMethod.SNEAK),
			new AbilityAction(fireShield, ActivationMethod.SNEAK_RELEASE)
		));

		sequences.put(fireWheel, new Sequence(
			new AbilityAction(fireShield, ActivationMethod.SNEAK),
			new AbilityAction(fireShield, ActivationMethod.INTERACT_BLOCK),
			new AbilityAction(fireShield, ActivationMethod.INTERACT_BLOCK),
			new AbilityAction(fireShield, ActivationMethod.SNEAK_RELEASE)
		));

		int amount = sequenceManager.registerSequences(sequences);
		Bending.getLog().info("Registered " + amount + " sequences!");

		//registerAbility("EarthBlast", EarthBlast.class, Element.EARTH, ActivationMethod.Punch, ActivationMethod.Sneak).setCanBypassCooldown(true);
		//registerAbility("WaterManipulation", WaterManipulation.class, Element.WATER, ActivationMethod.Punch, ActivationMethod.Sneak).setCanBypassCooldown(true);
		//registerAbility("DensityShift", DensityShift.class, Element.EARTH, ActivationMethod.Passive).setHarmless(true).setHidden(true);

		/*
        CollisionService collisionService = Game.getCollisionService();

	    registerAbility("Lightning", Lightning.class, Element.FIRE, ActivationMethod.Sneak);


        registerAbility("Suffocate", Suffocate.class, Element.AIR, ActivationMethod.Sneak);
        AbilityDescription airSweep = registerAbility("AirSweep", AirSweep.class, Element.AIR, ActivationMethod.Sequence);
        AbilityDescription twister = registerAbility("Twister", Twister.class, Element.AIR, ActivationMethod.Sequence);
        AbilityDescription airStream = registerAbility("AirStream", AirStream.class, Element.AIR, ActivationMethod.Sequence);

        registerAbility("Shockwave", Shockwave.class, Element.EARTH, ActivationMethod.Punch, ActivationMethod.Sneak, ActivationMethod.Fall);

        registerAbility("Catapult", Catapult.class, Element.EARTH, ActivationMethod.Punch).setHarmless(true);
        registerAbility("Collapse", Collapse.class, Element.EARTH, ActivationMethod.Punch, ActivationMethod.Sneak);
        registerAbility("RaiseEarth", RaiseEarth.class, Element.EARTH, ActivationMethod.Punch, ActivationMethod.Sneak);
        registerAbility("EarthSmash", EarthSmash.class, Element.EARTH, ActivationMethod.Punch, ActivationMethod.Sneak, ActivationMethod.Use);
        registerAbility("EarthArmor", EarthArmor.class, Element.EARTH, ActivationMethod.Sneak);
        registerAbility("EarthTunnel", EarthTunnel.class, Element.EARTH, ActivationMethod.Sneak);
        registerAbility("EarthGrab", EarthGrab.class, Element.EARTH, ActivationMethod.Punch);

        GenericAbilityDescription surgeDesc = registerAbility("Surge", Surge.class, Element.WATER, ActivationMethod.Punch, ActivationMethod.Sneak);
        surgeDesc.setCanBypassCooldown(true);
        surgeDesc.setSourcesPlants(true);
        registerAbility("SurgeWall", SurgeWall.class, Element.WATER, ActivationMethod.Punch).setHidden(true);
        registerAbility("SurgeWave", SurgeWave.class, Element.WATER, ActivationMethod.Punch).setHidden(true);
        registerAbility("BottleReturn", BottleReturn.class, Element.WATER, ActivationMethod.Punch).setHidden(true);
        GenericAbilityDescription torrentDesc = registerAbility("Torrent", Torrent.class, Element.WATER, ActivationMethod.Punch, ActivationMethod.Sneak);
        torrentDesc.setCanBypassCooldown(true);
        torrentDesc.setSourcesPlants(true);
        registerAbility("TorrentWave", TorrentWave.class, Element.WATER, ActivationMethod.Punch).setHidden(true);

        registerAbility("WaterArms", WaterArms.class, Element.WATER, ActivationMethod.Sneak);
        registerAbility("WaterBubble", WaterBubble.class, Element.WATER, ActivationMethod.Punch, ActivationMethod.Sneak);

        registerAbility("WaterSpout", WaterSpout.class, Element.WATER, ActivationMethod.Punch)
                .setSourcesPlants(true);
        registerAbility("PhaseChange", PhaseChange.class, Element.WATER, ActivationMethod.Punch, ActivationMethod.Sneak);
        registerAbility("OctopusForm", OctopusForm.class, Element.WATER, ActivationMethod.Punch, ActivationMethod.Sneak)
                .setSourcesPlants(true);
        registerAbility("IceBlast", IceBlast.class, Element.WATER, ActivationMethod.Punch, ActivationMethod.Sneak);

        ConfigurableAbilityDescription<WaterSpoutWave> waterSpoutWave = new ConfigurableAbilityDescription<>("WaterSpoutWave", Element.WATER, 0, WaterSpoutWave.class, ActivationMethod.Punch);

        waterSpoutWave.setConfigNode("abilities", "water", "waterspout", "wave");
        waterSpoutWave.setHidden(true);
        abilityRegistry.registerAbility(waterSpoutWave);

        MultiAbilityDescription pullDesc = new MultiAbilityDescription<>("WaterArmsPull", Element.WATER, 0, WaterArmsPull.class, ActivationMethod.Punch);
        pullDesc.setConfigNode("waterarms", "pull");
        pullDesc.setHidden(true);
        pullDesc.setDisplayName("Pull");
        abilityRegistry.registerAbility(pullDesc);

        MultiAbilityDescription punchDesc = new MultiAbilityDescription<>("WaterArmsPunch", Element.WATER, 0, WaterArmsPunch.class, ActivationMethod.Punch);
        punchDesc.setConfigNode("waterarms", "punch");
        punchDesc.setHidden(true);
        punchDesc.setDisplayName("Punch");
        abilityRegistry.registerAbility(punchDesc);

        MultiAbilityDescription grappleDesc = new MultiAbilityDescription<>("WaterArmsGrapple", Element.WATER, 0, WaterArmsGrapple.class, ActivationMethod.Punch);
        grappleDesc.setConfigNode("waterarms", "grapple");
        grappleDesc.setHidden(true);
        grappleDesc.setDisplayName("Grapple");
        abilityRegistry.registerAbility(grappleDesc);

        MultiAbilityDescription grabDesc = new MultiAbilityDescription<>("WaterArmsGrab", Element.WATER, 0, WaterArmsGrab.class, ActivationMethod.Punch);
        grabDesc.setConfigNode("waterarms", "grab");
        grabDesc.setHidden(true);
        grabDesc.setDisplayName("Grab");
        abilityRegistry.registerAbility(grabDesc);

        MultiAbilityDescription freezeDesc = new MultiAbilityDescription<>("WaterArmsFreeze", Element.WATER, 0, WaterArmsFreeze.class, ActivationMethod.Punch);
        freezeDesc.setConfigNode("waterarms", "freeze");
        freezeDesc.setHidden(true);
        freezeDesc.setDisplayName("Freeze");
        abilityRegistry.registerAbility(freezeDesc);

        MultiAbilityDescription spearDesc = new MultiAbilityDescription<>("WaterArmsSpear", Element.WATER, 0, WaterArmsSpear.class, ActivationMethod.Punch);
        spearDesc.setConfigNode("waterarms", "spear");
        spearDesc.setHidden(true);
        spearDesc.setDisplayName("Spear");
        abilityRegistry.registerAbility(spearDesc);

        sequenceService.registerSequence(airSweep, new Sequence(true,
                new AbilityAction(airSwipe, Action.Punch),
                new AbilityAction(airSwipe, Action.Punch),
                new AbilityAction(airBurst, Action.Sneak),
                new AbilityAction(airBurst, Action.Punch)
        ));

        sequenceService.registerSequence(twister, new Sequence(true,
                new AbilityAction(airShield, Action.Sneak),
                new AbilityAction(airShield, Action.SneakRelease),
                new AbilityAction(tornado, Action.Sneak),
                new AbilityAction(airBlast, Action.Punch)
        ));

        sequenceService.registerSequence(airStream, new Sequence(true,
                new AbilityAction(airShield, Action.Sneak),
                new AbilityAction(airSuction, Action.Punch),
                new AbilityAction(airBlast, Action.Punch)
        ));

        collisionService.registerCollision(airBlast, fireBlast, true, true);
        collisionService.registerCollision(airSwipe, fireBlast, false, true);

        collisionService.registerCollision(airShield, airBlast, false, true);
        collisionService.registerCollision(airShield, airSuction, false, true);
        collisionService.registerCollision(airShield, airStream, false, true);
        collisionService.registerCollision(airShield, fireBlast, false, true);
        collisionService.registerCollision(airShield, fireKick, false, true);
        collisionService.registerCollision(airShield, fireSpin, false, true);
        collisionService.registerCollision(airShield, fireWheel, false, true);

        collisionService.registerCollision(fireBlast, fireBlast, true, true);

        collisionService.registerCollision(fireShield, airBlast, false, true);
        collisionService.registerCollision(fireShield, airSuction, false, true);
        collisionService.registerCollision(fireShield, fireBlast, false, true);
        collisionService.registerCollision(fireShield, fireBlastCharged, false, true);
		 */
	}
}
