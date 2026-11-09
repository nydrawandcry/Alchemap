package view.common;

import javax.swing.JButton;
import java.awt.Color;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import javax.swing.border.EmptyBorder;
import javax.swing.UIManager;
import java.awt.Font;

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

    /**
     * Создаёт общую карточку с заголовком
     */
    public static JPanel card(String titleText) {
        JPanel panel = new JPanel();
        panel.setBackground(PANEL);

        panel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(72, 60, 88)
                        ),
                        new EmptyBorder(10, 10, 10, 10)
                )
        );

        panel.setName(titleText);

        panel.setBorder(
                BorderFactory.createTitledBorder(
                        panel.getBorder(),
                        titleText,
                        0,
                        0,
                        new Font(Font.SANS_SERIF, Font.BOLD, 14),
                        TEXT
                )
        );

        return panel;
    }

    /**
     * Применяет общую тему до создания панелей
     */
    public static void apply() {
        UIManager.put("Panel.background", PANEL);
        UIManager.put("Label.foreground", TEXT);

        UIManager.put("Button.background", PANEL_2);
        UIManager.put("Button.foreground", TEXT);

        UIManager.put("List.background", PANEL_2);
        UIManager.put("List.foreground", TEXT);

        UIManager.put("Table.background", PANEL_2);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put(
                "Table.gridColor",
                new Color(75, 65, 88)
        );

        UIManager.put("TableHeader.background", PANEL);
        UIManager.put("TableHeader.foreground", TEXT);

        UIManager.put("TextArea.background", PANEL_2);
        UIManager.put("TextArea.foreground", TEXT);
        UIManager.put("Spinner.background", PANEL_2);
    }
}