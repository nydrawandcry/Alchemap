package data;

import model.*;
import model.potions.*;

import java.util.List;

public interface AlchemyRepository extends AutoCloseable {
    List<Ingredient> loadIngredients() throws Exception;
    Ingredient createIngredient(NewIngredient ingredient) throws Exception;
    void deleteIngredient(int ingredientId) throws Exception;
    List<HistoryEntry> loadHistory() throws Exception;
    long saveExperiment(String name, List<ExperimentStep> steps, AlchemyResult result) throws Exception;
    void markAsRecipe(long experimentId, String recipeName) throws Exception;
    RecipeDetails loadRecipe(long experimentId) throws Exception;
    void renameExperiment(long experimentId, String name) throws Exception;
    List<PotionRule> loadPotionRules() throws Exception;
    long savePotionRule(PotionRule rule) throws Exception;
    String modeName();
    @Override default void close() throws Exception {}
}
