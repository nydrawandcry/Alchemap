package view.experiment;

import model.ActionStep;
import model.AlchemyAction;
import model.ExperimentStep;
import view.common.SectionCard;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.Objects;

import static view.common.UiTheme.*;

/**
 * Показывает цепочку шагов и передаёт запросы управления слушателю
 */
public final class ExperimentPanel extends JPanel {
    private final DefaultListModel<ExperimentStep> stepModel =
            new DefaultListModel<>();

    private final JList<ExperimentStep> stepList =
            new JList<>(stepModel);

    public ExperimentPanel(ExperimentActionListener listener) {
        super(new BorderLayout());

        Objects.requireNonNull(listener);
        setOpaque(false);

        SectionCard card = card("Цепочка эксперимента");
        JPanel content = card.body();
        content.setLayout(new BorderLayout(scale(10), scale(14)));

        stepList.setCellRenderer(new StepRenderer());
        content.add(
                new JScrollPane(stepList),
                BorderLayout.CENTER
        );

        JPanel south = new JPanel();
        south.setOpaque(false);
        south.setLayout(
                new BoxLayout(south, BoxLayout.Y_AXIS)
        );

        JLabel actionLabel =
                new JLabel("Действия с текущей смесью");
        actionLabel.setForeground(MUTED);
        actionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        south.add(actionLabel);
        south.add(Box.createVerticalStrut(6));

        JPanel actions = new JPanel(
                new GridLayout(3, 3, 6, 6)
        );
        actions.setOpaque(false);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (AlchemyAction action : AlchemyAction.values()) {
            JButton button = new JButton(action.label());
            button.setMargin(new Insets(scale(6), scale(6), scale(6), scale(6)));
            button.setToolTipText(action.description());

            button.addActionListener(
                    e -> listener.actionRequested(action)
            );

            actions.add(button);
        }

        south.add(actions);
        south.add(Box.createVerticalStrut(8));

        JPanel controls = new JPanel(new BorderLayout(0, 6));
        controls.setOpaque(false);
        controls.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton undo = new JButton("Отменить шаг");
        undo.setToolTipText("Удалить последний шаг эксперимента");
        undo.addActionListener(e -> listener.undoRequested());

        JButton clear = new JButton("Новый эксперимент");
        clear.addActionListener(
                e -> listener.newExperimentRequested()
        );

        JButton finish =
                primaryButton("Завершить эксперимент");
        finish.addActionListener(e -> listener.finishRequested());

        JPanel editControls = new JPanel(
                new GridLayout(1, 2, 6, 0)
        );
        editControls.setOpaque(false);
        editControls.add(undo);
        editControls.add(clear);

        controls.add(editControls, BorderLayout.NORTH);
        controls.add(finish, BorderLayout.CENTER);

        south.add(controls);

        content.add(south, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);
    }

    /**
     * Обновляет отображение по текущему состоянию эксперимента
     */
    public void showSteps(List<ExperimentStep> steps) {
        List<ExperimentStep> snapshot = List.copyOf(steps);

        int[] selected = stepList.getSelectedIndices();
        boolean appended = snapshot.size() > stepModel.size();

        stepModel.clear();
        stepModel.addAll(snapshot);

        for (int index : selected) {
            if (index < snapshot.size()) {
                stepList.addSelectionInterval(index, index);
            }
        }

        int last = stepModel.size() - 1;
        if (appended && last >= 0) {
            stepList.ensureIndexIsVisible(last);
        }
    }

    private static final class StepRenderer
            extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean selected,
                boolean focus
        ) {
            JLabel label =
                    (JLabel) super.getListCellRendererComponent(
                            list, value, index, selected, focus
                    );

            if (value instanceof ExperimentStep step) {
                label.setText(
                        String.format(
                                "%02d   %s",
                                index + 1,
                                step.displayText()
                        )
                );

                label.setBorder(
                        new EmptyBorder(7, 8, 7, 8)
                );

                if (!selected && value instanceof ActionStep) {
                    label.setForeground(
                            new Color(211, 180, 247)
                    );
                }
            }

            return label;
        }
    }
}
