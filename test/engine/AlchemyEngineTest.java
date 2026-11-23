package engine;

import model.*;
import model.potions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static support.Fixtures.*;

class AlchemyEngineTest {
    private final AlchemyEngine engine = new AlchemyEngine();

    @ParameterizedTest
    @CsvSource({
            "GRIND,11.5,11.2,10,10,10,11,9.6",
            "CUT,10.5,10.4,10,10,10,10.3,10",
            "BOIL,11.2,10.7,10,10,8.2,10.6,9.8",
            "BURN,2.2,11.8,15.7,2.5,4.5,8.2,7.2",
            "COOL,10,10,8.4,11.8,10,10,11.6",
            "FILTER,9.6,7.4,9.3,9.6,10,10,10.8",
            "DISTILL,9.4,10.6,10,10,14,11.4,9.3",
            "INFUSE,10.8,10,10,10,9.5,11.5,10.4",
            "DRY,9.7,10.5,10,10,11.5,10.5,10"
    })
    void eachActionChangesTheExpectedProperties(
            AlchemyAction action,
            double healing,
            double toxicity,
            double fire,
            double frost,
            double volatility,
            double magic,
            double stability
    ) {
        MixtureSnapshot s = engine.preview(List.of(
                new IngredientAdditionStep(herb(), 1),
                new ActionStep(action)
        ));

        assertArrayEquals(
                new double[]{
                        healing, toxicity, fire, frost,
                        volatility, magic, stability
                },
                new double[]{
                        s.healing(), s.toxicity(),
                        s.fire(), s.frost(), s.volatility(),
                        s.magic(), s.stability()
                },
                1e-9
        );

        assertEquals(1, s.ingredientCount());
        assertEquals(1, s.actionCount());
    }

    @Test
    void emptyExperimentHasZeroPropertiesAndEmptyResult() {
        var s = engine.preview(List.of());

        assertEquals(0, s.healing());
        assertEquals(0, s.ingredientCount());
        assertEquals("пусто", s.state());
        assertEquals("прозрачный", s.color());

        assertEquals(
                "Пустой котёл",
                engine.finish(List.of(), List.of()).name()
        );
    }

    @Test
    void quantityScalesTheMixtureAndDoesNotMutateIngredient() {
        Ingredient ingredient = herb();

        var steps = List.<ExperimentStep>of(
                new IngredientAdditionStep(ingredient, 2.5)
        );

        assertEquals(25, engine.preview(steps).healing());

        assertEquals(
                engine.preview(steps),
                engine.preview(steps)
        );

        assertEquals(10, ingredient.healing());
    }

    @Test
    void actionAffectsOnlyIngredientsAlreadyAdded() {
        var add = new IngredientAdditionStep(herb(), 1);
        var burn = new ActionStep(AlchemyAction.BURN);

        assertEquals(
                12.2,
                engine.preview(List.of(add, burn, add)).healing(),
                1e-9
        );

        assertEquals(
                4.4,
                engine.preview(List.of(add, add, burn)).healing(),
                1e-9
        );
    }

    @Test
    void snapshotRoundsFractionalPropertiesToOneDecimalPlace() {
        var ingredient = draft("Трава").toIngredient(1);

        var result = engine.preview(List.of(
                new IngredientAdditionStep(ingredient, 1)
        ));

        assertEquals(1.3, result.healing(), 1e-9);
        assertEquals(2.5, result.toxicity(), 1e-9);
    }

    @ParameterizedTest
    @CsvSource({
            "BOIL,жидкость,горячая",
            "DISTILL,дистиллят,горячая",
            "COOL,смесь компонентов,холодная",
            "BURN,порошок / зола,горячая"
    })
    void actionsChangeStateAndTemperature(
            AlchemyAction action,
            String state,
            String temperature
    ) {
        var s = engine.preview(List.of(
                new IngredientAdditionStep(herb(), 1),
                new ActionStep(action)
        ));

        assertEquals(state, s.state());
        assertEquals(temperature, s.temperature());
    }

    @Test
    void finishUsesHighestPriorityThenLowestIdAndIgnoresDisabledRules() {
        var disabled = new PotionRule(
                1, "Выключено", "Тип", "",
                1000, false, List.of()
        );

        var laterId = new PotionRule(
                3, "Позже", "Тип", "",
                100, true, List.of()
        );

        var winner = new PotionRule(
                2, "Победитель", "Тип", "Эффект",
                100, true, List.of()
        );

        var result = engine.finish(
                steps(herb()),
                List.of(laterId, fallback(), disabled, winner)
        );

        assertEquals("Победитель", result.name());
        assertEquals("Эффект", result.description());

        assertEquals(
                engine.preview(steps(herb())),
                result.snapshot()
        );
    }

    @Test
    void missingMatchingRuleIsAnError() {
        assertThrows(
                IllegalStateException.class,
                () -> engine.finish(steps(herb()), List.of())
        );
    }
}