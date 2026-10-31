package com.thejourney;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: "this player just reached milestone X, take a photo". */
public record MilestonePayload(String id) implements CustomPacketPayload {
    public static final Type<MilestonePayload> TYPE =
            new Type<>(TheJourney.id("milestone"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MilestonePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    MilestonePayload::id,
                    MilestonePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
