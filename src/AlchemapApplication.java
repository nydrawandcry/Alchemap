import data.AlchemyRepository;
import data.RepositoryFactory;
import model.Ingredient;
import view.MainFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.util.List;

public final class AlchemapApplication {
    private AlchemapApplication() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AlchemapApplication::start);
    }

    private static void start() {
        AlchemyRepository repository = null;
        try {
            RepositoryFactory.Result factory = RepositoryFactory.create();
            repository = factory.repository();
            List<Ingredient> ingredients = repository.loadIngredients();

            MainFrame frame = new MainFrame(
                    repository, ingredients, factory.warning()
            );
            frame.setVisible(true);
        } catch (Exception ex) {
            if (repository != null) {
                try { repository.close(); }
                catch (Exception closeError) { ex.addSuppressed(closeError); }
            }
            JOptionPane.showMessageDialog(
                    null,
                    "Не удалось открыть хранилище Alchemap.\n"
                            + "Если приложение уже запущено, закройте другое окно и повторите запуск.\n\n"
                            + ex.getMessage(),
                    "Запуск Alchemap",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
