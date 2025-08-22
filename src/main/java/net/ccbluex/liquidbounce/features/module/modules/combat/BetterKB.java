// Decompiled with: CFR 0.152
// Class Version: 8
package net.ccbluex.liquidbounce.features.module.modules.combat;

import java.util.Arrays;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.PropertyReference1Impl;
import kotlin.jvm.internal.Reflection;
import kotlin.ranges.IntRange;
import kotlin.reflect.KProperty;
import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.PacketEvent;
import net.ccbluex.liquidbounce.event.UpdateEvent;
import net.ccbluex.liquidbounce.features.module.Category;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.utils.MinecraftInstance;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.IntegerValue;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={1, 6, 0}, k=1, xi=48, d1={"\u0000@\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0008\u0002\n\u0002\u0010\u000b\n\u0002\u0008\u0006\n\u0002\u0010\b\n\u0002\u0008\u0016\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0002\u0008\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0008\u0003\n\u0002\u0018\u0002\n\u0002\u0008\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\u00b2\u0002\u0010\u0002J\u0010\u0010\u0019\u001a\u00020\u001a2\u0006\u0010\u001b\u001a\u00020\u001cH\u0007J\u0010\u0010\u001d\u001a\u00020\u001a2\u0006\u0010\u001e\u001a\u00020\u0004H\u0016J\u0010\u0010\u001f\u001a\u00020\u001a2\u0006\u0010\u001b\u001a\u00020 H\u0007J\u0006\u0010!\u001a\u00020\u0004R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u0005\u001a\u00020\u00048BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\u0008\u0008\u0010\t\u001a\u0004\u0008\u0006\u0010\u0007R\u001b\u0010\n\u001a\u00020\u000b8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\u0008\u000e\u0010\u000f\u001a\u0004\u0008\u000c\u0010\rR\u000e\u0010\u0010\u001a\u00020\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\u0012X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0013\u001a\u0004\u0018\u00010\u0014X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u0015\u001a\u00020\u00048BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\u0008\u0017\u0010\t\u001a\u0004\u0008\u0016\u0010\u0007R\u000e\u0010\u0018\u001a\u00020\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/combat/BetterKB;", "Lnet/ccbluex/liquidbounce/features/module/Module;", "()V", "blockInput", "", "debug", "getDebug", "()Z", "debug$delegate", "Lnet/ccbluex/liquidbounce/value/BoolValue;", "gticks", "", "getGticks", "()I", "gticks$delegate", "Lnet/ccbluex/liquidbounce/value/IntegerValue;", "isSprinting", "lastAttackTime", "", "lastAttackedEntity", "Lnet/minecraft/entity/Entity;", "sprint", "getSprint", "sprint$delegate", "ticks", "onPacket", "", "event", "Lnet/ccbluex/liquidbounce/event/PacketEvent;", "onToggle", "state", "onUpdate", "Lnet/ccbluex/liquidbounce/event/UpdateEvent;", "shouldBlockInput", "liquidbounce"})
public final class BetterKB extends Module {
    @NotNull
    public static final BetterKB INSTANCE;
    static final KProperty<Object>[] $$delegatedProperties;
    private static int ticks;
    private static boolean isSprinting;
    private static boolean blockInput;
    @NotNull
    private static final IntegerValue gticks$delegate;
    @NotNull
    private static final BoolValue debug$delegate;
    @NotNull
    private static final BoolValue sprint$delegate;
    @Nullable
    private static Entity lastAttackedEntity;
    private static long lastAttackTime;

    private BetterKB() {
        super("BetterKB", Category.TYLER, 0, false, false, null, null, false, false, false, 508, null);
    }

    private final int getGticks() {
        return ((Number)gticks$delegate.getValue(this, (KProperty<?>)$$delegatedProperties[0])).intValue();
    }

    private final boolean getDebug() {
        return (Boolean)debug$delegate.getValue((Object)this, (KProperty)$$delegatedProperties[1]);
    }

    private final boolean getSprint() {
        return (Boolean)sprint$delegate.getValue((Object)this, (KProperty)$$delegatedProperties[2]);
    }

    @Override
    public void onToggle(boolean state) {
        blockInput = false;
    }

    @EventTarget
    public final void onUpdate(@NotNull UpdateEvent event) {
        Intrinsics.checkNotNullParameter(event, "event");
        if (ticks == -1) {
            return;
        }
        int n = ticks;
        if ((ticks = n + 1) >= this.getGticks()) {
            ticks = -1;
            blockInput = false;
            return;
        }
    }

    @EventTarget
    public final void onPacket(@NotNull PacketEvent event) {
        Packet<?> packet;
        Intrinsics.checkNotNullParameter(event, "event");
        Packet<?> packet2 = packet = event.getPacket();
        if (packet2 instanceof C0BPacketEntityAction) {
            C0BPacketEntityAction.Action action = ((C0BPacketEntityAction)packet).func_180764_b();
            switch (action == null ? -1 : WhenMappings.$EnumSwitchMapping$0[action.ordinal()]) {
                case 1: {
                    if (this.getSprint()) {
                        ClientUtils.INSTANCE.displayChatMessage("started sprinting");
                    }
                    isSprinting = true;
                    break;
                }
                case 2: {
                    if (this.getSprint()) {
                        ClientUtils.INSTANCE.displayChatMessage("stopped sprinting");
                    }
                    isSprinting = false;
                }
            }
        } else if (packet2 instanceof S29PacketSoundEffect) {
            if (Intrinsics.areEqual(((S29PacketSoundEffect)packet).func_149212_c(), "game.player.hurt")) {
                Double lastAttackedToSound;
                Entity entity = lastAttackedEntity;
                Double d = lastAttackedToSound = entity == null ? null : Double.valueOf(entity.func_70011_f(((S29PacketSoundEffect)packet).func_149207_d(), ((S29PacketSoundEffect)packet).func_149211_e(), ((S29PacketSoundEffect)packet).func_149210_f()));
                if (lastAttackedToSound != null && ((S29PacketSoundEffect)packet).func_149208_g() == 1.0f && isSprinting && System.currentTimeMillis() - lastAttackTime < 1000L && lastAttackedToSound < 3.0) {
                    String string = "%.1f";
                    Object[] objectArray = new Object[]{lastAttackedToSound};
                    String string2 = String.format(string, Arrays.copyOf(objectArray, objectArray.length));
                    Intrinsics.checkNotNullExpressionValue(string2, "format(format, *args)");
                    String roundedDistance = string2;
                    if (this.getDebug()) {
                        StringBuilder stringBuilder = new StringBuilder().append("attacked ");
                        Entity entity2 = lastAttackedEntity;
                        Intrinsics.checkNotNull(entity2);
                        ClientUtils.INSTANCE.displayChatMessage(stringBuilder.append((Object)entity2.func_70005_c_()).append(" distance: ").append(roundedDistance).toString());
                    }
                    lastAttackTime = 0L;
                    blockInput = true;
                    ticks = 0;
                }
            }
        } else if (packet2 instanceof C02PacketUseEntity && ((C02PacketUseEntity)packet).func_149565_c() == C02PacketUseEntity.Action.ATTACK) {
            lastAttackTime = System.currentTimeMillis();
            Entity entity = ((C02PacketUseEntity)packet).func_149564_a((World)MinecraftInstance.mc.field_71441_e);
            if (entity != null) {
                Entity it = entity;
                boolean bl = false;
                lastAttackedEntity = it;
            }
        }
    }

    public final boolean shouldBlockInput() {
        return this.getState() && blockInput;
    }

    static {
        KProperty[] kPropertyArray = new KProperty[]{Reflection.property1(new PropertyReference1Impl(BetterKB.class, "gticks", "getGticks()I", 0)), Reflection.property1(new PropertyReference1Impl(BetterKB.class, "debug", "getDebug()Z", 0)), Reflection.property1(new PropertyReference1Impl(BetterKB.class, "sprint", "getSprint()Z", 0))};
        $$delegatedProperties = kPropertyArray;
        INSTANCE = new BetterKB();
        gticks$delegate = new IntegerValue("ticks", 1, new IntRange(0, 20), false, null, 24, null);
        debug$delegate = new BoolValue("debug", false, false, null, 12, null);
        sprint$delegate = new BoolValue("showsprint", false, false, null, 12, null);
    }

    @Metadata(mv={1, 6, 0}, k=3, xi=48)
    public static final class WhenMappings {
        public static final int[] $EnumSwitchMapping$0;

        static {
            int[] nArray = new int[C0BPacketEntityAction.Action.values().length];
            nArray[C0BPacketEntityAction.Action.START_SPRINTING.ordinal()] = 1;
            nArray[C0BPacketEntityAction.Action.STOP_SPRINTING.ordinal()] = 2;
            $EnumSwitchMapping$0 = nArray;
        }
    }
}
