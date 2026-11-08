package view;

import data.AlchemyRepository;
import engine.AlchemyEngine;
import model.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import view.history.HistoryPanel;
import view.common.NameDialogs;
import view.ingredients.IngredientDialog;
import view.ingredients.IngredientPanel;
import view.mixture.MixturePanel;

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

    private final DefaultListModel<ExperimentStep> stepModel = new DefaultListModel<>();
    private final JList<ExperimentStep> stepList = new JList<>(stepModel);

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

        configureLook();

        mixturePanel = new MixturePanel(this::saveRecipe);

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

    private void configureLook() {
        getContentPane().setBackground(BG);
        UIManager.put("Panel.background", PANEL);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Button.background", PANEL_2);
        UIManager.put("Button.foreground", TEXT);
        UIManager.put("List.background", PANEL_2);
        UIManager.put("List.foreground", TEXT);
        UIManager.put("Table.background", PANEL_2);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("Table.gridColor", new Color(75, 65, 88));
        UIManager.put("TableHeader.background", PANEL);
        UIManager.put("TableHeader.foreground", TEXT);
        UIManager.put("TextArea.background", PANEL_2);
        UIManager.put("TextArea.foreground", TEXT);
        UIManager.put("Spinner.background", PANEL_2);
    }

    private JComponent buildRoot() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        root.add(buildHeader(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Лаборатория", buildLaboratory());
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

    private JComponent buildLaboratory() {
        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(BG);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.insets = new Insets(0, 0, 0, 10);

        c.gridx = 0;
        c.weightx = 0.26;
        body.add(buildIngredientPanel(), c);

        c.gridx = 1;
        c.weightx = 0.44;
        body.add(buildExperimentPanel(), c);

        c.gridx = 2;
        c.weightx = 0.30;
        c.insets = new Insets(0, 0, 0, 0);
        body.add(mixturePanel, c);
        return body;
    }

    private JPanel buildIngredientPanel() {
        JPanel panel = card("Ингредиенты");
        panel.setLayout(new BorderLayout());

        panel.add(ingredientPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildExperimentPanel() {
        JPanel panel = card("Рабочая область · цепочка эксперимента");
        panel.setLayout(new BorderLayout(8, 8));

        stepList.setCellRenderer(new StepRenderer());
        panel.add(new JScrollPane(stepList), BorderLayout.CENTER);

        JPanel south = new JPanel();
        south.setOpaque(false);
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));

        JLabel actionLabel = new JLabel("Действия с текущей смесью");
        actionLabel.setForeground(MUTED);
        south.add(actionLabel);
        south.add(Box.createVerticalStrut(6));

        JPanel actions = new JPanel(new GridLayout(3, 3, 6, 6));
        actions.setOpaque(false);
        for (AlchemyAction action : AlchemyAction.values()) {
            JButton b = new JButton(action.label());
            b.setToolTipText(action.description());
            b.addActionListener(e -> addAction(action));
            actions.add(b);
        }
        south.add(actions);
        south.add(Box.createVerticalStrut(8));

        JPanel controls = new JPanel(new GridLayout(1, 3, 6, 0));
        controls.setOpaque(false);
        JButton undo = new JButton("Удалить последний шаг");
        undo.addActionListener(e -> undoStep());
        JButton clear = new JButton("Новый эксперимент");
        clear.addActionListener(e -> clearExperiment());
        JButton finish = primaryButton("Завершить эксперимент");
        finish.addActionListener(e -> finishExperiment());
        controls.add(undo);
        controls.add(clear);
        controls.add(finish);
        south.add(controls);

        panel.add(south, BorderLayout.SOUTH);
        return panel;
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
        stepModel.addElement(step);

        lastExperimentId = -1;
        lastResult = null;

        mixturePanel.showPending(false);
        updatePreview();

        int last = stepModel.size() - 1;
        if (last >= 0) {
            stepList.ensureIndexIsVisible(last);
        }
    }

    private void undoStep() {
        if (steps.isEmpty()) return;

        steps.remove(steps.size() - 1);
        stepModel.remove(stepModel.size() - 1);

        lastExperimentId = -1;
        lastResult = null;

        mixturePanel.showPending(steps.isEmpty());
        updatePreview();
    }

    private void clearExperiment() {
        steps.clear();
        stepModel.clear();

        lastExperimentId = -1;
        lastResult = null;

        mixturePanel.showPending(true);
        updatePreview();
    }

    private void updatePreview() {
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

    private static final class StepRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
            JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
            if (value instanceof ExperimentStep step) {
                l.setText(String.format("%02d   %s", index + 1, step.displayText()));
                l.setBorder(new EmptyBorder(7, 8, 7, 8));
                if (!selected && value instanceof ActionStep) l.setForeground(new Color(211, 180, 247));
            }
            return l;
        }
    }
}
