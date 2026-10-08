package com.cburch.logisim.gui.main;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.Timer;

import com.cburch.logisim.file.RecoveryIO;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectActions;

public final class RecoveryManager implements LibraryListener {
    private static boolean recoveryPromptShown;
    private final Project project;
    private final String id;
    private final Timer timer;

    public RecoveryManager(Project project) {
        this.project = project;
        this.id = RecoveryIO.createId(project);
        this.timer = new Timer(45000, new ActionListener() {
            public void actionPerformed(ActionEvent e) { saveIfNeeded(); }
        });
        this.timer.setInitialDelay(30000);
        this.timer.start();
        project.addLibraryListener(this);
    }

    public void saveIfNeeded() {
        if (!project.isFileDirty()) return;
        try {
            RecoveryIO.write(project, id);
        } catch (Exception ex) { }
    }

    public void closeCleanly() {
        timer.stop();
        project.removeLibraryListener(this);
        deleteOwnRecovery();
    }

    public void libraryChanged(LibraryEvent event) {
        if (event.getAction() == LibraryEvent.DIRTY_STATE && !project.isFileDirty()) deleteOwnRecovery();
    }

    private void deleteOwnRecovery() {
        File dir = new File(new File(System.getProperty("user.home"), ".logisim-sky"), "recovery");
        new File(dir, id + ".circ").delete();
        new File(dir, id + ".properties").delete();
        new File(dir, id + ".tmp").delete();
    }

    public static void offerPending(Frame frame, Project project) {
        if (recoveryPromptShown) return;
        recoveryPromptShown = true;
        List<RecoveryIO.Record> records = RecoveryIO.list();
        if (records.isEmpty()) return;
        RecoveryIO.Record record = records.get(0);
        String when = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new java.util.Date(record.getSavedAt()));
        String message = "Найдена несохранённая схема:\n" + record.getDisplayName() + "\nАвтосохранение: " + when + "\n\nВосстановить её?";
        Object[] options = { "Восстановить", "Удалить копию", "Позже" };
        int result = JOptionPane.showOptionDialog(frame, message, "Восстановление схемы", JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (result == 0) {
            ProjectActions.doRecover(frame, project, record);
        } else if (result == 1) {
            record.delete();
        }
    }
}
