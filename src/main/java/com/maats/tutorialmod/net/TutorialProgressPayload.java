package com.maats.tutorialmod.net;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C: voortgang van de huidige stap (geplaatste blokken en foute blokken).
 */
public record TutorialProgressPayload(
        int stepIndex, int placed, int total, List<BlockPos> wrong) implements CustomPacketPayload {

    public static final Type<TutorialProgressPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("tutorialmod", "progress"));

    public static final net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, TutorialProgressPayload> CODEC =
            new net.minecraft.network.codec.StreamCodec<>() {
                @Override
                public TutorialProgressPayload decode(RegistryFriendlyByteBuf buf) {
                    int stepIndex = buf.readVarInt();
                    int placed = buf.readVarInt();
                    int total = buf.readVarInt();
                    int n = buf.readVarInt();
                    List<BlockPos> wrong = new ArrayList<>(n);
                    for (int i = 0; i < n; i++) {
                        wrong.add(buf.readBlockPos());
                    }
                    return new TutorialProgressPayload(stepIndex, placed, total, wrong);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, TutorialProgressPayload value) {
                    buf.writeVarInt(value.stepIndex());
                    buf.writeVarInt(value.placed());
                    buf.writeVarInt(value.total());
                    buf.writeVarInt(value.wrong().size());
                    for (BlockPos pos : value.wrong()) {
                        buf.writeBlockPos(pos);
                    }
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
