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

package me.moros.bending.ability.earth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.FragileStructure;
import me.moros.bending.ability.fire.FlameRush;
import me.moros.bending.ability.water.FrostBreath;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.BendingFallingBlock;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.math.IntVector;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.EntityMethods;
import me.moros.bending.util.methods.VectorMethods;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.util.NumberConversions;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;

public class EarthSmash extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;
  private RemovalPolicy swappedSlotsPolicy;

  private EarthSmashState state;
  private Boulder boulder;

  public EarthSmash(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    Optional<EarthSmash> grabbed = Bending.game().abilityManager(user.world()).userInstances(user, EarthSmash.class)
      .filter(s -> s.state instanceof GrabState).findAny();

    if (method == Activation.SNEAK) {
      if (grabbed.isPresent() || tryGrab(user)) {
        return false;
      }
    } else if (method == Activation.ATTACK) {
      grabbed.ifPresent(EarthSmash::launchBoulder);
      return false;
    }

    if (user.onCooldown(description())) {
      return false;
    }

    this.user = user;
    loadConfig();

    state = new ChargeState();
    removalPolicy = Policies.builder().build();
    swappedSlotsPolicy = SwappedSlotsRemovalPolicy.of(description());

    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = Bending.configManager().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    if (!state.canSlotSwitch() && swappedSlotsPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    if (boulder != null && boulder.data.isEmpty()) {
      return UpdateResult.REMOVE;
    }
    return state.update();
  }

  private boolean createBoulder() {
    Block center = user.find(userConfig.selectRange, b -> EarthMaterials.isEarthNotLava(user, b));
    if (center == null) {
      return false;
    }

    // Check blocks above center
    for (int i = 0; i <= userConfig.radius; i++) {
      Block b = center.getRelative(BlockFace.UP, i + 1);
      if (!MaterialUtil.isTransparent(b) || !TempBlock.isBendable(b) || !user.canBuild(b)) {
        return false;
      }
    }

    boulder = new Boulder(user, center, userConfig.radius, userConfig.maxDuration);

    int minRequired = NumberConversions.ceil(Math.pow(userConfig.radius, 3) * 0.375);
    if (boulder.data.size() < minRequired) {
      boulder = null;
      return false;
    }

    state = new LiftState();
    user.addCooldown(description(), userConfig.cooldown);
    return true;
  }

  private void launchBoulder() {
    state = new ShotState();
  }

  private void grabBoulder() {
    state = new GrabState();
  }

  private static boolean tryGrab(@NonNull User user) {
    Block target = WorldMethods.rayTraceBlocks(user.world(), user.ray(), config.grabRange);
    EarthSmash earthSmash = getInstance(user, target, s -> s.state.canGrab());
    if (earthSmash == null) {
      return false;
    }
    Bending.game().abilityManager(user.world()).changeOwner(earthSmash, user);
    earthSmash.grabBoulder();
    return true;
  }

  public static void tryDestroy(@NonNull User user, @NonNull Block block) {
    if (user.sneaking() && user.selectedAbilityName().equals("EarthSmash")) {
      EarthSmash earthSmash = getInstance(user, block, x -> true);
      if (earthSmash != null && earthSmash.boulder != null) {
        earthSmash.boulder.shatter();
      }
    }
  }

  private static EarthSmash getInstance(User user, Block block, Predicate<EarthSmash> filter) {
    if (block == null) {
      return null;
    }
    AABB blockBounds = AABB.BLOCK_BOUNDS.at(new Vector3(block));
    return Bending.game().abilityManager(user.world()).instances(EarthSmash.class)
      .filter(filter)
      .filter(s -> s.boulder.preciseBounds.at(s.boulder.center).intersects(blockBounds))
      .findAny().orElse(null);
  }

  private void cleanAll() {
    for (Map.Entry<Block, BlockData> entry : boulder.data().entrySet()) {
      Block block = entry.getKey();
      if (block.getType() != entry.getValue().getMaterial()) {
        continue;
      }
      TempBlock.createAir(block);
    }
  }

  private void render() {
    for (Map.Entry<Block, BlockData> entry : boulder.data().entrySet()) {
      Block block = entry.getKey();
      if (!MaterialUtil.isTransparent(block)) {
        continue;
      }
      BlockMethods.tryBreakPlant(block);
      TempBlock.create(block, entry.getValue());
    }
  }

  @Override
  public void onDestroy() {
    if (boulder != null) {
      cleanAll();
    }
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @Override
  public void onUserChange(@NonNull User newUser) {
    this.user = newUser;
    boulder.user = newUser;
  }

  @Override
  public @NonNull Collection<@NonNull Collider> colliders() {
    return !state.canCollide() ? List.of() : List.of(boulder.collider());
  }

  @Override
  public void onCollision(@NonNull Collision collision) {
    Ability collidedAbility = collision.collidedAbility();
    if (collidedAbility instanceof FlameRush && ((FlameRush) collidedAbility).isFullyCharged()) {
      collision.removeSelf(true);
    }
    if (collision.removeSelf()) {
      if (collidedAbility instanceof FrostBreath) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        boulder.data.replaceAll((k, v) -> rand.nextBoolean() ? Material.ICE.createBlockData() : Material.PACKED_ICE.createBlockData());
        boulder.shatter();
      } else if (collidedAbility.description().element() == Element.FIRE || collidedAbility instanceof LavaDisk) {
        boulder.data.replaceAll((k, v) -> Material.MAGMA_BLOCK.createBlockData());
        boulder.shatter();
      }
    }
  }

  interface EarthSmashState extends Updatable {
    default boolean canGrab() {
      return false;
    }

    default boolean canCollide() {
      return true;
    }

    default boolean canSlotSwitch() {
      return false;
    }
  }

  private class ChargeState implements EarthSmashState {
    private final long startTime;

    private ChargeState() {
      startTime = System.currentTimeMillis();
    }

    @Override
    public @NonNull UpdateResult update() {
      if (System.currentTimeMillis() >= startTime + userConfig.chargeTime) {
        if (user.sneaking()) {
          ParticleUtil.create(Particle.SMOKE_NORMAL, user.mainHandSide().toLocation(user.world())).spawn();
          return UpdateResult.CONTINUE;
        } else {
          return createBoulder() ? UpdateResult.CONTINUE : UpdateResult.REMOVE;
        }
      } else if (user.sneaking()) {
        return UpdateResult.CONTINUE;
      }
      return UpdateResult.REMOVE;
    }

    @Override
    public boolean canCollide() {
      return false;
    }
  }

  private class LiftState implements EarthSmashState {
    private final Vector3 origin;
    private int tick = 0;

    private LiftState() {
      this.origin = new Vector3(boulder.center.toArray());
    }

    @Override
    public @NonNull UpdateResult update() {
      cleanAll();
      boulder.center(boulder.center.add(Vector3.PLUS_J).toBlock(boulder.world));
      SoundUtil.EARTH.play(boulder.center.toLocation(boulder.world));
      CollisionUtil.handleEntityCollisions(user, boulder.collider(), entity -> {
        entity.setVelocity(new Vector3(entity.getVelocity()).setY(userConfig.raiseEntityPush).clampVelocity());
        return true;
      }, true, true);
      render();
      clearSourceArea();
      return UpdateResult.CONTINUE;
    }

    private void clearSourceArea() {
      tick++;
      int half = (boulder.size - 1) / 2;
      if (tick >= boulder.size) {
        state = new IdleState();
      } else if (tick == half) {
        for (int z = -half; z <= half; z++) {
          for (int x = -half; x <= half; x++) {
            // Remove bottom layer
            if ((Math.abs(x) + Math.abs(z)) % 2 != 0) {
              Block block = origin.add(new Vector3(x, -1, z)).toBlock(boulder.world);
              if (EarthMaterials.isEarthNotLava(user, block)) {
                TempBlock.createAir(block, BendingProperties.EARTHBENDING_REVERT_TIME);
              }
            }
            // Remove top layer
            Block block = origin.add(new Vector3(x, 0, z)).toBlock(boulder.world);
            if (EarthMaterials.isEarthNotLava(user, block)) {
              TempBlock.createAir(block, BendingProperties.EARTHBENDING_REVERT_TIME);
            }
          }
        }
      }
    }

    @Override
    public boolean canSlotSwitch() {
      return true;
    }
  }

  private class GrabState implements EarthSmashState {
    private final double grabbedDistance;

    private GrabState() {
      this.grabbedDistance = Math.min(boulder.center.distance(user.eyeLocation()), userConfig.grabRange);
    }

    @Override
    public @NonNull UpdateResult update() {
      if (user.sneaking()) {
        Vector3 dir = user.direction().normalize().multiply(grabbedDistance);
        Block newCenter = user.eyeLocation().add(dir).toBlock(boulder.world);
        if (newCenter.equals(boulder.center.toBlock(boulder.world))) {
          return UpdateResult.CONTINUE;
        }
        boulder.updateData();
        cleanAll();
        if (boulder.isValidCenter(newCenter)) {
          boulder.center(newCenter);
        }
        render();
      } else {
        state = new IdleState();
      }
      return UpdateResult.CONTINUE;
    }
  }

  private class ShotState implements EarthSmashState {
    private final Set<Entity> affectedEntities;
    private final Vector3 origin;
    private final Vector3 direction;

    private ShotState() {
      affectedEntities = new HashSet<>();
      origin = new Vector3(boulder.center.toArray());
      direction = user.direction();
      SoundUtil.EARTH.play(boulder.center.toLocation(boulder.world));
    }

    @Override
    public @NonNull UpdateResult update() {
      CollisionUtil.handleEntityCollisions(user, boulder.collider(), this::onEntityHit);
      cleanAll();
      Block newCenter = boulder.center.add(direction).toBlock(boulder.world);
      if (!boulder.isValidBlock(newCenter)) {
        return UpdateResult.REMOVE;
      }
      boulder.center(newCenter);
      if (origin.distanceSq(boulder.center) > userConfig.shootRange * userConfig.shootRange) {
        return UpdateResult.REMOVE;
      }
      if (!boulder.blendSmash()) {
        return UpdateResult.REMOVE;
      }
      render();
      return UpdateResult.CONTINUE;
    }

    private boolean onEntityHit(Entity entity) {
      if (affectedEntities.contains(entity)) {
        return false;
      }
      affectedEntities.add(entity);
      DamageUtil.damageEntity(entity, user, userConfig.damage, description());
      Vector3 velocity = EntityMethods.entityCenter(entity).subtract(boulder.center).setY(userConfig.knockup).normalize();
      entity.setVelocity(velocity.multiply(userConfig.knockback).clampVelocity());
      return false;
    }

    @Override
    public boolean canGrab() {
      return true;
    }

    @Override
    public boolean canSlotSwitch() {
      return true;
    }
  }

  private class IdleState implements EarthSmashState {
    @Override
    public @NonNull UpdateResult update() {
      return System.currentTimeMillis() > boulder.expireTime ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
    }

    @Override
    public boolean canGrab() {
      return true;
    }

    @Override
    public boolean canSlotSwitch() {
      return true;
    }
  }

  private static class Boulder {
    private final Map<IntVector, BlockData> data;
    private final AABB bounds;
    private final AABB preciseBounds;
    private final World world;
    private User user;
    private Vector3 center;

    private final int size;
    private final long expireTime;

    private Boulder(User user, Block centerBlock, int size, long duration) {
      this.user = user;
      this.world = user.world();
      this.size = size;
      expireTime = System.currentTimeMillis() + duration;
      data = new HashMap<>();
      center = Vector3.center(centerBlock);
      double hr = size / 2.0;
      preciseBounds = new AABB(new Vector3(-hr, -hr, -hr), new Vector3(hr, hr, hr));
      bounds = preciseBounds.grow(Vector3.ONE);
      int half = (size - 1) / 2;
      IntVector tempVector = new IntVector(centerBlock.getRelative(BlockFace.DOWN, half)); // When mapping blocks use the real center block
      List<Material> earthData = new ArrayList<>();
      for (int dy = -half; dy <= half; dy++) {
        for (int dz = -half; dz <= half; dz++) {
          for (int dx = -half; dx <= half; dx++) {
            IntVector point = new IntVector(dx, dy, dz);
            Block block = tempVector.add(point).toBlock(world);
            if (!user.canBuild(block)) {
              continue;
            }
            BlockData bd = null;
            if (EarthMaterials.isEarthNotLava(user, block)) {
              bd = MaterialUtil.solidType(block.getBlockData());
              earthData.add(bd.getMaterial());
            } else if (MaterialUtil.isTransparent(block)) {
              if (earthData.isEmpty()) {
                bd = Material.DIRT.createBlockData();
              } else {
                bd = earthData.get(ThreadLocalRandom.current().nextInt(earthData.size())).createBlockData();
              }
            }
            if (bd != null && (Math.abs(dx) + Math.abs(dy) + Math.abs(dz)) % 2 == 0) {
              data.put(point, bd);
            }
          }
        }
      }
    }

    private boolean isValidBlock(Block block) {
      if (!MaterialUtil.isTransparent(block) || !TempBlock.isBendable(block)) {
        return false;
      }
      return user.canBuild(block);
    }

    private void updateData() {
      data.entrySet().removeIf(entry -> {
        Material type = center.add(new Vector3(entry.getKey())).toBlock(world).getType();
        return type != entry.getValue().getMaterial();
      });
    }

    private boolean blendSmash() {
      int originalSize = data.size();
      Collection<Block> removed = new ArrayList<>();
      Iterator<IntVector> iterator = data.keySet().iterator();
      while (iterator.hasNext()) {
        Block block = center.add(new Vector3(iterator.next())).toBlock(world);
        if (!isValidBlock(block)) {
          removed.add(block);
          iterator.remove();
        }
      }
      FragileStructure.tryDamageStructure(removed, 4 * removed.size());
      return !data.isEmpty() && originalSize - data.size() <= size;
    }

    private boolean isValidCenter(Block check) {
      Vector3 temp = Vector3.center(check);
      return data.keySet().stream().map(bv -> temp.add(new Vector3(bv)).toBlock(world)).allMatch(this::isValidBlock);
    }

    private void center(Block block) {
      this.center = Vector3.center(block);
    }

    private Collider collider() {
      return bounds.at(center);
    }

    private Map<Block, BlockData> data() {
      return data.entrySet().stream()
        .collect(Collectors.toMap(e -> center.add(new Vector3(e.getKey())).toBlock(world), Map.Entry::getValue));
    }

    private void shatter() {
      if (data.isEmpty()) {
        return;
      }
      for (Map.Entry<Block, BlockData> entry : data().entrySet()) {
        Vector3 velocity = VectorMethods.gaussianOffset(Vector3.ZERO, 0.2, 0.1, 0.2);
        Block block = entry.getKey();
        BlockData blockData = entry.getValue();
        TempBlock.createAir(block);
        new BendingFallingBlock(block, blockData, velocity, true, 5000);
        Location spawnLoc = block.getLocation().add(0.5, 0.5, 0.5);
        ParticleUtil.create(Particle.BLOCK_CRACK, spawnLoc).count(4)
          .offset(0.5, 0.5, 0.5).data(blockData).spawn();
        if (ThreadLocalRandom.current().nextBoolean()) {
          SoundUtil.playSound(spawnLoc, blockData.getSoundGroup().getBreakSound(), 1, 1);
        }
      }
      data.clear();
    }
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.RADIUS)
    public int radius;
    @Modifiable(Attribute.CHARGE_TIME)
    public long chargeTime;
    @Modifiable(Attribute.SELECTION)
    public double selectRange;
    @Modifiable(Attribute.DURATION)
    public long maxDuration;
    @Modifiable(Attribute.STRENGTH)
    public double raiseEntityPush;
    @Modifiable(Attribute.SELECTION)
    public double grabRange;
    @Modifiable(Attribute.RANGE)
    public double shootRange;
    @Modifiable(Attribute.DAMAGE)
    public double damage;
    @Modifiable(Attribute.STRENGTH)
    public double knockback;
    @Modifiable(Attribute.STRENGTH)
    public double knockup;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "earth", "earthsmash");

      cooldown = abilityNode.node("cooldown").getLong(7000);
      radius = Math.max(3, abilityNode.node("radius").getInt(3));
      chargeTime = abilityNode.node("charge-time").getLong(1250);
      selectRange = abilityNode.node("select-range").getDouble(10.0);
      maxDuration = abilityNode.node("max-duration").getLong(45000);
      raiseEntityPush = abilityNode.node("raise-entity-push").getDouble(0.85);
      grabRange = abilityNode.node("grab-range").getDouble(10.0);
      shootRange = abilityNode.node("range").getDouble(16.0);
      damage = abilityNode.node("damage").getDouble(3.5);
      knockback = abilityNode.node("knockback").getDouble(2.8);
      knockup = abilityNode.node("knockup").getDouble(0.15);

      if (radius % 2 == 0) {
        radius++;
      }
    }
  }
}
