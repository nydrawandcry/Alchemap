package view.laboratory;

import controller.ExperimentController;
import controller.ExperimentView;
import data.AlchemyRepository;
import model.ExperimentState;
import model.Ingredient;
import view.common.NameDialogs;
import view.ingredients.IngredientPanel;
import view.experiment.ExperimentPanel;
import view.mixture.MixturePanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static view.common.UiTheme.*;

/**
 * Собирает панели лаборатории и отображает состояние контроллера
 */
public final class LaboratoryPanel extends JPanel
        implements ExperimentView {

    private final ExperimentController controller;
    private final IngredientPanel ingredientPanel;
    private final ExperimentPanel experimentPanel;
    private final MixturePanel mixturePanel;

    public LaboratoryPanel(
            AlchemyRepository repository,
            List<Ingredient> ingredients,
            Runnable onHistoryChanged
    ) {
        super(new GridBagLayout());
        setBackground(BG);

        controller = new ExperimentController(
                repository,
                this,
                onHistoryChanged
        );

        ingredientPanel = new IngredientPanel(
                repository,
                ingredients,
                controller
        );

        experimentPanel = new ExperimentPanel(controller);
        mixturePanel = new MixturePanel(controller);

        buildLayout();
        controller.refresh();
    }

    private void buildLayout() {
        JPanel ingredientCard = card("Ингредиенты");
        ingredientCard.setLayout(new BorderLayout());
        ingredientCard.add(
                ingredientPanel,
                BorderLayout.CENTER
        );

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.insets = new Insets(0, 0, 0, 10);

        c.gridx = 0;
        c.weightx = 0.26;
        add(ingredientCard, c);

        c.gridx = 1;
        c.weightx = 0.44;
        add(experimentPanel, c);

        c.gridx = 2;
        c.weightx = 0.30;
        c.insets = new Insets(0, 0, 0, 0);
        add(mixturePanel, c);
    }

    @Override
    public void render(ExperimentState state) {
        experimentPanel.showSteps(state.steps());
        mixturePanel.showMixture(state.mixture());

        if (state.result() == null) {
            mixturePanel.showPending(
                    state.steps().isEmpty()
            );
        } else {
            mixturePanel.showResult(state.result());

            if (state.recipeSaved()) {
                mixturePanel.markRecipeSaved();
            }
        }
    }

    @Override
    public String askName(String title, String initial) {
        return NameDialogs.askName(this, title, initial);
    }

    @Override
    public void showInfo(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Alchemap",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    @Override
    public void showError(String title, String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                title,
                JOptionPane.ERROR_MESSAGE
        );
    }
}