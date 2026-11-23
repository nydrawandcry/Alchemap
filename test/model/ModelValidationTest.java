package model;

import model.potions.RecipeDetails;
import model.potions.TextValidation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static support.Fixtures.*;

class ModelValidationTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void requiredNameRejectsBlank(String text) {
        assertThrows(
                IllegalArgumentException.class,
                () -> TextValidation.required(text, 160, "Название")
        );
    }

    @Test
    void requiredNameTrimsAndHonours160CharacterBoundary() {
        assertEquals(
                "Имя",
                TextValidation.required(" Имя ", 160, "Название")
        );

        assertEquals(
                "я".repeat(160),
                TextValidation.required(
                        "я".repeat(160), 160, "Название"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> TextValidation.required(
                        "я".repeat(161), 160, "Название"
                )
        );
    }

    @Test
    void ingredientTextBoundariesAndNormalization() {
        NewIngredient ingredient =
                ingredient(" а ", " б ", null, 0, 0);

        assertAll(
                () -> assertEquals("а", ingredient.name()),
                () -> assertEquals("б", ingredient.category()),
                () -> assertEquals("", ingredient.description())
        );

        assertDoesNotThrow(() -> ingredient(
                "а".repeat(100),
                "б".repeat(60),
                "в".repeat(500),
                0,
                99_999.99
        ));

        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> ingredient(
                                "а".repeat(101), "б", "", 0, 1
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> ingredient(
                                "а", "б".repeat(61), "", 0, 1
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> ingredient(
                                "а", "б", "в".repeat(501), 0, 1
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> ingredient(" ", "б", "", 0, 1)
                )
        );
    }

    static Stream<Arguments> invalidStats() {
        double[] invalid = {
                -0.01,
                100_000,
                0.001,
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY
        };

        return IntStream.range(0, 7)
                .boxed()
                .flatMap(index -> Arrays.stream(invalid)
                        .mapToObj(value -> Arguments.of(index, value)));
    }

    @ParameterizedTest(
            name = "характеристика {0}: недопустимо {1}"
    )
    @MethodSource("invalidStats")
    void eachIngredientStatRejectsInvalidValues(
            int index,
            double value
    ) {
        assertThrows(
                IllegalArgumentException.class,
                () -> ingredient("а", "б", "", index, value)
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6})
    void eachIngredientStatAcceptsBothBoundaries(int index) {
        assertDoesNotThrow(
                () -> ingredient("а", "б", "", index, 0)
        );

        assertDoesNotThrow(
                () -> ingredient("а", "б", "", index, 99_999.99)
        );
    }

    @Test
    void ingredientRequiresPositivePersistentId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> draft("а").toIngredient(0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> draft("а").toIngredient(-1)
        );

        assertEquals(42, draft("а").toIngredient(42).id());
    }

    @Test
    void stateAndRecipeDefensivelyCopyTheirLists() {
        List<ExperimentStep> steps =
                new ArrayList<>(steps(herb()));

        ExperimentState state = new ExperimentState(
                steps, snapshot(), null, false
        );

        steps.clear();

        assertEquals(2, state.steps().size());

        assertThrows(
                UnsupportedOperationException.class,
                () -> state.steps().clear()
        );

        List<String> lines =
                new ArrayList<>(List.of("Шаг"));

        RecipeDetails recipe = new RecipeDetails(
                1, "Опыт", "Рецепт", "Зелье",
                "Тип", "Эффект", lines
        );

        lines.clear();

        assertEquals(List.of("Шаг"), recipe.steps());

        assertThrows(
                UnsupportedOperationException.class,
                () -> recipe.steps().clear()
        );
    }

    private static NewIngredient ingredient(
            String name,
            String category,
            String description,
            int index,
            double value
    ) {
        double[] stats = new double[7];
        Arrays.fill(stats, 1);
        stats[index] = value;

        return new NewIngredient(
                name, category, description,
                stats[0], stats[1], stats[2], stats[3],
                stats[4], stats[5], stats[6]
        );
    }
}