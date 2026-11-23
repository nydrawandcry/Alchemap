package controller;

import data.AlchemyRepository;
import model.*;
import model.potions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static support.Fixtures.*;

class ExperimentControllerTest {
    private StubRepository repository;
    private RecordingView view;
    private AtomicInteger historyRefreshes;
    private ExperimentController controller;

    @BeforeEach
    void setUp() {
        repository = new StubRepository();
        view = new RecordingView();
        historyRefreshes = new AtomicInteger();

        controller = new ExperimentController(
                repository,
                view,
                historyRefreshes::incrementAndGet
        );

        controller.refresh();
    }

    @Test
    void emptyExperimentCannotExecuteActionsOrBeSaved() {
        controller.actionRequested(AlchemyAction.BOIL);
        controller.finishRequested();

        assertTrue(view.state.steps().isEmpty());
        assertNotNull(view.info);
        assertEquals(0, repository.saves);
    }

    @Test
    void cancellingExperimentNameDoesNotSave() {
        view.answer = null;
        finish();

        assertEquals(0, repository.saves);
        assertNull(view.state.result());
        assertEquals(0, historyRefreshes.get());
    }

    @Test
    void successfulFinishPublishesSavedResult() {
        finish();

        assertEquals(1, repository.saves);
        assertEquals("Название", repository.savedName);
        assertEquals(steps(herb()), repository.savedSteps);
        assertEquals(repository.savedResult, view.state.result());
        assertEquals(1, historyRefreshes.get());

        assertThrows(
                UnsupportedOperationException.class,
                () -> repository.savedSteps.clear()
        );
    }

    @Test
    void recipeCanBeSavedOnceAndCancellationCanBeRetried() {
        finish();

        view.answer = null;
        controller.recipeSaveRequested();

        assertEquals(0, repository.recipeSaves);

        view.answer = "  Рецепт  ";
        controller.recipeSaveRequested();
        controller.recipeSaveRequested();

        assertEquals(1, repository.recipeSaves);
        assertEquals(42, repository.recipeExperimentId);
        assertEquals("Рецепт", repository.recipeName);
        assertTrue(view.state.recipeSaved());
        assertEquals(2, historyRefreshes.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"add", "action", "undo", "reset"})
    void anyChainChangeInvalidatesTheSavedResult(String operation) {
        finish();
        controller.recipeSaveRequested();

        switch (operation) {
            case "add" ->
                    controller.ingredientAddRequested(herb(), 1);
            case "action" ->
                    controller.actionRequested(AlchemyAction.COOL);
            case "undo" ->
                    controller.undoRequested();
            case "reset" ->
                    controller.newExperimentRequested();
        }

        assertNull(view.state.result());
        assertFalse(view.state.recipeSaved());

        controller.recipeSaveRequested();

        assertEquals(1, repository.recipeSaves);
    }

    @Test
    void failedExperimentWriteDoesNotPublishSuccessAndCanBeRetried() {
        repository.failSave = true;

        finish();

        assertNotNull(view.error);
        assertNull(view.state.result());
        assertEquals(0, historyRefreshes.get());

        repository.failSave = false;
        controller.finishRequested();

        assertNotNull(view.state.result());
        assertEquals(1, historyRefreshes.get());
    }

    @Test
    void failedRecipeWriteDoesNotMarkRecipeSaved() {
        finish();

        repository.failRecipe = true;
        controller.recipeSaveRequested();

        assertFalse(view.state.recipeSaved());
        assertNotNull(view.error);

        repository.failRecipe = false;
        controller.recipeSaveRequested();

        assertTrue(view.state.recipeSaved());
    }

    @Test
    void historyRefreshFailureDoesNotUndoAnAlreadySavedExperiment() {
        controller = new ExperimentController(
                repository,
                view,
                () -> {
                    throw new IllegalStateException(
                            "Журнал недоступен"
                    );
                }
        );

        finish();

        assertEquals(1, repository.saves);
        assertNotNull(view.state.result());
        assertTrue(view.error.contains("Данные сохранены"));
    }

    private void finish() {
        controller.ingredientAddRequested(herb(), 1);
        controller.actionRequested(AlchemyAction.BOIL);
        controller.finishRequested();
    }

    private static final class StubRepository
            implements AlchemyRepository {

        int saves;
        int recipeSaves;
        boolean failSave;
        boolean failRecipe;

        String savedName;
        String recipeName;
        long recipeExperimentId;

        List<ExperimentStep> savedSteps;
        AlchemyResult savedResult;

        @Override
        public List<PotionRule> loadPotionRules() {
            return List.of(fallback());
        }

        @Override
        public long saveExperiment(
                String name,
                List<ExperimentStep> steps,
                AlchemyResult result
        ) throws Exception {
            if (failSave) {
                throw new SQLException("Запись не удалась");
            }

            saves++;
            savedName = name;
            savedSteps = steps;
            savedResult = result;

            return 42;
        }

        @Override
        public void markAsRecipe(
                long id,
                String name
        ) throws Exception {
            if (failRecipe) {
                throw new SQLException("Запись не удалась");
            }

            recipeSaves++;
            recipeExperimentId = id;
            recipeName = name;
        }

        @Override
        public String modeName() {
            return "test";
        }

        @Override
        public List<Ingredient> loadIngredients() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Ingredient createIngredient(NewIngredient value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteIngredient(int id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<HistoryEntry> loadHistory() {
            throw new UnsupportedOperationException();
        }

        @Override
        public RecipeDetails loadRecipe(long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void renameExperiment(long id, String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long savePotionRule(PotionRule rule) {
            throw new UnsupportedOperationException();
        }
    }
}