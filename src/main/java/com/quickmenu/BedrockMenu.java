package com.quickmenu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;

import java.util.ArrayList;
import java.util.List;

/**
 * Formularios nativos para jugadores de Bedrock, vía Geyser.
 * Esta clase solo se carga si Geyser está presente. Los formularios se construyen con GeyserForms
 * (por reflexión) para no chocar con otras copias de Cumulus que haya en el servidor.
 */
public final class BedrockMenu {
	private BedrockMenu() {}

	/** Busca la conexión de Geyser del jugador por UUID y, si no, por nombre o UUID de Java. */
	private static GeyserConnection find(Player player) {
		try {
			GeyserApi api = GeyserApi.api();
			GeyserConnection c = api.connectionByUuid(player.getUniqueId());
			if (c != null) return c;
			for (GeyserConnection conn : api.onlineConnections()) {
				if (player.getUniqueId().equals(conn.javaUuid())) return conn;
				if (player.getName().equalsIgnoreCase(conn.javaUsername())) return conn;
			}
		} catch (Throwable t) {
			// Geyser aún no está listo o la API cambió: se trata como jugador de Java
		}
		return null;
	}

	public static boolean isBedrock(Player player) {
		return find(player) != null;
	}

	/** Texto para el comando /menu info. */
	public static String describe(Player player) {
		GeyserConnection c = find(player);
		return c == null ? "no se encontró conexión de Geyser (se te trata como Java)"
			: "conexión de Geyser encontrada (" + c.bedrockUsername() + ")";
	}

	private static void send(QuickMenuPlugin plugin, Player player, Object form) {
		GeyserConnection connection = find(player);
		if (connection == null) {
			plugin.getLogger().warning("No se pudo enviar el formulario a " + player.getName() + ": sin conexión de Geyser.");
			return;
		}
		try {
			GeyserForms.send(connection, form);
			plugin.debug("Formulario enviado a " + player.getName());
		} catch (Throwable t) {
			plugin.getLogger().warning("Error enviando el formulario a " + player.getName() + ": " + t);
		}
	}

	/** Las respuestas llegan en otro hilo: se pasan al hilo principal antes de usar Bukkit. */
	private static void sync(QuickMenuPlugin plugin, Runnable task) {
		Bukkit.getScheduler().runTask(plugin, task);
	}

	public static void open(QuickMenuPlugin plugin, Player player) {
		try {
			MenuService menu = plugin.menu();
			List<MenuButton> buttons = menu.buttonsFor(player);
			boolean canEdit = menu.personalEnabled(player);
			int personalCount = plugin.store().get(player.getUniqueId()).size();
			boolean canAdd = canEdit && personalCount < menu.maxPersonal();
			boolean canRemove = canEdit && personalCount > 0;

			List<String> labels = new ArrayList<>();
			for (MenuButton b : buttons) labels.add(MenuService.legacy(b.name()));
			int addIndex = -1, removeIndex = -1;
			if (canAdd) { addIndex = labels.size(); labels.add("\u00a7a+ A\u00f1adir comando"); }
			if (canRemove) { removeIndex = labels.size(); labels.add("\u00a7c- Quitar comando"); }

			final int add = addIndex, remove = removeIndex;
			Object form = GeyserForms.simple(
				MenuService.legacy(plugin.getConfig().getString("menu-title", "Men\u00fa r\u00e1pido")), labels,
				id -> sync(plugin, () -> {
					if (id == add) openAdd(plugin, player);
					else if (id == remove) openRemove(plugin, player);
					else if (id >= 0 && id < buttons.size()) menu.run(player, buttons.get(id));
				}));
			send(plugin, player, form);
		} catch (Throwable t) {
			plugin.getLogger().warning("No se pudo crear el formulario para " + player.getName() + ": " + t);
		}
	}

	private static void openAdd(QuickMenuPlugin plugin, Player player) {
		try {
			Object form = GeyserForms.inputs("A\u00f1adir comando",
				List.of(new String[] {"Nombre del bot\u00f3n", "Ejemplo: Home"}, new String[] {"Comando (sin /)", "Ejemplo: home"}),
				values -> {
					String name = values.get(0).trim();
					String command = MenuService.clean(values.get(1));
					sync(plugin, () -> {
						if (name.isEmpty() || command.isEmpty()) {
							player.sendMessage(MenuService.component("&cDebes escribir un nombre y un comando."));
						} else if (plugin.store().get(player.getUniqueId()).size() >= plugin.menu().maxPersonal()) {
							player.sendMessage(MenuService.component("&cLlegaste al m\u00e1ximo de botones personales."));
						} else {
							plugin.menu().addPersonal(player, name.length() > 20 ? name.substring(0, 20) : name, command);
							player.sendMessage(MenuService.component("&aBot\u00f3n a\u00f1adido: &f" + name));
						}
						open(plugin, player);
					});
				});
			send(plugin, player, form);
		} catch (Throwable t) {
			plugin.getLogger().warning("No se pudo crear el formulario para " + player.getName() + ": " + t);
		}
	}

	private static void openRemove(QuickMenuPlugin plugin, Player player) {
		try {
			List<PlayerStore.Entry> mine = plugin.store().get(player.getUniqueId());
			List<String> names = new ArrayList<>();
			for (PlayerStore.Entry e : mine) names.add(e.name() + "  (/" + e.command() + ")");

			Object form = GeyserForms.dropdown("Quitar comando", "Elige el bot\u00f3n a quitar", names, index ->
				sync(plugin, () -> {
					if (plugin.store().remove(player.getUniqueId(), index)) {
						player.sendMessage(MenuService.component("&7Bot\u00f3n eliminado."));
					}
					open(plugin, player);
				}));
			send(plugin, player, form);
		} catch (Throwable t) {
			plugin.getLogger().warning("No se pudo crear el formulario para " + player.getName() + ": " + t);
		}
	}
}
