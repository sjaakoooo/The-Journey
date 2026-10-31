package com.thejourney;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record JourneyRecapPayload(String text) implements CustomPacketPayload {
    public static final Type<JourneyRecapPayload> TYPE =
            new Type<>(TheJourney.id("recap"));

    public static final StreamCodec<RegistryFriendlyByteBuf, JourneyRecapPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    JourneyRecapPayload::text,
                    JourneyRecapPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}