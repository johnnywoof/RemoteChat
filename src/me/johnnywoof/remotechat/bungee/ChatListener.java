package me.johnnywoof.remotechat.bungee;

import me.johnnywoof.remotechat.Settings;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class ChatListener implements Listener {

	private final RemoteChat remoteChat;

	public ChatListener(RemoteChat remoteChat) {

		this.remoteChat = remoteChat;

	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onChat(ChatEvent event) {

		if (!event.isCancelled() && (Settings.showCommands || !event.isCommand())
				&& event.getSender() instanceof ProxiedPlayer) {

			ProxiedPlayer player = (ProxiedPlayer) event.getSender();

			if (event.isCommand()) {

				this.remoteChat.webServer.webServerHandler.broadcastChatMessage(player.getName(), "/" + event.getMessage());

			} else {

				this.remoteChat.webServer.webServerHandler.broadcastChatMessage(player.getName(), event.getMessage());

			}

		}

	}

}
