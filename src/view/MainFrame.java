package view;

import data.AlchemyRepository;
import engine.AlchemyEngine;
import model.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import view.history.HistoryPanel;
import view.common.NameDialogs;
import view.common.UiTheme;
import view.laboratory.LaboratoryPanel;
import view.ingredients.IngredientDialog;
import view.ingredients.IngredientPanel;
import view.mixture.MixturePanel;
import view.experiment.ExperimentPanel;
import view.experiment.ExperimentActionListener;

import static view.common.UiTheme.*;

import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class MainFrame extends JFrame {
    private final AlchemyRepository repository;
    private final AlchemyEngine engine = new AlchemyEngine();
    private final List<ExperimentStep> steps = new ArrayList<>();

    private final ExperimentPanel experimentPanel;

    private final JLabel modeLabel;
    private PotionRulesPanel potionRulesPanel;
    private HistoryPanel historyPanel;
    private final IngredientPanel ingredientPanel;
    private final MixturePanel mixturePanel;

    private long lastExperimentId = -1;
    private AlchemyResult lastResult;

    public MainFrame(AlchemyRepository repository, List<Ingredient> ingredients, String startupWarning) {
        super("Alchemap — алхимическая лаборатория");
        this.repository = repository;

        this.modeLabel = new JLabel("Хранилище: " + repository.modeName());

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (potionRulesPanel == null || potionRulesPanel.confirmDiscard()) dispose();
            }
        });
        setMinimumSize(new Dimension(1180, 760));
        setSize(1360, 820);
        setLocationRelativeTo(null);

        UiTheme.apply();

        mixturePanel = new MixturePanel(this::saveRecipe);
        experimentPanel = new ExperimentPanel(
                new ExperimentHandler()
        );

        ingredientPanel = new IngredientPanel(
                repository,
                ingredients,
                (ingredient, quantity) -> addStep(
                        new IngredientAdditionStep(ingredient, quantity)
                )
        );

        setContentPane(buildRoot());
        updatePreview();
        refreshHistory();

        if (startupWarning != null && !startupWarning.isBlank()) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    this, startupWarning, "Режим запуска", JOptionPane.INFORMATION_MESSAGE
            ));
        }
    }

    private JComponent buildRoot() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        root.add(buildHeader(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab(
                "Лаборатория",
                new LaboratoryPanel(
                        ingredientPanel,
                        experimentPanel,
                        mixturePanel
                )
        );
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
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        JLabel sub = new JLabel("Интерактивная алхимическая лаборатория для D&D");
        sub.setForeground(MUTED);
        sub.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setBackground(BG);
        text.add(title);
        text.add(sub);
        header.add(text, BorderLayout.WEST);
        modeLabel.setForeground(MUTED);
        header.add(modeLabel, BorderLayout.EAST);
        return header;
    }

    private JComponent buildHistory() {
        JPanel panel = card("Журнал опытов и сохранённые рецепты");
        panel.setLayout(new BorderLayout());

        historyPanel = new HistoryPanel(repository, ACCENT);
        panel.add(historyPanel, BorderLayout.CENTER);

        return panel;
    }

    private String askName(String title, String initial) {
        return NameDialogs.askName(this, title, initial);
    }

    private void addAction(AlchemyAction action) {
        if (steps.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Сначала добавьте хотя бы один ингредиент.");
            return;
        }
        addStep(new ActionStep(action));
    }

    private void addStep(ExperimentStep step) {
        steps.add(step);

        lastExperimentId = -1;
        lastResult = null;

        mixturePanel.showPending(false);
        updatePreview();
    }

    private void undoStep() {
        if (steps.isEmpty()) return;

        steps.remove(steps.size() - 1);

        lastExperimentId = -1;
        lastResult = null;

        mixturePanel.showPending(steps.isEmpty());
        updatePreview();
    }

    private void clearExperiment() {
        steps.clear();

        lastExperimentId = -1;
        lastResult = null;

        mixturePanel.showPending(true);
        updatePreview();
    }

    private void updatePreview() {
        experimentPanel.showSteps(steps);
        mixturePanel.showMixture(engine.preview(steps));
    }

    private void finishExperiment() {
        if (steps.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Эксперимент пуст. Добавьте ингредиенты.");
            return;
        }
        try {
            AlchemyResult result = engine.finish(steps, repository.loadPotionRules());
            String defaultName = "Опыт " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
            String name = askName("Название опыта · " + result.name(), defaultName);
            if (name == null) return;
            long id = repository.saveExperiment(name, List.copyOf(steps), result);
            lastExperimentId = id;
            lastResult = result;

            mixturePanel.showResult(result);
            refreshHistory();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Не удалось завершить и сохранить эксперимент: " + ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveRecipe() {
        if (lastExperimentId < 0 || lastResult == null) return;
        String name = askName("Название рецепта", lastResult.name());
        if (name == null) return;
        try {
            repository.markAsRecipe(lastExperimentId, name.trim());
            mixturePanel.markRecipeSaved();
            refreshHistory();
            JOptionPane.showMessageDialog(this, "Рецепт «" + name.trim() + "» сохранён.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Не удалось сохранить рецепт: " + ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshHistory() {
        historyPanel.refreshHistory();
    }

    @Override
    public void dispose() {
        try { repository.close(); } catch (Exception ignored) {}
        super.dispose();
    }

    /**
     * Связывает события панели с управлением экспериментом
     */
    private final class ExperimentHandler
            implements ExperimentActionListener {

        @Override
        public void actionRequested(AlchemyAction action) {
            addAction(action);
        }

        @Override
        public void undoRequested() {
            undoStep();
        }

        @Override
        public void newExperimentRequested() {
            clearExperiment();
        }

        @Override
        public void finishRequested() {
            finishExperiment();
        }
    }
}
