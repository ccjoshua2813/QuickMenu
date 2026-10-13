package com.quickmenu;

import org.bukkit.Material;

/** Un botón del menú, definido por el servidor o por el propio jugador. */
public record MenuButton(String name, Material icon, String command, boolean console,
						 String permission, boolean close, boolean personal) {

	public boolean visibleTo(org.bukkit.entity.Player player) {
		return permission == null || permission.isBlank() || player.hasPermission(permission);
	}

	/** Comando listo para ejecutar, con %player% reemplazado. */
	public String commandFor(org.bukkit.entity.Player player) {
		return command.replace("%player%", player.getName());
	}
}
