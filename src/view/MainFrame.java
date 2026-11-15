package view;

import data.AlchemyRepository;
import model.Ingredient;
import view.common.RoundedPanel;
import view.common.SectionCard;
import view.history.HistoryPanel;
import view.laboratory.LaboratoryPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import static view.common.UiTheme.*;

/**
 * Главное окно: вкладки, заголовок и жизненный цикл приложения
 */
public final class MainFrame extends JFrame {
    private final AlchemyRepository repository;
    private final JLabel modeLabel;
    private final HistoryPanel historyPanel;
    private final LaboratoryPanel laboratoryPanel;

    private PotionRulesPanel potionRulesPanel;

    public MainFrame(
            AlchemyRepository repository,
            List<Ingredient> ingredients,
            String startupWarning
    ) {
        super("Alchemap — алхимическая лаборатория");

        this.repository = repository;
        modeLabel = new JLabel(
                "Хранилище: " + repository.modeName()
        );

        setDefaultCloseOperation(
                WindowConstants.DO_NOTHING_ON_CLOSE
        );

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (potionRulesPanel == null
                        || potionRulesPanel.confirmDiscard()) {
                    dispose();
                }
            }
        });

        historyPanel = new HistoryPanel(repository, ACCENT);

        laboratoryPanel = new LaboratoryPanel(
                repository,
                ingredients,
                historyPanel::refreshHistory
        );

        setContentPane(buildRoot());
        configureWindowSize();
        historyPanel.refreshHistory();

        if (startupWarning != null
                && !startupWarning.isBlank()) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(
                            this,
                            startupWarning,
                            "Режим запуска",
                            JOptionPane.INFORMATION_MESSAGE
                    )
            );
        }
    }

    private JComponent buildRoot() {
        JPanel root = new JPanel(
                new BorderLayout(scale(16), scale(20))
        );
        root.setBackground(BG);
        root.setBorder(
                new EmptyBorder(scale(20), scale(20), scale(20), scale(20))
        );

        root.add(buildHeader(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        JScrollPane laboratoryScroll = new JScrollPane(laboratoryPanel);
        laboratoryScroll.setBorder(null);
        laboratoryScroll.setOpaque(false);
        laboratoryScroll.getViewport().setBackground(BG);
        tabs.addTab("Лаборатория", laboratoryScroll);
        tabs.addTab("Журнал опытов", buildHistory());

        potionRulesPanel = new PotionRulesPanel(repository);
        tabs.addTab("Редактор мастера", potionRulesPanel);

        root.add(tabs, BorderLayout.CENTER);

        return root;
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(scale(20), 0));
        header.setBackground(BG);

        JLabel title = new JLabel("Alchemap");
        title.setForeground(TEXT);
        title.putClientProperty("FlatLaf.styleClass", "h1");

        JLabel sub = new JLabel(
                "Интерактивная алхимическая лаборатория для D&D"
        );
        sub.setForeground(MUTED);

        JPanel text = new JPanel();
        text.setLayout(
                new BoxLayout(text, BoxLayout.Y_AXIS)
        );
        text.setBackground(BG);
        text.add(title);
        text.add(Box.createVerticalStrut(scale(4)));
        text.add(sub);

        header.add(text, BorderLayout.WEST);

        modeLabel.setForeground(MUTED);
        RoundedPanel storage = new RoundedPanel(new BorderLayout(), 24, OUTLINE);
        storage.setBackground(PANEL);
        storage.setBorder(new EmptyBorder(scale(9), scale(14), scale(9), scale(14)));
        storage.add(modeLabel, BorderLayout.CENTER);

        JPanel storageArea = new JPanel(new GridBagLayout());
        storageArea.setOpaque(false);
        storageArea.add(storage);
        header.add(storageArea, BorderLayout.EAST);

        return header;
    }

    private JComponent buildHistory() {
        SectionCard panel = card(
                "Журнал опытов и сохранённые рецепты"
        );
        panel.body().add(historyPanel, BorderLayout.CENTER);
        return panel;
    }

    private void configureWindowSize() {
        GraphicsConfiguration configuration = getGraphicsConfiguration();
        Rectangle screen = configuration.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
        int availableWidth = Math.max(1,
                screen.width - insets.left - insets.right - scale(24));
        int availableHeight = Math.max(1,
                screen.height - insets.top - insets.bottom - scale(24));

        setMinimumSize(new Dimension(
                Math.min(scale(1120), availableWidth),
                Math.min(scale(680), availableHeight)
        ));
        setSize(Math.min(scale(1360), availableWidth),
                Math.min(scale(820), availableHeight));
        setLocationRelativeTo(null);
    }

    @Override
    public void dispose() {
        try {
            repository.close();
        } catch (Exception ignored) {
        }

        super.dispose();
    }
}
