package ru.alchemap.data;

import ru.alchemap.model.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
                    if (step instanceof AddIngredientStep add) {
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
        String sql = """
                INSERT INTO saved_recipes(experiment_id, name)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE name = VALUES(name)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, experimentId);
            ps.setString(2, recipeName);
            ps.executeUpdate();
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
