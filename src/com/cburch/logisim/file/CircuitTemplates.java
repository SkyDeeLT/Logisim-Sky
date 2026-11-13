package com.cburch.logisim.file;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.Builtin;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.util.LocaleManager;

public final class CircuitTemplates {
	private static final String RESOURCES = "/resources/logisim/templates/";
	private static final String CIRCUIT_FILE = "circuit.circ";
	private static final String METADATA_FILE = "metadata.properties";

	public static final class Entry {
		private final String file;
		private final String[] catalogFields;
		private final String name;
		private final String category;

		private Entry(String[] fields) {
			file = fields[0];
			catalogFields = fields;
			name = null;
			category = null;
		}

		private Entry(String id, String name, String category) {
			file = id;
			catalogFields = null;
			this.name = name;
			this.category = category;
		}

		public String getId() { return (isBuiltin() ? "builtin:" : "user:") + file; }
		public boolean isBuiltin() { return catalogFields != null; }
		public String getName() { return isBuiltin() ? localizedField(3) : name; }
		public String getCategory() {
			if (isBuiltin()) return localizedField(1);
			return category.isEmpty() ? Strings.get("circuitTemplatesUserCategory") : category;
		}
		public String getDescription() { return isBuiltin() ? localizedField(5) : ""; }

		private String localizedField(int russianIndex) {
			return catalogFields[russianIndex + ("ru".equals(LocaleManager.getLocale().getLanguage()) ? 0 : 1)];
		}
	}

	private CircuitTemplates() { }

	public static List<Entry> list() throws IOException {
		List<Entry> entries = listBuiltins();
		List<Entry> personal = new ArrayList<Entry>();
		Path directory = directory();
		if (Files.exists(directory)) {
			try (DirectoryStream<Path> records = Files.newDirectoryStream(directory)) {
				for (Path record : records) {
					String id = record.getFileName().toString();
					if (!isUserId(id) || !Files.isDirectory(record, LinkOption.NOFOLLOW_LINKS)
							|| !Files.isRegularFile(record.resolve(CIRCUIT_FILE), LinkOption.NOFOLLOW_LINKS)
							|| !Files.isRegularFile(record.resolve(METADATA_FILE), LinkOption.NOFOLLOW_LINKS)) {
						continue;
					}
					Properties metadata = new Properties();
					try (InputStream input = Files.newInputStream(record.resolve(METADATA_FILE))) {
						metadata.load(input);
					} catch (IOException | IllegalArgumentException invalidMetadata) {
						continue;
					}
					String name = metadata.getProperty("name", "").trim();
					if (!name.isEmpty()) {
						personal.add(new Entry(id, name, metadata.getProperty("category", "").trim()));
					}
				}
			}
		}
		Collator collator = Collator.getInstance(LocaleManager.getLocale());
		personal.sort((left, right) -> {
			int byCategory = collator.compare(left.getCategory(), right.getCategory());
			if (byCategory != 0) return byCategory;
			int byName = collator.compare(left.getName(), right.getName());
			return byName == 0 ? left.getId().compareTo(right.getId()) : byName;
		});
		entries.addAll(personal);
		return entries;
	}

	public static LogisimFile load(Entry entry) throws IOException {
		return load(entry, null);
	}

	public static LogisimFile load(Entry entry, Loader destination) throws IOException {
		try (InputStream input = entry.isBuiltin()
				? openResource(entry.file)
				: openPersonal(entry)) {
			LogisimFile file = new XmlReader(new TemplateLoader(destination), true).readLibrary(input);
			Circuit main = file.getMainCircuit();
			if (main.getNonWires().isEmpty() && main.getWires().isEmpty()) {
				throw new IOException(Strings.get("circuitTemplatesEmpty"));
			}
			return file;
		} catch (SAXException | RuntimeException invalidTemplate) {
			throw new IOException(Strings.get("circuitTemplatesInvalid", detail(invalidTemplate)), invalidTemplate);
		}
	}

	public static Entry save(String name, String category, Project project,
			Collection<Component> selection) throws IOException {
		String title = name.trim();
		if (title.isEmpty()) throw new IOException(Strings.get("circuitTemplatesEmptyName"));
		if (selection.isEmpty()) throw new IOException(Strings.get("circuitTemplatesEmpty"));
		Entry entry = new Entry(UUID.randomUUID().toString(), title, category == null ? "" : category.trim());

		LogisimFile source = project.getLogisimFile();
		Circuit main = project.getCurrentCircuit();
		if (source.getLoader() == null || main == null || !source.contains(main)) {
			throw new IOException(Strings.get("circuitTemplatesOwned"));
		}
		Set<Component> components = new LinkedHashSet<Component>(selection);
		Set<Library> libraries = new LinkedHashSet<Library>();
		Map<Circuit, Collection<Component>> circuits = new LinkedHashMap<Circuit, Collection<Component>>();
		collectDependencies(source, main, components, circuits, libraries, new HashSet<Circuit>());
		boolean fullCircuit = components.equals(allComponents(main));

		Path parent = Files.createDirectories(directory());
		Path staging = Files.createTempDirectory(parent, ".save-");
		try {
			try (OutputStream output = Files.newOutputStream(staging.resolve(CIRCUIT_FILE))) {
				XmlWriter.writeTemplate(source, output, new TemplateLoader(source.getLoader()),
						main, circuits, libraries, fullCircuit);
			} catch (ParserConfigurationException | TransformerException | LoaderException failedWrite) {
				throw new IOException(Strings.get("circuitTemplatesInvalid", detail(failedWrite)), failedWrite);
			}
			Properties metadata = new Properties();
			metadata.setProperty("name", entry.name);
			metadata.setProperty("category", entry.category);
			try (OutputStream output = Files.newOutputStream(staging.resolve(METADATA_FILE))) {
				metadata.store(output, "Logisim Sky");
			}
			move(staging, parent.resolve(entry.file));
			return entry;
		} finally {
			removeRecord(staging);
		}
	}

	public static void remove(Entry entry) throws IOException {
		if (entry.isBuiltin()) throw new IOException(Strings.get("circuitTemplatesBuiltin"));
		Path parent = directory();
		Path removed = parent.resolve(".removed-" + UUID.randomUUID());
		move(parent.resolve(entry.file), removed);
		removeRecord(removed);
	}

	private static void collectDependencies(LogisimFile source, Circuit circuit, Collection<Component> components,
			Map<Circuit, Collection<Component>> circuits, Set<Library> libraries, Set<Circuit> visiting)
			throws IOException {
		if (visiting.contains(circuit)) {
			throw new IOException(Strings.get("circuitTemplatesCycle", circuit.getName()));
		}
		if (circuits.containsKey(circuit)) return;
		visiting.add(circuit);
		circuits.put(circuit, components);
		for (Component component : components) {
			if (component instanceof Wire) continue;
			ComponentFactory factory = component.getFactory();
			if (factory instanceof SubcircuitFactory) {
				Circuit dependency = ((SubcircuitFactory) factory).getSubcircuit();
				if (!source.contains(dependency)) {
					throw new IOException(Strings.get("circuitTemplatesExternal", factory.getDisplayName()));
				}
				collectDependencies(source, dependency, allComponents(dependency), circuits, libraries, visiting);
			} else {
				Library library = findBuiltin(source.getLoader().getBuiltin(), factory);
				if (library == null) {
					throw new IOException(Strings.get("circuitTemplatesExternal", factory.getDisplayName()));
				}
				libraries.add(library);
			}
		}
		visiting.remove(circuit);
	}

	private static Set<Component> allComponents(Circuit circuit) {
		Set<Component> components = new LinkedHashSet<Component>(circuit.getWires());
		components.addAll(circuit.getNonWires());
		return components;
	}

	private static Library findBuiltin(Builtin builtin, ComponentFactory factory) {
		for (Library library : builtin.getLibraries()) {
			if (library.contains(factory)) return library;
		}
		return null;
	}

	public static List<Entry> listBuiltins() throws IOException {
		List<Entry> entries = new ArrayList<Entry>();
		Set<String> files = new HashSet<String>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(openResource("catalog.tsv"),
				StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.trim().isEmpty() || line.startsWith("#")) continue;
				String[] fields = line.split("\t", -1);
				if (fields.length != 7 || !fields[0].matches("[a-zA-Z0-9_-]+\\.circ") || !files.add(fields[0])) {
					throw new IOException(Strings.get("circuitTemplatesCatalog"));
				}
				entries.add(new Entry(fields));
			}
		}
		return entries;
	}

	private static InputStream openResource(String name) throws IOException {
		InputStream input = CircuitTemplates.class.getResourceAsStream(RESOURCES + name);
		if (input == null) throw new IOException(Strings.get("circuitTemplatesMissing", name));
		return input;
	}

	private static InputStream openPersonal(Entry entry) throws IOException {
		Path record = directory().resolve(entry.file);
		Path file = record.resolve(CIRCUIT_FILE);
		if (!Files.isDirectory(record, LinkOption.NOFOLLOW_LINKS)
				|| !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
			throw new IOException(Strings.get("circuitTemplatesMissing", entry.getName()));
		}
		return Files.newInputStream(file, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
	}

	private static Path directory() {
		return Paths.get(System.getProperty("user.home"), ".logisim-sky", "templates");
	}

	private static boolean isUserId(String id) {
		try {
			return UUID.fromString(id).toString().equals(id);
		} catch (IllegalArgumentException invalidId) {
			return false;
		}
	}

	private static void move(Path source, Path destination) throws IOException {
		try {
			Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException unsupported) {
			Files.move(source, destination);
		}
	}

	private static void removeRecord(Path record) throws IOException {
		if (Files.isDirectory(record, LinkOption.NOFOLLOW_LINKS)) {
			Files.deleteIfExists(record.resolve(METADATA_FILE));
			Files.deleteIfExists(record.resolve(CIRCUIT_FILE));
		}
		Files.deleteIfExists(record);
	}

	private static String detail(Exception error) {
		return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
	}

	private static final class TemplateLoader extends Loader {
		private final Builtin builtin;

		TemplateLoader(Loader destination) {
			super(null);
			builtin = destination == null ? super.getBuiltin() : destination.getBuiltin();
		}

		@Override
		public Builtin getBuiltin() { return builtin; }

		@Override
		public Library loadLibrary(String desc) {
			if (!desc.startsWith("#")) {
				throw new LoaderException(Strings.get("circuitTemplatesExternal", desc));
			}
			Library library = builtin.getLibrary(desc.substring(1));
			if (library == null) {
				throw new LoaderException(Strings.get("circuitTemplatesExternal", desc));
			}
			return library;
		}

		@Override
		public void showError(String description) {
			throw new LoaderException(description);
		}
	}
}
