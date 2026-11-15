package view.common;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/** Панель с прозрачными углами и сглаженным фоном. arc — диаметр скругления. */
public class RoundedPanel extends JPanel {
    private final int arc;
    private final Color outline;

    public RoundedPanel(LayoutManager layout, int arc, Color outline) {
        super(layout);
        this.arc = arc;
        this.outline = outline;
        setOpaque(false);
        setBackground(UiTheme.PANEL);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            float diameter = UiTheme.scale(arc);
            RoundRectangle2D shape = new RoundRectangle2D.Float(
                    0.5f, 0.5f, Math.max(0, getWidth() - 1),
                    Math.max(0, getHeight() - 1), diameter, diameter
            );
            g.setColor(getBackground());
            g.fill(shape);
            if (outline != null) {
                g.setColor(outline);
                g.draw(shape);
            }
        } finally {
            g.dispose();
        }
    }
}
