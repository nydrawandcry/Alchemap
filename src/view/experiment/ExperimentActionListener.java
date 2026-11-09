package view.experiment;

import model.AlchemyAction;

import java.util.EventListener;

/**
 * Получает запросы панели управления экспериментом
 */
public interface ExperimentActionListener extends EventListener {
    void actionRequested(AlchemyAction action);

    void undoRequested();

    void newExperimentRequested();

    void finishRequested();
}