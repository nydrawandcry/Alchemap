package support;

import controller.ExperimentView;
import model.*;
import model.potions.*;
import org.junit.jupiter.api.function.Executable;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class Fixtures {
    private Fixtures() {}

    public static Ingredient herb() {
        return new Ingredient(
                1, "Тестовая трава", "Трава", "Описание",
                10, 10, 10, 10, 10, 10, 10
        );
    }

    public static NewIngredient draft(String name) {
        return new NewIngredient(
                name, "Трава", "Описание",
                1.25, 2.5, 3, 4, 5, 6, 7
        );
    }

    public static List<ExperimentStep> steps(Ingredient ingredient) {
        return List.of(
                new IngredientAdditionStep(ingredient, 1),
                new ActionStep(AlchemyAction.BOIL)
        );
    }

    public static MixtureSnapshot snapshot() {
        return new MixtureSnapshot(
                20, 3, 5, 7, 2, 11, 15,
                "жидкость", "холодная", "изумрудный", 2, 3
        );
    }

    public static AlchemyContext context() {
        return new AlchemyContext(
                snapshot(), true, false, true, 2
        );
    }

    public static PotionRule fallback() {
        return new PotionRule(
                1, "Тестовое зелье", "Зелье", "Эффект",
                0, true, List.of()
        );
    }

    public static void onEdt(Executable action) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            try {
                action.execute();
            } catch (Throwable ex) {
                failure.set(ex);
            }
        });

        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }

    public static final class RecordingView implements ExperimentView {
        public ExperimentState state;
        public String answer = "Название";
        public String info;
        public String error;

        @Override
        public void render(ExperimentState value) {
            state = value;
        }

        @Override
        public String askName(String title, String initial) {
            return answer;
        }

        @Override
        public void showInfo(String text) {
            info = text;
        }

        @Override
        public void showError(String title, String text) {
            error = text;
        }
    }
}