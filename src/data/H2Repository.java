package data;

import model.AlchemyAction;
import model.Ingredient;
import model.NewIngredient;
import model.potions.PotionRule;
import org.h2.tools.RunScript;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Локальная файловая база. Сервер и ручной импорт SQL не требуются. */
public final class H2Repository extends JdbcRepository {
    private static final int SCHEMA_VERSION = 1;

    public H2Repository() throws IOException, SQLException {
        this(defaultDataDirectory());
    }

    public H2Repository(Path directory) throws IOException, SQLException {
        super(openConnection(directory));
        try {
            initialize();
        } catch (IOException | SQLException | RuntimeException ex) {
            try { close(); }
            catch (SQLException closeError) { ex.addSuppressed(closeError); }
            throw ex;
        }
    }

    public static Path defaultDataDirectory() {
        String local = System.getenv("LOCALAPPDATA");
        if (local != null && !local.isBlank()) {
            return Path.of(local, "Alchemap");
        }
        return Path.of(System.getProperty("user.home"), ".alchemap");
    }

    private static Connection openConnection(Path directory)
            throws IOException, SQLException {
        Path absolute = directory.toAbsolutePath().normalize();
        Files.createDirectories(absolute);
        String file = absolute.resolve("alchemap").toString()
                .replace('\\', '/');
        if (file.contains(";")) {
            throw new IOException("Путь к базе не должен содержать символ ';'.");
        }
        return DriverManager.getConnection("jdbc:h2:file:" + file, "sa", "");
    }

    private void initialize() throws IOException, SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS app_schema (
                        id INT PRIMARY KEY CHECK (id = 1),
                        version INT NOT NULL
                    )
                    """);
        }

        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT version FROM app_schema WHERE id = 1")) {
            if (rows.next()) {
                if (rows.getInt(1) != SCHEMA_VERSION) {
                    throw new SQLException(
                            "Версия локальной базы не поддерживается. "
                                    + "Используйте подходящую версию Alchemap."
                    );
                }
                return;
            }
        }

        var input = H2Repository.class.getResourceAsStream("/db/h2-v1.sql");
        if (input == null) {
            throw new IOException("В сборке отсутствует db/h2-v1.sql.");
        }
        try (var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            RunScript.execute(connection, reader);
        }

        boolean oldAuto = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            seedData();
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO app_schema(id, version) VALUES (1, ?)")) {
                statement.setInt(1, SCHEMA_VERSION);
                statement.executeUpdate();
            }
            connection.commit();
        } catch (SQLException | RuntimeException ex) {
            try { connection.rollback(); }
            catch (SQLException rollback) { ex.addSuppressed(rollback); }
            throw ex;
        } finally {
            connection.setAutoCommit(oldAuto);
        }
    }

    private void seedData() throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO operations(code, name, description) VALUES (?, ?, ?)")) {
            for (AlchemyAction action : AlchemyAction.values()) {
                statement.setString(1, action.name());
                statement.setString(2, action.label());
                statement.setString(3, action.description());
                statement.addBatch();
            }
            statement.executeBatch();
        }

        for (Ingredient ingredient : DemoRepository.defaultIngredients()) {
            createIngredient(new NewIngredient(
                    ingredient.name(), ingredient.category(), ingredient.description(),
                    ingredient.healing(), ingredient.toxicity(), ingredient.fire(),
                    ingredient.frost(), ingredient.volatility(), ingredient.magic(),
                    ingredient.stability()
            ));
        }

        for (PotionRule rule : DefaultPotionRules.create()) {
            savePotionRule(new PotionRule(
                    0, rule.name(), rule.type(), rule.description(),
                    rule.priority(), rule.enabled(), rule.conditions()
            ));
        }
    }

    @Override
    protected String recipeUpsertSql() {
        return """
                MERGE INTO saved_recipes(experiment_id, name)
                KEY(experiment_id) VALUES (?, ?)
                """;
    }

    @Override
    public String modeName() {
        return "локальная база H2";
    }
}
