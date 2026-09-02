package ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import javax.swing.Icon;

/** Small scalable line icons so the UI has no external image dependency. */
final class UiIcon implements Icon {

    enum Kind {
        HOME, HISTORY, USER, LOGOUT, SEARCH, ADD, REMOVE, REPORT, SUN, MOON,
        CHEVRON_LEFT, EYE, EYE_OFF
    }

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
            case SUN -> {
                int center = s / 2;
                int radius = Math.max(3, s / 5);
                g.drawOval(center - radius, center - radius,
                        radius * 2, radius * 2);
                for (int angle = 0; angle < 360; angle += 45) {
                    double radians = Math.toRadians(angle);
                    int innerX = center + (int) Math.round(
                            Math.cos(radians) * (radius + 2));
                    int innerY = center + (int) Math.round(
                            Math.sin(radians) * (radius + 2));
                    int outerX = center + (int) Math.round(
                            Math.cos(radians) * (center - 1));
                    int outerY = center + (int) Math.round(
                            Math.sin(radians) * (center - 1));
                    g.drawLine(innerX, innerY, outerX, outerY);
                }
            }
            case MOON -> {
                Area crescent = new Area(new Ellipse2D.Double(
                        2, 2, s - 4, s - 4));
                crescent.subtract(new Area(new Ellipse2D.Double(
                        s * 0.36, 0, s * 0.68, s * 0.68)));
                g.fill(crescent);
            }
            case CHEVRON_LEFT -> {
                g.drawLine(s - 6, 3, 5, s / 2);
                g.drawLine(5, s / 2, s - 6, s - 3);
            }
            case EYE, EYE_OFF -> {
                Path2D eye = new Path2D.Double();
                eye.moveTo(1, s / 2.0);
                eye.curveTo(s * 0.28, 2, s * 0.72, 2, s - 1, s / 2.0);
                eye.curveTo(s * 0.72, s - 2, s * 0.28, s - 2, 1, s / 2.0);
                g.draw(eye);
                g.drawOval(s / 2 - 2, s / 2 - 2, 4, 4);
                if (kind == Kind.EYE_OFF) {
                    g.drawLine(2, 2, s - 2, s - 2);
                }
            }
        }
        g.dispose();
    }
}
