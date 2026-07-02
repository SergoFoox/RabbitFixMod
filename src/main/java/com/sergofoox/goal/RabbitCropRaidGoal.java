package com.sergofoox.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarrotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.Path;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public final class RabbitCropRaidGoal extends Goal {

    private static final int SEARCH_RADIUS = 16;
    private static final int SEARCH_HEIGHT = 2;
    private static final int FAILED_SEARCH_DELAY_TICKS = 100;
    private static final int SUCCESSFUL_RAID_DELAY_TICKS = 200;
    private static final int MAX_APPROACH_TICKS = 1200;
    private static final int PATH_RETRY_INTERVAL_TICKS = 10;
    private static final int STALL_RECOVERY_TICKS = 30;
    private static final double SPEED_MODIFIER = 0.7D;
    private static final double ARRIVAL_DISTANCE_SQUARED = 0.49D;
    private static final double STALL_RECOVERY_DISTANCE_SQUARED = 4.0D;
    private static final double MIN_PROGRESS_SQUARED = 0.0025D;

    private final Rabbit rabbit;
    private BlockPos cropPos;
    private Path initialPath;
    private int nextSearchTick;
    private int approachTicks;
    private int stalledTicks;
    private double lastDistanceSquared;
    private boolean cropEaten;

    public RabbitCropRaidGoal(Rabbit rabbit) {
        this.rabbit = rabbit;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.rabbit.tickCount < this.nextSearchTick || !mobGriefingEnabled()) {
            return false;
        }

        this.cropPos = findReachableCrop();
        if (this.cropPos == null) {
            this.nextSearchTick = this.rabbit.tickCount + FAILED_SEARCH_DELAY_TICKS;
            return false;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.cropEaten
                && this.approachTicks <= MAX_APPROACH_TICKS
                && this.cropPos != null
                && isMatureCarrot(this.cropPos);
    }

    @Override
    public void start() {
        this.approachTicks = 0;
        this.stalledTicks = 0;
        this.lastDistanceSquared = distanceToCropSqr(this.cropPos);
        this.cropEaten = false;
        this.rabbit.getNavigation().moveTo(this.initialPath, SPEED_MODIFIER);
    }

    @Override
    public void stop() {
        this.rabbit.getNavigation().stop();
        this.cropPos = null;
        this.initialPath = null;
    }

    @Override
    public void tick() {
        this.approachTicks++;
        this.rabbit.getLookControl().setLookAt(
                this.cropPos.getX() + 0.5D,
                this.cropPos.getY() + 0.5D,
                this.cropPos.getZ() + 0.5D,
                10.0F,
                this.rabbit.getMaxHeadXRot()
        );

        if (!hasArrived()) {
            double distanceSquared = distanceToCropSqr(this.cropPos);
            updateProgress(distanceSquared);

            if (this.rabbit.getNavigation().isDone()
                    || this.approachTicks % PATH_RETRY_INTERVAL_TICKS == 0) {
                moveToCrop();
            }

            if (this.stalledTicks >= STALL_RECOVERY_TICKS
                    && distanceSquared <= STALL_RECOVERY_DISTANCE_SQUARED
                    && this.rabbit.onGround()) {
                hopOverFinalBlockEdge();
            }
            return;
        }

        this.rabbit.getNavigation().stop();
        if (this.rabbit.onGround()) {
            eatCrop();
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private BlockPos findReachableCrop() {
        BlockPos origin = this.rabbit.blockPosition();
        List<BlockPos> crops = new ArrayList<>();

        for (int y = -SEARCH_HEIGHT; y <= SEARCH_HEIGHT; y++) {
            for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
                for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                    BlockPos candidate = origin.offset(x, y, z);
                    if (isMatureCarrot(candidate) && this.rabbit.isWithinHome(candidate)) {
                        crops.add(candidate);
                    }
                }
            }
        }

        crops.sort(Comparator.comparingDouble(this::distanceToCropSqr));
        for (BlockPos candidate : crops) {
            Path path = this.rabbit.getNavigation().createPath(candidate, 0);
            if (path != null) {
                this.initialPath = path;
                return candidate.immutable();
            }
        }

        return null;
    }

    private void moveToCrop() {
        this.rabbit.getNavigation().moveTo(
                this.cropPos.getX() + 0.5D,
                this.cropPos.getY(),
                this.cropPos.getZ() + 0.5D,
                0,
                SPEED_MODIFIER
        );
    }

    private void updateProgress(double distanceSquared) {
        if (distanceSquared + MIN_PROGRESS_SQUARED < this.lastDistanceSquared) {
            this.stalledTicks = 0;
        } else {
            this.stalledTicks++;
        }
        this.lastDistanceSquared = distanceSquared;
    }

    private void hopOverFinalBlockEdge() {
        this.rabbit.getMoveControl().setWantedPosition(
                this.cropPos.getX() + 0.5D,
                this.cropPos.getY(),
                this.cropPos.getZ() + 0.5D,
                SPEED_MODIFIER
        );
        this.rabbit.getJumpControl().jump();
        this.stalledTicks = 0;
    }

    private boolean hasArrived() {
        double x = this.rabbit.getX() - (this.cropPos.getX() + 0.5D);
        double y = this.rabbit.getY() - (this.cropPos.getY() + 0.5D);
        double z = this.rabbit.getZ() - (this.cropPos.getZ() + 0.5D);
        return x * x + y * y + z * z < ARRIVAL_DISTANCE_SQUARED;
    }

    private boolean isMatureCarrot(BlockPos pos) {
        Level level = this.rabbit.level();
        BlockState crop = level.getBlockState(pos);
        return level.getBlockState(pos.below()).is(BlockTags.SUPPORTS_CROPS)
                && crop.getBlock() instanceof CarrotBlock carrots
                && carrots.isMaxAge(crop);
    }

    private boolean mobGriefingEnabled() {
        return getServerLevel(this.rabbit).getGameRules().get(GameRules.MOB_GRIEFING);
    }

    private double distanceToCropSqr(BlockPos pos) {
        double x = this.rabbit.getX() - (pos.getX() + 0.5D);
        double y = this.rabbit.getY() - pos.getY();
        double z = this.rabbit.getZ() - (pos.getZ() + 0.5D);
        return x * x + y * y + z * z;
    }

    private void eatCrop() {
        Level level = this.rabbit.level();
        BlockState crop = level.getBlockState(this.cropPos);
        if (!(crop.getBlock() instanceof CarrotBlock carrots) || !carrots.isMaxAge(crop)) {
            return;
        }

        BlockState bittenCrop = crop.setValue(CarrotBlock.AGE, crop.getValue(CarrotBlock.AGE) - 1);
        level.setBlock(this.cropPos, bittenCrop, Block.UPDATE_CLIENTS);
        level.gameEvent(GameEvent.BLOCK_CHANGE, this.cropPos, GameEvent.Context.of(this.rabbit));
        level.levelEvent(2001, this.cropPos, Block.getId(crop));

        this.cropEaten = true;
        this.nextSearchTick = this.rabbit.tickCount + SUCCESSFUL_RAID_DELAY_TICKS;
    }
}
