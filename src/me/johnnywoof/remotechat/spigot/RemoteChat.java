package me.johnnywoof.remotechat.spigot;

import com.google.common.io.ByteStreams;
import me.johnnywoof.remotechat.Settings;
import me.johnnywoof.remotechat.web.WebServer;
import me.johnnywoof.remotechat.web.WebSocketServerIndexPage;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;

public class RemoteChat extends JavaPlugin {

	public final WebServer webServer = new WebServer();

	@Override
	public void onEnable() {

		this.getLogger().info("Loading configuration...");

		this.saveDefaultConfig();

		Settings.showCommands = this.getConfig().getBoolean("show_commands", false);
		Settings.httpRequiresAuth = this.getConfig().getBoolean("require_authentication", false);

		Settings.httpUsername = this.getConfig().getString("username", "admin");
		Settings.httpPassword = this.getConfig().getString("password", "password");
		Settings.realm = this.getConfig().getString("realm", "Minecraft Server Chat");

		Settings.bindIP = this.getConfig().getString("bind_ip", "0.0.0.0");
		Settings.bindPort = this.getConfig().getInt("bind_port", 8080);

		File siteFile = new File(this.getDataFolder(), "site_content.html");

		this.getLogger().info("Loading website content... (From " + siteFile.getPath() + ")");

		try {

			if (!siteFile.exists()) {

				InputStream is = this.getClass().getResourceAsStream("/site_content.html");
				OutputStream os = new FileOutputStream(siteFile);
				ByteStreams.copy(is, os);
				os.close();
				is.close();

			}

			WebSocketServerIndexPage.loadContent(siteFile.toPath());

		} catch (IOException e) {
			e.printStackTrace();
		}

		this.getLogger().info("Starting web server...");

		this.getServer().getScheduler().runTaskAsynchronously(this, this.webServer);

		this.getLogger().info("Registering listener...");

		this.getServer().getPluginManager().registerEvents(new SpigotChatListener(this), this);

		this.getLogger().info("Done!");

	}

	@Override
	public void onDisable() {


	}

}
