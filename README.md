<p align="center">
  <img src="assets/logisim-sky-icon.png" width="128" alt="Logisim Sky icon">
</p>

<h1 align="center">Logisim Sky</h1>

<p align="center">
  A modern desktop-focused derivative of classic Logisim 2.7.1.<br>
  Current public release: <strong>v1.0.0</strong>
</p>

## About

Logisim Sky keeps the classic Logisim 2.7.1 circuit format and simulation core while adding a more modern desktop workflow and interface.

## Highlights

- autosave every 45 seconds for modified projects;
- crash-recovery prompt for autosaved projects;
- component command palette on `Ctrl+K` / `Ctrl+P`;
- circuit tabs and quick subcircuit creation;
- live truth-table and Karnaugh-map preview;
- real-time diagnostics for floating pins and width mismatches;
- logic-analyzer timeline for selected wire networks;
- full wire-network highlighting with live values;
- undo/redo history panel;
- built-in combinational test cases;
- start screen with recent projects and bundled examples;
- collapsible side panels and focus mode;
- Midnight, Light, Aurora, Ocean and Ember themes;
- HiDPI UI, zoom and middle-button canvas panning.

## Compatibility

Logisim Sky keeps the classic Logisim `2.7.1` project serializer. UI state, analysis panels, tests, history, autosave metadata and themes are stored outside `.circ` files.

## Downloads

Prebuilt packages are published on the GitHub **Releases** page, not committed into the source repository.

For v1.0.0 the release is intended to contain:

- `Logisim-Sky-1.0-Apple-Silicon.zip` - macOS Apple Silicon;
- `Logisim-Sky-1.0.jar` - cross-platform Java archive;
- `Logisim-Sky-1.0-Windows-x64.exe` - Windows x64;

## Building from source

See [BUILDING.md](BUILDING.md).

The source tree targets Java 8 bytecode. The legacy Logisim runtime dependencies can be supplied through the original Logisim 2.7.1 JAR; FlatLaf 3.6.2 is used by the modern UI.

## Project layout

```text
src/                 Java source code
resources/           application resources and bundled examples
doc/                 classic Logisim help files
assets/              repository artwork
licenses/            third-party license texts
packaging/            manifest and Windows launcher config
tools/                local build helper
lib/                  dependency instructions only
```

## License

Logisim Sky is distributed under the GNU General Public License, version 2 or, at your option, any later version. See [COPYING.TXT](COPYING.TXT).

FlatLaf and launcher-related notices are listed in [THIRD-PARTY-NOTICES.txt](THIRD-PARTY-NOTICES.txt).

## Credits

Logisim was created by Carl Burch. Logisim Sky is a derivative project built on the classic Logisim 2.7.1 codebase.
