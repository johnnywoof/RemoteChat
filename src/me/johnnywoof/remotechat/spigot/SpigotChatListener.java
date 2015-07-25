package me.johnnywoof.remotechat.spigot;

import me.johnnywoof.remotechat.Settings;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class SpigotChatListener implements Listener {

	private final RemoteChat remoteChat;

	public SpigotChatListener(RemoteChat remoteChat) {

		this.remoteChat = remoteChat;

	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onAsyncChat(AsyncPlayerChatEvent event) {

		this.remoteChat.webServer.webServerHandler.broadcastChatMessage(event.getPlayer().getName(), event.getMessage());

	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPreCommand(PlayerCommandPreprocessEvent event) {

		if (Settings.showCommands) {

			this.remoteChat.webServer.webServerHandler.broadcastChatMessage(event.getPlayer().getName(),
					"/" + event.getMessage());

		}

	}

}
