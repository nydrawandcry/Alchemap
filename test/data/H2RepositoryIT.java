package data;

import controller.ExperimentController;
import engine.AlchemyEngine;
import model.*;
import model.potions.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.*;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static support.Fixtures.*;

class H2RepositoryIT {
    @TempDir
    Path directory;

    private H2Repository repository;

    @BeforeEach
    void open() throws Exception {
        repository = new H2Repository(directory);
    }

    @AfterEach
    void close() throws Exception {
        repository.close();
    }

    @Test
    void createsSchemaAndSeedsOnlyOnceEvenAfterDeletingTheEntireCatalog()
            throws Exception {
        assertTrue(repository.isPersistent());
        assertFalse(repository.loadIngredients().isEmpty());

        var rules = repository.loadPotionRules();
        assertFalse(rules.isEmpty());

        for (var ingredient : repository.loadIngredients()) {
            repository.deleteIngredient(ingredient.id());
        }

        reopen();

        assertTrue(repository.loadIngredients().isEmpty());
        assertEquals(rules, repository.loadPotionRules());
    }

    @Test
    void ingredientFieldsAndCaseInsensitiveUniquenessSurviveReopening()
            throws Exception {
        var ingredient = repository.createIngredient(
                draft("Тестовая трава")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> repository.createIngredient(draft(
                        ingredient.name().toUpperCase(Locale.ROOT)
                ))
        );

        reopen();

        assertTrue(
                repository.loadIngredients().contains(ingredient)
        );

        assertEquals(1.25, ingredient.healing());
    }

    @Test
    void experimentRecipeAndRenameSurviveReopening()
            throws Exception {
        var ingredient = repository.createIngredient(
                draft("Тестовая трава")
        );

        var steps = steps(ingredient);

        var result = new AlchemyEngine().finish(
                steps, List.of(fallback())
        );

        long id = repository.saveExperiment(
                "о".repeat(160), steps, result
        );

        repository.markAsRecipe(id, "Первое имя рецепта");
        repository.markAsRecipe(id, "р".repeat(160));
        repository.renameExperiment(id, "Новое имя опыта");

        var expected = repository.loadRecipe(id);

        reopen();

        assertEquals(expected, repository.loadRecipe(id));
        assertEquals("Новое имя опыта", expected.experimentName());
        assertEquals("р".repeat(160), expected.recipeName());
        assertEquals("Тестовое зелье", expected.resultName());

        assertEquals(
                List.of(
                        "+ 1× Тестовая трава",
                        "-> Сварить"
                ),
                expected.steps()
        );

        assertEquals(1, repository.loadHistory().size());

        assertTrue(
                repository.loadHistory().getFirst().savedRecipe()
        );
    }

    @Test
    void hardDeletionKeepsOldRecipesAndAlsoAllowsSavingAnAlreadyDeletedStep()
            throws Exception {
        var original = repository.createIngredient(
                draft("Удаляемая трава")
        );

        var steps = steps(original);

        var result = new AlchemyEngine().finish(
                steps, List.of(fallback())
        );

        long first = repository.saveExperiment(
                "Первый", steps, result
        );

        repository.markAsRecipe(first, "Первый рецепт");

        var historical = repository.loadRecipe(first);

        repository.deleteIngredient(original.id());

        var replacement = repository.createIngredient(
                draft(original.name())
        );

        assertNotEquals(original.id(), replacement.id());

        long second = repository.saveExperiment(
                "Второй", steps, result
        );

        repository.markAsRecipe(second, "Второй рецепт");

        reopen();

        assertEquals(
                historical,
                repository.loadRecipe(first)
        );

        assertEquals(
                historical.steps(),
                repository.loadRecipe(second).steps()
        );
    }

    @Test
    void failedStepWriteRollsBackExperimentAndEarlierSteps()
            throws Exception {
        sql("""
                ALTER TABLE experiment_steps
                ADD CONSTRAINT test_fail_step
                CHECK (step_order < 2)
                """);

        var steps = steps(
                repository.loadIngredients().getFirst()
        );

        var result = new AlchemyEngine().finish(
                steps, List.of(fallback())
        );

        assertThrows(
                SQLException.class,
                () -> repository.saveExperiment(
                        "Неудачный", steps, result
                )
        );

        assertTrue(repository.loadHistory().isEmpty());
        assertEquals(0, count("experiment_steps"));

        sql("""
                ALTER TABLE experiment_steps
                DROP CONSTRAINT test_fail_step
                """);

        repository.saveExperiment("Удачный", steps, result);

        // Эти запросы используют другую JDBC-сессию.
        assertEquals(1, count("experiments"));
        assertEquals(2, count("experiment_steps"));
    }

    @Test
    void failedRuleUpdateRestoresOldNameAndConditions()
            throws Exception {
        var original =
                repository.loadPotionRules().getFirst();

        sql("""
                ALTER TABLE potion_rule_conditions
                ADD CONSTRAINT test_fail_condition
                CHECK (threshold <> 12345)
                """);

        var edited = new PotionRule(
                original.id(),
                "Изменённое имя",
                original.type(),
                "Новое описание",
                original.priority(),
                true,
                List.of(RuleCondition.number(
                        1,
                        RuleMetric.HEALING,
                        RuleComparison.GE,
                        12345
                ))
        );

        assertThrows(
                SQLException.class,
                () -> repository.savePotionRule(edited)
        );

        assertEquals(
                original,
                repository.loadPotionRules().stream()
                        .filter(r -> r.id() == original.id())
                        .findFirst()
                        .orElseThrow()
        );

        sql("""
                ALTER TABLE potion_rule_conditions
                DROP CONSTRAINT test_fail_condition
                """);

        repository.savePotionRule(edited);

        reopen();

        assertEquals(
                edited,
                repository.loadPotionRules().stream()
                        .filter(r -> r.id() == edited.id())
                        .findFirst()
                        .orElseThrow()
        );
    }

    @Test
    void controllerEngineAndDatabaseCompleteARealWorkflow()
            throws Exception {
        long ruleId = repository.savePotionRule(new PotionRule(
                0,
                "Первый эффект",
                "Зелье",
                "Старое описание",
                10000,
                true,
                List.of()
        ));

        var view = new RecordingView();

        var controller = new ExperimentController(
                repository, view, () -> {}
        );

        controller.ingredientAddRequested(
                repository.loadIngredients().getFirst(), 1
        );

        controller.actionRequested(AlchemyAction.BOIL);

        view.answer = "Первый опыт";
        controller.finishRequested();

        view.answer = "Рецепт";
        controller.recipeSaveRequested();

        assertTrue(view.state.recipeSaved());

        long first =
                repository.loadHistory().getFirst().id();

        repository.savePotionRule(new PotionRule(
                ruleId,
                "Новый эффект",
                "Зелье",
                "Новое описание",
                10000,
                true,
                List.of()
        ));

        controller.newExperimentRequested();

        controller.ingredientAddRequested(
                repository.loadIngredients().getFirst(), 1
        );

        view.answer = "Второй опыт";
        controller.finishRequested();

        assertEquals(
                "Новый эффект",
                view.state.result().name()
        );

        assertNull(view.error);

        reopen();

        assertEquals(2, repository.loadHistory().size());

        assertEquals(
                "Первый эффект",
                repository.loadRecipe(first).resultName()
        );

        assertEquals(
                "Старое описание",
                repository.loadRecipe(first).resultDescription()
        );
    }

    @Test
    void rejectsAnUnsupportedSchemaVersion()
            throws Exception {
        sql("UPDATE app_schema SET version = 999 WHERE id = 1");

        repository.close();

        assertThrows(
                SQLException.class,
                () -> new H2Repository(directory)
        );
    }

    private void reopen() throws Exception {
        repository.close();
        repository = new H2Repository(directory);
    }

    private Connection inspect() throws SQLException {
        String file = directory.resolve("alchemap")
                .toAbsolutePath()
                .toString()
                .replace('\\', '/');

        return DriverManager.getConnection(
                "jdbc:h2:file:" + file, "sa", ""
        );
    }

    private void sql(String text) throws SQLException {
        try (
                var connection = inspect();
                var statement = connection.createStatement()
        ) {
            statement.execute(text);
        }
    }

    private int count(String table) throws SQLException {
        try (
                var connection = inspect();
                var statement = connection.createStatement();
                var rows = statement.executeQuery(
                        "SELECT COUNT(*) FROM " + table
                )
        ) {
            rows.next();
            return rows.getInt(1);
        }
    }
}