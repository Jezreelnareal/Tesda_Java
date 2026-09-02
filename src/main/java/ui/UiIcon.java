package ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;

/** Small scalable line icons so the UI has no external image dependency. */
final class UiIcon implements Icon {

    enum Kind { HOME, HISTORY, USER, LOGOUT, SEARCH, ADD, REMOVE, REPORT }

    private final Kind kind;
    private final int size;

    UiIcon(Kind kind) {
        this(kind, 18);
    }

    UiIcon(Kind kind, int size) {
        this.kind = kind;
        this.size = size;
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.translate(x, y);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(component.isEnabled() ? component.getForeground()
                : new Color(148, 163, 184));
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        int s = size;
        switch (kind) {
            case HOME -> {
                g.drawLine(2, s / 2, s / 2, 2);
                g.drawLine(s / 2, 2, s - 2, s / 2);
                g.drawRect(4, s / 2, s - 8, s / 2 - 3);
            }
            case HISTORY -> {
                g.drawArc(2, 2, s - 4, s - 4, -65, 290);
                g.drawLine(2, 2, 2, 7);
                g.drawLine(2, 2, 7, 2);
                g.drawLine(s / 2, 5, s / 2, s / 2);
                g.drawLine(s / 2, s / 2, s - 5, s / 2 + 2);
            }
            case USER -> {
                g.drawOval(s / 2 - 3, 2, 6, 6);
                g.drawArc(3, 9, s - 6, s - 7, 0, 180);
            }
            case LOGOUT -> {
                g.drawLine(2, 2, 2, s - 2);
                g.drawLine(2, 2, s / 2, 2);
                g.drawLine(2, s - 2, s / 2, s - 2);
                g.drawLine(7, s / 2, s - 2, s / 2);
                g.drawLine(s - 5, s / 2 - 3, s - 2, s / 2);
                g.drawLine(s - 5, s / 2 + 3, s - 2, s / 2);
            }
            case SEARCH -> {
                g.drawOval(2, 2, s - 7, s - 7);
                g.drawLine(s - 6, s - 6, s - 2, s - 2);
            }
            case ADD -> {
                g.drawOval(2, 2, s - 4, s - 4);
                g.drawLine(s / 2, 5, s / 2, s - 5);
                g.drawLine(5, s / 2, s - 5, s / 2);
            }
            case REMOVE -> {
                g.drawOval(2, 2, s - 4, s - 4);
                g.drawLine(5, s / 2, s - 5, s / 2);
            }
            case REPORT -> {
                g.drawRect(3, 2, s - 6, s - 4);
                g.drawLine(6, 7, s - 6, 7);
                g.drawLine(6, 11, s - 6, 11);
                g.drawLine(6, 15, s - 9, 15);
            }
        }
        g.dispose();
    }
}
