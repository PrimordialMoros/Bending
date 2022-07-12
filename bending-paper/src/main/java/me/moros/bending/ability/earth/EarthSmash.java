/*
 * Copyright 2020-2022 Moros
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

package me.moros.bending.ability.earth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import me.moros.bending.ability.common.FragileStructure;
import me.moros.bending.ability.fire.FlameRush;
import me.moros.bending.ability.water.FrostBreath;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.game.temporal.TempEntity;
import me.moros.bending.game.temporal.TempEntity.TempFallingBlock;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.math.Vector3i;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.properties.BendingProperties;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingEffect;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.VectorUtil;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.potion.PotionEffectType;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class EarthSmash extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;
  private RemovalPolicy swappedSlotsPolicy;

  private EarthSmashState state;
  private Boulder boulder;

  public EarthSmash(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    Optional<EarthSmash> grabbed = user.game().abilityManager(user.world()).userInstances(user, EarthSmash.class)
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
    userConfig = user.game().configProcessor().calculate(this, config);
  }

  @Override
  public UpdateResult update() {
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
    int radius = Math.max(3, userConfig.radius);
    if (radius % 2 == 0) {
      radius++;
    }
    // Check blocks above center
    for (int i = 0; i <= radius; i++) {
      Block b = center.getRelative(BlockFace.UP, i + 1);
      if (!MaterialUtil.isTransparent(b) || !TempBlock.isBendable(b) || !user.canBuild(b)) {
        return false;
      }
    }

    boulder = new Boulder(user, center, radius, userConfig.maxDuration);

    int minRequired = FastMath.ceil(Math.pow(radius, 3) * 0.375);
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

  private static boolean tryGrab(User user) {
    Block target = user.rayTrace(config.grabRange).blocks(user.world()).block();
    EarthSmash earthSmash = getInstance(user, target, s -> s.state.canGrab());
    if (earthSmash == null) {
      return false;
    }
    user.game().abilityManager(user.world()).changeOwner(earthSmash, user);
    earthSmash.grabBoulder();
    return true;
  }

  public static void tryDestroy(User user, Block block) {
    if (user.sneaking() && user.selectedAbilityName().equals("EarthSmash")) {
      EarthSmash earthSmash = getInstance(user, block, x -> true);
      if (earthSmash != null) {
        earthSmash.shatter();
      }
    }
  }

  private static @Nullable EarthSmash getInstance(User user, @Nullable Block block, Predicate<EarthSmash> filter) {
    if (block == null) {
      return null;
    }
    AABB blockBounds = AABB.BLOCK_BOUNDS.at(new Vector3d(block));
    return user.game().abilityManager(user.world()).instances(EarthSmash.class)
      .filter(filter)
      .filter(s -> s.boulder != null && s.boulder.preciseBounds.at(s.boulder.center).intersects(blockBounds))
      .findAny().orElse(null);
  }

  private void cleanAll() {
    for (var entry : boulder.data().entrySet()) {
      Block block = entry.getKey();
      if (block.getType() != entry.getValue().getMaterial()) {
        continue;
      }
      TempBlock.air().build(block);
    }
  }

  private void render() {
    for (var entry : boulder.data().entrySet()) {
      Block block = entry.getKey();
      if (!MaterialUtil.isTransparent(block)) {
        continue;
      }
      WorldUtil.tryBreakPlant(block);
      TempBlock.builder(entry.getValue()).build(block);
    }
  }

  private void shatter() {
    if (boulder != null && !boulder.data.isEmpty()) {
      Map<TempFallingBlock, ShardType> shards = new HashMap<>();
      for (var entry : boulder.data().entrySet()) {
        Vector3d velocity = VectorUtil.gaussianOffset(Vector3d.ZERO, 0.2, 0.1, 0.2);
        Block block = entry.getKey();
        BlockData blockData = entry.getValue();
        TempBlock.air().build(block);
        TempFallingBlock projectile = TempEntity.builder(blockData).velocity(velocity).duration(5000).build(block);
        shards.put(projectile, ShardType.from(blockData.getMaterial()));
        ParticleUtil.of(Particle.BLOCK_CRACK, Vector3d.center(block)).count(4).offset(0.5).data(blockData).spawn(block.getWorld());
        if (ThreadLocalRandom.current().nextBoolean()) {
          SoundUtil.of(blockData.getSoundGroup().getBreakSound()).play(block);
        }
      }
      boulder.data.clear();
      if (userConfig.shatterEffects) {
        boulder = null;
        state = new ShatteredState(shards);
      }
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
  public void onUserChange(User newUser) {
    this.user = newUser;
    boulder.user = newUser;
  }

  @Override
  public Collection<Collider> colliders() {
    return !state.canCollide() ? List.of() : List.of(boulder.collider());
  }

  @Override
  public void onCollision(Collision collision) {
    Ability collidedAbility = collision.collidedAbility();
    boolean shatter = collision.removeSelf();
    if (collidedAbility instanceof FlameRush other && other.isFullyCharged()) {
      shatter = true;
    } else if (collidedAbility instanceof FrostBreath) {
      ThreadLocalRandom rand = ThreadLocalRandom.current();
      boulder.data.replaceAll((k, v) -> rand.nextBoolean() ? Material.ICE.createBlockData() : Material.PACKED_ICE.createBlockData());
      shatter = true;
    } else if (collidedAbility.description().element() == Element.FIRE || collidedAbility instanceof LavaDisk) {
      boulder.data.replaceAll((k, v) -> Material.MAGMA_BLOCK.createBlockData());
      shatter = true;
    }
    if (shatter) {
      collision.removeSelf(false);
      shatter();
    }
  }

  @FunctionalInterface
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

  private final class ChargeState implements EarthSmashState {
    private final long startTime;

    private ChargeState() {
      startTime = System.currentTimeMillis();
    }

    @Override
    public UpdateResult update() {
      if (System.currentTimeMillis() >= startTime + userConfig.chargeTime) {
        if (user.sneaking()) {
          ParticleUtil.of(Particle.SMOKE_NORMAL, user.mainHandSide()).spawn(user.world());
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

  private final class LiftState implements EarthSmashState {
    private final Vector3d origin;
    private int tick = 0;
    private long nextLiftTime = 0;

    private LiftState() {
      this.origin = new Vector3d(boulder.center.toArray());
    }

    @Override
    public UpdateResult update() {
      Collider liftCollider = boulder.bounds.at(boulder.center.add(Vector3d.PLUS_J));
      CollisionUtil.handle(user, liftCollider, entity -> {
        Vector3d push = new Vector3d(entity.getVelocity()).withY(userConfig.raiseEntityPush);
        return EntityUtil.applyVelocity(EarthSmash.this, entity, push);
      }, true, true);

      long time = System.currentTimeMillis();
      if (time < nextLiftTime) {
        return UpdateResult.CONTINUE;
      }
      nextLiftTime = time + 70;
      cleanAll();
      boulder.center(boulder.center.add(Vector3d.PLUS_J).toBlock(boulder.world));
      SoundUtil.EARTH.play(boulder.world, boulder.center);
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
              Block block = origin.add(new Vector3d(x, -1, z)).toBlock(boulder.world);
              if (EarthMaterials.isEarthNotLava(user, block)) {
                TempBlock.air().duration(BendingProperties.instance().earthRevertTime()).build(block);
              }
            }
            // Remove top layer
            Block block = origin.add(new Vector3d(x, 0, z)).toBlock(boulder.world);
            if (EarthMaterials.isEarthNotLava(user, block)) {
              TempBlock.air().duration(BendingProperties.instance().earthRevertTime()).build(block);
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

  private final class GrabState implements EarthSmashState {
    private final double grabbedDistance;

    private GrabState() {
      this.grabbedDistance = Math.min(boulder.center.distance(user.eyeLocation()), userConfig.grabRange);
    }

    @Override
    public UpdateResult update() {
      if (user.sneaking()) {
        Vector3d dir = user.direction().normalize().multiply(grabbedDistance);
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

  private final class ShotState implements EarthSmashState {
    private final Set<Entity> affectedEntities;
    private final Vector3d origin;
    private final Vector3d direction;
    private Vector3d location;
    private int buffer;
    private final int speed = 18;

    private ShotState() {
      affectedEntities = new HashSet<>();
      origin = new Vector3d(boulder.center.toArray());
      location = new Vector3d(origin.toArray());
      direction = user.direction();
      SoundUtil.EARTH.play(boulder.world, boulder.center);
      buffer = speed;
    }

    @Override
    public UpdateResult update() {
      buffer += speed;
      if (buffer < 20) {
        return UpdateResult.CONTINUE;
      }
      buffer -= 20;
      CollisionUtil.handle(user, boulder.collider(), this::onEntityHit);
      cleanAll();
      location = location.add(direction);
      Block newCenter = location.toBlock(boulder.world);
      if (!boulder.isValidBlock(newCenter)) {
        shatter();
        return UpdateResult.CONTINUE;
      }
      boulder.center(newCenter);
      if (origin.distanceSq(boulder.center) > userConfig.shootRange * userConfig.shootRange) {
        return UpdateResult.REMOVE;
      }
      if (!boulder.blendSmash()) {
        shatter();
        return UpdateResult.CONTINUE;
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
      Vector3d velocity = EntityUtil.entityCenter(entity).subtract(boulder.center).withY(userConfig.knockup).normalize();
      EntityUtil.applyVelocity(EarthSmash.this, entity, velocity.multiply(userConfig.knockback));
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
    public UpdateResult update() {
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

  private enum ShardType {
    MAGMA, SAND, ICE, MUD, ROCK;

    private static ShardType from(Material material) {
      if (EarthMaterials.LAVA_BENDABLE.isTagged(material)) {
        return ShardType.MAGMA;
      } else if (EarthMaterials.SAND_BENDABLE.isTagged(material)) {
        return ShardType.SAND;
      } else if (WaterMaterials.ICE_BENDABLE.isTagged(material)) {
        return ShardType.ICE;
      } else if (EarthMaterials.MUD_BENDABLE.isTagged(material)) {
        return ShardType.MUD;
      } else {
        return ShardType.ROCK;
      }
    }
  }

  private final class ShatteredState implements EarthSmashState {
    private final Map<TempFallingBlock, ShardType> pieces;
    private final Set<Entity> affectedEntities;

    private ShatteredState(Map<TempFallingBlock, ShardType> pieces) {
      this.pieces = pieces;
      affectedEntities = new HashSet<>();
    }

    @Override
    public UpdateResult update() {
      pieces.entrySet().removeIf(this::tryCollisions);
      return pieces.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
    }

    private boolean tryCollisions(Entry<TempFallingBlock, ShardType> entry) {
      TempFallingBlock fb = entry.getKey();
      if (!fb.isValid()) {
        return true;
      }
      return CollisionUtil.handle(user, AABB.BLOCK_BOUNDS.at(fb.center()), e -> onEntityHit(e, entry.getValue()));
    }

    private boolean onEntityHit(Entity entity, ShardType type) {
      if (!affectedEntities.contains(entity)) {
        affectedEntities.add(entity);
        DamageUtil.damageEntity(entity, user, userConfig.shatterDamage, description());
        if (entity.isValid()) {
          switch (type) {
            case MAGMA -> BendingEffect.FIRE_TICK.apply(user, entity, userConfig.fireTicks);
            case SAND ->
              EntityUtil.tryAddPotion(entity, PotionEffectType.BLINDNESS, FastMath.round(userConfig.sandDuration / 50.0), userConfig.sandPower - 1);
            case ICE -> BendingEffect.FROST_TICK.apply(user, entity, userConfig.freezeTicks);
            case MUD ->
              EntityUtil.tryAddPotion(entity, PotionEffectType.SLOW, FastMath.round(userConfig.mudDuration / 50.0), userConfig.mudPower - 1);
          }
          return true;
        }
      }
      return false;
    }

    @Override
    public boolean canCollide() {
      return false;
    }

    @Override
    public boolean canSlotSwitch() {
      return true;
    }
  }

  private static final class Boulder {
    private final Map<Vector3i, BlockData> data;
    private final AABB bounds;
    private final AABB preciseBounds;
    private final World world;
    private User user;
    private Vector3d center;

    private final int size;
    private final long expireTime;

    private Boulder(User user, Block centerBlock, int size, long duration) {
      this.user = user;
      this.world = user.world();
      this.size = size;
      expireTime = System.currentTimeMillis() + duration;
      data = new HashMap<>();
      center = Vector3d.center(centerBlock);
      double hr = size / 2.0;
      preciseBounds = new AABB(new Vector3d(-hr, -hr, -hr), new Vector3d(hr, hr, hr));
      bounds = preciseBounds.grow(Vector3d.ONE);
      int half = (size - 1) / 2;
      Vector3i tempVector = new Vector3i(centerBlock.getRelative(BlockFace.DOWN, half)); // When mapping blocks use the real center block
      List<Material> earthData = new ArrayList<>();
      for (int dy = -half; dy <= half; dy++) {
        for (int dz = -half; dz <= half; dz++) {
          for (int dx = -half; dx <= half; dx++) {
            Vector3i point = new Vector3i(dx, dy, dz);
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
        Material type = center.add(entry.getKey().toVector3d()).toBlock(world).getType();
        return type != entry.getValue().getMaterial();
      });
    }

    private boolean blendSmash() {
      int originalSize = data.size();
      Collection<Block> removed = new ArrayList<>();
      Iterator<Vector3i> iterator = data.keySet().iterator();
      while (iterator.hasNext()) {
        Block block = center.add(iterator.next().toVector3d()).toBlock(world);
        if (!isValidBlock(block)) {
          removed.add(block);
          iterator.remove();
        }
      }
      FragileStructure.tryDamageStructure(removed, 4 * removed.size());
      return !data.isEmpty() && originalSize - data.size() <= size;
    }

    private boolean isValidCenter(Block check) {
      Vector3d temp = Vector3d.center(check);
      return data.keySet().stream().map(v -> temp.add(v.toVector3d()).toBlock(world)).allMatch(this::isValidBlock);
    }

    private void center(Block block) {
      this.center = Vector3d.center(block);
    }

    private Collider collider() {
      return bounds.at(center);
    }

    private Map<Block, BlockData> data() {
      return data.entrySet().stream()
        .collect(Collectors.toMap(e -> center.add(e.getKey().toVector3d()).toBlock(world), Entry::getValue));
    }
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 7000;
    @Modifiable(Attribute.RADIUS)
    private int radius = 3;
    @Modifiable(Attribute.CHARGE_TIME)
    private long chargeTime = 1250;
    @Modifiable(Attribute.SELECTION)
    private double selectRange = 10;
    @Modifiable(Attribute.DURATION)
    private long maxDuration = 45000;
    @Modifiable(Attribute.STRENGTH)
    private double raiseEntityPush = 0.85;
    @Modifiable(Attribute.SELECTION)
    private double grabRange = 10;
    @Modifiable(Attribute.RANGE)
    private double shootRange = 16;
    @Modifiable(Attribute.DAMAGE)
    private double damage = 3.5;
    @Modifiable(Attribute.STRENGTH)
    private double knockback = 2.8;
    @Modifiable(Attribute.STRENGTH)
    private double knockup = 0.15;
    private boolean shatterEffects = true;
    @Modifiable(Attribute.DAMAGE)
    private double shatterDamage = 1;
    @Modifiable(Attribute.FIRE_TICKS)
    private int fireTicks = 25;
    @Modifiable(Attribute.FREEZE_TICKS)
    private int freezeTicks = 60;
    @Modifiable(Attribute.STRENGTH)
    private int mudPower = 2;
    @Modifiable(Attribute.DURATION)
    private long mudDuration = 1500;
    @Modifiable(Attribute.STRENGTH)
    private int sandPower = 2;
    @Modifiable(Attribute.DURATION)
    private long sandDuration = 1500;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "earth", "earthsmash");
    }
  }
}
