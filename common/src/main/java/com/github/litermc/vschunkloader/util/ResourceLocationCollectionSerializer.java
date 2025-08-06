package com.github.litermc.vschunkloader.util;

import net.minecraft.resources.ResourceLocation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Collection;

public class ResourceLocationCollectionSerializer extends JsonSerializer<Collection<ResourceLocation>> {
	@Override
	public void serialize(
		final Collection<ResourceLocation> locations,
		final JsonGenerator generator,
		final SerializerProvider serializers
	) throws IOException {
		generator.writeStartArray();
		for (final ResourceLocation location : locations) {
			generator.writeString(location.toString());
		}
		generator.writeEndArray();
	}
}
