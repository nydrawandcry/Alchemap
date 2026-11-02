import data.*;
import model.Ingredient;
import view.MainFrame;

import javax.swing.*;
import java.util.List;

public final class AlchemapApplication {
    private AlchemapApplication() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            data.RepositoryFactory.Result factory = data.RepositoryFactory.create();
            AlchemyRepository repository = factory.repository();
            String warning = factory.warning();
            List<Ingredient> ingredients;

            try {
                ingredients = repository.loadIngredients();
                if (ingredients.isEmpty()) throw new IllegalStateException("таблица ingredients пуста");
            } catch (Exception ex) {
                try { repository.close(); } catch (Exception ignored) {}
                repository = new DemoRepository();
                ingredients = DemoRepository.defaultIngredients();
                String additional = "База доступна, но данные загрузить не удалось: " + ex.getMessage() +
                        ". Используется демонстрационный набор ингредиентов.";
                warning = warning == null ? additional : warning + "\n" + additional;
            }

            MainFrame frame = new MainFrame(repository, ingredients, warning);
            frame.setVisible(true);
        });
    }
}