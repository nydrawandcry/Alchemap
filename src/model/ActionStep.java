package model;

/**
 * Манипуляция с ингредиентом
 * @param action манипуляция, которая производится над компонентом
 */
public record ActionStep(AlchemyAction action) implements ExperimentStep {
    @Override
    public String displayText() {
        return "-> " + action.label();
    }
}
