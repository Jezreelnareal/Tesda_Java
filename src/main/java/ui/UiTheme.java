package ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;

/** Shared semantic theme for every JCash screen. */
public final class UiTheme {

    static Color NAVY;
    static Color NAVY_LIGHT;
    static Color TEAL;
    static Color TEAL_DARK;
    static Color BACKGROUND;
    static Color SURFACE;
    static Color TEXT;
    static Color MUTED;
    static Color BORDER;
    static Color SUCCESS;
    static Color DANGER;
    static Color WARNING;

    static final Font FONT_PLAIN = new Font("Segoe UI", Font.PLAIN, 14);
    static final Font FONT_MEDIUM = new Font("Segoe UI Semibold", Font.BOLD, 14);
    static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 28);
    static final Font FONT_BALANCE = new Font("Segoe UI", Font.BOLD, 34);

    private static boolean dark;

    static {
        applyPalette(false);
    }

    private UiTheme() {
    }

    public static void install() {
        System.setProperty("flatlaf.useWindowDecorations", "false");
        UIManager.put("Component.arc", 12);
        UIManager.put("Button.arc", 12);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("ScrollBar.width", 12);
        FlatLightLaf.setup();
    }

    static JButton themeButton(Window owner) {
        JButton button = lightButton(dark ? "Light mode" : "Dark mode");
        button.setToolTipText("Switch between light and dark mode");
        button.addActionListener(event -> {
            toggle(owner);
            button.setText(dark ? "Light mode" : "Dark mode");
        });
        return button;
    }

    private static void toggle(Window owner) {
        Map<Color, Color> replacements = paletteReplacements(!dark);
        dark = !dark;
        applyPalette(dark);
        if (dark) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        FlatLaf.updateUI();
        remapTree(owner, replacements);
        owner.repaint();
    }

    private static Map<Color, Color> paletteReplacements(boolean targetDark) {
        Color[] old = paletteValues();
        boolean previous = dark;
        applyPalette(targetDark);
        Color[] next = paletteValues();
        applyPalette(previous);
        Map<Color, Color> replacements = new LinkedHashMap<>();
        for (int index = 0; index < old.length; index++) {
            replacements.put(old[index], next[index]);
        }
        return replacements;
    }

    private static Color[] paletteValues() {
        return new Color[]{NAVY, NAVY_LIGHT, TEAL, TEAL_DARK, BACKGROUND,
                SURFACE, TEXT, MUTED, BORDER, SUCCESS, DANGER, WARNING};
    }

    private static void applyPalette(boolean darkMode) {
        if (darkMode) {
            NAVY = new Color(10, 24, 40);
            NAVY_LIGHT = new Color(27, 48, 69);
            TEAL = new Color(20, 184, 166);
            TEAL_DARK = new Color(13, 148, 136);
            BACKGROUND = new Color(15, 23, 42);
            SURFACE = new Color(30, 41, 59);
            TEXT = new Color(241, 245, 249);
            MUTED = new Color(148, 163, 184);
            BORDER = new Color(51, 65, 85);
            SUCCESS = new Color(52, 211, 153);
            DANGER = new Color(251, 113, 133);
            WARNING = new Color(251, 191, 36);
        } else {
            NAVY = new Color(16, 42, 67);
            NAVY_LIGHT = new Color(36, 59, 83);
            TEAL = new Color(0, 168, 150);
            TEAL_DARK = new Color(0, 132, 118);
            BACKGROUND = new Color(244, 247, 251);
            SURFACE = Color.WHITE;
            TEXT = new Color(31, 41, 55);
            MUTED = new Color(100, 116, 139);
            BORDER = new Color(221, 229, 239);
            SUCCESS = new Color(22, 139, 94);
            DANGER = new Color(205, 55, 70);
            WARNING = new Color(190, 114, 20);
        }
    }

    private static void remapTree(Component component, Map<Color, Color> replacements) {
        if (component == null) {
            return;
        }
        Color background = replacements.get(component.getBackground());
        Color foreground = replacements.get(component.getForeground());
        if (background != null) {
            component.setBackground(background);
        }
        if (component instanceof JComponent styled
                && "heading".equals(styled.getClientProperty("jcash.colorRole"))) {
            component.setForeground(dark ? TEXT : NAVY);
        } else if (component instanceof JComponent styled
                && "lightAction".equals(styled.getClientProperty("jcash.colorRole"))) {
            component.setForeground(dark ? TEXT : NAVY);
        } else if (foreground != null
                && !Color.WHITE.equals(component.getForeground())) {
            component.setForeground(foreground);
        }
        if (component instanceof RoundedPanel panel) {
            panel.remap(replacements);
        }
        if (component instanceof FlatButton button) {
            button.remap(replacements);
        }
        if (component instanceof JButton button
                && "Switch between light and dark mode".equals(button.getToolTipText())) {
            button.setText(dark ? "Light mode" : "Dark mode");
        }
        if (component instanceof JTextField field) {
            field.setCaretColor(TEAL);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                remapTree(child, replacements);
            }
        }
    }

    static JLabel label(String text, Color color, Font font) {
        JLabel label = new JLabel(text);
        boolean heading = color.equals(NAVY);
        label.setForeground(heading && dark ? TEXT : color);
        if (heading) {
            label.putClientProperty("jcash.colorRole", "heading");
        }
        label.setFont(font);
        return label;
    }

    static JTextField styleField(JTextField field) {
        field.setFont(FONT_PLAIN);
        field.setForeground(TEXT);
        field.setBackground(SURFACE);
        field.setCaretColor(TEAL_DARK);
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Required");
        field.setBorder(compoundBorder(10, 12));
        return field;
    }

    static JButton primaryButton(String text) {
        return button(text, TEAL, Color.WHITE, TEAL_DARK);
    }

    static JButton darkButton(String text) {
        return button(text, NAVY_LIGHT, Color.WHITE,
                dark ? new Color(42, 65, 88) : new Color(49, 75, 101));
    }

    static JButton lightButton(String text) {
        JButton button = button(text, SURFACE, dark ? TEXT : NAVY,
                dark ? new Color(51, 65, 85) : new Color(235, 241, 247));
        button.putClientProperty("jcash.colorRole", "lightAction");
        return button;
    }

    private static JButton button(String text, Color background, Color foreground, Color hover) {
        JButton button = new FlatButton(text, background, hover);
        button.setFont(FONT_MEDIUM);
        button.setForeground(foreground);
        button.setBorder(BorderFactory.createEmptyBorder(11, 18, 11, 18));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE,
                FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        return button;
    }

    static void setNavigationActive(JButton active, Iterable<JButton> buttons) {
        for (JButton button : buttons) {
            boolean selected = button == active;
            if (button instanceof FlatButton flatButton) {
                flatButton.setColors(selected ? TEAL_DARK : NAVY_LIGHT,
                        selected ? TEAL : new Color(49, 75, 101));
            }
            button.repaint();
        }
    }

    private static Border compoundBorder(int vertical, int horizontal) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(vertical, horizontal, vertical, horizontal)
        );
    }

    static final class RoundedPanel extends JPanel {
        private Color fillColor;
        private final int arc;

        RoundedPanel(Color fillColor, int arc) {
            this.fillColor = fillColor;
            this.arc = arc;
            setOpaque(false);
        }

        private void remap(Map<Color, Color> replacements) {
            fillColor = replacements.getOrDefault(fillColor, fillColor);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setColor(fillColor);
            graphics2D.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            graphics2D.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class FlatButton extends JButton {
        private Color normalColor;
        private Color hoverColor;
        private boolean hovered;

        private FlatButton(String text, Color normalColor, Color hoverColor) {
            super(text);
            this.normalColor = normalColor;
            this.hoverColor = hoverColor;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent event) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent event) {
                    hovered = false;
                    repaint();
                }
            });
        }

        private void setColors(Color normal, Color hover) {
            normalColor = normal;
            hoverColor = hover;
        }

        private void remap(Map<Color, Color> replacements) {
            normalColor = replacements.getOrDefault(normalColor, normalColor);
            hoverColor = replacements.getOrDefault(hoverColor, hoverColor);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = normalColor;
            if (!isEnabled()) {
                fill = dark ? new Color(71, 85, 105) : new Color(203, 213, 225);
            } else if (getModel().isPressed()) {
                fill = hoverColor.darker();
            } else if (hovered) {
                fill = hoverColor;
            }
            graphics2D.setColor(fill);
            graphics2D.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            graphics2D.dispose();
            super.paintComponent(graphics);
        }
    }
}
