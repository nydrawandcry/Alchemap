package model;

import java.time.LocalDateTime;

/**
 * Запись для истории приготовления зелиц
 * @param id уникальный идентификатор зелья
 * @param name название
 * @param createdAt
 * @param resultName
 * @param resultType
 * @param savedRecipe
 */
public record HistoryEntry(
        long id,
        String name,
        LocalDateTime createdAt,
        String resultName,
        String resultType,
        boolean savedRecipe
) {}
