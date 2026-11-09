package model;

import java.util.List;

/**
 * Состояние для отображения; result равен null у незавершённого опыта
 */
public record ExperimentState(
        List<ExperimentStep> steps,
        MixtureSnapshot mixture,
        AlchemyResult result,
        boolean recipeSaved
) {
    public ExperimentState {
        steps = List.copyOf(steps);
    }
}