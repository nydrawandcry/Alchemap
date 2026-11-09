package controller;

import model.ExperimentState;

/**
 * Действия интерфейса, необходимые контроллеру опыта
 */
public interface ExperimentView {
    void render(ExperimentState state);

    String askName(String title, String initial);

    void showInfo(String message);

    void showError(String title, String message);
}