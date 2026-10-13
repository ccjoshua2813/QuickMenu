package com.quickmenu;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Botones personales de cada jugador, guardados en playerdata/UUID.yml. */
public final class PlayerStore {
	public record Entry(String name, String command) {}

	private final File folder;
	private final Map<UUID, List<Entry>> cache = new HashMap<>();

	public PlayerStore(File dataFolder) {
		this.folder = new File(dataFolder, "playerdata");
		folder.mkdirs();
	}

	public synchronized List<Entry> get(UUID id) {
		return cache.computeIfAbsent(id, this::load);
	}

	public synchronized void add(UUID id, String name, String command) {
		get(id).add(new Entry(name, command));
		save(id);
	}

	public synchronized boolean remove(UUID id, int index) {
		List<Entry> list = get(id);
		if (index < 0 || index >= list.size()) return false;
		list.remove(index);
		save(id);
		return true;
	}

	public synchronized void forget(UUID id) {
		cache.remove(id);
	}

	private List<Entry> load(UUID id) {
		List<Entry> out = new ArrayList<>();
		File file = new File(folder, id + ".yml");
		if (!file.exists()) return out;
		YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
		for (Map<?, ?> m : yml.getMapList("buttons")) {
			Object n = m.get("name"), c = m.get("command");
			if (n != null && c != null) out.add(new Entry(n.toString(), c.toString()));
		}
		return out;
	}

	private void save(UUID id) {
		YamlConfiguration yml = new YamlConfiguration();
		List<Map<String, String>> maps = new ArrayList<>();
		for (Entry e : get(id)) maps.add(Map.of("name", e.name(), "command", e.command()));
		yml.set("buttons", maps);
		try {
			yml.save(new File(folder, id + ".yml"));
		} catch (IOException ex) {
			throw new RuntimeException("No se pudieron guardar los botones de " + id, ex);
		}
	}
}
