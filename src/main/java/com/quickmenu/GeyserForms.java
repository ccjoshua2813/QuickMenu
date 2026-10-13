package com.quickmenu;

import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Construye y envía formularios de Bedrock usando SIEMPRE las clases de Cumulus que carga Geyser.
 *
 * Si otro plugin (por ejemplo Floodgate) trae su propia copia de Cumulus, referenciar la clase Form
 * directamente hace que Java encuentre dos clases distintas y falle con un LinkageError. Aquí las clases
 * se piden al cargador de Geyser y se usan por reflexión, así nunca se mezclan.
 */
public final class GeyserForms {
	private static ClassLoader loader;

	private GeyserForms() {}

	/** Permite fijar el cargador a mano (solo para pruebas). */
	public static void initLoader(ClassLoader l) {
		loader = l;
	}

	public static void init(Plugin geyserPlugin) {
		loader = geyserPlugin.getClass().getClassLoader();
	}

	private static Class<?> cls(String name) throws ClassNotFoundException {
		return Class.forName(name, true, loader);
	}

	// ---------------------------------------------------------------- formularios
	/** Lista de botones; al pulsar uno se llama a {@code onClick} con su posición. */
	public static Object simple(String title, List<String> buttons, IntConsumer onClick) throws Exception {
		Object b = cls("org.geysermc.cumulus.form.SimpleForm").getMethod("builder").invoke(null);
		b = call(b, "title", title);
		for (String text : buttons) b = call(b, "button", text);
		Consumer<Object> handler = response -> {
			try {
				onClick.accept((Integer) call(response, "clickedButtonId"));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
		b = call(b, "validResultHandler", handler);
		return call(b, "build");
	}

	/** Cuadros de texto: cada elemento es {etiqueta, ejemplo}; devuelve lo escrito en el mismo orden. */
	public static Object inputs(String title, List<String[]> fields, Consumer<List<String>> onSubmit) throws Exception {
		Object b = cls("org.geysermc.cumulus.form.CustomForm").getMethod("builder").invoke(null);
		b = call(b, "title", title);
		for (String[] f : fields) b = call(b, "input", f[0], f[1]);
		Consumer<Object> handler = response -> {
			try {
				List<String> values = new ArrayList<>();
				for (int i = 0; i < fields.size(); i++) {
					Object v = call(response, "asInput", i);
					values.add(v == null ? "" : v.toString());
				}
				onSubmit.accept(values);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
		b = call(b, "validResultHandler", handler);
		return call(b, "build");
	}

	/** Lista desplegable; devuelve la posición elegida. */
	public static Object dropdown(String title, String label, List<String> options, IntConsumer onChoose) throws Exception {
		Object b = cls("org.geysermc.cumulus.form.CustomForm").getMethod("builder").invoke(null);
		b = call(b, "title", title);
		b = call(b, "dropdown", label, options);
		Consumer<Object> handler = response -> {
			try {
				onChoose.accept((Integer) call(response, "asDropdown", 0));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
		b = call(b, "validResultHandler", handler);
		return call(b, "build");
	}

	/** Envía el formulario a una conexión de Geyser. */
	public static void send(Object connection, Object form) throws Exception {
		Method m = cls("org.geysermc.geyser.api.connection.GeyserConnection")
			.getMethod("sendForm", cls("org.geysermc.cumulus.form.Form"));
		m.invoke(connection, form);
	}

	// ---------------------------------------------------------------- reflexión
	/** Llama a un método público por nombre, buscándolo en las interfaces y clases públicas del objeto. */
	private static Object call(Object target, String name, Object... args) throws Exception {
		for (Method m : publicMethods(target.getClass())) {
			if (!m.getName().equals(name) || m.getParameterCount() != args.length || m.isVarArgs()) continue;
			if (matches(m.getParameterTypes(), args)) return m.invoke(target, args);
		}
		throw new NoSuchMethodException(target.getClass().getName() + "#" + name);
	}

	private static boolean matches(Class<?>[] types, Object[] args) {
		for (int i = 0; i < types.length; i++) {
			Class<?> t = types[i];
			if (t.isPrimitive()) {
				t = t == int.class ? Integer.class : t == boolean.class ? Boolean.class : t;
			}
			if (args[i] == null || !t.isInstance(args[i])) return false;
		}
		return true;
	}

	/** Métodos públicos declarados en tipos públicos (los de clases internas no públicas no se pueden invocar). */
	private static List<Method> publicMethods(Class<?> type) {
		Set<Class<?>> seen = new LinkedHashSet<>();
		collect(type, seen);
		List<Method> out = new ArrayList<>();
		for (Class<?> c : seen) {
			if (!Modifier.isPublic(c.getModifiers())) continue;
			for (Method m : c.getMethods()) {
				if (Modifier.isPublic(m.getDeclaringClass().getModifiers())) out.add(m);
			}
		}
		return out;
	}

	private static void collect(Class<?> c, Set<Class<?>> out) {
		if (c == null || !out.add(c)) return;
		collect(c.getSuperclass(), out);
		for (Class<?> i : c.getInterfaces()) collect(i, out);
	}
}
