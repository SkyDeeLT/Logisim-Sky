package com.cburch.logisim.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import com.cburch.logisim.proj.Project;

public final class RecoveryIO {
    public static final class Record {
        private final File circuitFile;
        private final File metaFile;
        private final File originalFile;
        private final String displayName;
        private final long savedAt;

        Record(File circuitFile, File metaFile, File originalFile, String displayName, long savedAt) {
            this.circuitFile = circuitFile;
            this.metaFile = metaFile;
            this.originalFile = originalFile;
            this.displayName = displayName;
            this.savedAt = savedAt;
        }

        public File getCircuitFile() { return circuitFile; }
        public File getOriginalFile() { return originalFile; }
        public String getDisplayName() { return displayName; }
        public long getSavedAt() { return savedAt; }

        public void delete() {
            circuitFile.delete();
            metaFile.delete();
        }
    }

    private RecoveryIO() { }

    private static File getDirectory() {
        File dir = new File(new File(System.getProperty("user.home"), ".logisim-sky"), "recovery");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static String createId(Project project) {
        Loader loader = project.getLogisimFile().getLoader();
        File main = loader == null ? null : loader.getMainFile();
        String base = main == null ? project.getLogisimFile().getName() : main.getAbsolutePath();
        long now = System.currentTimeMillis();
        return Integer.toHexString(base.hashCode()) + "-" + Long.toHexString(now);
    }

    public static void write(Project project, String id) throws IOException {
        File dir = getDirectory();
        File circ = new File(dir, id + ".circ");
        File meta = new File(dir, id + ".properties");
        File temp = new File(dir, id + ".tmp");
        Loader loader = project.getLogisimFile().getLoader();
        FileOutputStream out = new FileOutputStream(temp);
        try {
            project.getLogisimFile().write(out, loader);
        } finally {
            out.close();
        }
        if (circ.exists() && !circ.delete()) throw new IOException("Unable to replace recovery file");
        if (!temp.renameTo(circ)) throw new IOException("Unable to finalize recovery file");
        Properties props = new Properties();
        File main = loader == null ? null : loader.getMainFile();
        props.setProperty("displayName", project.getLogisimFile().getName());
        props.setProperty("savedAt", Long.toString(System.currentTimeMillis()));
        if (main != null) props.setProperty("originalPath", main.getAbsolutePath());
        FileOutputStream metaOut = new FileOutputStream(meta);
        try {
            props.store(metaOut, "Logisim Sky recovery");
        } finally {
            metaOut.close();
        }
    }

    public static List<Record> list() {
        File dir = getDirectory();
        File[] metas = dir.listFiles();
        if (metas == null) return Collections.emptyList();
        ArrayList<Record> records = new ArrayList<Record>();
        for (File meta : metas) {
            if (!meta.getName().endsWith(".properties")) continue;
            String id = meta.getName().substring(0, meta.getName().length() - ".properties".length());
            File circ = new File(dir, id + ".circ");
            if (!circ.isFile() || circ.length() == 0) continue;
            Properties props = new Properties();
            FileInputStream in = null;
            try {
                in = new FileInputStream(meta);
                props.load(in);
                String path = props.getProperty("originalPath", "");
                File original = path.length() == 0 ? null : new File(path);
                String display = props.getProperty("displayName", "Recovered circuit");
                long saved = Long.parseLong(props.getProperty("savedAt", "0"));
                records.add(new Record(circ, meta, original, display, saved));
            } catch (Exception ex) {
                circ.delete();
                meta.delete();
            } finally {
                if (in != null) try { in.close(); } catch (IOException ex) { }
            }
        }
        Collections.sort(records, new Comparator<Record>() {
            public int compare(Record a, Record b) {
                return a.savedAt < b.savedAt ? 1 : (a.savedAt == b.savedAt ? 0 : -1);
            }
        });
        return records;
    }

    public static void bindOriginal(Loader loader, File original) {
        loader.setMainFile(original);
    }
}
