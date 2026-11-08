package view.common;

import javax.swing.JButton;
import java.awt.Color;

/**
 * Общие цвета и оформление основных кнопок приложения
 */
public final class UiTheme {
    public static final Color BG = new Color(25, 22, 32);
    public static final Color PANEL = new Color(38, 32, 48);
    public static final Color PANEL_2 = new Color(48, 39, 60);
    public static final Color ACCENT = new Color(153, 109, 210);
    public static final Color TEXT = new Color(238, 233, 244);
    public static final Color MUTED = new Color(190, 180, 204);

    private UiTheme() {}

    public static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(ACCENT);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        return button;
    }
}