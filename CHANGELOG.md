# Changelog

## v1.0.2

- Added a Templates tab with editable examples for Boolean functions, CMOS gates, computer arithmetic, and multiplexers and ALUs.
- Save a selection or the active circuit as a reusable template. Templates are shared across projects on the same computer.
- Preview and place a template on the canvas with one click. Undo and redo include the inserted fragment and any subcircuits it requires.

## v1.0.1

### Fixed

- Released closed projects, removed subcircuits, and obsolete simulation states while preserving undo history.
- Stopped Live Analysis feedback from probes and removed listener buildup during truth-table calculations and RAM resets.
- Fixed repeated idle repainting in the properties panel and a busy loop after a manual tick in paused simulation.
- Reduced grid rendering work and logic analyzer allocations while preserving their output.

Classic Logisim 2.7.1 `.circ` compatibility is preserved.

## v1.0.0

First public Logisim Sky release.

### Added

- autosave and crash recovery;
- component command palette;
- circuit tabs and faster subcircuit workflow;
- live truth-table and Karnaugh preview;
- circuit diagnostics;
- logic-analyzer timeline;
- wire-network highlighting and live values;
- undo/redo history panel;
- combinational test cases;
- recent-project start screen and bundled examples;
- collapsible panels and focus mode;
- five UI themes;
- HiDPI-oriented desktop UI improvements.

### Compatibility

- project serializer remains compatible with classic Logisim 2.7.1;
- interface-specific state is stored outside `.circ` project files.
