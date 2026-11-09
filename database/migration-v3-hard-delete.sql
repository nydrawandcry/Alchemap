USE alchemap;

ALTER TABLE experiment_steps
DROP FOREIGN KEY fk_step_ingredient;

ALTER TABLE experiment_steps
    ADD CONSTRAINT fk_step_ingredient
        FOREIGN KEY (ingredient_id)
            REFERENCES ingredients(id)
            ON DELETE SET NULL;

-- Окончательно удаляем ингредиенты, скрытые прежним механизмом удаления.
SET @alchemap_previous_safe_updates = @@SESSION.sql_safe_updates;
SET SESSION sql_safe_updates = 0;

DELETE FROM ingredients WHERE enabled = FALSE;

SET SESSION sql_safe_updates = @alchemap_previous_safe_updates;