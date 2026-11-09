package view;

import data.AlchemyRepository;
import model.Ingredient;
import view.common.UiTheme;
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

        setMinimumSize(new Dimension(1180, 760));
        setSize(1360, 820);
        setLocationRelativeTo(null);

        UiTheme.apply();

        historyPanel = new HistoryPanel(repository, ACCENT);

        laboratoryPanel = new LaboratoryPanel(
                repository,
                ingredients,
                historyPanel::refreshHistory
        );

        setContentPane(buildRoot());
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
                new BorderLayout(12, 12)
        );
        root.setBackground(BG);
        root.setBorder(
                new EmptyBorder(14, 14, 14, 14)
        );

        root.add(buildHeader(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Лаборатория", laboratoryPanel);
        tabs.addTab("Журнал опытов", buildHistory());

        potionRulesPanel = new PotionRulesPanel(repository);
        tabs.addTab("Редактор мастера", potionRulesPanel);

        root.add(tabs, BorderLayout.CENTER);

        return root;
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG);

        JLabel title = new JLabel("ALCHEMAP");
        title.setForeground(TEXT);
        title.setFont(
                new Font(Font.SANS_SERIF, Font.BOLD, 28)
        );

        JLabel sub = new JLabel(
                "Интерактивная алхимическая лаборатория для D&D"
        );
        sub.setForeground(MUTED);
        sub.setFont(
                new Font(Font.SANS_SERIF, Font.PLAIN, 14)
        );

        JPanel text = new JPanel();
        text.setLayout(
                new BoxLayout(text, BoxLayout.Y_AXIS)
        );
        text.setBackground(BG);
        text.add(title);
        text.add(sub);

        header.add(text, BorderLayout.WEST);

        modeLabel.setForeground(MUTED);
        header.add(modeLabel, BorderLayout.EAST);

        return header;
    }

    private JComponent buildHistory() {
        JPanel panel = card(
                "Журнал опытов и сохранённые рецепты"
        );
        panel.setLayout(new BorderLayout());
        panel.add(historyPanel, BorderLayout.CENTER);
        return panel;
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