package data;

import engine.AlchemyEngine;
import model.potions.PotionRule;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static support.Fixtures.*;

class DemoRepositoryTest {

    @Test
    void ingredientNamesAreUniqueIgnoringCaseAndCanBeReusedAfterDeletion()
            throws Exception {
        var repository = new DemoRepository();

        var first = repository.createIngredient(
                draft("Тестовая трава")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> repository.createIngredient(draft(
                        "Тестовая трава".toUpperCase(Locale.ROOT)
                ))
        );

        repository.deleteIngredient(first.id());

        var replacement = repository.createIngredient(
                draft(first.name())
        );

        assertNotEquals(first.id(), replacement.id());

        assertFalse(
                repository.loadIngredients().stream()
                        .anyMatch(i -> i.id() == first.id())
        );
    }

    @Test
    void deletionAndRenamingPreserveHistoricalRecipe()
            throws Exception {
        var repository = new DemoRepository();

        var ingredient = repository.createIngredient(
                draft("Тестовая трава")
        );

        var steps = steps(ingredient);

        var result = new AlchemyEngine().finish(
                steps, List.of(fallback())
        );

        long id = repository.saveExperiment(
                "Опыт", steps, result
        );

        repository.markAsRecipe(id, "Рецепт");

        var original = repository.loadRecipe(id);

        repository.deleteIngredient(ingredient.id());
        repository.renameExperiment(id, "Новое имя");

        var recipe = repository.loadRecipe(id);

        assertEquals("Новое имя", recipe.experimentName());
        assertEquals("Рецепт", recipe.recipeName());
        assertEquals(original.steps(), recipe.steps());
        assertEquals(original.resultName(), recipe.resultName());

        assertTrue(
                repository.loadHistory().getFirst().savedRecipe()
        );
    }

    @Test
    void unknownIdsCannotCreateRecipesOrUpdateRules() {
        var repository = new DemoRepository();

        assertThrows(
                IllegalArgumentException.class,
                () -> repository.markAsRecipe(999, "Имя")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> repository.renameExperiment(999, "Имя")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> repository.loadRecipe(999)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> repository.savePotionRule(new PotionRule(
                        999, "Имя", "Тип", "",
                        0, true, List.of()
                ))
        );
    }

    @Test
    void defaultCatalogHasUniqueIdsAndAnEnabledFallback() {
        var rules = DefaultPotionRules.create();

        assertEquals(
                rules.size(),
                rules.stream()
                        .map(PotionRule::id)
                        .distinct()
                        .count()
        );

        assertTrue(
                rules.stream().anyMatch(
                        r -> r.enabled() && r.conditions().isEmpty()
                )
        );

        assertFalse(new DemoRepository().isPersistent());
    }
}