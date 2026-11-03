package model.potions;

import java.util.List;

/** Текст шагов — исторический снимок, независимый от последующих правок ингредиентов и зелий. */
public record RecipeDetails(long experimentId, String experimentName, String recipeName,
                             String resultName, String resultType, String resultDescription,
                             List<String> steps) {
    public RecipeDetails { steps = List.copyOf(steps); }
}
