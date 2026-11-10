package data;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class MySqlRepository extends JdbcRepository {
    public MySqlRepository(Properties config) throws SQLException {
        super(DriverManager.getConnection(
                config.getProperty("db.url",
                        "jdbc:mysql://localhost:3306/alchemap?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"),
                config.getProperty("db.user", "root"),
                config.getProperty("db.password", "")
        ));
    }

    @Override
    protected String recipeUpsertSql() {
        return """
                INSERT INTO saved_recipes(experiment_id, name)
                VALUES (?, ?) AS incoming
                ON DUPLICATE KEY UPDATE name = incoming.name
                """;
    }

    @Override
    public String modeName() {
        return "MySQL";
    }
}
