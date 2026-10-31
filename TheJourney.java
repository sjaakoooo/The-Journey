package com.thejourney;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public class TheJourney implements ModInitializer {
	public static final String MOD_ID = "the-journey";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playC2S().register(
				JourneyRecapPayload.TYPE,
				JourneyRecapPayload.CODEC
		);

		PayloadTypeRegistry.playS2C().register(
				JourneyRecapPayload.TYPE,
				JourneyRecapPayload.CODEC
		);

		PayloadTypeRegistry.playS2C().register(
				MilestonePayload.TYPE,
				MilestonePayload.CODEC
		);

		ModSounds.register();
		Milestones.register();

		ServerPlayNetworking.registerGlobalReceiver(
				JourneyRecapPayload.TYPE,
				(payload, context) -> {
					ServerPlayer player = context.player();
					String recap = RecapBuilder.create(player);

					ServerPlayNetworking.send(
							player,
							new JourneyRecapPayload(recap)
					);
				}
		);
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
