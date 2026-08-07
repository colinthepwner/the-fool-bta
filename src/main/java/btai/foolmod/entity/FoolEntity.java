package btai.foolmod.entity;

import btai.foolmod.act.BlockDoorAct;
import btai.foolmod.act.BreakTorchAct;
import btai.foolmod.act.FoolAct;
import btai.foolmod.act.StealChestAct;
import btai.foolmod.act.WoolBuildAct;
import btai.foolmod.block.JoxeWard;
import btai.foolmod.item.FoolItems;
import btai.foolmod.path.FoolPathfinder;
import com.mojang.nbt.tags.CompoundTag;
import com.mojang.nbt.tags.ListTag;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.block.tag.BlockTags;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.EntityItem;
import net.minecraft.core.entity.monster.MobHuman;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.entity.projectile.ProjectileArrow;
import net.minecraft.core.enums.EnumBlockSoundEffectType;
import net.minecraft.core.enums.EnumDropCause;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.player.inventory.container.Container;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.DyeColor;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.phys.HitResult;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pos.TilePos;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class FoolEntity extends MobHuman {

	private static final float PROWL_SPEED = 0.105f;

	private static final float FLEE_SPEED = 0.17f;

	private static final double SENSE_RANGE = 24.0;
	private static final double WATCH_RANGE = 48.0;

	private static final double WATCH_FOV_DOT = 0.78;

	private static final double PANIC_RANGE = 11.0;

	private static final double APPROACH_UNTIL = 5.0;

	private static final double STRIKE_RANGE = 3.6;

	private static final double STRIKE_VERTICAL = 2.0;
	private static final double STRIKE_APPROACH = 9.0;

	private static final double STRIKE_COMMIT = 4.5;
	private static final int STRIKE_COOLDOWN = 900;
	private static final int STRIKE_TIMEOUT = 100;
	private static final int STRIKE_DAMAGE = 2;

	private static final int COVER_MAX_AGE = 200;

	private static final int COVER_REPICK_GAP = 25;

	private static final int HEADING_LOCK = 100;

	private static final double HEADING_KEEP_DOT = 0.15;

	private static final double HEADING_MAX_TURN_COS = 0.5;

	private static final int BLOCK_ACTION_GAP = 5;

	private static final int KILL_CREDIT_TICKS = 100;

	private static final double PUNCH_RANGE = 3.0;

	private static final int PUNCH_COOLDOWN = 30;

	private static final double ARROW_MIN_RANGE = 7.0;
	private static final double ARROW_MAX_RANGE = 26.0;

	private static final int ARROW_CHANCE = 70;
	private static final int ARROW_COOLDOWN_MIN = 300;
	private static final int ARROW_COOLDOWN_VAR = 400;

	private static final int BUILD_NEAR_RADIUS = 48;

	private static final float FLEE_SPEED_PANIC = 0.26f;

	private static final double PANIC_TAPER = 16.0;

	private static final boolean DEBUG = System.getenv("FOOLMOD_DEBUG") != null;

	private static final double COVER_INCUMBENT_BONUS = 260.0;

	private static final double MAX_FLEE_CLIMB = 5.0;

	private static final double MAX_FLEE_DROP = 4.0;

	private static final int SPOTTED_DWELL = 8;

	private static final int VANISH_UNSEEN_TICKS = 4;
	private static final double VANISH_MIN_DIST = 2.5;
	private static final int FLEE_MAX_TICKS = 1200;
	private static final int LEASH_DESPAWN = 96;

	private static final int MISCHIEF_GAP = 60;
	private static final int IDLE_SOUND_GAP = 220;

	private static final int NAV_NODES = 6000;
	private static final int NAV_SLICE = 9000;
	private static final int NAV_MAX_SEARCH_TICKS = 3;
	private static final int NAV_FAIL_BACKOFF = 40;
	private static final int MAX_HOP_BLOCKED = 14;
	private static final int AVOID_TTL = 400;

	private static final int SWING_SLOT = 3;

	public static final String SOUND_IDLE = "foolmod:mob.fool.idle";

	public static final String SOUND_HIT = "foolmod:mob.fool.hit";

	private final List<ItemStack> loot = new ArrayList<>();

	private FoolAct act;

	private int tricksLeft = -1;
	private boolean leaving;
	private int mischiefCooldown;
	private int idleSoundTimer;

	private boolean fleeing;
	private int fleeTicks;
	private int unseenTicks;
	private int[] coverSpot;
	private int coverAge;
	private int repickCooldown;
	private int headingLock;
	private int fleeBlockedTicks;
	private int punchCooldown;
	private int arrowCooldown;

	private int lastStruckByTick = Integer.MIN_VALUE;

	private int spottedTicks;
	private int[] swimTarget;
	private int swimCommit;
	private boolean lookedThisTick;

	private boolean seenThisFlight;

	private double fleeHeadX, fleeHeadZ;
	private Player fleeFrom;

	private boolean striking;
	private int strikeTicks;
	private int strikeCooldown;
	private Player strikeTarget;

	private int[] wanderSpot;
	private int wanderTicks;
	private int[] buildTarget;
	private int buildSeekCooldown;
	private int[] approachGoal;

	private FoolPathfinder.PathfinderState pathfinderState;
	private List<int[]> navPath;
	private int navIndex;
	private long navGoalKey = Long.MIN_VALUE;
	private FoolPathfinder.Search navSearch;
	private long navSearchGoal = Long.MIN_VALUE;
	private int navSearchTicks;
	private long navFailGoal = Long.MIN_VALUE;
	private int navFailUntil;
	private int navStuckTicks;
	private double navLastX, navLastY, navLastZ;
	private int blockedTicks;
	private int hopBlockedTicks;
	private boolean directSteer;
	private final Map<Long, Integer> avoidCells = new HashMap<>();

	private boolean hasOpenedDoor;
	private int openedDoorX, openedDoorY, openedDoorZ;
	private double openedDoorEntryDX, openedDoorEntryDZ;

	private boolean woolRed = true;

	private final Set<Long> placedBlocks = new HashSet<>();

	private int blockActionCooldown;

	private int placeFailCooldown;

	private int placeFails;

	private boolean swinging;
	private int swingTicks;
	private int swingSyncOut;
	private int swingSyncSeen;

	public FoolEntity(World world) {
		super(world);
		this.moveSpeed = 1.0f;
		this.speed = PROWL_SPEED;
		this.setSize(0.6f, 1.8f);
		this.entityData.define(SWING_SLOT, 0, Integer.class);
		this.strikeCooldown = 200 + this.random.nextInt(400);
		this.idleSoundTimer = this.random.nextInt(IDLE_SOUND_GAP);
	}

	private FoolPathfinder.PathfinderState pathfinderState() {
		if (pathfinderState == null) {
			pathfinderState = new FoolPathfinder.PathfinderState(NAV_NODES);
		}
		return pathfinderState;
	}

	@Override
	public boolean canSpawnHere() {
		if (this.world == null) {
			return false;
		}
		Long last = LAST_SPAWN.get(this.world);
		if (last != null && this.world.getTotalWorldTime() - last < SPAWN_COOLDOWN_TICKS) {
			return false;
		}
		if (this.random.nextInt(RARITY_ROLL) != 0) {
			return false;
		}
		return hasValidSpawnSurroundings();
	}

	public boolean hasValidSpawnSurroundings() {
		int bx = MathHelper.floor(this.x);
		int by = MathHelper.floor(this.bb.minY);
		int bz = MathHelper.floor(this.z);
		if (Blocks.hasTag(this.world.getBlockId(bx, by, bz), BlockTags.PREVENT_MOB_SPAWNS)) {
			return false;
		}
		if (!this.world.areBlocksLoaded(
				new TilePos(this.bb.minX - 1.0, this.bb.minY - 1.0, this.bb.minZ - 1.0),
				new TilePos(this.bb.maxX + 1.0, this.bb.maxY + 1.0, this.bb.maxZ + 1.0))) {
			return false;
		}
		if (!this.world.checkIfAABBIsClear(this.bb)
				|| !this.world.getCubes(this, this.bb).isEmpty()
				|| this.world.getIsAnyLiquid(this.bb)) {
			return false;
		}

		if (underPlayerBuiltRoof(bx, by, bz)) {
			return false;
		}

		return nearPlayerBuild(bx, by, bz, BUILD_NEAR_RADIUS) || nearAnyPlayer(64.0);
	}

	private boolean underPlayerBuiltRoof(int bx, int by, int bz) {
		for (int dy = 1; dy <= 6; dy++) {
			if (FoolPathfinder.isPlayerBuilt(this.world, bx, by + dy, bz)) {
				return true;
			}
		}
		return false;
	}

	private boolean nearAnyPlayer(double r) {
		Player p = this.world.getClosestPlayerToEntity(this, r);
		return p != null;
	}

	private boolean nearPlayerBuild(int bx, int by, int bz, int r) {
		for (int dx = -r; dx <= r; dx += 3) {
			for (int dz = -r; dz <= r; dz += 3) {
				int x = bx + dx, z = bz + dz;
				if (!FoolPathfinder.chunkLoaded(this.world, x, z)) continue;
				for (int dy = -6; dy <= 10; dy += 2) {
					int y = by + dy;
					if (y < 2) continue;
					if (FoolPathfinder.isPlayerBuilt(this.world, x, y, z)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public static final int RARITY_ROLL = 40;

	public static final long SPAWN_COOLDOWN_TICKS = 20L * 60L * 2L;

	private static final Map<World, Long> LAST_SPAWN =
			java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

	@Override
	public void spawnInit() {
		super.spawnInit();
		if (this.world != null && !this.world.isClientSide) {
			LAST_SPAWN.put(this.world, this.world.getTotalWorldTime());
		}
	}

	@Override
	public int getMaxSpawnedInChunk() {
		return 1;
	}

	@Override
	public int getMaxPerPlayer() {
		return 1;
	}

	@Override
	public int getMaxHealth() {
		return 10;
	}

	@Override
	protected boolean canDespawn() {
		return true;
	}

	@Override
	protected void updateAI() {
		if (this.world == null || this.world.isClientSide || this.removed) {
			return;
		}
		tickTimers();

		Player near = nearestVictim(-1.0);

		if (escapeWaterTick()) {
			settleLook();
			return;
		}

		if (this.tickCount % 20 == 0 && JoxeWard.isWarded(this.world,
				MathHelper.floor(this.x), MathHelper.floor(this.bb.minY), MathHelper.floor(this.z))) {
			vaporise();
			return;
		}

		punchIfCrowded(near);
		maybeShootArrow(near);

		if (leashCheck()) {
			return;
		}

		if (fleeing) {
			if (!fleeTick()) {
				navigationTick();
			}
			settleLook();
			return;
		}

		if (striking) {
			Player quarry = (strikeTarget != null && !strikeTarget.removed) ? strikeTarget : near;
			strikeTick(quarry);
			navigationTick();
			settleLook();
			return;
		}

		if (near != null && isWatchedBy(near)) {
			spottedTicks++;
		} else {
			spottedTicks = 0;
		}
		if (spottedTicks >= SPOTTED_DWELL) {
			spottedTicks = 0;
			beginFlee(near);
			navigationTick();
			settleLook();
			return;
		}

		if (near != null) {
			double d = near.distanceTo(this);
			if (d < PANIC_RANGE) {

				if (strikeCooldown <= 0 && canEntityBeSeen(near) && !isWatchedBy(near)) {
					beginStrike(near);
				} else {
					beginFlee(near);
				}
				navigationTick();
				settleLook();
				return;
			}
			if (strikeCooldown <= 0 && d < STRIKE_APPROACH && canEntityBeSeen(near) && !isWatchedBy(near)
					&& this.random.nextInt(90) == 0) {
				beginStrike(near);
				navigationTick();
				settleLook();
				return;
			}
		}

		this.speed = PROWL_SPEED;
		setSprinting(false);
		if (tricksLeft < 0) {
			tricksLeft = 4 + this.random.nextInt(5);
		}
		if (leavingTick()) {
			return;
		}
		if (act != null) {
			if (act.tick(this)) {
				act = null;
				if (--tricksLeft <= 0) {
					leaving = true;
				}
				mischiefCooldown = MISCHIEF_GAP + this.random.nextInt(MISCHIEF_GAP);
			}
		} else if (mischiefCooldown <= 0) {

			chooseAct(near);
		} else if (!approachPlayerTick(near)) {
			seekBuildTick();
		}
		navigationTick();
	}

	private void settleLook() {
		if (lookedThisTick) {
			lookedThisTick = false;
			return;
		}
		this.xRot += (0.0f - this.xRot) * 0.25f;
		if (Math.abs(this.xRot) < 0.5f) {
			this.xRot = 0.0f;
		}
	}

	private void tickTimers() {
		if (mischiefCooldown > 0) mischiefCooldown--;
		if (strikeCooldown > 0) strikeCooldown--;
		if (blockActionCooldown > 0) blockActionCooldown--;
		if (punchCooldown > 0) punchCooldown--;
		if (arrowCooldown > 0) arrowCooldown--;
		if (!avoidCells.isEmpty()) {
			avoidCells.values().removeIf(expiry -> expiry <= this.tickCount);
		}
		if (--idleSoundTimer <= 0) {
			idleSoundTimer = IDLE_SOUND_GAP / 2 + this.random.nextInt(IDLE_SOUND_GAP);
			this.world.playSoundAtEntity(null, this, SOUND_IDLE, 0.8f, 0.9f + this.random.nextFloat() * 0.2f);
		}
	}

	private boolean leashCheck() {
		Player p = this.world.getClosestPlayerToEntity(this, -1.0);
		if (p == null || p.distanceTo(this) > LEASH_DESPAWN) {
			this.remove();
			return true;
		}
		return false;
	}

	private Player nearestVictim(double range) {
		Player best = null;
		double bestSq = range < 0.0 ? Double.MAX_VALUE : range * range;
		for (Player p : this.world.players) {
			if (p == null || p.removed || !p.getGamemode().hasHostileMobs()) continue;
			double dSq = p.distanceToSqr(this);
			if (dSq < bestSq) {
				bestSq = dSq;
				best = p;
			}
		}
		return best;
	}

	private boolean leavingTick() {
		if (!leaving) {
			return false;
		}
		if (!isWatched() && farEnoughToVanish()) {
			vanish();
			return true;
		}
		if (isWatched()) {
			beginFlee(nearestVictim(-1.0));
			return false;
		}
		wanderTick();
		return true;
	}

	private void chooseAct(Player near) {

		List<FoolAct> options = new ArrayList<>(List.of(
				new BreakTorchAct(), new StealChestAct(), new BlockDoorAct(), new WoolBuildAct()));
		java.util.Collections.shuffle(options, this.random);
		for (FoolAct candidate : options) {
			if (candidate.seek(this)) {
				act = candidate;
				return;
			}
		}

		mischiefCooldown = MISCHIEF_GAP;
		if (!approachPlayerTick(near)) {
			seekBuildTick();
		}
	}

	private boolean approachPlayerTick(Player target) {
		if (target == null) {
			return false;
		}
		if (target.distanceTo(this) < APPROACH_UNTIL) {
			approachGoal = null;
			return false;
		}

		if (approachGoal == null || dist2(approachGoal[0], approachGoal[2], target.x, target.z) > 64.0) {
			int tx = MathHelper.floor(target.x);
			int tz = MathHelper.floor(target.z);
			int ty = groundAt(tx, MathHelper.floor(target.bb.minY), tz);
			approachGoal = new int[]{tx, ty == Integer.MIN_VALUE ? MathHelper.floor(target.bb.minY) : ty, tz};
		}
		if (!navigateTo(approachGoal[0], approachGoal[1], approachGoal[2], APPROACH_UNTIL)) {
			approachGoal = null;
			return false;
		}
		return true;
	}

	private static double dist2(double ax, double az, double bx, double bz) {
		double dx = ax + 0.5 - bx, dz = az + 0.5 - bz;
		return dx * dx + dz * dz;
	}

	private void seekBuildTick() {
		if (buildSeekCooldown > 0) {
			buildSeekCooldown--;
			if (buildTarget != null && !arrivedAt(buildTarget, 6.0)) {
				navigateTo(buildTarget[0], buildTarget[1], buildTarget[2], 4.0);
				return;
			}
		}
		int[] found = nearestPlayerBuiltBlock(40);
		if (found == null) {
			wanderTick();
			return;
		}
		buildTarget = found;
		buildSeekCooldown = 100;
		if (!navigateTo(found[0], found[1], found[2], 4.0)) {
			buildTarget = null;
			wanderTick();
		}
	}

	private int[] nearestPlayerBuiltBlock(int r) {
		int bx = MathHelper.floor(this.x), by = MathHelper.floor(this.bb.minY), bz = MathHelper.floor(this.z);
		int[] best = null;
		double bestSq = Double.MAX_VALUE;
		for (int dx = -r; dx <= r; dx += 2) {
			for (int dz = -r; dz <= r; dz += 2) {
				int x = bx + dx, z = bz + dz;
				if (!FoolPathfinder.chunkLoaded(this.world, x, z)) continue;
				for (int dy = -6; dy <= 8; dy++) {
					int y = by + dy;
					if (y < 2) continue;
					if (!FoolPathfinder.isPlayerBuilt(this.world, x, y, z)) continue;
					double d = dx * dx + dy * dy + dz * dz;
					if (d < bestSq) {
						bestSq = d;
						best = new int[]{x, y, z};
					}
					break;
				}
			}
		}
		return best;
	}

	private void wanderTick() {
		if (wanderSpot != null && (++wanderTicks > 200 || arrivedAt(wanderSpot, 2.0))) {
			wanderSpot = null;
		}
		if (wanderSpot == null) {
			wanderSpot = randomNearbyGround(10 + this.random.nextInt(10));
			wanderTicks = 0;
		}
		if (wanderSpot != null && !navigateTo(wanderSpot[0], wanderSpot[1], wanderSpot[2], 1.5)) {
			wanderSpot = null;
		}
	}

	public void beginFlee(Player from) {
		if (!fleeing) {
			fleeing = true;
			fleeTicks = 0;
			unseenTicks = 0;
			coverSpot = null;
			coverAge = 0;
			repickCooldown = 0;
			headingLock = 0;
			fleeBlockedTicks = 0;
			seenThisFlight = false;
			fleeHeadX = 0.0;
			fleeHeadZ = 0.0;
			act = null;
			striking = false;
			clearPath();
		}
		fleeFrom = from;
	}

	private boolean fleeTick() {
		setSprinting(true);

		double gap = fleeFrom == null ? PANIC_TAPER : fleeFrom.distanceTo(this);
		double panic = Math.max(0.0, Math.min(1.0, (PANIC_TAPER - gap) / PANIC_TAPER));
		this.speed = (float) (FLEE_SPEED + (FLEE_SPEED_PANIC - FLEE_SPEED) * panic);

		if (fleeFrom == null || fleeFrom.removed) {
			fleeFrom = nearestVictim(-1.0);
		}

		if (isWatched()) {
			unseenTicks = 0;
			seenThisFlight = true;
		} else if (seenThisFlight && ++unseenTicks >= VANISH_UNSEEN_TICKS && farEnoughToVanish()) {
			vanish();
			return true;
		}

		if (++fleeTicks > FLEE_MAX_TICKS) {

			fleeing = false;
			setSprinting(false);
			this.speed = PROWL_SPEED;
			leaving = true;
			return false;
		}
		if (seenThisFlight && fleeFrom != null && fleeFrom.distanceTo(this) > 30.0 && !isWatched()) {
			vanish();
			return true;
		}

		updateFleeHeading();
		if (repickCooldown > 0) repickCooldown--;

		if (coverSpot != null && headingOpposes(coverSpot)) {
			coverSpot = null;
			repickCooldown = 0;
		}

		if (coverSpot != null && arrivedAt(coverSpot, 2.0) && !isWatched()) {
			clearPath();
			this.directSteer = false;
			return true;
		}

		boolean stale = coverSpot == null || ++coverAge > COVER_MAX_AGE || arrivedAt(coverSpot, 2.0);
		if (stale && repickCooldown <= 0) {
			int[] pick = pickCoverSpot(fleeFrom);
			if (pick != null) {
				coverSpot = pick;
				coverAge = 0;
				alignHeadingTo(pick);
			} else if (coverSpot != null && arrivedAt(coverSpot, 2.0)) {
				coverSpot = null;
			}
			repickCooldown = COVER_REPICK_GAP;
		}

		if (coverSpot != null) {

			if (!navigateTo(coverSpot[0], coverSpot[1], coverSpot[2], 1.2, true, true)) {
				avoidCell(coverSpot[0], coverSpot[1], coverSpot[2]);
				coverSpot = null;
				repickCooldown = COVER_REPICK_GAP;
			}
			debugFlee("cover");
			return false;
		}
		sprintAlongHeading();
		debugFlee("sprint");

		return false;
	}

	private void debugFlee(String mode) {
		if (!DEBUG || this.tickCount % 20 != 0) {
			return;
		}
		double bearing = Math.toDegrees(Math.atan2(fleeHeadZ, fleeHeadX));
		String target = coverSpot == null ? "-" : (coverSpot[0] + "," + coverSpot[1] + "," + coverSpot[2]);
		String node = "-";
		if (navPath != null && navIndex < navPath.size()) {
			int a = navPath.get(navIndex)[3];
			node = a == FoolPathfinder.PILLAR ? "PILLAR" : a == FoolPathfinder.BRIDGE ? "BRIDGE" : "move";
		}
		btai.foolmod.FoolMod.LOGGER.info(String.format(
				"flee[%s] pos=%.1f,%.1f,%.1f bearing=%.0f lock=%d target=%s path=%s node=%s act=%d "
						+ "dPlayer=%.1f watched=%s blocked=%d",
				mode, this.x, this.bb.minY, this.z, bearing, headingLock, target,
				navPath == null ? "none" : (navIndex + "/" + navPath.size()), node, blockActionCooldown,
				fleeFrom == null ? -1.0 : fleeFrom.distanceTo(this), isWatched(), fleeBlockedTicks));
	}

	private void updateFleeHeading() {
		if (fleeFrom == null) {
			return;
		}
		double ax = this.x - fleeFrom.x;
		double az = this.z - fleeFrom.z;
		double len = Math.sqrt(ax * ax + az * az);
		if (len < 1.0E-4) {
			return;
		}
		ax /= len;
		az /= len;

		if (fleeHeadX == 0.0 && fleeHeadZ == 0.0) {
			fleeHeadX = ax;
			fleeHeadZ = az;
			headingLock = HEADING_LOCK;
			return;
		}
		if (headingLock > 0) {
			headingLock--;
			return;
		}
		if (coverSpot != null) {
			alignHeadingTo(coverSpot);
			return;
		}

		double awayness = fleeHeadX * ax + fleeHeadZ * az;
		if (awayness > HEADING_KEEP_DOT) {
			headingLock = HEADING_LOCK;
			return;
		}
		turnHeadingToward(ax, az, HEADING_MAX_TURN_COS);
		headingLock = HEADING_LOCK;
	}

	private boolean headingOpposes(int[] spot) {
		if (fleeHeadX == 0.0 && fleeHeadZ == 0.0) {
			return false;
		}
		double dx = spot[0] + 0.5 - this.x;
		double dz = spot[2] + 0.5 - this.z;
		double len = Math.sqrt(dx * dx + dz * dz);
		if (len < 1.5) {
			return false;
		}
		return (dx / len) * fleeHeadX + (dz / len) * fleeHeadZ < 0.0;
	}

	private void alignHeadingTo(int[] spot) {
		double dx = spot[0] + 0.5 - this.x;
		double dz = spot[2] + 0.5 - this.z;
		double len = Math.sqrt(dx * dx + dz * dz);
		if (len < 1.0E-4) {
			return;
		}
		fleeHeadX = dx / len;
		fleeHeadZ = dz / len;
		headingLock = HEADING_LOCK;
	}

	private void turnHeadingToward(double tx, double tz, double maxTurnCos) {
		double dot = fleeHeadX * tx + fleeHeadZ * tz;
		if (dot >= maxTurnCos) {
			fleeHeadX = tx;
			fleeHeadZ = tz;
			return;
		}

		double bx = fleeHeadX + (tx - fleeHeadX) * 0.5;
		double bz = fleeHeadZ + (tz - fleeHeadZ) * 0.5;
		double n = Math.sqrt(bx * bx + bz * bz);
		if (n <= 1.0E-4) {

			bx = -fleeHeadZ;
			bz = fleeHeadX;
			n = Math.sqrt(bx * bx + bz * bz);
		}
		fleeHeadX = bx / n;
		fleeHeadZ = bz / n;
	}

	private int[] blockingCellInArc(double hx, double hz, int feetY) {
		double base = Math.atan2(hz, hx);
		int[] best = null;
		double bestOffset = Double.MAX_VALUE;
		for (int step = -ARC_STEPS; step <= ARC_STEPS; step++) {
			double offset = step * ARC_STEP_RAD;
			double a = base + offset;
			double dx = Math.cos(a), dz = Math.sin(a);
			for (double reach = 0.65; reach <= 1.35; reach += 0.35) {
				int cx = MathHelper.floor(this.x + dx * reach);
				int cz = MathHelper.floor(this.z + dz * reach);
				if (cx == MathHelper.floor(this.x) && cz == MathHelper.floor(this.z)) {
					continue;
				}
				if (!FoolPathfinder.blocksMotion(this.world, cx, feetY, cz)
						&& !FoolPathfinder.blocksMotion(this.world, cx, feetY + 1, cz)) {
					continue;
				}
				if (Math.abs(offset) < bestOffset) {
					bestOffset = Math.abs(offset);
					best = new int[]{cx, feetY, cz};
				}
				break;
			}
		}
		return best;
	}

	private static final int ARC_STEPS = 5;
	private static final double ARC_STEP_RAD = Math.toRadians(18);

	private void deflectHeadingAroundObstacle() {
		double base = Math.atan2(fleeHeadZ, fleeHeadX);
		double bestScore = -Double.MAX_VALUE;
		double bestX = fleeHeadX, bestZ = fleeHeadZ;
		for (int step = -DEFLECT_STEPS; step <= DEFLECT_STEPS; step++) {
			if (step == 0) {
				continue;
			}
			double a = base + step * DEFLECT_STEP_RAD;
			double dx = Math.cos(a), dz = Math.sin(a);

			double score = openDistanceAlong(dx, dz) - Math.abs(step) * 0.55;
			if (score > bestScore) {
				bestScore = score;
				bestX = dx;
				bestZ = dz;
			}
		}
		fleeHeadX = bestX;
		fleeHeadZ = bestZ;
		headingLock = HEADING_LOCK;
		fleeBlockedTicks = 0;
	}

	private static final int DEFLECT_STEPS = 5;
	private static final double DEFLECT_STEP_RAD = Math.toRadians(30);

	private double openDistanceAlong(double dx, double dz) {
		int fy = MathHelper.floor(this.bb.minY);
		for (int i = 1; i <= 8; i++) {
			int x = MathHelper.floor(this.x + dx * i);
			int z = MathHelper.floor(this.z + dz * i);
			if (!FoolPathfinder.chunkLoaded(this.world, x, z)) return i;
			if (FoolPathfinder.blocksMotion(this.world, x, fy, z)
					|| FoolPathfinder.blocksMotion(this.world, x, fy + 1, z)) {
				return i;
			}
		}
		return 8;
	}

	private void sprintAlongHeading() {
		if (fleeHeadX == 0.0 && fleeHeadZ == 0.0) {
			return;
		}
		if (this.horizontalCollision) {
			if (++fleeBlockedTicks > 8) {
				deflectHeadingAroundObstacle();
			}
		} else if (fleeBlockedTicks > 0) {
			fleeBlockedTicks--;
		}
		clearPath();
		steerTowardPoint(this.x + fleeHeadX * 10.0, this.z + fleeHeadZ * 10.0, this.moveSpeed);
		directSteer = true;
	}

	private boolean escapeWaterTick() {
		if (!this.isInWater()) {
			return false;
		}
		boolean headUnder = FoolPathfinder.isWater(this.world,
				MathHelper.floor(this.x), MathHelper.floor(this.y + this.getHeadHeight()), MathHelper.floor(this.z));
		boolean footing = FoolPathfinder.isStandable(this.world,
				MathHelper.floor(this.x), MathHelper.floor(this.bb.minY), MathHelper.floor(this.z));
		if (!headUnder && footing) {
			return false;
		}
		this.isJumping = true;
		this.speed = FLEE_SPEED_PANIC;

		if (swimCommit > 0) swimCommit--;
		if (swimTarget != null && (swimCommit <= 0 || !stillWater(swimTarget))) {
			swimTarget = null;
		}
		if (swimTarget == null && swimCommit <= 0) {
			swimTarget = nearestDryLand(24);
			swimCommit = 60;
		}
		if (swimTarget != null) {
			steerTowardPoint(swimTarget[0] + 0.5, swimTarget[2] + 0.5, this.moveSpeed);
		} else if (fleeHeadX != 0.0 || fleeHeadZ != 0.0) {

			steerTowardPoint(this.x + fleeHeadX * 10.0, this.z + fleeHeadZ * 10.0, this.moveSpeed);
		} else {
			steerTowardPoint(this.x + 10.0, this.z, this.moveSpeed);
		}

		clearPath();
		this.directSteer = false;
		return true;
	}

	private boolean stillWater(int[] cell) {
		return FoolPathfinder.isStandable(this.world, cell[0], cell[1], cell[2])
				&& !FoolPathfinder.isWater(this.world, cell[0], cell[1], cell[2]);
	}

	private int[] nearestDryLand(int r) {
		int bx = MathHelper.floor(this.x), by = MathHelper.floor(this.bb.minY), bz = MathHelper.floor(this.z);
		int[] best = null;
		double bestSq = Double.MAX_VALUE;
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				int x = bx + dx, z = bz + dz;
				if (!FoolPathfinder.chunkLoaded(this.world, x, z)) continue;
				for (int dy = -4; dy <= 1; dy++) {
					int y = by + dy;
					if (!FoolPathfinder.isStandable(this.world, x, y, z)) continue;
					if (!FoolPathfinder.isClear(this.world, x, y, z)) continue;
					if (FoolPathfinder.isWater(this.world, x, y, z)) continue;
					double d = dx * dx + dy * dy + dz * dz;
					if (d < bestSq) {
						bestSq = d;
						best = new int[]{x, y, z};
					}
				}
			}
		}
		return best;
	}

	private void maybeShootArrow(Player target) {
		if (target == null || arrowCooldown > 0 || this.world == null || this.world.isClientSide) {
			return;
		}
		double dist = target.distanceTo(this);
		if (dist < ARROW_MIN_RANGE || dist > ARROW_MAX_RANGE) {
			return;
		}
		if (!canEntityBeSeen(target) || this.random.nextInt(ARROW_CHANCE) != 0) {
			return;
		}
		double dx = target.x - this.x;
		double dz = target.z - this.z;
		double flat = Math.sqrt(dx * dx + dz * dz);
		if (flat < 1.0E-3) {
			return;
		}
		double dy = (target.y + target.getHeadHeight() * 0.65) - (this.y + this.getHeadHeight());

		double sideX = -dz / flat;
		double sideZ = dx / flat;
		double miss = (this.random.nextBoolean() ? 1.0 : -1.0) * (0.9 + this.random.nextDouble() * 0.7);

		ProjectileArrow arrow = new ProjectileArrow(this.world,
				this.x, this.y + this.getHeadHeight() - 0.1, this.z, ProjectileArrow.TYPE_NORMAL);
		arrow.owner = this;
		arrow.damage = 0;
		arrow.setHeading(dx + sideX * miss, dy + 0.12, dz + sideZ * miss, 1.5f, 0.6f);
		this.world.entityJoinedWorld(arrow);
		this.world.playSoundAtEntity(null, this, "random.bow", 0.9f, 1.3f);
		lookAtEntity(target);
		swing();
		arrowCooldown = ARROW_COOLDOWN_MIN + this.random.nextInt(ARROW_COOLDOWN_VAR);
	}

	private void punchIfCrowded(Player near) {
		if (near == null || punchCooldown > 0) {
			return;
		}
		double dx = near.x - this.x, dz = near.z - this.z;
		if (dx * dx + dz * dz > PUNCH_RANGE * PUNCH_RANGE) {
			return;
		}
		if (Math.abs(near.y - this.y) > STRIKE_VERTICAL) {
			return;
		}
		near.hurt(this, STRIKE_DAMAGE, DamageType.COMBAT);
		swing();

		if (!fleeing) {
			lookAtEntity(near);
		}

		punchCooldown = PUNCH_COOLDOWN;
		beginFlee(near);
	}

	private boolean farEnoughToVanish() {
		Player p = this.world.getClosestPlayerToEntity(this, -1.0);
		return p == null || p.distanceTo(this) > VANISH_MIN_DIST;
	}

	public boolean isWatched() {
		for (Player p : this.world.players) {
			if (isWatchedBy(p)) {
				return true;
			}
		}
		return false;
	}

	public boolean isWatchedBy(Player p) {
		if (p == null || p.removed) {
			return false;
		}
		if (p.distanceToSqr(this) > WATCH_RANGE * WATCH_RANGE) {
			return false;
		}
		if (!inViewCone(p)) {
			return false;
		}
		return bodyVisibleTo(p);
	}

	private boolean bodyVisibleTo(Player p) {
		double ex = p.x, ey = p.y + p.getHeadHeight(), ez = p.z;
		double base = this.bb.minY;

		for (double h : CHECK_HEIGHTS) {
			if (clearLineTo(ex, ey, ez, this.x, base + h, this.z)) {
				return true;
			}
		}

		for (double h : CHECK_HEIGHTS) {
			for (double[] o : CHECK_OFFSETS) {
				if (clearLineTo(ex, ey, ez, this.x + o[0], base + h, this.z + o[1])) {
					return true;
				}
			}
		}
		return false;
	}

	private static final double[] CHECK_HEIGHTS = {0.15, 0.95, 1.70};

	private static final double[][] CHECK_OFFSETS = {{0.29, 0.0}, {-0.29, 0.0}, {0.0, 0.29}, {0.0, -0.29}};

	private boolean inViewCone(Player p) {
		Vector3dc look = p.getViewVector(1.0f);
		if (look == null) {
			return true;
		}
		double dx = this.x - p.x;
		double dy = (this.y + this.getHeadHeight()) - (p.y + p.getHeadHeight());
		double dz = this.z - p.z;
		double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (len < 1.0E-4) {
			return true;
		}
		double dot = (dx * look.x() + dy * look.y() + dz * look.z()) / len;
		return dot > WATCH_FOV_DOT;
	}

	private int[] pickCoverSpot(Player from) {
		if (from == null) {
			return null;
		}
		double eyeX = from.x, eyeY = from.y + from.getHeadHeight(), eyeZ = from.z;
		int[] best = null;
		double bestScore = -Double.MAX_VALUE;
		int fx = MathHelper.floor(this.x);
		int fy = MathHelper.floor(this.bb.minY);
		int fz = MathHelper.floor(this.z);
		double curDist = Math.sqrt((this.x - from.x) * (this.x - from.x) + (this.z - from.z) * (this.z - from.z));

		for (int dir = 0; dir < 16; dir++) {
			double ang = dir * (Math.PI * 2.0 / 16.0);
			double dx = Math.cos(ang), dz = Math.sin(ang);
			for (int r = 5; r <= 17; r += 4) {
				int tx = fx + (int) Math.round(dx * r);
				int tz = fz + (int) Math.round(dz * r);
				if (!FoolPathfinder.chunkLoaded(this.world, tx, tz)) continue;
				int ty = groundAt(tx, fy, tz);
				if (ty == Integer.MIN_VALUE) continue;

				if (isPlacedBlock(tx, ty - 1, tz)) continue;
				boolean hidden = !clearLineTo(eyeX, eyeY, eyeZ, tx + 0.5, ty + 1.0, tz + 0.5);
				boolean incumbent = coverSpot != null
						&& coverSpot[0] == tx && coverSpot[1] == ty && coverSpot[2] == tz;

				if (!hidden) {
					continue;
				}
				double score = coverScore(this.x, this.bb.minY, this.z, from.x, from.z, fleeHeadX, fleeHeadZ,
						tx + 0.5, ty, tz + 0.5, curDist, r, hidden, incumbent);
				if (score > bestScore) {
					bestScore = score;
					best = new int[]{tx, ty, tz};
				}
			}
		}
		return best;
	}

	public static double coverScore(double foolX, double foolY, double foolZ, double playerX, double playerZ,
			double headX, double headZ, double candX, double candY, double candZ,
			double curDistFromPlayer, int radius, boolean hidden, boolean incumbent) {

		double climb = candY - foolY;
		if (climb > MAX_FLEE_CLIMB || climb < -MAX_FLEE_DROP) {
			return Double.NEGATIVE_INFINITY;
		}
		double dirX = candX - foolX;
		double dirZ = candZ - foolZ;
		double dirLen = Math.sqrt(dirX * dirX + dirZ * dirZ);
		if (dirLen < 1.0E-4) {
			return Double.NEGATIVE_INFINITY;
		}
		dirX /= dirLen;
		dirZ /= dirLen;

		if (headX != 0.0 || headZ != 0.0) {
			if (dirX * headX + dirZ * headZ < 0.5) {
				return Double.NEGATIVE_INFINITY;
			}
		}

		double candDist = Math.sqrt((candX - playerX) * (candX - playerX) + (candZ - playerZ) * (candZ - playerZ));
		if (candDist < curDistFromPlayer - 2.0) {
			return Double.NEGATIVE_INFINITY;
		}
		double alignment = (headX == 0.0 && headZ == 0.0) ? 0.0 : dirX * headX + dirZ * headZ;

		return (hidden ? 800.0 : 0.0)
				+ candDist * 3.0
				+ alignment * 120.0
				- radius * 0.8
				- Math.abs(climb) * 25.0
				+ (incumbent ? COVER_INCUMBENT_BONUS : 0.0);
	}

	private boolean clearLineTo(double x1, double y1, double z1, double x2, double y2, double z2) {
		return this.world.checkBlockCollisionBetweenPoints(
				new Vector3d(x1, y1, z1), new Vector3d(x2, y2, z2)) == null;
	}

	private void vaporise() {
		for (int i = 0; i < 30; i++) {
			this.world.spawnParticle("largesmoke",
					this.x + (this.random.nextDouble() - 0.5) * 0.8,
					this.bb.minY + this.random.nextDouble() * 1.9,
					this.z + (this.random.nextDouble() - 0.5) * 0.8,
					(this.random.nextDouble() - 0.5) * 0.14, 0.06, (this.random.nextDouble() - 0.5) * 0.14,
					0, false);
		}
		this.world.playSoundAtEntity(null, this, SOUND_HIT, 1.0f, 0.6f);

		this.remove();
	}

	public boolean isWardedCell(int x, int y, int z) {
		return JoxeWard.isWarded(this.world, x, y, z);
	}

	private void vanish() {
		for (int i = 0; i < 18; i++) {
			this.world.spawnParticle("largesmoke",
					this.x + (this.random.nextDouble() - 0.5) * 0.7,
					this.bb.minY + this.random.nextDouble() * 1.8,
					this.z + (this.random.nextDouble() - 0.5) * 0.7,
					(this.random.nextDouble() - 0.5) * 0.08, 0.02, (this.random.nextDouble() - 0.5) * 0.08, 0, false);
		}

		this.remove();
	}

	private void beginStrike(Player target) {
		striking = true;
		strikeTicks = 0;
		strikeTarget = target;
		act = null;
		clearPath();
	}

	private void strikeTick(Player target) {
		if (target == null || target.removed || ++strikeTicks > STRIKE_TIMEOUT) {
			striking = false;
			strikeCooldown = STRIKE_COOLDOWN / 2;
			beginFlee(target);
			return;
		}

		double dx = target.x - this.x;
		double dz = target.z - this.z;
		double d = Math.sqrt(dx * dx + dz * dz);
		double dy = Math.abs(target.y - this.y);

		if (d > STRIKE_COMMIT && isWatchedBy(target)) {
			striking = false;
			strikeCooldown = STRIKE_COOLDOWN / 2;
			beginFlee(target);
			return;
		}
		this.speed = FLEE_SPEED;
		setSprinting(true);
		lookAtEntity(target);
		if (d <= STRIKE_RANGE && dy <= STRIKE_VERTICAL) {
			target.hurt(this, STRIKE_DAMAGE, DamageType.COMBAT);
			swing();
			striking = false;
			strikeCooldown = STRIKE_COOLDOWN;
			beginFlee(target);
			return;
		}
		if (d < 7.0) {
			approachEntity(target);
		} else {
			navigateTo(MathHelper.floor(target.x), MathHelper.floor(target.bb.minY), MathHelper.floor(target.z), 1.5);
		}
	}

	public boolean navigateTo(int x, int y, int z, double acceptRadius) {
		return navigateTo(x, y, z, acceptRadius, true, true);
	}

	public boolean navigateTo(int x, int y, int z, double acceptRadius, boolean canPlace, boolean mayBreak) {
		long goal = navKey(x, y, z);
		if (goal == navGoalKey && navPath != null && navIndex < navPath.size()) {
			return true;
		}
		if (goal == navFailGoal && this.tickCount < navFailUntil) {
			return false;
		}
		int sx = MathHelper.floor(this.x);
		int sy = MathHelper.floor(this.bb.minY);
		int sz = MathHelper.floor(this.z);
		if (navSearch == null || navSearchGoal != goal) {
			navSearch = FoolPathfinder.beginSearch(this, pathfinderState(), sx, sy, sz, x, y, z,
					acceptRadius, NAV_NODES, canPlace && placeFailCooldown <= 0, mayBreak, currentAvoid());
			navSearchGoal = goal;
			navSearchTicks = 0;
		}

		navSearchTicks++;
		if (!navSearch.advance(NAV_SLICE) && navSearchTicks < NAV_MAX_SEARCH_TICKS) {
			return true;
		}
		if (!navSearch.isDone()) {
			navSearch.advance(Integer.MAX_VALUE);
		}
		navGoalKey = goal;
		navPath = navSearch.result();
		navSearch = null;
		navSearchGoal = Long.MIN_VALUE;
		navIndex = 0;
		navStuckTicks = 0;
		navLastX = this.x;
		navLastY = this.y;
		navLastZ = this.z;
		boolean ok = navPath != null && !navPath.isEmpty();
		if (!ok) {
			navFailGoal = goal;
			navFailUntil = this.tickCount + NAV_FAIL_BACKOFF;
		}
		return ok;
	}

	private void navigationTick() {
		this.moveStrafing = 0.0f;
		this.isJumping = false;
		if (placeFailCooldown > 0) placeFailCooldown--;

		if (directSteer) {
			directSteer = false;
			int feetY = MathHelper.floor(this.bb.minY + 0.5);

			double hx = fleeHeadX, hz = fleeHeadZ;
			if (hx == 0.0 && hz == 0.0) {
				double yaw = Math.toRadians(this.yRot);
				hx = -Math.sin(yaw);
				hz = Math.cos(yaw);
			}

			int[] blocker = blockingCellInArc(hx, hz, feetY);
			int ax = blocker != null ? blocker[0] : MathHelper.floor(this.x + hx * 0.7);
			int az = blocker != null ? blocker[2] : MathHelper.floor(this.z + hz * 0.7);

			boolean lowBlocked = FoolPathfinder.blocksMotion(this.world, ax, feetY, az);
			boolean highBlocked = FoolPathfinder.blocksMotion(this.world, ax, feetY + 1, az);
			boolean ownHeadClear = !FoolPathfinder.blocksMotion(this.world,
					MathHelper.floor(this.x), feetY + 2, MathHelper.floor(this.z));

			boolean hoppable = lowBlocked && !highBlocked && ownHeadClear;

			if (lowBlocked || highBlocked) {
				if (!startBreakingIfObstructed(ax, feetY, az)) {
					startBreakingIfObstructed(ax, feetY + 1, az);
				}
			}

			fleeHop(lowBlocked && !hoppable);
			if ((this.isInWater() || this.isInLava()) && this.random.nextFloat() < 0.8f) {
				this.isJumping = true;
			}

			stuckCheck();
			if (navStuckTicks > 20) {
				navStuckTicks = 0;
				deflectHeadingAroundObstacle();
			}
			return;
		}
		if (navPath == null) {
			this.moveForward = 0.0f;
			return;
		}

		updateDoorClosing();

		while (navIndex < navPath.size()) {
			int[] n = navPath.get(navIndex);

			if (n[3] != FoolPathfinder.MOVE) break;
			boolean isLast = navIndex == navPath.size() - 1;
			double r = isLast ? 0.8 : 0.6;
			double ddx = n[0] + 0.5 - this.x;
			double ddz = n[2] + 0.5 - this.z;
			double distSq = ddx * ddx + ddz * ddz;
			double yGap = this.bb.minY - (double) n[1];

			boolean yMatch = n[1] > this.bb.minY + 0.05 ? yGap > -0.5 : Math.abs(yGap) < 1.1;

			boolean fellPast = distSq < r * r && this.bb.minY < n[1] - 1.05
					&& !isLast && navPath.get(navIndex + 1)[1] <= MathHelper.floor(this.bb.minY + 1.1);
			if ((distSq < r * r && yMatch) || fellPast) {
				navIndex++;
			} else if (!isLast) {
				int[] next = navPath.get(navIndex + 1);
				double ndx = next[0] + 0.5 - this.x;
				double ndz = next[2] + 0.5 - this.z;

				if (ndx * ndx + ndz * ndz < distSq && Math.abs(this.bb.minY - (double) next[1]) < 0.1) {
					navIndex++;
				} else {
					break;
				}
			} else {
				break;
			}
		}
		if (navIndex >= navPath.size()) {
			navPath = null;
			navGoalKey = Long.MIN_VALUE;
			this.moveForward = 0.0f;
			return;
		}

		int[] node = navPath.get(navIndex);
		if (node[3] == FoolPathfinder.PILLAR) {
			pillarTick(node);
			return;
		}
		if (node[3] == FoolPathfinder.BRIDGE) {
			bridgeTick(node);
			return;
		}

		int feetY = MathHelper.floor(this.bb.minY + 0.5);

		if (openDoorsForMove(node, feetY)) {
			this.moveForward = 0.0f;
			return;
		}

		if (startBreakingIfObstructed(node[0], node[1], node[2])
				|| startBreakingIfObstructed(node[0], node[1] + 1, node[2])) {
			return;
		}

		if (node[1] > feetY && startBreakingIfObstructed(
				MathHelper.floor(this.x), feetY + 2, MathHelper.floor(this.z))) {
			return;
		}

		steerToward(node[0], node[2], this.moveSpeed);

		if (this.isInWater() && !this.onGround) {
			this.isJumping = true;
		}

		double dirX = node[0] + 0.5 - this.x;
		double dirZ = node[2] + 0.5 - this.z;
		double dirLen = Math.sqrt(dirX * dirX + dirZ * dirZ);
		int stepX = 0, stepZ = 0;
		if (dirLen > 1.0E-4) {
			if (Math.abs(dirX) / dirLen > 0.38) stepX = dirX > 0.0 ? 1 : -1;
			if (Math.abs(dirZ) / dirLen > 0.38) stepZ = dirZ > 0.0 ? 1 : -1;
		}
		int fx = MathHelper.floor(this.x) + stepX;
		int fz = MathHelper.floor(this.z) + stepZ;
		boolean forwardFeetSolid = (stepX != 0 || stepZ != 0) && FoolPathfinder.blocksMotion(this.world, fx, feetY, fz);
		boolean forwardHeadSolid = (stepX != 0 || stepZ != 0) && FoolPathfinder.blocksMotion(this.world, fx, feetY + 1, fz);

		boolean ownHeadClear = !FoolPathfinder.blocksMotion(this.world, MathHelper.floor(this.x), feetY + 2, MathHelper.floor(this.z));
		boolean forwardClearAbove = !forwardHeadSolid && !FoolPathfinder.blocksMotion(this.world, fx, feetY + 2, fz);

		boolean hopOver = this.horizontalCollision && forwardFeetSolid && forwardClearAbove && ownHeadClear
				&& !FoolPathfinder.isFenceLike(this.world, fx, feetY, fz);
		boolean nodeClearAbove = !FoolPathfinder.blocksMotion(this.world, node[0], node[1] + 1, node[2]);
		boolean stepUp = node[1] > feetY && ownHeadClear && nodeClearAbove;

		boolean snagged = this.horizontalCollision && navStuckTicks > 6;
		boolean wantJump = !snagged && (stepUp || hopOver);
		if (wantJump && this.horizontalCollision) {
			if (++hopBlockedTicks > MAX_HOP_BLOCKED) {
				wantJump = false;
			}
		} else if (hopBlockedTicks > 0) {

			hopBlockedTicks--;
		}

		if (wantJump) {
			this.isJumping = true;
			blockedTicks = 0;
		} else if (this.horizontalCollision && (forwardFeetSolid || snagged)) {
			if (++blockedTicks > 10) {
				blockedTicks = 0;
				handleWedged(fx, feetY, fz);
				return;
			}
		} else if (!this.horizontalCollision) {
			blockedTicks = 0;
		}

		fleeHop(stepUp || hopOver);

		if (ownHeadClear && (this.isInWater() || this.isInLava()) && this.random.nextFloat() < 0.8f) {
			this.isJumping = true;
		}
		stuckCheck();
	}

	private void fleeHop(boolean climbing) {
		if (!fleeing || !this.onGround || this.isInWater() || climbing) {
			return;
		}
		int feetY = MathHelper.floor(this.bb.minY + 0.5);
		if (FoolPathfinder.blocksMotion(this.world, MathHelper.floor(this.x), feetY + 2, MathHelper.floor(this.z))) {
			return;
		}
		this.isJumping = true;
	}

	private void handleWedged(int fx, int feetY, int fz) {
		if (startBreakingIfObstructed(fx, feetY, fz) || startBreakingIfObstructed(fx, feetY + 1, fz)) {
			return;
		}
		avoidCell(fx, feetY, fz);
		avoidCell(fx, feetY + 1, fz);
		clearPath();
	}

	private boolean startBreakingIfObstructed(int x, int y, int z) {
		if (this.world == null || this.world.isClientSide) {
			return false;
		}
		if (!FoolPathfinder.blocksMotion(this.world, x, y, z)) {
			return false;
		}
		if (!mayBreakCell(x, y, z) || !withinReach(x, y, z) || !canSeeBlock(x, y, z)) {
			return false;
		}

		this.moveForward = 0.0f;
		if (blockActionCooldown > 0) {
			return true;
		}
		faceBlock(x, y, z);
		breakAndPocket(x, y, z);
		return true;
	}

	public boolean canBlockAction() {
		return blockActionCooldown <= 0;
	}

	private void spendBlockAction() {
		blockActionCooldown = BLOCK_ACTION_GAP;
	}

	public boolean mayBreakCell(int x, int y, int z) {
		if (FoolPathfinder.isOperableDoor(this.world, x, y, z)) {
			return false;
		}
		if (FoolPathfinder.isFenceLike(this.world, x, y, z)) {
			return false;
		}
		if (!FoolPathfinder.isBreakable(this.world, x, y, z)) {
			return false;
		}

		return isPlacedBlock(x, y, z) || !FoolPathfinder.isPlayerBuilt(this.world, x, y, z);
	}

	private void pillarTick(int[] node) {
		this.moveForward = 0.0f;
		int px = node[0];
		int pz = node[2];

		int floorY = node[1] - 1;

		if (FoolPathfinder.isSolid(this.world, px, floorY, pz)) {

			if (this.onGround) {
				this.isJumping = true;
			}
			placeFails = 0;
			return;
		}

		double dx = px + 0.5 - this.x;
		double dz = pz + 0.5 - this.z;
		if (dx * dx + dz * dz > 0.36 && this.onGround) {
			steerTowardPoint(px + 0.5, pz + 0.5, this.moveSpeed);
			return;
		}

		if (this.bb.minY >= floorY + 0.95) {
			if (placeWoolBeneath(px, floorY, pz)) {
				placeFails = 0;
			} else if (++placeFails > 30) {
				placeFails = 0;
				placeFailCooldown = 60;
				clearPath();
			}
			return;
		}
		if (this.onGround && blockActionCooldown <= 2) {
			this.isJumping = true;
		}
	}

	private void bridgeTick(int[] node) {
		int bx = node[0];
		int bz = node[2];
		int floorY = node[1] - 1;
		if (FoolPathfinder.isSolid(this.world, bx, floorY, bz)) {
			placeFails = 0;
			steerToward(bx, bz, this.moveSpeed * 0.7f);
			return;
		}
		if (!withinReach(bx, floorY, bz)) {
			steerToward(bx, bz, this.moveSpeed * 0.5f);
			return;
		}
		this.moveForward = 0.0f;
		faceBlock(bx, floorY, bz);
		if (placeWool(bx, floorY, bz)) {
			placeFails = 0;
		} else if (++placeFails > 30) {
			placeFails = 0;
			placeFailCooldown = 60;
			clearPath();
		}
	}

	public boolean isPlacedBlock(int x, int y, int z) {
		return placedBlocks.contains(FoolPathfinder.packKey(x, y, z));
	}

	private void stuckCheck() {
		double dx = this.x - navLastX, dy = this.y - navLastY, dz = this.z - navLastZ;
		if (dx * dx + dy * dy + dz * dz < 0.0016) {
			if (++navStuckTicks > 45) {
				navStuckTicks = 0;
				if (navPath != null && navIndex < navPath.size()) {
					int[] n = navPath.get(navIndex);
					avoidCell(n[0], n[1], n[2]);
				}
				clearPath();
			}
		} else {
			navStuckTicks = 0;
			navLastX = this.x;
			navLastY = this.y;
			navLastZ = this.z;
		}
	}

	private void avoidCell(int x, int y, int z) {
		avoidCells.put(FoolPathfinder.packKey(x, y, z), this.tickCount + AVOID_TTL);
	}

	private Set<Long> currentAvoid() {
		return avoidCells.isEmpty() ? null : new HashSet<>(avoidCells.keySet());
	}

	public void clearPath() {
		navPath = null;
		navSearch = null;
		navGoalKey = Long.MIN_VALUE;
		navSearchGoal = Long.MIN_VALUE;
		navIndex = 0;
	}

	private static long navKey(int x, int y, int z) {
		return FoolPathfinder.packKey(x, y, z);
	}

	public boolean hasPath() {
		return navPath != null;
	}

	public void stopMoving() {
		clearPath();
		this.moveForward = 0.0f;
		this.moveStrafing = 0.0f;
		this.isJumping = false;
	}

	public boolean isBreaking() {
		return false;
	}

	private void steerToward(int cx, int cz, float speed) {
		steerTowardPoint(cx + 0.5, cz + 0.5, speed);
	}

	public void approachEntity(Entity target) {
		clearPath();
		this.isJumping = false;
		this.directSteer = true;
		lookAtEntity(target);
		steerTowardPoint(target.x, target.z, this.moveSpeed);
	}

	private void steerTowardPoint(double tx, double tz, float speed) {
		double dx = tx - this.x;
		double dz = tz - this.z;
		double hdist = Math.sqrt(dx * dx + dz * dz);
		if (hdist < 0.25) {

			this.moveForward = 0.0f;
			return;
		}
		if (this.horizontalCollision && hdist < 0.6) {
			this.moveForward = speed;
			return;
		}
		float targetYaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
		float need = wrapDegrees(targetYaw - this.yRot);
		float turn = need * 0.18f;
		if (turn > 45.0f) turn = 45.0f;
		if (turn < -45.0f) turn = -45.0f;
		this.yRot += turn;

		this.yBodyRot += wrapDegrees(this.yRot - this.yBodyRot) * 0.30f;

		float align = Math.abs(need) > 90.0f ? 0.15f : Math.max(0.35f, 1.0f - Math.abs(need) / 120.0f);
		float near = hdist < 1.0 ? (float) Math.max(0.35, hdist) : 1.0f;
		this.moveForward = speed * align * near;
	}

	private static float wrapDegrees(float a) {
		while (a < -180.0f) a += 360.0f;
		while (a >= 180.0f) a -= 360.0f;
		return a;
	}

	public void lookAtEntity(Entity e) {
		lookToward(e.x, e.y + (e instanceof Player ? ((Player) e).getHeadHeight() : 0.0f), e.z);
	}

	public void lookToward(double tx, double ty, double tz) {
		lookedThisTick = true;
		double dx = tx - this.x;
		double dy = ty - (this.y + this.getHeadHeight());
		double dz = tz - this.z;
		double horiz = Math.sqrt(dx * dx + dz * dz);

		this.yRot = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
		this.xRot = (float) (-(Math.atan2(dy, horiz) * 180.0 / Math.PI));
	}

	public void faceBlock(int x, int y, int z) {
		lookToward(x + 0.5, y + 0.5, z + 0.5);
	}

	private boolean openDoorsForMove(int[] node, int feetY) {
		if (this.world == null || this.world.isClientSide) {
			return false;
		}
		int bx = MathHelper.floor(this.x);
		int bz = MathHelper.floor(this.z);
		int stepX = (int) Math.signum(node[0] + 0.5 - this.x);
		int stepZ = (int) Math.signum(node[2] + 0.5 - this.z);
		if (stepX != 0 && (tryOpenForPassage(bx + stepX, feetY, bz) || tryOpenForPassage(bx + stepX, feetY + 1, bz))) {
			return true;
		}
		if (stepZ != 0 && (tryOpenForPassage(bx, feetY, bz + stepZ) || tryOpenForPassage(bx, feetY + 1, bz + stepZ))) {
			return true;
		}
		if (node[1] < feetY && tryOpenForPassage(bx, feetY - 1, bz)) {
			return true;
		}
		if (node[1] > feetY && tryOpenForPassage(bx, feetY + 2, bz)) {
			return true;
		}
		return false;
	}

	private boolean tryOpenForPassage(int x, int y, int z) {
		if (!FoolPathfinder.isOperableDoor(this.world, x, y, z) || FoolPathfinder.isDoorOpen(this.world, x, y, z)) {
			return false;
		}
		if (!withinReach(x, y, z)) {
			return false;
		}
		if (!setDoorOpen(x, y, z, true)) {
			return false;
		}
		hasOpenedDoor = true;
		openedDoorX = x;
		openedDoorY = y;
		openedDoorZ = z;
		openedDoorEntryDX = this.x - (x + 0.5);
		openedDoorEntryDZ = this.z - (z + 0.5);
		return true;
	}

	private boolean setDoorOpen(int x, int y, int z, boolean open) {
		if (this.world == null || this.world.isClientSide) {
			return false;
		}
		if (!FoolPathfinder.isOperableDoor(this.world, x, y, z)
				|| FoolPathfinder.isDoorOpen(this.world, x, y, z) == open) {
			return false;
		}
		Block<?> block = this.world.getBlock(x, y, z);
		if (block == null) {
			return false;
		}
		faceBlock(x, y, z);
		block.onBlockRightClicked(this.world, x, y, z, null, Side.TOP, 0.5, 0.5);
		swing();
		return true;
	}

	private void updateDoorClosing() {
		if (!hasOpenedDoor) {
			return;
		}
		if (this.world == null || this.world.isClientSide
				|| !FoolPathfinder.isOperableDoor(this.world, openedDoorX, openedDoorY, openedDoorZ)) {
			hasOpenedDoor = false;
			return;
		}
		double dx = this.x - (openedDoorX + 0.5);
		double dz = this.z - (openedDoorZ + 0.5);
		double dy = (this.y + this.getHeadHeight()) - (openedDoorY + 0.5);
		if (dx * dx + dy * dy + dz * dz > 3.4 * 3.4) {
			hasOpenedDoor = false;
			return;
		}
		boolean crossed = dx * openedDoorEntryDX + dz * openedDoorEntryDZ < 0.0;
		if (crossed && Math.sqrt(dx * dx + dz * dz) > 0.6 && withinReach(openedDoorX, openedDoorY, openedDoorZ)) {
			setDoorOpen(openedDoorX, openedDoorY, openedDoorZ, false);
			hasOpenedDoor = false;
		}
	}

	private static final double REACH_SQ = 20.25;

	public boolean withinReach(int x, int y, int z) {
		double dx = x + 0.5 - this.x;
		double dy = y + 0.5 - (this.y + this.getHeadHeight());
		double dz = z + 0.5 - this.z;
		return dx * dx + dy * dy + dz * dz <= REACH_SQ;
	}

	public boolean canSeeBlock(int x, int y, int z) {
		double ex = this.x, ey = this.y + this.getHeadHeight(), ez = this.z;
		double cx = x + 0.5, cy = y + 0.5, cz = z + 0.5;
		HitResult hit = this.world.checkBlockCollisionBetweenPoints(
				new Vector3d(ex, ey, ez), new Vector3d(cx, cy, cz));
		if (hit == null) {
			return true;
		}
		Vector3dc at = hit.location;

		return at.x() >= x - 0.02 && at.x() <= x + 1.02
				&& at.y() >= y - 0.02 && at.y() <= y + 1.02
				&& at.z() >= z - 0.02 && at.z() <= z + 1.02;
	}

	public boolean arrivedAt(int[] cell, double radius) {
		double dx = cell[0] + 0.5 - this.x;
		double dz = cell[2] + 0.5 - this.z;
		return dx * dx + dz * dz <= radius * radius;
	}

	public boolean breakAndPocket(int x, int y, int z) {
		if (blockActionCooldown > 0) {
			return false;
		}
		Block<?> block = this.world.getBlock(x, y, z);
		if (block == null) {
			return false;
		}
		int meta = this.world.getBlockMetadata(x, y, z);
		TileEntity te = this.world.getTileEntity(x, y, z);
		ItemStack[] drops = block.getBreakResult(this.world, EnumDropCause.PROPER_TOOL, x, y, z, meta, te);
		if (!this.world.setBlockWithNotify(x, y, z, 0)) {
			return false;
		}
		placedBlocks.remove(FoolPathfinder.packKey(x, y, z));
		this.world.playBlockSoundEffect(null, x + 0.5, y + 0.5, z + 0.5, block, EnumBlockSoundEffectType.DIG);
		if (drops != null) {
			for (ItemStack stack : drops) {
				if (stack != null && stack.stackSize > 0) {
					pocket(stack.copy());
				}
			}
		}
		spendBlockAction();
		swing();
		return true;
	}

	public boolean placeWool(int x, int y, int z) {
		return placeWool(x, y, z, false);
	}

	public boolean placeWoolBeneath(int x, int y, int z) {
		return placeWool(x, y, z, true);
	}

	private boolean placeWool(int x, int y, int z, boolean allowSelfOverlap) {
		if (blockActionCooldown > 0) {
			return false;
		}
		if (this.world == null || FoolPathfinder.blocksMotion(this.world, x, y, z)) {
			return false;
		}
		if (!allowSelfOverlap && occupiesCell(x, y, z)) {
			return false;
		}
		int meta = (woolRed ? DyeColor.RED : DyeColor.BLUE).blockMeta;
		if (!this.world.setBlockAndMetadataWithNotify(x, y, z, Blocks.WOOL.id(), meta)) {
			return false;
		}

		woolRed = !woolRed;
		placedBlocks.add(FoolPathfinder.packKey(x, y, z));
		this.world.playBlockSoundEffect(null, x + 0.5, y + 0.5, z + 0.5, Blocks.WOOL, EnumBlockSoundEffectType.PLACE);
		spendBlockAction();
		faceBlock(x, y, z);
		swing();
		return true;
	}

	public boolean occupiesCell(int x, int y, int z) {
		return this.bb.maxX > x && this.bb.minX < x + 1
				&& this.bb.maxY > y && this.bb.minY < y + 1
				&& this.bb.maxZ > z && this.bb.minZ < z + 1;
	}

	public Container containerAt(int x, int y, int z) {
		if (this.world == null) {
			return null;
		}
		TileEntity te = this.world.getTileEntity(x, y, z);
		return te instanceof Container ? (Container) te : null;
	}

	public void pocket(ItemStack stack) {
		if (stack == null || stack.stackSize <= 0) {
			return;
		}
		for (ItemStack held : loot) {
			if (held.canStackWith(stack)) {
				int room = held.getMaxStackSize() - held.stackSize;
				int move = Math.min(room, stack.stackSize);
				held.stackSize += move;
				stack.stackSize -= move;
				if (stack.stackSize <= 0) {
					return;
				}
			}
		}
		loot.add(stack);
	}

	public int lootCount() {
		return loot.size();
	}

	public int[] findNearestBlock(Set<Integer> ids, int radius, int vertical, Set<Long> skip) {
		if (this.world == null || ids.isEmpty()) {
			return null;
		}
		int bx = MathHelper.floor(this.x);
		int by = MathHelper.floor(this.bb.minY);
		int bz = MathHelper.floor(this.z);
		int[] best = null;
		double bestSq = Double.MAX_VALUE;
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				int X = bx + dx, Z = bz + dz;
				if (!FoolPathfinder.chunkLoaded(this.world, X, Z)) continue;
				for (int dy = -vertical; dy <= vertical; dy++) {
					int Y = by + dy;
					if (Y < 1) continue;
					if (!ids.contains(this.world.getBlockId(X, Y, Z))) continue;
					if (skip != null && skip.contains(FoolPathfinder.packKey(X, Y, Z))) continue;
					double d = dx * dx + dy * dy + dz * dz;
					if (d < bestSq) {
						bestSq = d;
						best = new int[]{X, Y, Z};
					}
				}
			}
		}
		return best;
	}

	public int groundAt(int x, int nearY, int z) {
		for (int dy = 0; dy <= 8; dy++) {
			for (int s = 0; s < 2; s++) {
				int y = nearY + (s == 0 ? dy : -dy);
				if (y < 2) continue;
				if (FoolPathfinder.isStandable(this.world, x, y, z)
						&& FoolPathfinder.isClear(this.world, x, y, z)
						&& FoolPathfinder.isClear(this.world, x, y + 1, z)) {
					return y;
				}
			}
		}
		return Integer.MIN_VALUE;
	}

	public int[] randomNearbyGround(int r) {
		for (int attempt = 0; attempt < 12; attempt++) {
			double ang = this.random.nextDouble() * Math.PI * 2.0;
			int x = MathHelper.floor(this.x + Math.cos(ang) * r);
			int z = MathHelper.floor(this.z + Math.sin(ang) * r);
			if (!FoolPathfinder.chunkLoaded(this.world, x, z)) continue;
			int y = groundAt(x, MathHelper.floor(this.bb.minY), z);
			if (y != Integer.MIN_VALUE) {
				return new int[]{x, y, z};
			}
		}
		return null;
	}

	public java.util.Random rng() {
		return this.random;
	}

	@Override
	public void onLivingUpdate() {
		this.prevSwingProgress = this.swingProgress;
		super.onLivingUpdate();
		if (this.world != null && this.world.isClientSide) {

			int c = this.entityData.getInt(SWING_SLOT);
			if (c != this.swingSyncSeen) {
				this.swingSyncSeen = c;
				this.swingTicks = -1;
				this.swinging = true;
			}
		}
		if (this.swinging) {
			if (++this.swingTicks >= 8) {
				this.swingTicks = 0;
				this.swinging = false;
			}
		} else {
			this.swingTicks = 0;
		}
		this.swingProgress = (float) this.swingTicks / 8.0f;
	}

	public void swing() {
		if (!this.swinging || this.swingTicks >= 4) {
			this.swingTicks = -1;
			this.swinging = true;
			if (this.world != null && !this.world.isClientSide) {
				this.entityData.set(SWING_SLOT, ++swingSyncOut);
			}
		}
	}

	@Override
	public boolean hurt(Entity attacker, int damage, DamageType type) {
		boolean hit = super.hurt(attacker, damage, type);
		if (hit && attacker != null) {
			lastStruckByTick = this.tickCount;
		}
		if (hit && !this.world.isClientSide && attacker instanceof Player) {
			striking = false;
			strikeCooldown = STRIKE_COOLDOWN;
			beginFlee((Player) attacker);
		}
		return hit;
	}

	@Override
	protected void dropDeathItems() {
		super.dropDeathItems();
		if (this.tickCount - lastStruckByTick > KILL_CREDIT_TICKS) {
			loot.clear();
			return;
		}
		if (this.world != null && !this.world.isClientSide) {
			dropStack(new ItemStack(FoolItems.joxeDust, 1));
			if (this.random.nextInt(10) < 3) {
				if (this.random.nextInt(10) < 3) {
					dropStack(new ItemStack(FoolItems.foolsGoldIngot, 1));
				} else {
					dropStack(new ItemStack(FoolItems.joxeDust, 1));
				}
			}
		}
		spillLoot();
	}

	private void dropStack(ItemStack stack) {
		EntityItem item = new EntityItem(this.world, this.x, this.y + 0.5, this.z, stack);
		item.pickupDelay = 10;
		this.world.entityJoinedWorld(item);
	}

	private void spillLoot() {
		if (this.world == null || this.world.isClientSide) {
			return;
		}
		for (ItemStack stack : loot) {
			if (stack == null || stack.stackSize <= 0) continue;
			EntityItem item = new EntityItem(this.world, this.x, this.y + 0.5, this.z, stack.copy());
			item.pickupDelay = 10;
			this.world.entityJoinedWorld(item);
		}
		loot.clear();
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		ListTag lootTag = new ListTag();
		for (ItemStack stack : loot) {
			if (stack == null || stack.stackSize <= 0) continue;
			CompoundTag t = new CompoundTag();
			stack.writeToNBT(t);
			lootTag.addTag(t);
		}
		tag.put("FoolLoot", lootTag);
		tag.putBoolean("WoolRed", woolRed);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		loot.clear();
		ListTag lootTag = tag.getList("FoolLoot");
		for (int i = 0; i < lootTag.tagCount(); i++) {
			ItemStack stack = ItemStack.readItemStackFromNbt((CompoundTag) lootTag.tagAt(i));
			if (stack != null) {
				loot.add(stack);
			}
		}
		woolRed = tag.getBoolean("WoolRed");
	}

	@Override
	protected String getHurtSound() {
		return SOUND_HIT;
	}

	@Override
	protected String getDeathSound() {
		return SOUND_HIT;
	}

	@Override
	protected float getSoundVolume() {
		return 0.9f;
	}
}
