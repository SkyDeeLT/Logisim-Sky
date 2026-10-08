#!/usr/bin/env python3
from __future__ import annotations

import os
import shutil
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src"
BASE_JAR = ROOT / "lib" / "logisim-generic-2.7.1.jar"
FLATLAF_JAR = ROOT / "lib" / "flatlaf-3.6.2.jar"
BUILD = ROOT / "build"
CLASSES = BUILD / "classes"
STAGE = BUILD / "stage"
DIST = ROOT / "dist"
OUT_JAR = DIST / "Logisim-Sky-1.0.jar"
MANIFEST = ROOT / "packaging" / "MANIFEST.MF"


def die(message: str) -> None:
    print(f"ERROR: {message}", file=sys.stderr)
    raise SystemExit(1)


def safe_extract(jar: Path, target: Path, skip_meta: bool = False) -> None:
    with zipfile.ZipFile(jar) as zf:
        for info in zf.infolist():
            name = info.filename
            upper = name.upper()
            if skip_meta and (
                upper == "META-INF/MANIFEST.MF"
                or upper.endswith(".SF")
                or upper.endswith(".RSA")
                or upper.endswith(".DSA")
            ):
                continue
            dest = (target / name).resolve()
            if target.resolve() not in dest.parents and dest != target.resolve():
                die(f"Unsafe path in archive: {name}")
            if info.is_dir():
                dest.mkdir(parents=True, exist_ok=True)
            else:
                dest.parent.mkdir(parents=True, exist_ok=True)
                with zf.open(info) as src, open(dest, "wb") as out:
                    shutil.copyfileobj(src, out)


def copy_tree(src: Path, dst: Path) -> None:
    if not src.exists():
        return
    for path in src.rglob("*"):
        rel = path.relative_to(src)
        out = dst / rel
        if path.is_dir():
            out.mkdir(parents=True, exist_ok=True)
        else:
            out.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(path, out)


def main() -> None:
    if not BASE_JAR.exists():
        die(f"Missing {BASE_JAR.relative_to(ROOT)}")
    if not FLATLAF_JAR.exists():
        die(f"Missing {FLATLAF_JAR.relative_to(ROOT)}")
    if not MANIFEST.exists():
        die("Missing packaging/MANIFEST.MF")

    shutil.rmtree(BUILD, ignore_errors=True)
    CLASSES.mkdir(parents=True)
    STAGE.mkdir(parents=True)
    DIST.mkdir(parents=True, exist_ok=True)

    java_files = sorted(str(p) for p in SRC.rglob("*.java"))
    if not java_files:
        die("No Java source files found")

    javac = shutil.which("javac")
    if not javac:
        die("javac was not found in PATH")

    version = subprocess.run(
        [javac, "-version"], capture_output=True, text=True, check=False
    )
    version_text = (version.stdout + version.stderr).strip()
    major = 8
    try:
        token = version_text.split()[-1]
        major = int(token.split(".")[0] if not token.startswith("1.") else token.split(".")[1])
    except Exception:
        pass

    cp = os.pathsep.join([str(BASE_JAR), str(FLATLAF_JAR)])
    cmd = [javac, "-encoding", "UTF-8", "-cp", cp, "-d", str(CLASSES)]
    if major >= 9:
        cmd += ["--release", "8"]
    else:
        cmd += ["-source", "8", "-target", "8"]
    cmd += java_files

    print("Compiling source...")
    subprocess.run(cmd, check=True)

    print("Preparing runtime archive...")
    safe_extract(BASE_JAR, STAGE, skip_meta=False)
    copy_tree(CLASSES, STAGE)
    copy_tree(ROOT / "resources", STAGE / "resources")
    copy_tree(ROOT / "doc", STAGE / "doc")
    copy_tree(ROOT / "licenses", STAGE / "licenses")
    copy_tree(ROOT / "src", STAGE / "src")

    # Merge FlatLaf runtime classes and resources without replacing our manifest.
    safe_extract(FLATLAF_JAR, STAGE, skip_meta=True)

    for name in ["README.md", "BUILDING.md", "COPYING.TXT", "THIRD-PARTY-NOTICES.txt"]:
        src = ROOT / name
        if src.exists():
            shutil.copy2(src, STAGE / name)

    meta = STAGE / "META-INF"
    meta.mkdir(parents=True, exist_ok=True)
    shutil.copy2(MANIFEST, meta / "MANIFEST.MF")

    print(f"Creating {OUT_JAR.relative_to(ROOT)}...")
    if OUT_JAR.exists():
        OUT_JAR.unlink()
    with zipfile.ZipFile(OUT_JAR, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as zf:
        for path in sorted(STAGE.rglob("*")):
            if path.is_file():
                zf.write(path, path.relative_to(STAGE).as_posix())

    print("Build complete:", OUT_JAR)


if __name__ == "__main__":
    main()
