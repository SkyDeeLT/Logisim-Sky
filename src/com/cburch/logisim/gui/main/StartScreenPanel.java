package com.cburch.logisim.gui.main;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.cburch.logisim.DarkTheme;
import com.cburch.logisim.file.LoadFailedException;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectActions;

public final class StartScreenPanel extends JPanel {
    private final Frame frame;
    private final Project project;
    private final JPanel recentList = new JPanel();

    public StartScreenPanel(Frame frame, Project project) {
        super(new BorderLayout());
        this.frame = frame;
        this.project = project;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(DarkTheme.scale(42), DarkTheme.scale(48), DarkTheme.scale(42), DarkTheme.scale(48)));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = new JLabel("Logisim Sky");
        title.setFont(title.getFont().deriveFont(Font.BOLD, DarkTheme.scale(28.0f)));
        JLabel sub = new JLabel("Создавайте, анализируйте и тестируйте цифровые схемы");
        sub.setForeground(DarkTheme.FOREGROUND_MUTED);
        sub.setBorder(BorderFactory.createEmptyBorder(DarkTheme.scale(6), 0, DarkTheme.scale(18), 0));
        top.add(title, BorderLayout.NORTH);
        top.add(sub, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 2, DarkTheme.scale(24), 0));
        center.setOpaque(false);
        JPanel actions = new JPanel(new GridLayout(0, 1, 0, DarkTheme.scale(10)));
        actions.setOpaque(false);
        JButton create = bigButton("Новая схема", "Пустой проект");
        JButton open = bigButton("Открыть схему", "Файл .circ");
        JButton examples = bigButton("Примеры", "Быстрый старт и учебные схемы");
        create.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                project.setStartupScreen(false);
                frame.setStartScreenVisible(false);
            }
        });
        open.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { ProjectActions.doOpen(frame, project); }
        });
        examples.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object[] choices = { "XOR из базовых элементов", "Распространение сигнала", "Отмена" };
                int selected = javax.swing.JOptionPane.showOptionDialog(frame, "Выберите пример", "Примеры",
                        javax.swing.JOptionPane.DEFAULT_OPTION, javax.swing.JOptionPane.PLAIN_MESSAGE, null, choices, choices[0]);
                if (selected == 0) openExample("/resources/examples/xor.circ");
                else if (selected == 1) openExample("/resources/examples/propagation.circ");
            }
        });
        actions.add(create);
        actions.add(open);
        actions.add(examples);
        center.add(actions);

        JPanel recentWrap = new JPanel(new BorderLayout());
        recentWrap.setOpaque(false);
        JLabel recentTitle = new JLabel("Недавние проекты");
        recentTitle.setFont(recentTitle.getFont().deriveFont(Font.BOLD, DarkTheme.scale(15.0f)));
        recentTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, DarkTheme.scale(8), 0));
        recentWrap.add(recentTitle, BorderLayout.NORTH);
        recentList.setLayout(new javax.swing.BoxLayout(recentList, javax.swing.BoxLayout.Y_AXIS));
        recentList.setOpaque(false);
        JScrollPane scroll = new JScrollPane(recentList);
        scroll.setBorder(BorderFactory.createLineBorder(DarkTheme.BORDER_SOFT));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        recentWrap.add(scroll, BorderLayout.CENTER);
        center.add(recentWrap);
        add(center, BorderLayout.CENTER);
        refreshRecent();
    }

    public void refreshRecent() {
        recentList.removeAll();
        List<File> files = AppPreferences.getRecentFiles();
        if (files.isEmpty()) {
            JLabel empty = new JLabel("Недавних проектов пока нет");
            empty.setForeground(DarkTheme.FOREGROUND_MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(DarkTheme.scale(12), DarkTheme.scale(12), DarkTheme.scale(12), DarkTheme.scale(12)));
            recentList.add(empty);
        } else {
            int count = Math.min(8, files.size());
            for (int i = 0; i < count; i++) {
                final File f = files.get(i);
                if (f == null) continue;
                JButton b = new JButton();
                b.setHorizontalAlignment(JButton.LEFT);
                b.setBorder(BorderFactory.createEmptyBorder(DarkTheme.scale(8), DarkTheme.scale(10), DarkTheme.scale(8), DarkTheme.scale(10)));
                String date = f.exists() ? new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(f.lastModified())) : "файл недоступен";
                b.setText("<html><b>" + escape(f.getName()) + "</b><br><span style='color:#8a96a8'>" + escape(date) + "</span></html>");
                b.setToolTipText(f.getAbsolutePath());
                b.setMaximumSize(new Dimension(Integer.MAX_VALUE, DarkTheme.scale(54)));
                b.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) { ProjectActions.doOpen(frame, project, f); }
                });
                recentList.add(b);
            }
        }
        recentList.revalidate();
        recentList.repaint();
    }


    private void openExample(String resource) {
        InputStream in = StartScreenPanel.class.getResourceAsStream(resource);
        if (in == null) return;
        try {
            Loader loader = new Loader(frame);
            LogisimFile file = loader.openLogisimFile(in);
            if (file != null) {
                project.setStartupScreen(false);
                project.setLogisimFile(file);
                loader.setParent(frame);
                frame.setStartScreenVisible(false);
            }
        } catch (Exception ex) {
            javax.swing.JOptionPane.showMessageDialog(frame, "Не удалось открыть пример: " + ex.getMessage(),
                    "Примеры", javax.swing.JOptionPane.ERROR_MESSAGE);
        } finally {
            try { in.close(); } catch (Exception ex) { }
        }
    }

    private JButton bigButton(String title, String subtitle) {
        JButton b = new JButton("<html><b>" + title + "</b><br><span style='color:#8a96a8'>" + subtitle + "</span></html>");
        b.setHorizontalAlignment(JButton.LEFT);
        b.setBorder(BorderFactory.createEmptyBorder(DarkTheme.scale(14), DarkTheme.scale(16), DarkTheme.scale(14), DarkTheme.scale(16)));
        b.setPreferredSize(new Dimension(DarkTheme.scale(260), DarkTheme.scale(70)));
        return b;
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
