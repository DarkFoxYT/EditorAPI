package net.dark.editorapi.model;

import java.util.UUID;
import net.dark.editorapi.scene.SceneObject;
import net.dark.editorapi.scene.SceneObjectType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class TriggerZone implements SceneObject {
    private final UUID id;
    private String name;
    private BlockPos pos1;
    private BlockPos pos2;
    private boolean showFill;
    private boolean triggerEnter;
    private boolean triggerExit;
    private boolean triggerWhileInside;
    private boolean triggerTimeInside;
    private int whileInsideIntervalTicks;
    private int requiredTimeTicks;
    private int delayTicks;
    private float radius;
    private TriggerOnceMode onceMode;
    private TriggerTargetMode targetMode;
    private UUID eventId;
    private UUID enterEventId;
    private UUID exitEventId;
    private boolean visible;
    private boolean locked;

    public TriggerZone(
            UUID id,
            String name,
            BlockPos pos1,
            BlockPos pos2,
            boolean showFill,
            boolean triggerEnter,
            boolean triggerExit,
            boolean triggerWhileInside,
            boolean triggerTimeInside,
            int whileInsideIntervalTicks,
            int requiredTimeTicks,
            int delayTicks,
            float radius,
            TriggerOnceMode onceMode,
            TriggerTargetMode targetMode,
            UUID eventId,
            UUID enterEventId,
            UUID exitEventId,
            boolean visible,
            boolean locked
    ) {
        this.id = id;
        this.name = name;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.showFill = showFill;
        this.triggerEnter = triggerEnter;
        this.triggerExit = triggerExit;
        this.triggerWhileInside = triggerWhileInside;
        this.triggerTimeInside = triggerTimeInside;
        this.whileInsideIntervalTicks = whileInsideIntervalTicks;
        this.requiredTimeTicks = requiredTimeTicks;
        this.delayTicks = delayTicks;
        this.radius = radius;
        this.onceMode = onceMode;
        this.targetMode = targetMode;
        this.eventId = eventId;
        this.enterEventId = enterEventId;
        this.exitEventId = exitEventId;
        this.visible = visible;
        this.locked = locked;
    }

    public static TriggerZone createDefault(String name, UUID eventId) {
        return new TriggerZone(
                UUID.randomUUID(),
                name,
                BlockPos.ORIGIN,
                BlockPos.ORIGIN.add(3, 3, 3),
                true,
                true,
                true,
                false,
                false,
                20,
                40,
                0,
                6.0F,
                TriggerOnceMode.NONE,
                TriggerTargetMode.TRIGGERING_PLAYER,
                eventId,
                eventId,
                eventId,
                true,
                false
        );
    }

    public UUID id() {
        return this.id;
    }

    public String name() {
        return this.name;
    }

    @Override
    public SceneObjectType type() {
        return SceneObjectType.TRIGGER_ZONE;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BlockPos pos1() {
        return this.pos1;
    }

    public void setPos1(BlockPos pos1) {
        this.pos1 = pos1;
    }

    public BlockPos pos2() {
        return this.pos2;
    }

    public void setPos2(BlockPos pos2) {
        this.pos2 = pos2;
    }

    public boolean showFill() {
        return this.showFill;
    }

    public void setShowFill(boolean showFill) {
        this.showFill = showFill;
    }

    public boolean triggerEnter() {
        return this.triggerEnter;
    }

    public void setTriggerEnter(boolean triggerEnter) {
        this.triggerEnter = triggerEnter;
    }

    public boolean triggerExit() {
        return this.triggerExit;
    }

    public void setTriggerExit(boolean triggerExit) {
        this.triggerExit = triggerExit;
    }

    public boolean triggerWhileInside() {
        return this.triggerWhileInside;
    }

    public void setTriggerWhileInside(boolean triggerWhileInside) {
        this.triggerWhileInside = triggerWhileInside;
    }

    public boolean triggerTimeInside() {
        return this.triggerTimeInside;
    }

    public void setTriggerTimeInside(boolean triggerTimeInside) {
        this.triggerTimeInside = triggerTimeInside;
    }

    public int whileInsideIntervalTicks() {
        return this.whileInsideIntervalTicks;
    }

    public void setWhileInsideIntervalTicks(int whileInsideIntervalTicks) {
        this.whileInsideIntervalTicks = Math.max(1, whileInsideIntervalTicks);
    }

    public int requiredTimeTicks() {
        return this.requiredTimeTicks;
    }

    public void setRequiredTimeTicks(int requiredTimeTicks) {
        this.requiredTimeTicks = Math.max(1, requiredTimeTicks);
    }

    public int delayTicks() {
        return this.delayTicks;
    }

    public void setDelayTicks(int delayTicks) {
        this.delayTicks = Math.max(0, delayTicks);
    }

    public float radius() {
        return this.radius;
    }

    public void setRadius(float radius) {
        this.radius = Math.max(0.0F, radius);
    }

    public TriggerOnceMode onceMode() {
        return this.onceMode;
    }

    public void setOnceMode(TriggerOnceMode onceMode) {
        this.onceMode = onceMode;
    }

    public TriggerTargetMode targetMode() {
        return this.targetMode;
    }

    public void setTargetMode(TriggerTargetMode targetMode) {
        this.targetMode = targetMode;
    }

    public UUID eventId() {
        return this.eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID enterEventId() {
        return this.enterEventId != null ? this.enterEventId : this.eventId;
    }

    public void setEnterEventId(UUID enterEventId) {
        this.enterEventId = enterEventId;
    }

    public UUID exitEventId() {
        return this.exitEventId != null ? this.exitEventId : this.eventId;
    }

    public void setExitEventId(UUID exitEventId) {
        this.exitEventId = exitEventId;
    }

    @Override
    public boolean visible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public boolean locked() {
        return this.locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public Box box() {
        return new Box(this.pos1).expand(1.0D).union(new Box(this.pos2).expand(1.0D));
    }

    public Vec3d center() {
        return box().getCenter();
    }
}
