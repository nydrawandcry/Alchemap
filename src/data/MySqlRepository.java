package data;

import model.*;
import model.potions.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;

public final class MySqlRepository implements AlchemyRepository {
    private final Connection connection;

    public MySqlRepository(Properties config) throws SQLException {
        String url = config.getProperty("db.url", "jdbc:mysql://localhost:3306/alchemap?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
        String user = config.getProperty("db.user", "root");
        String password = config.getProperty("db.password", "");
        this.connection = DriverManager.getConnection(url, user, password);
    }

    @Override
    public List<Ingredient> loadIngredients() throws SQLException {
        String sql = """
                SELECT id, name, category, description, healing, toxicity, fire, frost,
                       volatility, magic, stability
                FROM ingredients
                WHERE enabled = 1
                ORDER BY name
                """;
        List<Ingredient> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new Ingredient(
                        rs.getInt("id"), rs.getString("name"), rs.getString("category"), rs.getString("description"),
                        rs.getDouble("healing"), rs.getDouble("toxicity"), rs.getDouble("fire"), rs.getDouble("frost"),
                        rs.getDouble("volatility"), rs.getDouble("magic"), rs.getDouble("stability")
                ));
            }
        }
        return result;
    }

    /**
     * Добавляет ингредиент и возвращает назначенный базой ID
     * @param draft добавляемый новый ингредиент
     * @return добавленный ингредиент
     * @throws SQLException исключение если шото пошло не так
     */
    @Override
    public Ingredient createIngredient(NewIngredient draft)
            throws SQLException {
        String sql = """
            INSERT INTO ingredients
                (name, category, description, healing, toxicity,
                 fire, frost, volatility, magic, stability, enabled)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)
            """;

        try (PreparedStatement ps = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, draft.name());
            ps.setString(2, draft.category());
            ps.setString(3, draft.description());
            ps.setDouble(4, draft.healing());
            ps.setDouble(5, draft.toxicity());
            ps.setDouble(6, draft.fire());
            ps.setDouble(7, draft.frost());
            ps.setDouble(8, draft.volatility());
            ps.setDouble(9, draft.magic());
            ps.setDouble(10, draft.stability());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException(
                            "MySQL не вернул ID ингредиента."
                    );
                }

                return draft.toIngredient(keys.getInt(1));
            }
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1062) {
                throw new IllegalArgumentException(
                        "Название ингредиента уже используется. "
                                + "Выберите другое.",
                        ex
                );
            }

            throw ex;
        }
    }

    @Override
    public List<HistoryEntry> loadHistory() throws SQLException {
        String sql = """
                SELECT e.id, e.name, e.created_at, e.result_name, e.result_type,
                       CASE WHEN sr.id IS NULL THEN 0 ELSE 1 END AS saved_recipe
                FROM experiments e
                LEFT JOIN saved_recipes sr ON sr.experiment_id = e.id
                ORDER BY e.created_at DESC
                LIMIT 100
                """;
        List<HistoryEntry> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("created_at");
                result.add(new HistoryEntry(
                        rs.getLong("id"), rs.getString("name"),
                        ts == null ? LocalDateTime.now() : ts.toLocalDateTime(),
                        rs.getString("result_name"), rs.getString("result_type"), rs.getBoolean("saved_recipe")
                ));
            }
        }
        return result;
    }

    @Override
    public long saveExperiment(String name, List<ExperimentStep> steps, AlchemyResult result) throws SQLException {
        name = TextValidation.required(name, 160, "Название опыта");
        boolean oldAuto = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            long id;
            String expSql = """
                    INSERT INTO experiments(name, result_name, result_type, result_description,
                                            healing, toxicity, fire, frost, volatility, magic, stability,
                                            state, temperature, color)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;
            try (PreparedStatement ps = connection.prepareStatement(expSql, Statement.RETURN_GENERATED_KEYS)) {
                MixtureSnapshot s = result.snapshot();
                ps.setString(1, name);
                ps.setString(2, result.name());
                ps.setString(3, result.type());
                ps.setString(4, result.description());
                ps.setDouble(5, s.healing());
                ps.setDouble(6, s.toxicity());
                ps.setDouble(7, s.fire());
                ps.setDouble(8, s.frost());
                ps.setDouble(9, s.volatility());
                ps.setDouble(10, s.magic());
                ps.setDouble(11, s.stability());
                ps.setString(12, s.state());
                ps.setString(13, s.temperature());
                ps.setString(14, s.color());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("MySQL не вернул ID эксперимента");
                    id = keys.getLong(1);
                }
            }

            String stepSql = """
                    INSERT INTO experiment_steps(experiment_id, step_order, step_type, ingredient_id, quantity, action_code, display_text)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """;
            try (PreparedStatement ps = connection.prepareStatement(stepSql)) {
                int order = 1;
                for (ExperimentStep step : steps) {
                    ps.setLong(1, id);
                    ps.setInt(2, order++);
                    if (step instanceof IngredientAdditionStep add) {
                        ps.setString(3, "INGREDIENT");
                        ps.setInt(4, add.ingredient().id());
                        ps.setDouble(5, add.quantity());
                        ps.setNull(6, Types.VARCHAR);
                    } else if (step instanceof ActionStep action) {
                        ps.setString(3, "ACTION");
                        ps.setNull(4, Types.INTEGER);
                        ps.setNull(5, Types.DECIMAL);
                        ps.setString(6, action.action().name());
                    }
                    ps.setString(7, step.displayText());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            connection.commit();
            return id;
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(oldAuto);
        }
    }

    @Override
    public void markAsRecipe(long experimentId, String recipeName) throws SQLException {
        recipeName = TextValidation.required(recipeName, 160, "Название рецепта");
        String sql = """
                INSERT INTO saved_recipes(experiment_id, name)
                VALUES (?, ?) AS incoming
                ON DUPLICATE KEY UPDATE name = incoming.name
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, experimentId);
            ps.setString(2, recipeName);
            ps.executeUpdate();
        }
    }

    @Override
    public RecipeDetails loadRecipe(long experimentId) throws SQLException {
        String sql = """
                SELECT e.name AS experiment_name, r.name AS recipe_name,
                       e.result_name, e.result_type, e.result_description
                FROM experiments e JOIN saved_recipes r ON r.experiment_id = e.id
                WHERE e.id = ?
                """;
        String experimentName, recipeName, resultName, resultType, description;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, experimentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Сохранённый рецепт не найден. Обновите журнал.");
                experimentName = rs.getString("experiment_name");
                recipeName = rs.getString("recipe_name");
                resultName = rs.getString("result_name");
                resultType = rs.getString("result_type");
                description = rs.getString("result_description");
            }
        }
        List<String> steps = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT display_text FROM experiment_steps WHERE experiment_id = ? ORDER BY step_order")) {
            ps.setLong(1, experimentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) steps.add(rs.getString("display_text"));
            }
        }
        return new RecipeDetails(experimentId, experimentName, recipeName,
                resultName, resultType, description, steps);
    }

    @Override
    public void renameExperiment(long experimentId, String name) throws SQLException {
        name = TextValidation.required(name, 160, "Название опыта");
        try (PreparedStatement ps = connection.prepareStatement("UPDATE experiments SET name = ? WHERE id = ?")) {
            ps.setString(1, name);
            ps.setLong(2, experimentId);
            if (ps.executeUpdate() != 1) throw new SQLException("Опыт не найден. Обновите журнал.");
        }
    }

    @Override
    public List<PotionRule> loadPotionRules() throws SQLException {
        Map<Long, List<RuleCondition>> conditions = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT rule_id, alternative_group, metric, comparison, other_metric, multiplier, threshold
                FROM potion_rule_conditions ORDER BY rule_id, condition_order
                """); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String other = rs.getString("other_metric");
                try {
                    RuleCondition condition = new RuleCondition(rs.getInt("alternative_group"),
                            RuleMetric.valueOf(rs.getString("metric")),
                            RuleComparison.valueOf(rs.getString("comparison")),
                            other == null ? null : RuleMetric.valueOf(other),
                            rs.getDouble("multiplier"), rs.getDouble("threshold"));
                    conditions.computeIfAbsent(rs.getLong("rule_id"), ignored -> new ArrayList<>()).add(condition);
                } catch (IllegalArgumentException ex) {
                    throw new SQLException("Некорректное условие результата в базе: " + ex.getMessage(), ex);
                }
            }
        }
        List<PotionRule> rules = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT id, name, result_type, description, priority, enabled
                FROM potion_rules ORDER BY priority DESC, id
                """); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long id = rs.getLong("id");
                rules.add(new PotionRule(id, rs.getString("name"), rs.getString("result_type"),
                        rs.getString("description"), rs.getInt("priority"), rs.getBoolean("enabled"),
                        conditions.getOrDefault(id, List.of())));
            }
        }
        return rules;
    }

    @Override
    public long savePotionRule(PotionRule rule) throws SQLException {
        boolean oldAuto = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            long id = rule.id();
            String sql = id == 0
                    ? "INSERT INTO potion_rules(name, result_type, description, priority, enabled) VALUES (?, ?, ?, ?, ?)"
                    : "UPDATE potion_rules SET name = ?, result_type = ?, description = ?, priority = ?, enabled = ? WHERE id = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, rule.name()); ps.setString(2, rule.type()); ps.setString(3, rule.description());
                ps.setInt(4, rule.priority()); ps.setBoolean(5, rule.enabled());
                if (id != 0) ps.setLong(6, id);
                if (ps.executeUpdate() != 1) throw new SQLException("Правило не найдено. Обновите список.");
                if (id == 0) {
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("MySQL не вернул ID правила.");
                        id = keys.getLong(1);
                    }
                }
            }
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM potion_rule_conditions WHERE rule_id = ?")) {
                ps.setLong(1, id); ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO potion_rule_conditions(rule_id, condition_order, alternative_group,
                        metric, comparison, other_metric, multiplier, threshold)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                int order = 1;
                for (RuleCondition c : rule.conditions()) {
                    ps.setLong(1, id); ps.setInt(2, order++); ps.setInt(3, c.group());
                    ps.setString(4, c.metric().name()); ps.setString(5, c.comparison().name());
                    if (c.otherMetric() == null) ps.setNull(6, Types.VARCHAR);
                    else ps.setString(6, c.otherMetric().name());
                    ps.setDouble(7, c.multiplier()); ps.setDouble(8, c.threshold());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            connection.commit();
            return id;
        } catch (SQLException ex) {
            try { connection.rollback(); } catch (SQLException rollback) { ex.addSuppressed(rollback); }
            throw ex;
        } finally {
            connection.setAutoCommit(oldAuto);
        }
    }

    @Override
    public String modeName() {
        return "MySQL";
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}
