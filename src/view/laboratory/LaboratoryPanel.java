package view.laboratory;

import view.ingredients.IngredientPanel;
import view.experiment.ExperimentPanel;
import view.mixture.MixturePanel;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import static view.common.UiTheme.*;

/**
 * Собирает три панели лаборатории и определяет их расположение
 */
public final class LaboratoryPanel extends JPanel {

    public LaboratoryPanel(
            IngredientPanel ingredientPanel,
            ExperimentPanel experimentPanel,
            MixturePanel mixturePanel
    ) {
        super(new GridBagLayout());
        setBackground(BG);

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
}