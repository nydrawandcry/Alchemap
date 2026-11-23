package model.potions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static model.potions.RuleMetric.*;
import static model.potions.RuleComparison.*;
import static model.potions.RuleCondition.number;
import static support.Fixtures.*;

class RulesTest {

    @ParameterizedTest
    @CsvSource({
            "GE,3,3,true",
            "GE,2,3,false",
            "GE,4,3,true",
            "LE,3,3,true",
            "LE,4,3,false",
            "LE,2,3,true",
            "GT,3,3,false",
            "GT,4,3,true",
            "GT,2,3,false",
            "LT,3,3,false",
            "LT,2,3,true",
            "LT,4,3,false",
            "EQ,3,3,true",
            "EQ,2,3,false",
            "NE,3,3,false",
            "NE,2,3,true"
    })
    void comparisonBoundaries(
            RuleComparison comparison,
            double left,
            double right,
            boolean expected
    ) {
        assertEquals(expected, comparison.test(left, right));
    }

    @ParameterizedTest
    @CsvSource({
            "HEALING,20",
            "TOXICITY,3",
            "FIRE,5",
            "FROST,7",
            "VOLATILITY,2",
            "MAGIC,11",
            "STABILITY,15",
            "CONFLICT,5",
            "USEFUL,20",
            "INGREDIENT_COUNT,2",
            "ACTION_COUNT,3",
            "BURN_COUNT,2",
            "LIQUID,1",
            "DISTILLED,0",
            "COOLED,1",
            "COLD,1"
    })
    void metricReadsTheCorrectProperty(
            RuleMetric metric,
            double expected
    ) {
        assertEquals(expected, metric.value(context()), 1e-9);
    }

    @Test
    void booleanFlagsCanBeZeroAndOne() {
        AlchemyContext dry = new AlchemyContext(
                snapshot(), false, true, false, 0
        );

        assertAll(
                () -> assertEquals(0, LIQUID.value(dry)),
                () -> assertEquals(1, DISTILLED.value(dry)),
                () -> assertEquals(0, COOLED.value(dry))
        );
    }

    @Test
    void conditionUsesOtherMetricMultiplierAndOffset() {
        assertTrue(new RuleCondition(
                1, HEALING, GE, TOXICITY, 2, 14
        ).matches(context()));

        assertFalse(new RuleCondition(
                1, HEALING, GE, TOXICITY, 2, 15
        ).matches(context()));

        assertTrue(new RuleCondition(
                1, HEALING, EQ, TOXICITY, -1, 23
        ).matches(context()));

        assertTrue(
                number(1, HEALING, EQ, 20).matches(context())
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 100})
    void conditionRejectsInvalidGroup(int group) {
        assertThrows(
                IllegalArgumentException.class,
                () -> number(group, HEALING, GE, 1)
        );
    }

    @ParameterizedTest
    @ValueSource(doubles = {
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            1_000_000_001,
            -1_000_000_001
    })
    void conditionRejectsInvalidNumbers(double value) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RuleCondition(
                        1, HEALING, GE, null, value, 0
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> number(1, HEALING, GE, value)
        );
    }

    @Test
    void conditionAcceptsNumericAndGroupBoundaries() {
        assertDoesNotThrow(() -> new RuleCondition(
                1, HEALING, GE, null, -1e9, 1e9
        ));

        assertDoesNotThrow(
                () -> number(99, HEALING, GE, -1e9)
        );

        assertThrows(
                NullPointerException.class,
                () -> number(1, null, GE, 0)
        );

        assertThrows(
                NullPointerException.class,
                () -> number(1, HEALING, null, 0)
        );
    }

    @Test
    void conditionsAreAndWithinGroupAndOrBetweenGroups() {
        List<RuleCondition> firstGroup = List.of(
                number(1, HEALING, GE, 15),
                number(1, TOXICITY, LE, 2)
        );

        assertFalse(
                rule(1, 10, true, firstGroup).matches(context())
        );

        List<RuleCondition> alternatives =
                new ArrayList<>(firstGroup);

        alternatives.add(number(2, COOLED, EQ, 1));

        assertTrue(
                rule(1, 10, true, alternatives).matches(context())
        );

        assertFalse(
                rule(1, 10, false, alternatives).matches(context())
        );
    }

    @Test
    void unconditionalRuleStillRespectsEnabledFlag() {
        assertTrue(
                rule(1, 0, true, List.of()).matches(context())
        );

        assertFalse(
                rule(1, 0, false, List.of()).matches(context())
        );
    }

    @Test
    void ruleValidatesDataAndCopiesConditions() {
        assertDoesNotThrow(() -> new PotionRule(
                0,
                "а".repeat(160),
                "б".repeat(80),
                "в".repeat(800),
                10000,
                true,
                List.of()
        ));

        assertThrows(
                IllegalArgumentException.class,
                () -> new PotionRule(
                        -1, "а", "б", "", 0, true, List.of()
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new PotionRule(
                        0, "а".repeat(161), "б", "",
                        0, true, List.of()
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new PotionRule(
                        0, "а", "б".repeat(81), "",
                        0, true, List.of()
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new PotionRule(
                        0, "а", "б", "в".repeat(801),
                        0, true, List.of()
                )
        );

        for (int priority : new int[]{-1, 10001}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> rule(1, priority, true, List.of())
            );
        }

        var source = new ArrayList<>(
                List.of(number(1, HEALING, GE, 1))
        );

        var saved = rule(1, 0, true, source);

        source.clear();

        assertEquals(1, saved.conditions().size());

        assertThrows(
                UnsupportedOperationException.class,
                () -> saved.conditions().clear()
        );
    }

    private PotionRule rule(
            long id,
            int priority,
            boolean enabled,
            List<RuleCondition> conditions
    ) {
        return new PotionRule(
                id, "Зелье", "Тип", "",
                priority, enabled, conditions
        );
    }
}