package btai.foolmod.entity;

import btai.foolmod.act.BlockDoorAct;
import btai.foolmod.act.BreakTorchAct;
import btai.foolmod.act.FoolAct;
import btai.foolmod.act.StealChestAct;
import btai.foolmod.act.WoolBuildAct;
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
import net.minecraft.core.enums.EnumBlockSoundEffectType;
import net.minecraft.core.enums.EnumDropCause;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.player.inventory.container.Container;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.DyeColor;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pos.TilePos;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class FoolEntity extends MobHuman {

	private static final float PROWL_SPEED = 0.105f;

	private static final float FLEE_SPEED = 0.135f;

	private static final double SENSE_RANGE = 24.0;
	private static final double WATCH_RANGE = 48.0;

	private static final double WATCH_FOV_DOT = 0.40;

	private static final double PANIC_RANGE = 6.5;

	private static final double STRIKE_RANGE = 3.6;

	private static final double STRIKE_VERTICAL = 2.0;
	private static final double STRIKE_APPROACH = 9.0;

	private static final double STRIKE_COMMIT = 4.5;
	private static final int STRIKE_COOLDOWN = 900;
	private static final int STRIKE_TIMEOUT = 100;
	private static final int STRIKE_DAMAGE = 2;

	private static final int COVER_MAX_AGE = 200;

	private static final int COVER_REPICK_GAP = 25;

	private static final double HEADING_EASE = 0.05;

	private static final double COVER_INCUMBENT_BONUS = 260.0;

	private static final int VANISH_UNSEEN_TICKS = 25;
	private static final double VANISH_MIN_DIST = 7.0;
	private static final int FLEE_MAX_TICKS = 1200;
	private static final int LEASH_DESPAWN = 96;

	private static final int MISCHIEF_GAP = 60;
	private static final int IDLE_SOUND_GAP = 220;

	private static final int NAV_NODES = 6000;
	private static final int NAV_SLICE = 3000;
	private static final int NAV_MAX_SEARCH_TICKS = 6;
	private static final int NAV_FAIL_BACKOFF = 40;
	private static final int MAX_HOP_BLOCKED = 14;
	private static final int AVOID_TTL = 400;

	private static final int SWING_SLOT = 3;

	public static final String SOUND_IDLE = "foolmod:mob.fool.idle";

	public static final String SOUND_HIT = "foolmod:mob.fool.hit";

	private final List<ItemStack> loot = new ArrayList<>();

	private FoolAct act;
	private int mischiefCooldown;
	private int idleSoundTimer;

	private boolean fleeing;
	private int fleeTicks;
	private int unseenTicks;
	private int[] coverSpot;
	private int coverAge;
	private int repickCooldown;

	private double fleeHeadX, fleeHeadZ;
	private Player fleeFrom;

	private boolean striking;
	private int strikeTicks;
	private int strikeCooldown;
	private Player strikeTarget;

	private int[] wanderSpot;
	private int wanderTicks;

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

	private boolean breaking;
	private int breakX, breakY, breakZ;
	private int breakTicksLeft;

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
		return this.world.checkIfAABBIsClear(this.bb)
				&& this.world.getCubes(this, this.bb).isEmpty()
				&& !this.world.getIsAnyLiquid(this.bb);
	}

	public static final int RARITY_ROLL = 900;

	public static final long SPAWN_COOLDOWN_TICKS = 20L * 60L * 25L;

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

		Player near = nearestVictim(SENSE_RANGE);

		if (leashCheck()) {
			return;
		}

		if (fleeing) {
			fleeTick();
			navigationTick();
			return;
		}

		if (striking) {
			Player quarry = (strikeTarget != null && !strikeTarget.removed) ? strikeTarget : near;
			strikeTick(quarry);
			navigationTick();
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
				return;
			}
			if (strikeCooldown <= 0 && d < STRIKE_APPROACH && canEntityBeSeen(near) && !isWatchedBy(near)
					&& this.random.nextInt(90) == 0) {
				beginStrike(near);
				navigationTick();
				return;
			}
		}

		this.speed = PROWL_SPEED;
		setSprinting(false);
		if (act != null) {
			if (act.tick(this)) {
				act = null;
				mischiefCooldown = MISCHIEF_GAP + this.random.nextInt(MISCHIEF_GAP);
			}
		} else if (mischiefCooldown <= 0) {
			chooseAct();
		} else {
			wanderTick();
		}
		navigationTick();
	}

	private void tickTimers() {
		if (mischiefCooldown > 0) mischiefCooldown--;
		if (strikeCooldown > 0) strikeCooldown--;
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
		double bestSq = range * range;
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

	private void chooseAct() {

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
		wanderTick();
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
			fleeHeadX = 0.0;
			fleeHeadZ = 0.0;
			act = null;
			striking = false;
			clearPath();
		}
		fleeFrom = from;
	}

	private void fleeTick() {
		this.speed = FLEE_SPEED;
		setSprinting(true);

		if (fleeFrom == null || fleeFrom.removed) {
			fleeFrom = nearestVictim(WATCH_RANGE);
		}

		if (isWatched()) {
			unseenTicks = 0;
		} else if (++unseenTicks >= VANISH_UNSEEN_TICKS && farEnoughToVanish()) {
			vanish();
			return;
		}

		if (++fleeTicks > FLEE_MAX_TICKS) {
			fleeing = false;
			setSprinting(false);
			this.speed = PROWL_SPEED;
			return;
		}
		if (fleeFrom != null && fleeFrom.distanceTo(this) > 30.0 && !isWatched()) {
			vanish();
			return;
		}

		updateFleeHeading();
		if (repickCooldown > 0) repickCooldown--;

		boolean stale = coverSpot == null || ++coverAge > COVER_MAX_AGE || arrivedAt(coverSpot, 2.0);
		if (stale && repickCooldown <= 0) {
			int[] pick = pickCoverSpot(fleeFrom);
			if (pick != null) {
				coverSpot = pick;
				coverAge = 0;
			} else if (coverSpot != null && arrivedAt(coverSpot, 2.0)) {
				coverSpot = null;
			}
			repickCooldown = COVER_REPICK_GAP;
		}

		if (coverSpot != null) {
			if (!navigateTo(coverSpot[0], coverSpot[1], coverSpot[2], 1.2)) {
				avoidCell(coverSpot[0], coverSpot[1], coverSpot[2]);
				coverSpot = null;
				repickCooldown = COVER_REPICK_GAP;
			}
			return;
		}
		sprintAlongHeading();
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
			return;
		}
		fleeHeadX += (ax - fleeHeadX) * HEADING_EASE;
		fleeHeadZ += (az - fleeHeadZ) * HEADING_EASE;
		double n = Math.sqrt(fleeHeadX * fleeHeadX + fleeHeadZ * fleeHeadZ);
		if (n > 1.0E-4) {
			fleeHeadX /= n;
			fleeHeadZ /= n;
		}
	}

	private void sprintAlongHeading() {
		if (fleeHeadX == 0.0 && fleeHeadZ == 0.0) {
			return;
		}
		int tx = MathHelper.floor(this.x + fleeHeadX * 14.0);
		int tz = MathHelper.floor(this.z + fleeHeadZ * 14.0);
		int ty = groundAt(tx, MathHelper.floor(this.bb.minY), tz);
		if (ty == Integer.MIN_VALUE || !navigateTo(tx, ty, tz, 2.5)) {

			steerTowardPoint(this.x + fleeHeadX * 8.0, this.z + fleeHeadZ * 8.0, this.moveSpeed);
			directSteer = true;
		}
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
		if (!canEntityBeSeen(p)) {
			return false;
		}
		return inViewCone(p);
	}

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
				boolean hidden = !clearLineTo(eyeX, eyeY, eyeZ, tx + 0.5, ty + 1.0, tz + 0.5);
				boolean incumbent = coverSpot != null
						&& coverSpot[0] == tx && coverSpot[1] == ty && coverSpot[2] == tz;
				double score = coverScore(this.x, this.z, from.x, from.z, fleeHeadX, fleeHeadZ,
						tx + 0.5, tz + 0.5, curDist, r, hidden, incumbent);
				if (score > bestScore) {
					bestScore = score;
					best = new int[]{tx, ty, tz};
				}
			}
		}
		return best;
	}

	public static double coverScore(double foolX, double foolZ, double playerX, double playerZ,
			double headX, double headZ, double candX, double candZ,
			double curDistFromPlayer, int radius, boolean hidden, boolean incumbent) {
		double dirX = candX - foolX;
		double dirZ = candZ - foolZ;
		double dirLen = Math.sqrt(dirX * dirX + dirZ * dirZ);
		if (dirLen < 1.0E-4) {
			return Double.NEGATIVE_INFINITY;
		}
		dirX /= dirLen;
		dirZ /= dirLen;

		if (headX != 0.0 || headZ != 0.0) {
			if (dirX * headX + dirZ * headZ < -0.1) {
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
				+ alignment * 40.0
				- radius * 0.8
				+ (incumbent ? COVER_INCUMBENT_BONUS : 0.0);
	}

	private boolean clearLineTo(double x1, double y1, double z1, double x2, double y2, double z2) {
		return this.world.checkBlockCollisionBetweenPoints(
				new Vector3d(x1, y1, z1), new Vector3d(x2, y2, z2)) == null;
	}

	private void vanish() {
		for (int i = 0; i < 18; i++) {
			this.world.spawnParticle("largesmoke",
					this.x + (this.random.nextDouble() - 0.5) * 0.7,
					this.bb.minY + this.random.nextDouble() * 1.8,
					this.z + (this.random.nextDouble() - 0.5) * 0.7,
					(this.random.nextDouble() - 0.5) * 0.08, 0.02, (this.random.nextDouble() - 0.5) * 0.08, 0, false);
		}
		spillLoot();
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
			this.world.playSoundAtEntity(null, this, SOUND_HIT, 1.0f, 1.0f);
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
		if (goal == navGoalKey && breaking) {
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
			fleeHop(false);
			if ((this.isInWater() || this.isInLava()) && this.random.nextFloat() < 0.8f) {
				this.isJumping = true;
			}
			return;
		}
		if (breaking) {
			this.moveForward = 0.0f;
			breakingTick();
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
		if (this.world == null || this.world.isClientSide || breaking) {
			return false;
		}
		if (!FoolPathfinder.blocksMotion(this.world, x, y, z)) {
			return false;
		}
		if (!mayBreakCell(x, y, z)) {
			return false;
		}
		if (!withinReach(x, y, z)) {
			return false;
		}
		Block<?> block = this.world.getBlock(x, y, z);
		if (block == null) {
			return false;
		}
		breaking = true;
		breakX = x;
		breakY = y;
		breakZ = z;
		breakTicksLeft = FoolPathfinder.mineTicks(block);
		return true;
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

	private void breakingTick() {
		if (this.world == null || this.world.isClientSide) {
			breaking = false;
			return;
		}

		if (!FoolPathfinder.blocksMotion(this.world, breakX, breakY, breakZ)
				|| !withinReach(breakX, breakY, breakZ) || !mayBreakCell(breakX, breakY, breakZ)) {
			breaking = false;
			return;
		}
		faceBlock(breakX, breakY, breakZ);
		if (this.tickCount % 4 == 0) {
			swing();
			Block<?> block = this.world.getBlock(breakX, breakY, breakZ);
			if (block != null) {
				this.world.playBlockSoundEffect(null, breakX + 0.5, breakY + 0.5, breakZ + 0.5,
						block, EnumBlockSoundEffectType.MINE);
			}
		}
		if (--breakTicksLeft > 0) {
			return;
		}
		breaking = false;
		breakAndPocket(breakX, breakY, breakZ);
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
		if (dx * dx + dz * dz > 0.09 && this.onGround) {
			steerTowardPoint(px + 0.5, pz + 0.5, this.moveSpeed * 0.6f);
			return;
		}

		if (this.bb.minY >= floorY + 0.55) {
			if (placeWool(px, floorY, pz)) {
				placeFails = 0;
			} else if (++placeFails > 30) {
				placeFails = 0;
				placeFailCooldown = 60;
				clearPath();
			}
			return;
		}
		if (this.onGround) {
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
		breaking = false;
		this.moveForward = 0.0f;
		this.moveStrafing = 0.0f;
		this.isJumping = false;
	}

	public boolean isBreaking() {
		return breaking;
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
		return clearLineTo(this.x, this.y + this.getHeadHeight(), this.z, x + 0.5, y + 0.5, z + 0.5)
				|| withinReach(x, y, z);
	}

	public boolean arrivedAt(int[] cell, double radius) {
		double dx = cell[0] + 0.5 - this.x;
		double dz = cell[2] + 0.5 - this.z;
		return dx * dx + dz * dz <= radius * radius;
	}

	public boolean breakAndPocket(int x, int y, int z) {
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
		swing();
		return true;
	}

	public boolean placeWool(int x, int y, int z) {
		if (this.world == null || FoolPathfinder.blocksMotion(this.world, x, y, z) || occupiesCell(x, y, z)) {
			return false;
		}
		int meta = (woolRed ? DyeColor.RED : DyeColor.BLUE).blockMeta;
		if (!this.world.setBlockAndMetadataWithNotify(x, y, z, Blocks.WOOL.id(), meta)) {
			return false;
		}

		woolRed = !woolRed;
		placedBlocks.add(FoolPathfinder.packKey(x, y, z));
		this.world.playBlockSoundEffect(null, x + 0.5, y + 0.5, z + 0.5, Blocks.WOOL, EnumBlockSoundEffectType.PLACE);
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
		spillLoot();
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
		return SOUND_IDLE;
	}

	@Override
	protected String getDeathSound() {
		return SOUND_IDLE;
	}

	@Override
	protected float getSoundVolume() {
		return 0.9f;
	}
}
