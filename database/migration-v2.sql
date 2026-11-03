-- Alchemap v2. Run once before starting the updated application.
-- Additive and repeatable: existing experiments, recipes and edited rules are preserved.
USE alchemap;

CREATE TABLE IF NOT EXISTS potion_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    seed_key VARCHAR(80) NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    result_type VARCHAR(80) NOT NULL,
    description VARCHAR(800) NOT NULL,
    priority INT NOT NULL DEFAULT 100,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_potion_priority CHECK (priority BETWEEN 0 AND 10000)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS potion_rule_conditions (
    rule_id BIGINT NOT NULL,
    condition_order INT NOT NULL,
    alternative_group INT NOT NULL,
    metric VARCHAR(40) NOT NULL,
    comparison VARCHAR(4) NOT NULL,
    other_metric VARCHAR(40) NULL,
    multiplier DOUBLE NOT NULL DEFAULT 1,
    threshold DOUBLE NOT NULL DEFAULT 0,
    PRIMARY KEY (rule_id, condition_order),
    CONSTRAINT fk_condition_potion_rule FOREIGN KEY (rule_id)
        REFERENCES potion_rules(id) ON DELETE CASCADE,
    CONSTRAINT chk_condition_group CHECK (alternative_group BETWEEN 1 AND 99)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

START TRANSACTION;

INSERT IGNORE INTO potion_rules(seed_key, name, result_type, description, priority, enabled) VALUES ('v1-1', 'Зелье малого лечения', 'Зелье', 'Стабильный лечебный состав. В игровой сессии может восстанавливать небольшое количество хитов.', 1000, TRUE);
SET @seed_rule_id = IF(ROW_COUNT() = 1, LAST_INSERT_ID(), NULL);
INSERT INTO potion_rule_conditions(rule_id, condition_order, alternative_group, metric, comparison, other_metric, multiplier, threshold) SELECT @seed_rule_id, 1, 1, 'HEALING', 'GE', NULL, 1.0, 20.0 WHERE @seed_rule_id IS NOT NULL;
INSERT INTO potion_rule_conditions(rule_id, condition_order, alternative_group, metric, comparison, other_metric, multiplier, threshold) SELECT @seed_rule_id, 2, 1, 'TOXICITY', 'LE', NULL, 1.0, 10.0 WHERE @seed_rule_id IS NOT NULL;
INSERT INTO potion_rule_conditions(rule_id, condition_order, alternative_group, metric, comparison, other_metric, multiplier, threshold) SELECT @seed_rule_id, 3, 1, 'STABILITY', 'GE', NULL, 1.0, 12.0 WHERE @seed_rule_id IS NOT NULL;
INSERT INTO potion_rule_conditions(rule_id, condition_order, alternative_group, metric, comparison, other_metric, multiplier, threshold) SELECT @seed_rule_id, 4, 1, 'LIQUID', 'EQ', NULL, 1.0, 1.0 WHERE @seed_rule_id IS NOT NULL;

INSERT IGNORE INTO potion_rules(seed_key, name, result_type, description, priority, enabled) VALUES ('v1-2', 'Эликсир огненного дыхания', 'Зелье', 'Горячая магическая смесь с выраженным огненным профилем. Требует осторожного хранения.', 900, TRUE);
SET @seed_rule_id = IF(ROW_COUNT() = 1, LAST_INSERT_ID(), NULL);
INSERT INTO potion_rule_conditions(rule_id, condition_order, alternative_group, metric, comparison, other_metric, multiplier, threshold) SELECT @seed_rule_id, 1, 1, 'FIRE', 'GE', NULL, 1.0, 18.0 WHERE @seed_rule_id IS NOT NULL;
INSERT INTO potion_rule_conditions(rule_id, condition_order, alternative_group, metric, comparison, other_metric, multiplier, threshold) SELECT @seed_rule_id, 2, 1, 'MAGIC', 'GE', NULL, 1.0, 8.0 WHERE @seed_rule_id IS NOT NULL;
INSERT INTO potion_rule_conditions(rule_id, condition_order, alternative_group, metric, comparison, other_metric, multiplier, threshold) SELECT @seed_rule_id, 3, 1, 'LIQUID', 'EQ', NULL, 1.0, 1.0 WHERE @seed_rule_id IS NOT NULL;
INSERT INTO potion_rule_conditions(rule_id, condition_order, alternative_group, metric, comparison, other_metric, multiplier, threshold) SELECT @seed_rule_id, 4, 1, 'COLD', 'EQ', NULL, 1.0, 0.0 WHERE @seed_rule_id IS NOT NULL;

INSERT IGNORE INTO potion_rules(seed_key, name, result_type, description, priority, enabled) VALUES ('v1-3', 'Морозная эссенция', 'Реагент', 'Концентрированный холодный реагент, пригодный для зелий, ловушек и зачарования.', 800, TRUE);
SET @seed_rule_id = IF(ROW_COUNT() = 1, LAST_INSERT_ID(), NULL);
INSERT INTO potion_rule_conditions(rule_id, condition_order, alternative_group, metric, comparison, other_metric, multiplier, threshold) SELECT @seed_rule_id, 1, 1, 'FROST', 'GE', NULL, 1.0, 16.0 WHERE @seed_rule_id IS NOT NULL;
INSERT INTO potion_rule_conditions(rule_id, condition_order, alternative_group, metric, comparison, other_metric, multiplier, threshold) SELECT @seed_rule_id, 2, 1, 'DISTILLED', 'EQ', NULL, 1.0, 1.0 WHERE @seed_rule_id IS NOT NULL;
INSERT INTO potion_rule_conditions(rule_id, condition_order, alternative_group, metric, comparison, other_metric, multiplier, threshold) SELECT @seed_rule_id, 3, 1, 'COOLED', 'EQ', NULL, 1.0, 1.0 WHERE @seed_rule_id IS NOT NULL;

INSERT IGNORE INTO potion_rules(seed_key, name, result_type, description, priority, enabled) VALUES ('v1-4', 'Яд туманного укуса', 'Яд', 'Токсичный концентрат. Летучесть делает его особенно опасным при вскрытии сосуда.', 700, TRUE);
SET @seed_rule_id = IF(ROW_COUNT() = 1, LAST_INSERT_ID(), NULL);
INSERT INTO potion_rule_conditions(rule_id, condition_order, alternative_group, metric, comparison, other_metric, multiplier, threshold) SELECT @seed_rule_id, 1, 1, 'TOXICITY', 'GE', NULL, 1.0, 20.0 WHERE @seed_rule_id IS NOT NULL;
INSERT INTO potion_rule_conditions(rule_id, condition_order, alternative_group, metric, comparison, other_metric, multiplier, threshold) SELECT @seed_rule_id, 2, 1, 'VOLATILITY', 'GE', NULL, 1.0, 7.0 WHERE @seed_rule_id IS NOT NULL;
INSERT INTO potion_rule_conditions(rule_id, condition_order, alternative_group, metric, comparison, other_metric, multiplier, threshold) SELECT @seed_rule_id, 3, 2, 'TOXICITY', 'GE', NULL, 1.0, 20.0 WHERE @seed_rule_id IS NOT NULL;
INSERT INTO potion_rule_conditions(rule_id, condition_order, alternative_group, metric, comparison, other_metric, multiplier, threshold) SELECT @seed_rule_id, 4, 2, 'DISTILLED', 'EQ', NULL, 1.0, 1.0 WHERE @seed_rule_id IS NOT NULL;

INSERT IGNORE INTO potion_rules(seed_key, name, result_type, description, priority, enabled) VALUES ('v1-5', 'Нестабильная алхимическая смесь', 'Нестабильная смесь', 'Состав конфликтует сам с собой. Его эффект непредсказуем, а хранение потенциально опасно.', 600, TRUE);
SET @seed_rule_id = IF(ROW_COUNT() = 1, LAST_INSERT_ID(), NULL);
INSERT INTO potion_rule_conditions(rule_id, condition_order, alternative_group, metric, comparison, other_metric, multiplier, threshold) SELECT @seed_rule_id, 1, 1, 'STABILITY', 'LT', NULL, 1.0, 5.0 WHERE @seed_rule_id IS NOT NULL;
INSERT INTO potion_rule_conditions(rule_id, condition_order, alternative_group, metric, comparison, other_metric, multiplier, threshold) SELECT @seed_rule_id, 2, 2, 'CONFLICT', 'GE', NULL, 1.0, 10.0 WHERE @seed_rule_id IS NOT NULL;
INSERT INTO potion_rule_conditions(rule_id, condition_order, alternative_group, metric, comparison, other_metric, multiplier, threshold) SELECT @seed_rule_id, 3, 3, 'VOLATILITY', 'GE', NULL, 1.0, 18.0 WHERE @seed_rule_id IS NOT NULL;
INSERT INTO potion_rule_conditions(rule_id, condition_order, alternative_group, metric, comparison, other_metric, multiplier, threshold) SELECT @seed_rule_id, 4, 3, 'FIRE', 'GE', NULL, 1.0, 10.0 WHERE @seed_rule_id IS NOT NULL;

INSERT IGNORE INTO potion_rules(seed_key, name, result_type, description, priority, enabled) VALUES ('v1-6', 'Бесполезный осадок', 'Бесполезный осадок', 'Активные свойства почти разрушены. В колбе осталась алхимически малоценная масса.', 500, TRUE);
SET @seed_rule_id = IF(ROW_COUNT() = 1, LAST_INSERT_ID(), NULL);
INSERT INTO potion_rule_conditions(rule_id, condition_order, alternative_group, metric, comparison, other_metric, multiplier, threshold) SELECT @seed_rule_id, 1, 1, 'USEFUL', 'LT', NULL, 1.0, 8.0 WHERE @seed_rule_id IS NOT NULL;
INSERT INTO potion_rule_conditions(rule_id, condition_order, alternative_group, metric, comparison, other_metric, multiplier, threshold) SELECT @seed_rule_id, 2, 2, 'BURN_COUNT', 'GE', NULL, 1.0, 2.0 WHERE @seed_rule_id IS NOT NULL;
INSERT INTO potion_rule_conditions(rule_id, condition_order, alternative_group, metric, comparison, other_metric, multiplier, threshold) SELECT @seed_rule_id, 3, 2, 'MAGIC', 'LT', NULL, 1.0, 5.0 WHERE @seed_rule_id IS NOT NULL;
INSERT INTO potion_rule_conditions(rule_id, condition_order, alternative_group, metric, comparison, other_metric, multiplier, threshold) SELECT @seed_rule_id, 4, 2, 'TOXICITY', 'LT', NULL, 1.0, 10.0 WHERE @seed_rule_id IS NOT NULL;

INSERT IGNORE INTO potion_rules(seed_key, name, result_type, description, priority, enabled) VALUES ('v1-7', 'Сырой алхимический яд', 'Яд', 'Токсичность доминирует над полезными свойствами. Эффект есть, но состав ещё грубый.', 400, TRUE);
SET @seed_rule_id = IF(ROW_COUNT() = 1, LAST_INSERT_ID(), NULL);
INSERT INTO potion_rule_conditions(rule_id, condition_order, alternative_group, metric, comparison, other_metric, multiplier, threshold) SELECT @seed_rule_id, 1, 1, 'TOXICITY', 'GT', 'HEALING', 1.15, 0.0 WHERE @seed_rule_id IS NOT NULL;
INSERT INTO potion_rule_conditions(rule_id, condition_order, alternative_group, metric, comparison, other_metric, multiplier, threshold) SELECT @seed_rule_id, 2, 1, 'TOXICITY', 'GT', NULL, 1.0, 12.0 WHERE @seed_rule_id IS NOT NULL;

INSERT IGNORE INTO potion_rules(seed_key, name, result_type, description, priority, enabled) VALUES ('v1-8', 'Травяной восстановительный настой', 'Зелье', 'Лечебные свойства выражены сильнее побочных. Получился простой пригодный настой.', 300, TRUE);
SET @seed_rule_id = IF(ROW_COUNT() = 1, LAST_INSERT_ID(), NULL);
INSERT INTO potion_rule_conditions(rule_id, condition_order, alternative_group, metric, comparison, other_metric, multiplier, threshold) SELECT @seed_rule_id, 1, 1, 'HEALING', 'GE', NULL, 1.0, 12.0 WHERE @seed_rule_id IS NOT NULL;
INSERT INTO potion_rule_conditions(rule_id, condition_order, alternative_group, metric, comparison, other_metric, multiplier, threshold) SELECT @seed_rule_id, 2, 1, 'HEALING', 'GE', 'TOXICITY', 1.0, 0.0 WHERE @seed_rule_id IS NOT NULL;

INSERT IGNORE INTO potion_rules(seed_key, name, result_type, description, priority, enabled) VALUES ('v1-9', 'Алхимический реагент', 'Реагент', 'Смесь имеет заметные активные свойства, но не сформировалась в законченное зелье.', 200, TRUE);
SET @seed_rule_id = IF(ROW_COUNT() = 1, LAST_INSERT_ID(), NULL);
INSERT INTO potion_rule_conditions(rule_id, condition_order, alternative_group, metric, comparison, other_metric, multiplier, threshold) SELECT @seed_rule_id, 1, 1, 'MAGIC', 'GE', NULL, 1.0, 10.0 WHERE @seed_rule_id IS NOT NULL;
INSERT INTO potion_rule_conditions(rule_id, condition_order, alternative_group, metric, comparison, other_metric, multiplier, threshold) SELECT @seed_rule_id, 2, 2, 'FIRE', 'GE', NULL, 1.0, 12.0 WHERE @seed_rule_id IS NOT NULL;
INSERT INTO potion_rule_conditions(rule_id, condition_order, alternative_group, metric, comparison, other_metric, multiplier, threshold) SELECT @seed_rule_id, 3, 3, 'FROST', 'GE', NULL, 1.0, 12.0 WHERE @seed_rule_id IS NOT NULL;

INSERT IGNORE INTO potion_rules(seed_key, name, result_type, description, priority, enabled) VALUES ('v1-10', 'Мутная алхимическая смесь', 'Бесполезный осадок', 'Компоненты взаимодействуют слишком слабо. Результат можно использовать только как сырьё для нового опыта.', 0, TRUE);
SET @seed_rule_id = IF(ROW_COUNT() = 1, LAST_INSERT_ID(), NULL);

COMMIT;
SELECT COUNT(*) AS potion_rules_count FROM potion_rules;
