-- Alchemap MVP
-- Импортировать целиком в MySQL Workbench.
-- Скрипт пересоздаёт таблицы внутри БД alchemap, но не удаляет саму БД.

CREATE DATABASE IF NOT EXISTS alchemap
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE alchemap;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS saved_recipes;
DROP TABLE IF EXISTS experiment_steps;
DROP TABLE IF EXISTS experiments;
DROP TABLE IF EXISTS operations;
DROP TABLE IF EXISTS ingredients;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE ingredients (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(60) NOT NULL,
    description VARCHAR(500) NOT NULL,
    healing DECIMAL(7,2) NOT NULL DEFAULT 0,
    toxicity DECIMAL(7,2) NOT NULL DEFAULT 0,
    fire DECIMAL(7,2) NOT NULL DEFAULT 0,
    frost DECIMAL(7,2) NOT NULL DEFAULT 0,
    volatility DECIMAL(7,2) NOT NULL DEFAULT 0,
    magic DECIMAL(7,2) NOT NULL DEFAULT 0,
    stability DECIMAL(7,2) NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE operations (
    code VARCHAR(30) PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(500) NOT NULL
);

CREATE TABLE experiments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(160) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    result_name VARCHAR(160) NOT NULL,
    result_type VARCHAR(80) NOT NULL,
    result_description VARCHAR(800) NOT NULL,
    healing DECIMAL(9,2) NOT NULL DEFAULT 0,
    toxicity DECIMAL(9,2) NOT NULL DEFAULT 0,
    fire DECIMAL(9,2) NOT NULL DEFAULT 0,
    frost DECIMAL(9,2) NOT NULL DEFAULT 0,
    volatility DECIMAL(9,2) NOT NULL DEFAULT 0,
    magic DECIMAL(9,2) NOT NULL DEFAULT 0,
    stability DECIMAL(9,2) NOT NULL DEFAULT 0,
    state VARCHAR(80) NOT NULL,
    temperature VARCHAR(80) NOT NULL,
    color VARCHAR(80) NOT NULL
);

CREATE TABLE experiment_steps (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    experiment_id BIGINT NOT NULL,
    step_order INT NOT NULL,
    step_type ENUM('INGREDIENT', 'ACTION') NOT NULL,
    ingredient_id INT NULL,
    quantity DECIMAL(8,2) NULL,
    action_code VARCHAR(30) NULL,
    display_text VARCHAR(220) NOT NULL,
    CONSTRAINT fk_step_experiment
        FOREIGN KEY (experiment_id) REFERENCES experiments(id) ON DELETE CASCADE,
    CONSTRAINT fk_step_ingredient
        FOREIGN KEY (ingredient_id) REFERENCES ingredients(id) ON DELETE SET NULL,
    CONSTRAINT fk_step_action
        FOREIGN KEY (action_code) REFERENCES operations(code) ON DELETE RESTRICT,
    UNIQUE KEY uq_experiment_order (experiment_id, step_order)
);

CREATE TABLE saved_recipes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    experiment_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recipe_experiment
        FOREIGN KEY (experiment_id) REFERENCES experiments(id) ON DELETE CASCADE
);

INSERT INTO operations(code, name, description) VALUES
('GRIND', 'Измельчить', 'Раскрывает активные свойства, но слегка снижает стабильность.'),
('CUT', 'Нарезать', 'Слабо усиливает свойства без серьёзного вмешательства.'),
('BOIL', 'Сварить', 'Переводит смесь в жидкое состояние и усиливает растворимые свойства.'),
('BURN', 'Сжечь', 'Разрушает лечебные свойства, усиливает огненные и токсичные.'),
('COOL', 'Охладить', 'Повышает стабильность и усиливает холодные свойства.'),
('FILTER', 'Фильтровать', 'Снижает токсичность и удаляет часть тяжёлых примесей.'),
('DISTILL', 'Перегнать', 'Концентрирует летучие и магические свойства.'),
('INFUSE', 'Настоять', 'Мягко усиливает лечебные и магические свойства.'),
('DRY', 'Высушить', 'Снижает воду, повышает концентрацию и летучесть сырья.');

INSERT INTO ingredients
(name, category, description, healing, toxicity, fire, frost, volatility, magic, stability)
VALUES
('Помет беса', 'Органика', 'Дурно пахнущий.', 12, 1, 1, 3, 13, 12, 33),
('Лунная трава', 'Трава', 'Светлая трава, усиливающая восстановительные настои.', 5, 0.5, 0, 1, 1, 2, 3),
('Пепел саламандры', 'Минерал', 'Горячий алхимический пепел с выраженной огненной природой.', 0, 2, 6, 0, 2, 2, 1),
('Слизь тролля', 'Органика', 'Вязкая регенерирующая субстанция. Полезна, но плохо очищена.', 4, 2.5, 0, 0, 0.5, 1, 4),
('Серебряная пыль', 'Минерал', 'Магически проводящий порошок для стабилизации сложных составов.', 0.5, 0, 0, 0.5, 1, 5, 5),
('Ночная спорынья', 'Гриб', 'Тёмные споры, содержащие сильные токсичные алкалоиды.', 0, 7, 0, 0, 3, 1, 1),
('Морозник', 'Трава', 'Редкая холодная трава, реагирующая на охлаждение.', 1, 1, 0, 6, 1, 2, 3),
('Кровь василиска', 'Органика', 'Опасный магический компонент с высокой токсичностью.', 0, 6, 1, 0, 2, 5, 2),
('Роса фей', 'Жидкость', 'Лёгкая магическая жидкость, усиливающая настои и эссенции.', 3, 0, 0, 1, 3, 6, 4),
('Корень мандрагоры', 'Корень', 'Сильный универсальный реагент с лечебными и токсичными фракциями.', 3, 3, 0, 0, 1, 4, 3),
('Уголь драконьего дуба', 'Минерал', 'Пористый уголь, который хорошо переносит нагрев и фильтрацию.', 0, 1, 4, 0, 1, 1, 4),
('Лепесток снежной розы', 'Цветок', 'Хрупкий лепесток с выраженным морозным эффектом.', 2, 0, 0, 5, 2, 3, 2),
('Жёлчь виверны', 'Органика', 'Агрессивный токсичный реагент, особенно опасный после перегонки.', 0, 8, 1, 0, 4, 2, 1);

-- Быстрая проверка импорта:
SELECT COUNT(*) AS ingredients_count FROM ingredients;
SELECT COUNT(*) AS operations_count FROM operations;
