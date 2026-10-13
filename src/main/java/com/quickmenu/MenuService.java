package com.quickmenu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Reúne los botones (del servidor y personales) y ejecuta sus comandos. */
public final class MenuService {
	private static final LegacyComponentSerializer AMP = LegacyComponentSerializer.legacyAmpersand();
	private static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.legacySection();

	private final QuickMenuPlugin plugin;
	private List<MenuButton> serverButtons = List.of();

	public MenuService(QuickMenuPlugin plugin) {
		this.plugin = plugin;
		reload();
	}

	public void reload() {
		List<MenuButton> list = new ArrayList<>();
		ConfigurationSection section = plugin.getConfig().getConfigurationSection("buttons");
		if (section != null) {
			for (String key : section.getKeys(false)) {
				ConfigurationSection b = section.getConfigurationSection(key);
				if (b == null) continue;
				String command = clean(b.getString("command", ""));
				if (command.isEmpty()) continue;
				Material icon = Material.matchMaterial(b.getString("icon", "PAPER"));
				list.add(new MenuButton(
					b.getString("name", key),
					icon == null ? Material.PAPER : icon,
					command,
					"console".equalsIgnoreCase(b.getString("run-as", "player")),
					b.getString("permission", ""),
					b.getBoolean("close", true),
					false));
			}
		}
		serverButtons = List.copyOf(list);
	}

	/** Botones que este jugador puede ver: primero los del servidor, luego los suyos. */
	public List<MenuButton> buttonsFor(Player player) {
		List<MenuButton> out = new ArrayList<>();
		for (MenuButton b : serverButtons) if (b.visibleTo(player)) out.add(b);
		for (PlayerStore.Entry e : plugin.store().get(player.getUniqueId())) {
			out.add(new MenuButton(e.name(), Material.PAPER, e.command(), false, "", true, true));
		}
		return out;
	}

	public boolean personalEnabled(Player player) {
		return plugin.getConfig().getBoolean("personal-buttons.enabled", true)
			&& player.hasPermission("quickmenu.personal");
	}

	public int maxPersonal() {
		return Math.max(1, plugin.getConfig().getInt("personal-buttons.max-per-player", 6));
	}

	/** Índice de un botón personal dentro de la lista de este jugador, o -1 si no lo es. */
	public int personalIndex(Player player, MenuButton button) {
		if (!button.personal()) return -1;
		List<PlayerStore.Entry> mine = plugin.store().get(player.getUniqueId());
		for (int i = 0; i < mine.size(); i++) {
			if (mine.get(i).name().equals(button.name()) && mine.get(i).command().equals(button.command())) return i;
		}
		return -1;
	}

	public void run(Player player, MenuButton button) {
		if (!button.visibleTo(player)) {
			player.sendMessage(AMP.deserialize(plugin.getConfig().getString("no-permission-message", "&cSin permiso.")));
			return;
		}
		String command = button.commandFor(player);
		if (button.console()) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
		else player.performCommand(command);
	}

	/** Abre el menú con el formato que corresponda: formulario para Bedrock, inventario para Java. */
	public void open(Player player) {
		if (plugin.geyserPresent() && BedrockMenu.isBedrock(player)) BedrockMenu.open(plugin, player);
		else plugin.javaMenu().open(player);
	}

	// ---- utilidades de texto
	public static Component component(String ampersandText) {
		return AMP.deserialize(ampersandText);
	}

	/** Texto con códigos de color de sección (§), para los formularios de Bedrock. */
	public static String legacy(String ampersandText) {
		return SECTION.serialize(AMP.deserialize(ampersandText));
	}

	public static String clean(String command) {
		String c = command == null ? "" : command.trim();
		while (c.startsWith("/")) c = c.substring(1).trim();
		return c;
	}

	public void addPersonal(Player player, String name, String command) {
		plugin.store().add(player.getUniqueId(), name, clean(command));
	}
}
