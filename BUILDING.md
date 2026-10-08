# Building Logisim Sky 1.0

Logisim Sky targets Java 8 bytecode and is based on the classic Logisim 2.7.1 source tree with the Sky desktop extensions included in this repository.

## Requirements

1. JDK 8 or newer.
2. Original Logisim 2.7.1 JAR, placed at:
   `lib/logisim-generic-2.7.1.jar`
3. FlatLaf 3.6.2 JAR, placed at:
   `lib/flatlaf-3.6.2.jar`
4. Optional: Launch4j 3.50 to create the Windows `.exe` launcher.

The `lib/` directory intentionally does not contain dependency binaries.

## Build the JAR

From the repository root:

```bash
python tools/build.py
```

The resulting file is written to:

```text
dist/Logisim-Sky-1.0.jar
```

The build helper:

1. compiles all Java sources to Java 8 bytecode;
2. starts from the classic Logisim 2.7.1 runtime archive so its legacy helper libraries remain available;
3. overlays the current Logisim Sky classes, resources and documentation;
4. merges FlatLaf 3.6.2 runtime classes;
5. writes the Logisim Sky manifest and license/source documentation into the final JAR.

## Windows EXE

After the JAR has been built, Launch4j can wrap it using:

```text
packaging/launch4j.xml
```

The expected output name is:

```text
Logisim-Sky-1.0-Windows-x64.exe
```

You may need to adjust the local Launch4j executable path depending on your system.

## Important compatibility detail

`com.cburch.logisim.Main.VERSION` remains `2.7.1`. The Sky release version is tracked separately as `SKY_UI_VERSION = "1.0"`. This is intentional so classic `.circ` project compatibility is preserved.
