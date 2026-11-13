/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.file;

import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.cburch.draw.model.AbstractCanvasObject;
import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.Main;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeDefaultProvider;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.InputEventUtil;
import com.cburch.logisim.util.StringUtil;

class XmlWriter {
	static void write(LogisimFile file, OutputStream out, LibraryLoader loader)
			throws ParserConfigurationException,
				TransformerConfigurationException, TransformerException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		Document doc = docBuilder.newDocument();
		XmlWriter context = new XmlWriter(file, doc, loader, file.getLibraries());
		context.fromLogisimFile();

		writeDocument(doc, out);
	}

	static void writeTemplate(LogisimFile file, OutputStream out, LibraryLoader loader, Circuit main,
			Map<Circuit, Collection<Component>> circuits, Set<Library> libraries, boolean fullCircuit)
			throws ParserConfigurationException, TransformerException {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		XmlWriter writer = new XmlWriter(file, doc, loader, libraries);
		Element project = writer.createProjectElement();
		for (Library library : libraries) {
			project.appendChild(writer.fromLibrary(library, false));
		}
		project.appendChild(writer.fromMainCircuit(main));
		for (Map.Entry<Circuit, Collection<Component>> entry : circuits.entrySet()) {
			Circuit circuit = entry.getKey();
			Element element;
			if (circuit == main && !fullCircuit) {
				element = writer.fromCircuitHeader(circuit, false);
				for (Component component : entry.getValue()) {
					element.appendChild(component instanceof Wire
							? writer.fromWire((Wire) component)
							: writer.fromComponent(component));
				}
			} else {
				element = writer.fromCircuit(circuit);
			}
			project.appendChild(element);
		}
		writeDocument(doc, out);
	}

	private static void writeDocument(Document doc, OutputStream out)
			throws TransformerException {
		TransformerFactory tfFactory = TransformerFactory.newInstance();
		try {
			tfFactory.setAttribute("indent-number", Integer.valueOf(2));
		} catch (IllegalArgumentException e) { }
		Transformer tf = tfFactory.newTransformer();
		tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		tf.setOutputProperty(OutputKeys.INDENT, "yes");
		try {
			tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", 
					"2");
		} catch (IllegalArgumentException e) { }

		Source src = new DOMSource(doc);
		Result dest = new StreamResult(out);
		tf.transform(src, dest);
	}

	private final LogisimFile file;
	private final Document doc;
	private final LibraryLoader loader;
	private final Iterable<Library> componentLibraries;
	private final HashMap<Library,String> libs = new HashMap<Library,String>();

	private XmlWriter(LogisimFile file, Document doc, LibraryLoader loader,
			Iterable<Library> componentLibraries) {
		this.file = file;
		this.doc = doc;
		this.loader = loader;
		this.componentLibraries = componentLibraries;
	}

	private Element createProjectElement() {
		Element ret = doc.createElement("project");
		doc.appendChild(ret);
		ret.appendChild(doc.createTextNode("\nThis file is intended to be "
				+ "loaded by Logisim (http://www.cburch.com/logisim/).\n"));
		ret.setAttribute("version", "1.0");
		ret.setAttribute("source", Main.VERSION_NAME);
		return ret;
	}

	Element fromLogisimFile() {
		Element ret = createProjectElement();
		for (Library lib : file.getLibraries()) {
			Element elt = fromLibrary(lib, true);
			if (elt != null) ret.appendChild(elt);
		}

		if (file.getMainCircuit() != null) {
			ret.appendChild(fromMainCircuit(file.getMainCircuit()));
		}

		ret.appendChild(fromOptions());
		ret.appendChild(fromMouseMappings());
		ret.appendChild(fromToolbarData());

		for (Circuit circ : file.getCircuits()) {
			ret.appendChild(fromCircuit(circ));
		}
		return ret;
	}

	private Element fromMainCircuit(Circuit circuit) {
		Element element = doc.createElement("main");
		element.setAttribute("name", circuit.getName());
		return element;
	}

	Element fromLibrary(Library lib, boolean writeToolDefaults) {
		Element ret = doc.createElement("lib");
		if (libs.containsKey(lib)) return null;
		String name = "" + libs.size();
		String desc = loader.getDescriptor(lib);
		if (desc == null) {
			loader.showError("library location unknown: "
				+ lib.getName());
			return null;
		}
		libs.put(lib, name);
		ret.setAttribute("name", name);
		ret.setAttribute("desc", desc);
		if (!writeToolDefaults) return ret;
		for (Tool t : lib.getTools()) {
			AttributeSet attrs = t.getAttributeSet();
			if (attrs != null) {
				Element toAdd = doc.createElement("tool");
				toAdd.setAttribute("name", t.getName());
				addAttributeSetContent(toAdd, attrs, t);
				if (toAdd.getChildNodes().getLength() > 0) {
					ret.appendChild(toAdd);
				}
			}
		}
		return ret;
	}

	Element fromOptions() {
		Element elt = doc.createElement("options");
		addAttributeSetContent(elt, file.getOptions().getAttributeSet(), null);
		return elt;
	}

	Element fromMouseMappings() {
		Element elt = doc.createElement("mappings");
		MouseMappings map = file.getOptions().getMouseMappings();
		for (Map.Entry<Integer,Tool> entry : map.getMappings().entrySet()) {
			Integer mods = entry.getKey();
			Tool tool = entry.getValue();
			Element toolElt = fromTool(tool);
			String mapValue = InputEventUtil.toString(mods.intValue());
			toolElt.setAttribute("map", mapValue);
			elt.appendChild(toolElt);
		}
		return elt;
	}

	Element fromToolbarData() {
		Element elt = doc.createElement("toolbar");
		ToolbarData toolbar = file.getOptions().getToolbarData();
		for (Tool tool : toolbar.getContents()) {
			if (tool == null) {
				elt.appendChild(doc.createElement("sep"));
			} else {
				elt.appendChild(fromTool(tool));
			}
		}
		return elt;
	}

	Element fromTool(Tool tool) {
		Library lib = findLibrary(tool);
		String lib_name;
		if (lib == null) {
			loader.showError(StringUtil.format("tool `%s' not found",
				tool.getDisplayName()));
			return null;
		} else if (lib == file) {
			lib_name = null;
		} else {
			lib_name = libs.get(lib);
			if (lib_name == null) {
				loader.showError("unknown library within file");
				return null;
			}
		}

		Element elt = doc.createElement("tool");
		if (lib_name != null) elt.setAttribute("lib", lib_name);
		elt.setAttribute("name", tool.getName());
		addAttributeSetContent(elt, tool.getAttributeSet(), tool);
		return elt;
	}

	private Element fromCircuitHeader(Circuit circuit, boolean writeAppearance) {
		Element ret = doc.createElement("circuit");
		ret.setAttribute("name", circuit.getName());
		addAttributeSetContent(ret, circuit.getStaticAttributes(), null);
		if (writeAppearance && !circuit.getAppearance().isDefaultAppearance()) {
			Element appear = doc.createElement("appear");
			for (Object o : circuit.getAppearance().getObjectsFromBottom()) {
				if (o instanceof AbstractCanvasObject) {
					Element elt = ((AbstractCanvasObject) o).toSvgElement(doc);
					if (elt != null) {
						appear.appendChild(elt);
					}
				}
			}
			ret.appendChild(appear);
		}
		return ret;
	}

	Element fromCircuit(Circuit circuit) {
		Element ret = fromCircuitHeader(circuit, true);
		for (Wire w : circuit.getWires()) {
			ret.appendChild(fromWire(w));
		}
		for (Component comp : circuit.getNonWires()) {
			Element elt = fromComponent(comp);
			if (elt != null) ret.appendChild(elt);
		}
		return ret;
	}

	Element fromComponent(Component comp) {
		ComponentFactory source = comp.getFactory();
		Library lib = findLibrary(source);
		String lib_name;
		if (lib == null) {
			loader.showError(source.getName() + " component not found");
			return null;
		} else if (lib == file) {
			lib_name = null;
		} else {
			lib_name = libs.get(lib);
			if (lib_name == null) {
				loader.showError("unknown library within file");
				return null;
			}
		}

		Element ret = doc.createElement("comp");
		if (lib_name != null) ret.setAttribute("lib", lib_name);
		ret.setAttribute("name", source.getName());
		ret.setAttribute("loc", comp.getLocation().toString());
		addAttributeSetContent(ret, comp.getAttributeSet(), comp.getFactory());
		return ret;
	}

	Element fromWire(Wire w) {
		Element ret = doc.createElement("wire");
		ret.setAttribute("from", w.getEnd0().toString());
		ret.setAttribute("to", w.getEnd1().toString());
		return ret;
	}

	void addAttributeSetContent(Element elt, AttributeSet attrs,
			AttributeDefaultProvider source) {
		if (attrs == null) return;
		LogisimVersion ver = Main.VERSION;
		if (source != null && source.isAllDefaultValues(attrs, ver)) return;
		for (Attribute<?> attrBase : attrs.getAttributes()) {
			@SuppressWarnings("unchecked")
			Attribute<Object> attr = (Attribute<Object>) attrBase;
			Object val = attrs.getValue(attr);
			if (attrs.isToSave(attr) && val != null) {
				Object dflt = source == null ? null : source.getDefaultAttributeValue(attr, ver);
				if (dflt == null || !dflt.equals(val)) {
					Element a = doc.createElement("a");
					a.setAttribute("name", attr.getName());
					String value = attr.toStandardString(val);
					if (value.indexOf("\n") >= 0) {
						a.appendChild(doc.createTextNode(value));
					} else {
						a.setAttribute("val", attr.toStandardString(val));
					}
					elt.appendChild(a);
				}
			}
		}
	}

	Library findLibrary(Tool tool) {
		if (libraryContains(file, tool)) {
			return file;
		}
		for (Library lib : file.getLibraries()) {
			if (libraryContains(lib, tool)) return lib;
		}
		return null;
	}

	Library findLibrary(ComponentFactory source) {
		if (file.contains(source)) {
			return file;
		}
		for (Library lib : componentLibraries) {
			if (lib.contains(source)) return lib;
		}
		return null;
	}

	boolean libraryContains(Library lib, Tool query) {
		for (Tool tool : lib.getTools()) {
			if (tool.sharesSource(query)) return true;
		}
		return false;
	}
}
