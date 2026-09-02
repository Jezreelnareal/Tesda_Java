package ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.JWindow;
import javax.swing.border.EmptyBorder;

/** Branded modal dialogs and lightweight success notifications. */
final class UiDialogs {

    private UiDialogs() {
    }

    static boolean form(Component parent, String title, JComponent content,
                        String actionText) {
        return showDecision(parent, title, content, actionText, "Cancel");
    }

    static boolean confirm(Component parent, String title, String message,
                           String actionText) {
        JTextArea text = messageArea(message);
        return showDecision(parent, title, text, actionText, "Cancel");
    }

    static void message(Component parent, String title, String message,
                        boolean error) {
        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBackground(UiTheme.SURFACE);
        content.setBorder(new EmptyBorder(6, 4, 0, 4));
        JLabel heading = UiTheme.label(error ? "Request not completed" : "All done",
                error ? UiTheme.DANGER : UiTheme.SUCCESS,
                UiTheme.FONT_MEDIUM);
        content.add(heading, BorderLayout.NORTH);
        content.add(messageArea(message), BorderLayout.CENTER);
        showDecision(parent, title, content, "Close", null);
    }

    static void componentMessage(Component parent, String title,
                                 JComponent content) {
        showDecision(parent, title, content, "Close", null);
    }

    static void toast(Component parent, String message) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        if (owner == null || !owner.isShowing()) {
            return;
        }
        JWindow toast = new JWindow(owner);
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setBackground(UiTheme.NAVY_LIGHT);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.TEAL),
                new EmptyBorder(12, 16, 12, 16)));
        JLabel marker = new JLabel("\u2713", SwingConstants.CENTER);
        marker.setForeground(UiTheme.SUCCESS);
        marker.setFont(UiTheme.FONT_TITLE.deriveFont(18f));
        JLabel label = UiTheme.label(message, java.awt.Color.WHITE,
                UiTheme.FONT_MEDIUM);
        card.add(marker, BorderLayout.WEST);
        card.add(label, BorderLayout.CENTER);
        toast.setContentPane(card);
        toast.pack();
        toast.setLocation(owner.getX() + owner.getWidth() - toast.getWidth() - 24,
                owner.getY() + owner.getHeight() - toast.getHeight() - 36);
        toast.setVisible(true);
        Timer timer = new Timer(2800, event -> toast.dispose());
        timer.setRepeats(false);
        timer.start();
    }

    private static boolean showDecision(Component parent, String title,
                                        JComponent content, String primaryText,
                                        String cancelText) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, title,
                JDialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        boolean[] accepted = {false};

        JPanel shell = new JPanel(new BorderLayout(0, 20));
        shell.setBackground(UiTheme.SURFACE);
        shell.setBorder(new EmptyBorder(24, 26, 20, 26));
        JLabel heading = UiTheme.label(title, UiTheme.NAVY,
                UiTheme.FONT_TITLE.deriveFont(20f));
        JPanel center = new JPanel(new BorderLayout(0, 16));
        center.setOpaque(false);
        center.add(heading, BorderLayout.NORTH);
        center.add(content, BorderLayout.CENTER);
        shell.add(center, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        if (cancelText != null) {
            JButton cancel = UiTheme.lightButton(cancelText);
            cancel.addActionListener(event -> dialog.dispose());
            actions.add(cancel);
        }
        JButton accept = UiTheme.primaryButton(primaryText);
        accept.addActionListener(event -> {
            accepted[0] = true;
            dialog.dispose();
        });
        actions.add(accept);
        shell.add(actions, BorderLayout.SOUTH);

        dialog.setContentPane(shell);
        dialog.getRootPane().setDefaultButton(accept);
        bindEscape(dialog.getRootPane(), dialog);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(390, dialog.getHeight()));
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return accepted[0];
    }

    private static JTextArea messageArea(String message) {
        JTextArea text = new JTextArea(message);
        text.setEditable(false);
        text.setOpaque(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setColumns(34);
        text.setFont(UiTheme.FONT_PLAIN);
        text.setForeground(UiTheme.TEXT);
        text.setBorder(BorderFactory.createEmptyBorder());
        return text;
    }

    private static void bindEscape(JRootPane root, JDialog dialog) {
        InputMap input = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actions = root.getActionMap();
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        actions.put("close", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                dialog.dispose();
            }
        });
    }
}
