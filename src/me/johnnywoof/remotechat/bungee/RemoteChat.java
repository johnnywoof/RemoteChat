package me.johnnywoof.remotechat.bungee;

import com.google.common.io.ByteStreams;
import me.johnnywoof.remotechat.Settings;
import me.johnnywoof.remotechat.web.WebServer;
import me.johnnywoof.remotechat.web.WebSocketServerIndexPage;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;

public class RemoteChat extends Plugin {

	public final WebServer webServer = new WebServer();

	@Override
	public void onEnable() {

		this.getLogger().info("Loading configuration...");

		this.saveDefaultConfig();

		try {

			Configuration yml = ConfigurationProvider.getProvider(YamlConfiguration.class).load(this.getConfig());

			Settings.showCommands = yml.getBoolean("show_commands", false);
			Settings.httpRequiresAuth = yml.getBoolean("require_authentication", false);

			Settings.httpUsername = yml.getString("username", "admin");
			Settings.httpPassword = yml.getString("password", "password");
			Settings.realm = yml.getString("realm", "Minecraft Server Chat");

			Settings.bindIP = yml.getString("bind_ip", "0.0.0.0");
			Settings.bindPort = yml.getInt("bind_port", 8080);

			File siteFile = new File(this.getDataFolder(), "site_content.html");

			this.getLogger().info("Loading website content... (From " + siteFile.getPath() + ")");

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

		this.getProxy().getScheduler().runAsync(this, this.webServer);

		this.getLogger().info("Registering listener...");

		this.getProxy().getPluginManager().registerListener(this, new ChatListener(this));

		this.getLogger().info("Done!");

	}

	@Override
	public void onDisable() {

		//this.getLogger().info("Stopping web server...");

		//this.webServer.stopServer();

	}

	/**
	 * Generates a file object for the config file
	 *
	 * @return The config file object
	 */
	private File getConfig() {

		return new File(this.getDataFolder(), "config.yml");

	}

	/**
	 * Saves the default plugin configuration file from the jar
	 */
	public void saveDefaultConfig() {

		if (!this.getDataFolder().exists()) {
			this.getDataFolder().mkdir();
		}

		File configFile = new File(this.getDataFolder(), "config.yml");
		if (!configFile.exists()) {
			try {
				configFile.createNewFile();
				try (InputStream is = this.getClass().getResourceAsStream("/config.yml");
					 OutputStream os = new FileOutputStream(configFile)) {
					ByteStreams.copy(is, os);
					os.close();
					is.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
