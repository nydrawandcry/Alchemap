package model;

/**
 * Итоговый результат после выполнения всех манипуляций
 * @param name название зелья
 * @param type тип зелья
 * @param description описание зелья
 * @param snapshot последний сделанный снимок зелья (= итоговым характеристикам зелья)
 */
public record AlchemyResult(
        String name,
        String type,
        String description,
        MixtureSnapshot snapshot
) {}