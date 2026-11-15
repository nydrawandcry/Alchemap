package view.common;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.JButton;
import java.awt.Color;

/**
 * Общая палитра, тема и фабрики компонентов Alchemap
 */
public final class UiTheme {
    public static final Color BG = new Color(25, 22, 32);
    public static final Color PANEL = new Color(38, 32, 48);
    public static final Color PANEL_2 = new Color(48, 39, 60);
    public static final Color ACCENT = new Color(153, 109, 210);
    public static final Color TEXT = new Color(238, 233, 244);
    public static final Color MUTED = new Color(190, 180, 204);
    public static final Color OUTLINE = new Color(65, 54, 80);

    private UiTheme() {}

    public static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.putClientProperty("FlatLaf.styleClass", "primary");
        return button;
    }

    /**
     * Создаёт общую карточку с заголовком
     */
    public static SectionCard card(String titleText) {
        return new SectionCard(titleText);
    }

    /**
     * Применяет общую тему до создания панелей
     */
    public static void apply() {
        FlatLaf.registerCustomDefaultsSource("themes");
        if (!FlatDarkLaf.setup()) {
            throw new IllegalStateException("Не удалось подключить тему Alchemap.");
        }
    }

    /**
     * Размеры собственной отрисовки и отступы учитывают масштаб FlatLaf
     */
    public static int scale(int value) {
        return UIScale.scale(value);
    }
}
