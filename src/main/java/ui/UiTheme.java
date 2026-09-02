package ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Window;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.UIManager;

/** Shared semantic theme for every JCash screen. */
public final class UiTheme {

    private static final Color EMPTINESS = new Color(251, 251, 252);
    private static final Color CITY_LIGHTS = new Color(224, 229, 233);
    private static final Color LIGHT_TEAL = new Color(178, 201, 197);
    private static final Color RETRO_LIME = new Color(27, 212, 136);
    private static final Color WISH_UPON_A_STAR = new Color(69, 130, 139);
    private static final Color ENAMELLED_JEWEL = new Color(5, 91, 101);
    private static final Color DEEP_JEWEL = new Color(3, 47, 53);

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
    private static final String THEME_BUTTON_ROLE = "themeToggle";

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
        JButton button = lightButton("");
        button.putClientProperty("jcash.controlRole", THEME_BUTTON_ROLE);
        button.setPreferredSize(new Dimension(42, 42));
        button.setMinimumSize(new Dimension(42, 42));
        updateThemeButton(button);
        button.addActionListener(event -> {
            toggle(owner);
            updateThemeButton(button);
        });
        return button;
    }

    private static void updateThemeButton(JButton button) {
        String action = dark ? "Switch to light mode" : "Switch to dark mode";
        if (button instanceof FlatButton flatButton) {
            flatButton.setColors(SURFACE, lightActionHover());
        }
        button.setForeground(dark ? TEXT : NAVY);
        button.setText(null);
        button.setIcon(new UiIcon(dark ? UiIcon.Kind.SUN : UiIcon.Kind.MOON, 20));
        button.setToolTipText(action);
        button.getAccessibleContext().setAccessibleName(action);
        button.getAccessibleContext().setAccessibleDescription(action);
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
            NAVY = DEEP_JEWEL;
            NAVY_LIGHT = ENAMELLED_JEWEL;
            TEAL = RETRO_LIME;
            TEAL_DARK = new Color(20, 177, 113);
            BACKGROUND = new Color(3, 42, 47);
            SURFACE = new Color(5, 70, 78);
            TEXT = EMPTINESS;
            MUTED = LIGHT_TEAL;
            BORDER = WISH_UPON_A_STAR;
            SUCCESS = new Color(29, 197, 126);
            DANGER = new Color(251, 113, 133);
            WARNING = new Color(251, 191, 36);
        } else {
            NAVY = ENAMELLED_JEWEL;
            NAVY_LIGHT = WISH_UPON_A_STAR;
            TEAL = RETRO_LIME;
            TEAL_DARK = new Color(20, 177, 113);
            BACKGROUND = CITY_LIGHTS;
            SURFACE = EMPTINESS;
            TEXT = new Color(18, 63, 68);
            MUTED = new Color(82, 117, 122);
            BORDER = LIGHT_TEAL;
            SUCCESS = new Color(18, 151, 96);
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
        } else if (component instanceof JComponent styled
                && "textAction".equals(styled.getClientProperty(
                        "jcash.colorRole"))) {
            component.setForeground(dark ? TEXT : NAVY);
        } else if (component instanceof JComponent styled
                && "passwordToggle".equals(styled.getClientProperty(
                        "jcash.colorRole"))) {
            component.setForeground(MUTED);
        } else if (foreground != null
                && !Color.WHITE.equals(component.getForeground())) {
            component.setForeground(foreground);
        }
        if (component instanceof RoundedPanel panel) {
            panel.remap(replacements);
        }
        if (component instanceof FlatButton button) {
            button.remap(replacements);
            if ("primaryAction".equals(button.getClientProperty(
                    "jcash.colorRole"))) {
                button.setColors(TEAL, TEAL_DARK);
                button.setForeground(DEEP_JEWEL);
            } else if ("lightAction".equals(button.getClientProperty(
                    "jcash.colorRole"))) {
                button.setColors(SURFACE, lightActionHover());
            } else if ("darkAction".equals(button.getClientProperty(
                    "jcash.colorRole"))) {
                button.setColors(NAVY_LIGHT, darkActionHover());
            }
        }
        if (component instanceof JButton button
                && THEME_BUTTON_ROLE.equals(button.getClientProperty(
                        "jcash.controlRole"))) {
            updateThemeButton(button);
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
        field.putClientProperty(FlatClientProperties.STYLE, "arc: 10");
        field.putClientProperty(
                FlatClientProperties.TEXT_FIELD_PADDING,
                new Insets(10, 12, 10, 12)
        );
        Dimension preferred = field.getPreferredSize();
        field.setPreferredSize(new Dimension(preferred.width, 44));
        field.setMinimumSize(new Dimension(0, 44));
        return field;
    }

    static void installPasswordVisibilityToggle(JPasswordField field) {
        JButton toggle = new JButton();
        char hiddenEcho = field.getEchoChar() == 0
                ? '\u2022' : field.getEchoChar();
        Dimension size = new Dimension(30, 30);
        toggle.setIcon(new UiIcon(UiIcon.Kind.EYE, 18));
        toggle.setPreferredSize(size);
        toggle.setMinimumSize(size);
        toggle.setMaximumSize(size);
        toggle.setForeground(MUTED);
        toggle.setContentAreaFilled(false);
        toggle.setBorderPainted(false);
        toggle.setFocusPainted(false);
        toggle.setOpaque(false);
        toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggle.setToolTipText("Show PIN");
        toggle.getAccessibleContext().setAccessibleName("Show PIN");
        toggle.putClientProperty("jcash.colorRole", "passwordToggle");
        toggle.addActionListener(event -> {
            boolean showing = field.getEchoChar() == 0;
            field.setEchoChar(showing ? hiddenEcho : (char) 0);
            toggle.setIcon(new UiIcon(showing
                    ? UiIcon.Kind.EYE : UiIcon.Kind.EYE_OFF, 18));
            String action = showing ? "Show PIN" : "Hide PIN";
            toggle.setToolTipText(action);
            toggle.getAccessibleContext().setAccessibleName(action);
            field.requestFocusInWindow();
        });
        field.putClientProperty(
                FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT,
                toggle
        );
    }

    static void hidePassword(JPasswordField field) {
        if (field == null) {
            return;
        }
        field.setEchoChar('\u2022');
        Object trailing = field.getClientProperty(
                FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT
        );
        if (trailing instanceof JButton toggle) {
            toggle.setIcon(new UiIcon(UiIcon.Kind.EYE, 18));
            toggle.setToolTipText("Show PIN");
            toggle.getAccessibleContext().setAccessibleName("Show PIN");
        }
    }

    static JButton primaryButton(String text) {
        JButton button = button(text, TEAL, DEEP_JEWEL, TEAL_DARK);
        button.putClientProperty("jcash.colorRole", "primaryAction");
        return button;
    }

    static JButton darkButton(String text) {
        JButton button = button(text, NAVY_LIGHT, Color.WHITE,
                darkActionHover());
        button.putClientProperty("jcash.colorRole", "darkAction");
        return button;
    }

    static JButton lightButton(String text) {
        JButton button = button(text, SURFACE, dark ? TEXT : NAVY,
                lightActionHover());
        button.putClientProperty("jcash.colorRole", "lightAction");
        return button;
    }

    static JButton textButton(String text) {
        JButton button = new JButton(text);
        button.setFont(FONT_MEDIUM);
        button.setForeground(dark ? TEXT : NAVY);
        button.setBorder(BorderFactory.createEmptyBorder(11, 18, 11, 18));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty("jcash.colorRole", "textAction");
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent event) {
                button.setForeground(dark ? LIGHT_TEAL : WISH_UPON_A_STAR);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent event) {
                button.setForeground(dark ? TEXT : NAVY);
            }
        });
        return button;
    }

    private static Color lightActionHover() {
        return dark ? WISH_UPON_A_STAR : LIGHT_TEAL;
    }

    private static Color darkActionHover() {
        return dark ? WISH_UPON_A_STAR : ENAMELLED_JEWEL;
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
