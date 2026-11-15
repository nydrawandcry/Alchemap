package view.common;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;

/** Карточка с заголовком и независимой областью содержимого. */
public final class SectionCard extends RoundedPanel {
    private final JPanel body = new JPanel(new BorderLayout());

    public SectionCard(String title) {
        super(new BorderLayout(0, UiTheme.scale(14)), 32, UiTheme.OUTLINE);
        int padding = UiTheme.scale(16);
        setBorder(new EmptyBorder(padding, padding, padding, padding));
        setName(title);

        JLabel heading = new JLabel(title);
        heading.putClientProperty("FlatLaf.styleClass", "h3");
        heading.setForeground(UiTheme.TEXT);
        add(heading, BorderLayout.NORTH);

        body.setOpaque(false);
        add(body, BorderLayout.CENTER);
    }

    /** Добавляйте компоненты и задавайте layout здесь, сохраняя заголовок карточки. */
    public JPanel body() {
        return body;
    }
}
