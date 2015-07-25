package me.johnnywoof.remotechat.web;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WebSocketServerIndexPage {

	public static ByteBuf content = null;

	public static void loadContent(Path filePath) throws IOException {

		content = Unpooled.copiedBuffer(Files.readAllBytes(filePath));

	}

	public static ByteBuf getContent() {

		return content.copy();

	}

	private WebSocketServerIndexPage() {
		// Unused
	}
}