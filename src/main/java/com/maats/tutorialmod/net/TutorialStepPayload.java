package com.maats.tutorialmod.net;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C: de inhoud van de huidige bouwstap (ghost blocks).
 * stepIndex = -1 betekent: geen actieve tutorial (alles wissen bij de client).
 */
public record TutorialStepPayload(
        String tutorialName, int stepIndex, int stepCount, String stepName,
        List<Entry> entries) implements CustomPacketPayload {

    public record Entry(BlockPos pos, CompoundTag stateTag) {
    }

    public static final Type<TutorialStepPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("tutorialmod", "step"));

    public static final net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, TutorialStepPayload> CODEC =
            new net.minecraft.network.codec.StreamCodec<>() {
                @Override
                public TutorialStepPayload decode(RegistryFriendlyByteBuf buf) {
                    String tutorialName = buf.readUtf();
                    int stepIndex = buf.readVarInt();
                    int stepCount = buf.readVarInt();
                    String stepName = buf.readUtf();
                    int n = buf.readVarInt();
                    List<Entry> entries = new ArrayList<>(n);
                    for (int i = 0; i < n; i++) {
                        BlockPos pos = buf.readBlockPos();
                        CompoundTag tag = buf.readNbt();
                        entries.add(new Entry(pos, tag == null ? new CompoundTag() : tag));
                    }
                    return new TutorialStepPayload(tutorialName, stepIndex, stepCount, stepName, entries);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, TutorialStepPayload value) {
                    buf.writeUtf(value.tutorialName);
                    buf.writeVarInt(value.stepIndex);
                    buf.writeVarInt(value.stepCount);
                    buf.writeUtf(value.stepName);
                    buf.writeVarInt(value.entries().size());
                    for (Entry e : value.entries()) {
                        buf.writeBlockPos(e.pos());
                        buf.writeNbt(e.stateTag());
                    }
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
