package net.ccbluex.liquidbounce.features.module.modules.combat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import kotlin.Unit;
import kotlin.collections.ArraysKt;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.PropertyReference1Impl;
import kotlin.jvm.internal.Ref;
import kotlin.jvm.internal.Reflection;
import kotlin.random.Random;
import kotlin.ranges.IntRange;
import kotlin.ranges.RangesKt;
import kotlin.reflect.KProperty;
import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.Render3DEvent;
import net.ccbluex.liquidbounce.event.UpdateEvent;
import net.ccbluex.liquidbounce.features.module.Category;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.utils.EntityUtils;
import net.ccbluex.liquidbounce.utils.MinecraftInstance;
import net.ccbluex.liquidbounce.utils.RotationUtils;
import net.ccbluex.liquidbounce.utils.extensions.PlayerExtensionKt;
import net.ccbluex.liquidbounce.utils.misc.RandomUtils;
import net.ccbluex.liquidbounce.utils.timing.TimeUtils;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.FloatValue;
import net.ccbluex.liquidbounce.value.IntegerValue;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.jetbrains.annotations.NotNull;

public final class BetterAutoClicker extends Module {
    @NotNull
    public static final BetterAutoClicker INSTANCE;
    static final KProperty<Object>[] $$delegatedProperties;
    @NotNull
    private static final BoolValue simulateDoubleClicking$delegate;
    @NotNull
    private static final IntegerValue maxCPSValue;
    @NotNull
    private static final IntegerValue maxCPS$delegate;
    @NotNull
    private static final IntegerValue minCPS$delegate;
    @NotNull
    private static final BoolValue debug$delegate;
    @NotNull
    private static final BoolValue debug2$delegate;
    @NotNull
    private static final BoolValue debug3$delegate;
    @NotNull
    private static final BoolValue debug4$delegate;
    @NotNull
    private static final BoolValue left$delegate;
    @NotNull
    private static final BoolValue jitter$delegate;
    @NotNull
    private static final BoolValue block$delegate;
    @NotNull
    private static final FloatValue fov$delegate;
    @NotNull
    private static final FloatValue range$delegate;
    @NotNull
    private static final FloatValue enemyFOV$delegate;
    @NotNull
    private static final IntegerValue blockDelay$delegate;
    private static int leftDelay;
    private static long leftLastSwing;
    private static long lastBlocking;
    private static boolean shouldJitter;

    private BetterAutoClicker() {
        super("BetterAutoClicker", Category.TYLER, 0, false, false, null, null, false, false, false, 508, null);
    }

    private final boolean getSimulateDoubleClicking() {
        return (Boolean)simulateDoubleClicking$delegate.getValue(this, $$delegatedProperties[0]);
    }

    private final int getMaxCPS() {
        return ((Number)maxCPS$delegate.getValue(this, $$delegatedProperties[1])).intValue();
    }

    private final int getMinCPS() {
        return ((Number)minCPS$delegate.getValue(this, $$delegatedProperties[2])).intValue();
    }

    private final boolean getDebug() {
        return (Boolean)debug$delegate.getValue(this, $$delegatedProperties[3]);
    }

    private final boolean getDebug2() {
        return (Boolean)debug2$delegate.getValue(this, $$delegatedProperties[4]);
    }

    private final boolean getDebug3() {
        return (Boolean)debug3$delegate.getValue(this, $$delegatedProperties[5]);
    }

    private final boolean getDebug4() {
        return (Boolean)debug4$delegate.getValue(this, $$delegatedProperties[6]);
    }

    private final boolean getLeft() {
        return (Boolean)left$delegate.getValue(this, $$delegatedProperties[7]);
    }

    private final boolean getJitter() {
        return (Boolean)jitter$delegate.getValue(this, $$delegatedProperties[8]);
    }

    private final boolean getBlock() {
        return (Boolean)block$delegate.getValue(this, $$delegatedProperties[9]);
    }

    private final float getFov() {
        return ((Number)fov$delegate.getValue(this, $$delegatedProperties[10])).floatValue();
    }

    private final float getRange() {
        return ((Number)range$delegate.getValue(this, $$delegatedProperties[11])).floatValue();
    }

    private final float getEnemyFOV() {
        return ((Number)enemyFOV$delegate.getValue(this, $$delegatedProperties[12])).floatValue();
    }

    private final int getBlockDelay() {
        return ((Number)blockDelay$delegate.getValue(this, $$delegatedProperties[13])).intValue();
    }

    private final boolean getShouldAutoClick() {
        return MinecraftInstance.mc.field_71439_g.field_71075_bZ.field_75098_d || MinecraftInstance.mc.field_71476_x.field_72313_a != MovingObjectPosition.MovingObjectType.BLOCK;
    }

    @Override
    public void onDisable() {
        leftLastSwing = 0L;
        lastBlocking = 0L;
    }

    @EventTarget
    public final void onRender3D(@NotNull Render3DEvent event) {
        Intrinsics.checkNotNullParameter(event, "event");
        EntityPlayerSP entityPlayerSP = MinecraftInstance.mc.field_71439_g;
        if (entityPlayerSP != null) {
            int doubleClick;
            EntityPlayerSP thePlayer = entityPlayerSP;
            long time = System.currentTimeMillis();
            int n = doubleClick = INSTANCE.getSimulateDoubleClicking() ? RandomUtils.INSTANCE.nextInt(-1, 1) : 0;
            if (INSTANCE.getBlock() && thePlayer.field_70733_aJ > 0.0f && !MinecraftInstance.mc.field_71474_y.field_74313_G.func_151470_d()) {
                MinecraftInstance.mc.field_71474_y.field_74313_G.field_151474_i = 0;
            }
            if (MinecraftInstance.mc.field_71474_y.field_74312_F.func_151470_d() && !MinecraftInstance.mc.field_71474_y.field_74313_G.func_151470_d() && INSTANCE.getShouldAutoClick()) {
                if (INSTANCE.getLeft() && time - leftLastSwing >= (long)leftDelay) {
                    INSTANCE.handleLeftClick(time, doubleClick);
                } else if (INSTANCE.getBlock() && INSTANCE.isWorthBlocking() && MinecraftInstance.mc.field_71474_y.field_74312_F.field_151474_i != 0) {
                    INSTANCE.handleBlock(time);
                }
            }
        }
    }

    @EventTarget
    public final void onTick(@NotNull UpdateEvent event) {
        Intrinsics.checkNotNullParameter(event, "event");
        if (this.getDebug3()) {
            ClientUtils.INSTANCE.displayChatMessage(Intrinsics.stringPlus("should block: ", this.isWorthBlocking()));
        }
        EntityPlayerSP entityPlayerSP = MinecraftInstance.mc.field_71439_g;
        if (entityPlayerSP != null) {
            EntityPlayerSP thePlayer = entityPlayerSP;
            boolean bl2 = shouldJitter = MinecraftInstance.mc.field_71476_x.field_72313_a != MovingObjectPosition.MovingObjectType.BLOCK && (thePlayer.field_82175_bq || MinecraftInstance.mc.field_71474_y.field_74312_F.field_151474_i != 0);
            if (INSTANCE.getJitter() && INSTANCE.getLeft() && INSTANCE.getShouldAutoClick() && shouldJitter) {
                if (Random.Default.nextBoolean()) {
                    PlayerExtensionKt.setFixedSensitivityYaw(thePlayer, PlayerExtensionKt.getFixedSensitivityYaw(thePlayer) + RandomUtils.INSTANCE.nextFloat(-1.0f, 1.0f));
                }
                if (Random.Default.nextBoolean()) {
                    PlayerExtensionKt.setFixedSensitivityPitch(thePlayer, PlayerExtensionKt.getFixedSensitivityPitch(thePlayer) + RandomUtils.INSTANCE.nextFloat(-1.0f, 1.0f));
                }
            }
        }
    }

    private final boolean isWorthBlocking() {
        Item[] itemArray = new Item[]{Items.field_151041_m, Items.field_151052_q, Items.field_151040_l, Items.field_151010_B, Items.field_151048_u, Items.field_151053_p, Items.field_151049_t, Items.field_151036_c, Items.field_151006_E, Items.field_151056_x};
        List<Item> swordOrAxeItems = CollectionsKt.listOf(itemArray);
        itemArray = new EnumAction[]{EnumAction.BLOCK};
        ItemStack itemStack = MinecraftInstance.mc.field_71439_g.func_70694_bm();
        if (ArraysKt.contains(itemArray, itemStack == null ? null : itemStack.func_77975_n())) {
            Object v1;
            List list = MinecraftInstance.mc.field_71441_e.field_72996_f;
            Intrinsics.checkNotNullExpressionValue(list, "mc.theWorld.loadedEntityList");
            Iterable iterable = list;
            Collection destination = new ArrayList();
            for (Object element : iterable) {
                Entity it = (Entity)element;
                Ref.BooleanRef result = new Ref.BooleanRef();
                Intrinsics.checkNotNullExpressionValue(it, "it");
                Backtrack.INSTANCE.runWithNearestTrackedDistance(it, new Function0<Unit>() {
                    @Override
                    public Unit invoke() {
                        if (!EntityUtils.INSTANCE.isSelected(it, true) || !MinecraftInstance.mc.field_71439_g.func_70685_l(it)) {
                            result.element = false;
                            return Unit.INSTANCE;
                        }
                        Entity entity = MinecraftInstance.mc.field_71439_g;
                        Intrinsics.checkNotNullExpressionValue(entity, "mc.thePlayer");
                        if (!(PlayerExtensionKt.getDistanceToEntityBox(entity, it) <= (double)BetterAutoClicker.access$getRange(BetterAutoClicker.INSTANCE))) {
                            result.element = false;
                            return Unit.INSTANCE;
                        }
                        if (!(RotationUtils.INSTANCE.rotationDifference(it) <= BetterAutoClicker.access$getFov(BetterAutoClicker.INSTANCE))) {
                            result.element = false;
                            return Unit.INSTANCE;
                        }
                        EntityLivingBase living = it instanceof EntityLivingBase ? (EntityLivingBase)it : null;
                        Item item = living == null ? null : (living.func_70694_bm() == null ? null : living.func_70694_bm().func_77973_b());
                        if (!swordOrAxeItems.contains(item)) {
                            result.element = false;
                            return Unit.INSTANCE;
                        }
                        result.element = BetterAutoClicker.access$isWithinEnemyFOV(BetterAutoClicker.INSTANCE, it, false);
                        return Unit.INSTANCE;
                    }
                });
                if (!result.element) continue;
                destination.add(element);
            }
            Iterable minIterable = (List)destination;
            Iterator iterator = minIterable.iterator();
            if (!iterator.hasNext()) {
                v1 = null;
            } else {
                Object minElem = iterator.next();
                if (!iterator.hasNext()) {
                    v1 = minElem;
                } else {
                    Entity it = (Entity)minElem;
                    Entity entity = MinecraftInstance.mc.field_71439_g;
                    Intrinsics.checkNotNullExpressionValue(entity, "mc.thePlayer");
                    double minValue = PlayerExtensionKt.getDistanceToEntityBox(entity, it);
                    do {
                        Object e = iterator.next();
                        Entity it2 = (Entity)e;
                        Entity entity2 = MinecraftInstance.mc.field_71439_g;
                        Intrinsics.checkNotNullExpressionValue(entity2, "mc.thePlayer");
                        double value = PlayerExtensionKt.getDistanceToEntityBox(entity2, it2);
                        if (Double.compare(minValue, value) <= 0) continue;
                        minElem = e;
                        minValue = value;
                    } while (iterator.hasNext());
                    v1 = minElem;
                }
            }
            Entity scaryEntity2 = (Entity)v1;
            if (scaryEntity2 == null) {
                return false;
            }
            if (this.getDebug2()) {
                this.isWithinEnemyFOV(scaryEntity2, true);
            }
            return true;
        }
        return false;
    }

    private final boolean isWithinEnemyFOV(Entity entity, boolean test) {
        float enemyYaw = entity.field_70177_z;
        float enemyPitch = entity.field_70125_A;
        Vec3 enemyEyePos = entity.func_174824_e(1.0f);
        AxisAlignedBB playerAABB = MinecraftInstance.mc.field_71439_g.func_174813_aQ();
        Intrinsics.checkNotNullExpressionValue(playerAABB, "playerAABB");
        Intrinsics.checkNotNullExpressionValue(enemyEyePos, "enemyEyePos");
        Vec3 closestPoint = this.getClosestPointOnAABB(playerAABB, enemyEyePos);
        float deltaYaw = this.normalizeAngle(this.getYawToPoint(entity, closestPoint) - enemyYaw);
        float deltaPitch = this.normalizeAngle(this.getPitchToPoint(entity, closestPoint) - enemyPitch);
        if (test) {
            ClientUtils.INSTANCE.displayChatMessage("angle yaw: " + deltaYaw + " pitch: " + deltaPitch);
        }
        return Math.abs(deltaYaw) <= this.getEnemyFOV() / 2.0f && Math.abs(deltaPitch) <= this.getEnemyFOV() / 2.0f;
    }

    private final float normalizeAngle(float angle) {
        float normalizedAngle = angle % 360.0f;
        if (normalizedAngle > 180.0f) {
            normalizedAngle -= 360.0f;
        }
        if (normalizedAngle < -180.0f) {
            normalizedAngle += 360.0f;
        }
        return normalizedAngle;
    }

    private final Vec3 getClosestPointOnAABB(AxisAlignedBB aabb, Vec3 point) {
        double x = RangesKt.coerceIn(point.field_72450_a, aabb.field_72340_a, aabb.field_72336_d);
        double y = RangesKt.coerceIn(point.field_72448_b, aabb.field_72338_b, aabb.field_72337_e);
        double z = RangesKt.coerceIn(point.field_72449_c, aabb.field_72339_c, aabb.field_72334_f);
        return new Vec3(x, y, z);
    }

    private final float getYawToPoint(Entity entity, Vec3 point) {
        double deltaX = point.field_72450_a - entity.field_70165_t;
        double deltaZ = point.field_72449_c - entity.field_70161_v;
        return (float)Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0f;
    }

    private final float getPitchToPoint(Entity entity, Vec3 point) {
        double deltaY = point.field_72448_b - (entity.field_70163_u + (double)entity.func_70047_e());
        double distance = entity.func_70011_f(point.field_72450_a, entity.field_70163_u, point.field_72449_c);
        return (float)Math.toDegrees(Math.atan2(deltaY, distance)) * -1.0f;
    }

    private final void handleLeftClick(long time, int doubleClick) {
        int n = 1 + doubleClick;
        for (int i = 0; i < n; ++i) {
            if (INSTANCE.getDebug()) {
                ClientUtils.INSTANCE.displayChatMessage("clicked");
            }
            KeyBinding.func_74507_a(MinecraftInstance.mc.field_71474_y.field_74312_F.func_151463_i());
            leftLastSwing = time;
            leftDelay = TimeUtils.INSTANCE.randomClickDelay(INSTANCE.getMinCPS(), INSTANCE.getMaxCPS());
        }
    }

    private final void handleBlock(long time) {
        if (time - lastBlocking >= (long)this.getBlockDelay()) {
            if (this.getDebug4()) {
                ClientUtils.INSTANCE.displayChatMessage("blocking");
            }
            KeyBinding.func_74507_a(MinecraftInstance.mc.field_71474_y.field_74313_G.func_151463_i());
            lastBlocking = time;
        }
    }

    public static final float access$getRange(BetterAutoClicker $this) {
        return $this.getRange();
    }

    public static final float access$getFov(BetterAutoClicker $this) {
        return $this.getFov();
    }

    public static final boolean access$isWithinEnemyFOV(BetterAutoClicker $this, Entity entity, boolean test) {
        return $this.isWithinEnemyFOV(entity, test);
    }

    public static final int access$getMinCPS(BetterAutoClicker $this) {
        return $this.getMinCPS();
    }

    public static final int access$getMaxCPS(BetterAutoClicker $this) {
        return $this.getMaxCPS();
    }

    public static final IntegerValue access$getMaxCPSValue$p() {
        return maxCPSValue;
    }

    public static final boolean access$getBlock(BetterAutoClicker $this) {
        return $this.getBlock();
    }

    static {
        KProperty[] props = new KProperty[]{
            Reflection.property1(new PropertyReference1Impl(BetterAutoClicker.class, "simulateDoubleClicking", "getSimulateDoubleClicking()Z", 0)),
            Reflection.property1(new PropertyReference1Impl(BetterAutoClicker.class, "maxCPS", "getMaxCPS()I", 0)),
            Reflection.property1(new PropertyReference1Impl(BetterAutoClicker.class, "minCPS", "getMinCPS()I", 0)),
            Reflection.property1(new PropertyReference1Impl(BetterAutoClicker.class, "debug", "getDebug()Z", 0)),
            Reflection.property1(new PropertyReference1Impl(BetterAutoClicker.class, "debug2", "getDebug2()Z", 0)),
            Reflection.property1(new PropertyReference1Impl(BetterAutoClicker.class, "debug3", "getDebug3()Z", 0)),
            Reflection.property1(new PropertyReference1Impl(BetterAutoClicker.class, "debug4", "getDebug4()Z", 0)),
            Reflection.property1(new PropertyReference1Impl(BetterAutoClicker.class, "left", "getLeft()Z", 0)),
            Reflection.property1(new PropertyReference1Impl(BetterAutoClicker.class, "jitter", "getJitter()Z", 0)),
            Reflection.property1(new PropertyReference1Impl(BetterAutoClicker.class, "block", "getBlock()Z", 0)),
            Reflection.property1(new PropertyReference1Impl(BetterAutoClicker.class, "fov", "getFov()F", 0)),
            Reflection.property1(new PropertyReference1Impl(BetterAutoClicker.class, "range", "getRange()F", 0)),
            Reflection.property1(new PropertyReference1Impl(BetterAutoClicker.class, "enemyFOV", "getEnemyFOV()F", 0)),
            Reflection.property1(new PropertyReference1Impl(BetterAutoClicker.class, "blockDelay", "getBlockDelay()I", 0))
        };
        $$delegatedProperties = props;
        INSTANCE = new BetterAutoClicker();
        simulateDoubleClicking$delegate = new BoolValue("SimulateDoubleClicking", false, false, null, 12, null);
        IntRange range = new IntRange(1, 20);
        maxCPS$delegate = maxCPSValue = new IntegerValue("MaxCPS", 8, range, false, null, 24, null) {
            @NotNull
            protected Integer onChange(int oldValue, int newValue) {
                return RangesKt.coerceAtLeast(newValue, BetterAutoClicker.access$getMinCPS(BetterAutoClicker.INSTANCE));
            }
        };
        IntRange range2 = new IntRange(1, 20);
        minCPS$delegate = new IntegerValue("MinCPS", 5, range2, false, null, 24, null) {
            @NotNull
            protected Integer onChange(int oldValue, int newValue) {
                return RangesKt.coerceAtMost(newValue, BetterAutoClicker.access$getMaxCPS(BetterAutoClicker.INSTANCE));
            }

            public boolean isSupported() {
                return !BetterAutoClicker.access$getMaxCPSValue$p().isMinimal();
            }
        };
        debug$delegate = new BoolValue("showattacks", false, false, null, 12, null);
        debug2$delegate = new BoolValue("shownemyangletoyou", false, false, null, 12, null);
        debug3$delegate = new BoolValue("showshouldblock", false, false, null, 12, null);
        debug4$delegate = new BoolValue("showblocks", false, false, null, 12, null);
        left$delegate = new BoolValue("Left", true, false, null, 12, null);
        jitter$delegate = new BoolValue("Jitter", false, false, null, 12, null);
        block$delegate = new BoolValue("AutoBlock", false, false, null, 12, null);
        fov$delegate = new FloatValue("TargetInFOVforBlock", 180.0f, RangesKt.rangeTo(1.0f, 180.0f), false, null, 24, null);
        range$delegate = new FloatValue("TargetInRangeForBlock", 4.4f, RangesKt.rangeTo(1.0f, 8.0f), false, null, 24, null);
        enemyFOV$delegate = new FloatValue("EnemyFOV", 90.0f, RangesKt.rangeTo(1.0f, 180.0f), false, null, 24, null);
        blockDelay$delegate = new IntegerValue("BlockDelay", 50, new IntRange(0, 100), false, new Function0<Boolean>() {
            @Override
            public Boolean invoke() {
                return BetterAutoClicker.INSTANCE.getBlock();
            }
        }, 8, null);
        leftDelay = TimeUtils.INSTANCE.randomClickDelay(INSTANCE.getMinCPS(), INSTANCE.getMaxCPS());
    }
}
