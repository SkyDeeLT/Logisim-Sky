package com.cburch.logisim.gui.main;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingUtilities;

import com.cburch.logisim.DarkTheme;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.file.LogisimFileActions;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.wiring.Tunnel;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;

/** Places a circuit fragment with the same canvas coordinates and undo model as the component tools. */
public final class TemplatePlacementTool extends Tool {
    private final String name;
    private final LogisimFile destination;
    private final Circuit target;
    private final Tool previous;
    private final List<Component> components = new ArrayList<>();
    private final List<Circuit> dependencies = new ArrayList<>();
    private final List<Library> libraries = new ArrayList<>();
    private final int originX;
    private final int originY;
    private Map<String, String> tunnelLabels;
    private Location cursor;

    private TemplatePlacementTool(Project project, LogisimFile template, String name) throws IOException {
        this.name = name;
        destination = project.getLogisimFile();
        target = project.getCurrentCircuit();
        Tool active = project.getTool();
        previous = active instanceof TemplatePlacementTool ? ((TemplatePlacementTool) active).previous : active;

        Circuit main = template.getMainCircuit();
        if (!destination.contains(target)) throw new IOException(Strings.get("templatePlacementReadOnly"));
        if (main == null || (main.getNonWires().isEmpty() && main.getWires().isEmpty())) {
            throw new IOException(Strings.get("templatePlacementEmpty"));
        }
        components.addAll(main.getNonWires());
        components.addAll(main.getWires());
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        for (Component component : components) {
            Bounds bounds = component.getBounds();
            minX = Math.min(minX, bounds.getX());
            minY = Math.min(minY, bounds.getY());
        }
        originX = Math.floorDiv(minX, 10) * 10;
        originY = Math.floorDiv(minY, 10) * 10;

        Set<ComponentFactory> factories = new HashSet<>();
        collectDependencies(template, main, new HashSet<Circuit>(), factories);
        for (Library library : template.getLibraries()) {
            if (destination.getLibraries().contains(library)) continue;
            for (ComponentFactory factory : factories) {
                if (library.contains(factory)) {
                    libraries.add(library);
                    break;
                }
            }
        }
        tunnelLabels = planTunnelLabels();
    }

    public static void start(Project project, LogisimFile template, String name) throws IOException {
        TemplatePlacementTool placement = new TemplatePlacementTool(project, template, name);
        Frame frame = project.getFrame();
        project.setStartupScreen(false);
        frame.setEditorView(Frame.EDIT_LAYOUT);
        project.setTool(placement);
        frame.getCanvas().requestFocusInWindow();
    }

    private void collectDependencies(LogisimFile template, Circuit circuit, Set<Circuit> visiting,
            Set<ComponentFactory> factories) throws IOException {
        if (!visiting.add(circuit)) throw new IOException(Strings.get("templatePlacementDependency"));
        for (Component component : circuit.getNonWires()) {
            ComponentFactory factory = component.getFactory();
            if (factory instanceof SubcircuitFactory) {
                Circuit dependency = ((SubcircuitFactory) factory).getSubcircuit();
                if (dependencies.contains(dependency)) continue;
                if (!template.contains(dependency)) throw new IOException(Strings.get("templatePlacementDependency"));
                collectDependencies(template, dependency, visiting, factories);
                dependencies.add(dependency);
            } else {
                factories.add(factory);
            }
        }
        visiting.remove(circuit);
    }

    @Override public String getName() { return "Template Placement"; }
    @Override public String getDisplayName() { return Strings.get("templatePlacementAction", name); }
    @Override public String getDescription() { return Strings.get("templatePlacementHint"); }

    private boolean isCurrent(Canvas canvas) {
        return canvas.getProject().getLogisimFile() == destination && canvas.getCircuit() == target;
    }

    private static Location gridLocation(MouseEvent event) {
        return Location.create(Canvas.snapXToGrid(event.getX()), Canvas.snapYToGrid(event.getY()));
    }

    @Override public void mouseMoved(Canvas canvas, Graphics g, MouseEvent event) {
        Location next = gridLocation(event);
        if (next.equals(cursor)) return;
        cursor = next;
        canvas.repaint();
    }

    @Override public void mouseEntered(Canvas canvas, Graphics g, MouseEvent event) {
        mouseMoved(canvas, g, event);
    }

    @Override public void mouseExited(Canvas canvas, Graphics g, MouseEvent event) {
        deselect(canvas);
    }

    @Override public void deselect(Canvas canvas) {
        if (cursor != null) {
            cursor = null;
            canvas.repaint();
        }
    }

    @Override public void keyPressed(Canvas canvas, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.VK_ESCAPE) {
            canvas.getProject().setTool(previous);
            event.consume();
        }
    }

    @Override public void draw(Canvas canvas, ComponentDrawContext context) {
        if (cursor == null || !isCurrent(canvas)) return;
        int dx = cursor.getX() - originX;
        int dy = cursor.getY() - originY;
        Color color = DarkTheme.ACCENT;
        Graphics graphics = context.getGraphics();
        for (Component component : components) {
            if (component instanceof Wire) {
                Wire wire = (Wire) component;
                Location first = wire.getEndLocation(0).translate(dx, dy);
                Location second = wire.getEndLocation(1).translate(dx, dy);
                graphics.setColor(color);
                graphics.drawLine(first.getX(), first.getY(), second.getX(), second.getY());
            } else {
                Location location = component.getLocation().translate(dx, dy);
                component.getFactory().drawGhost(context, color, location.getX(), location.getY(),
                        placementAttributes(component, false));
            }
        }
    }

    @Override public void mousePressed(Canvas canvas, Graphics g, MouseEvent event) {
        if (!SwingUtilities.isLeftMouseButton(event)) return;
        Project project = canvas.getProject();
        if (!isCurrent(canvas) || !destination.contains(target)) {
            project.setTool(previous);
            canvas.setErrorMessage(Strings.getter("templatePlacementCircuitChanged"));
            return;
        }
        Location anchor = gridLocation(event);
        int dx = anchor.getX() - originX;
        int dy = anchor.getY() - originY;
        tunnelLabels = planTunnelLabels();
        List<Component> translated = new ArrayList<>(components.size());
        for (Component component : components) {
            Component copy = translate(component, dx, dy);
            Bounds bounds = copy.getBounds();
            if (bounds.getX() < 0 || bounds.getY() < 0 || conflicts(copy)) {
                canvas.setErrorMessage(Strings.getter("templatePlacementConflict"));
                return;
            }
            translated.add(copy);
        }

        renameDependencies();
        List<Action> prerequisites = new ArrayList<>();
        if (!libraries.isEmpty()) {
            prerequisites.add(LogisimFileActions.loadLibraries(libraries.toArray(new Library[0])));
        }
        for (Circuit circuit : dependencies) prerequisites.add(LogisimFileActions.addCircuit(circuit));
        CircuitMutation mutation = new CircuitMutation(target);
        mutation.addAll(translated);
        project.doAction(new Placement(getDisplayName(), prerequisites, mutation.toAction(null)));
        project.setTool(previous);
        event.consume();
    }

    private boolean conflicts(Component component) {
        if (target.hasConflict(component)) return true;
        if (component instanceof Wire) return false;
        for (Component existing : target.getAllContaining(component.getLocation())) {
            if (!(existing instanceof Wire) && existing.getBounds().equals(component.getBounds())) return true;
        }
        return false;
    }

    private Component translate(Component component, int dx, int dy) {
        if (component instanceof Wire) {
            Wire wire = (Wire) component;
            return Wire.create(wire.getEndLocation(0).translate(dx, dy), wire.getEndLocation(1).translate(dx, dy));
        }
        return component.getFactory().createComponent(component.getLocation().translate(dx, dy),
                placementAttributes(component, true));
    }

    private AttributeSet placementAttributes(Component component, boolean copy) {
        AttributeSet attrs = component.getAttributeSet();
        String label = tunnelLabel(component);
        String renamed = label == null ? null : tunnelLabels.get(label);
        if (copy || renamed != null) attrs = (AttributeSet) attrs.clone();
        if (renamed != null) attrs.setValue(StdAttr.LABEL, renamed);
        return attrs;
    }

    private static String tunnelLabel(Component component) {
        if (!(component.getFactory() instanceof Tunnel)) return null;
        String label = component.getAttributeSet().getValue(StdAttr.LABEL).trim();
        return label.isEmpty() ? null : label;
    }

    private Map<String, String> planTunnelLabels() {
        Set<String> taken = new HashSet<>();
        for (Component component : target.getNonWires()) {
            String label = tunnelLabel(component);
            if (label != null) taken.add(label);
        }
        Set<String> sourceLabels = new HashSet<>();
        for (Component component : components) {
            String label = tunnelLabel(component);
            if (label != null) sourceLabels.add(label);
        }
        Set<String> reserved = new HashSet<>(sourceLabels);
        reserved.addAll(taken);
        Map<String, String> renames = new HashMap<>();
        for (String label : sourceLabels) {
            if (!taken.contains(label)) continue;
            int suffix = 2;
            String candidate = label + "_" + suffix;
            while (!reserved.add(candidate)) candidate = label + "_" + (++suffix);
            renames.put(label, candidate);
        }
        return renames;
    }

    private void renameDependencies() {
        Set<String> taken = new HashSet<>();
        for (Tool tool : destination.getTools()) taken.add(tool.getName());
        for (Circuit circuit : dependencies) {
            String base = circuit.getName();
            String candidate = base;
            for (int suffix = 2; !taken.add(candidate); suffix++) candidate = base + "_" + suffix;
            if (!base.equals(candidate)) circuit.setName(candidate);
        }
    }

    private static final class Placement extends Action {
        private final String name;
        private final List<Action> prerequisites;
        private final Action mutation;

        Placement(String name, List<Action> prerequisites, Action mutation) {
            this.name = name;
            this.prerequisites = prerequisites;
            this.mutation = mutation;
        }

        @Override public String getName() { return name; }

        @Override public void doIt(Project project) {
            for (Action action : prerequisites) action.doIt(project);
            mutation.doIt(project);
        }

        @Override public void undo(Project project) {
            mutation.undo(project);
            for (int i = prerequisites.size() - 1; i >= 0; i--) prerequisites.get(i).undo(project);
        }
    }
}
