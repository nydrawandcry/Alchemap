package controller;

import data.AlchemyRepository;
import engine.AlchemyEngine;
import model.*;
import view.experiment.ExperimentActionListener;
import view.ingredients.IngredientAddListener;
import view.mixture.RecipeSaveListener;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Управляет шагами, расчётом и сохранением опыта
 */
public final class ExperimentController
        implements ExperimentActionListener,
        IngredientAddListener,
        RecipeSaveListener {

    private final AlchemyRepository repository;
    private final ExperimentView view;
    private final Runnable onHistoryChanged;

    private final AlchemyEngine engine = new AlchemyEngine();
    private final List<ExperimentStep> steps = new ArrayList<>();

    private long lastExperimentId = -1;
    private AlchemyResult lastResult;
    private boolean recipeSaved;

    public ExperimentController(
            AlchemyRepository repository,
            ExperimentView view,
            Runnable onHistoryChanged
    ) {
        this.repository = Objects.requireNonNull(repository);
        this.view = Objects.requireNonNull(view);
        this.onHistoryChanged =
                Objects.requireNonNull(onHistoryChanged);
    }

    /**
     * Вызывается после создания всех элементов интерфейса
     */
    public void refresh() {
        view.render(
                new ExperimentState(
                        steps,
                        engine.preview(steps),
                        lastResult,
                        recipeSaved
                )
        );
    }

    @Override
    public void ingredientAddRequested(
            Ingredient ingredient,
            double quantity
    ) {
        addStep(new IngredientAdditionStep(ingredient, quantity));
    }

    @Override
    public void actionRequested(AlchemyAction action) {
        if (steps.isEmpty()) {
            view.showInfo(
                    "Сначала добавьте хотя бы один ингредиент."
            );
            return;
        }

        addStep(new ActionStep(action));
    }

    private void addStep(ExperimentStep step) {
        steps.add(step);
        invalidateResult();
        refresh();
    }

    @Override
    public void undoRequested() {
        if (steps.isEmpty()) return;

        steps.remove(steps.size() - 1);
        invalidateResult();
        refresh();
    }

    @Override
    public void newExperimentRequested() {
        steps.clear();
        invalidateResult();
        refresh();
    }

    private void invalidateResult() {
        lastExperimentId = -1;
        lastResult = null;
        recipeSaved = false;
    }

    @Override
    public void finishRequested() {
        if (steps.isEmpty()) {
            view.showInfo(
                    "Эксперимент пуст. Добавьте ингредиенты."
            );
            return;
        }

        try {
            AlchemyResult result = engine.finish(
                    steps,
                    repository.loadPotionRules()
            );

            String defaultName = "Опыт " + LocalDateTime.now()
                    .format(
                            DateTimeFormatter.ofPattern(
                                    "dd.MM.yyyy HH:mm:ss"
                            )
                    );

            String name = view.askName(
                    "Название опыта · " + result.name(),
                    defaultName
            );

            if (name == null) return;

            long id = repository.saveExperiment(
                    name,
                    List.copyOf(steps),
                    result
            );

            lastExperimentId = id;
            lastResult = result;
            recipeSaved = false;

            refresh();
            notifyHistoryChanged();
        } catch (Exception ex) {
            view.showError(
                    "Ошибка",
                    "Не удалось завершить и сохранить эксперимент: "
                            + ex.getMessage()
            );
        }
    }

    @Override
    public void recipeSaveRequested() {
        if (lastExperimentId < 0
                || lastResult == null
                || recipeSaved) {
            return;
        }

        String name = view.askName(
                "Название рецепта",
                lastResult.name()
        );

        if (name == null) return;

        try {
            repository.markAsRecipe(
                    lastExperimentId,
                    name.trim()
            );

            recipeSaved = true;

            refresh();
            notifyHistoryChanged();

            view.showInfo(
                    "Рецепт «" + name.trim() + "» сохранён."
            );
        } catch (Exception ex) {
            view.showError(
                    "Ошибка",
                    "Не удалось сохранить рецепт: "
                            + ex.getMessage()
            );
        }
    }

    private void notifyHistoryChanged() {
        try {
            onHistoryChanged.run();
        } catch (RuntimeException ex) {
            view.showError(
                    "Журнал опытов",
                    "Данные сохранены, но обновить журнал не удалось: "
                            + ex.getMessage()
            );
        }
    }
}