package liquibase.databricks.database;

import liquibase.CatalogAndSchema;
import liquibase.GlobalConfiguration;
import liquibase.Scope;
import liquibase.configuration.ConfiguredValue;
import liquibase.database.AbstractJdbcDatabase;
import liquibase.database.DatabaseConnection;
import liquibase.exception.DatabaseException;
import liquibase.executor.ExecutorService;
import liquibase.statement.core.GetViewDefinitionStatement;
import liquibase.structure.DatabaseObject;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.GetViewDefinitionStatement;
import liquibase.statement.core.RawCallStatement;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Catalog;
import liquibase.structure.core.Column;
import liquibase.structure.core.ForeignKey;
import liquibase.structure.core.Index;
import liquibase.structure.core.PrimaryKey;
import liquibase.structure.core.Schema;
import liquibase.structure.core.Sequence;
import liquibase.structure.core.Table;
import liquibase.structure.core.UniqueConstraint;
import liquibase.structure.core.View;
import liquibase.util.JdbcUtil;
import liquibase.util.StringUtil;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Collections;
import java.util.regex.Pattern;

public class DatabricksDatabase extends AbstractJdbcDatabase {

    // define env variables for database
    public static final String PRODUCT_NAME = "databricks";
    private static final Integer DATABRICKS_PRIORITY = 55;

    // Set default catalog - must be unity Catalog Enabled
    private static final String DEFAULT_CATALOG = "main";

    // Set default Schema of given catalog
    private static final String DEFAULT_SCHEMA = "default";

    // This is from the new INFORMATION_SCHEMA() database
    private Set<String> systemTablesAndViews = new HashSet<>();

    //Define data type names enabled for auto-increment columns - currently only BIGINT
    public static final List<String> VALID_AUTO_INCREMENT_COLUMN_TYPE_NAMES = Collections.unmodifiableList(Arrays.asList("BIGINT"));


    public DatabricksDatabase() {

        super.setCurrentDateTimeFunction("current_timestamp()");
        super.addReservedWords(getDatabricksReservedWords());
        super.defaultAutoIncrementStartWith = BigInteger.ONE;
        super.defaultAutoIncrementBy = BigInteger.ONE;
        super.setDefaultCatalogName(DEFAULT_CATALOG);
        super.setDefaultSchemaName(DEFAULT_SCHEMA);
    }

    @Override
    public String getShortName() {
        return "databricks";
    }

    @Override
    public String getDefaultDatabaseProductName() {
        return PRODUCT_NAME;
    }

    @Override
    public Set<String> getSystemViews() {
        return systemTablesAndViews;
    }

    @Override
    public Integer getDefaultPort() {
        return 443;
    }

    @Override
    public int getPriority() {
        return DATABRICKS_PRIORITY;
    }

    @Override
    public String getCurrentDateTimeFunction() {
        return "current_timestamp()";
    }

    @Override
    public boolean isCorrectDatabaseImplementation(DatabaseConnection conn) throws DatabaseException {
        return PRODUCT_NAME.equalsIgnoreCase(conn.getDatabaseProductName());
    }

    @Override
    public String getDefaultDriver(String url) {
        if (url.startsWith("jdbc:databricks:")) {
            return "com.databricks.Driver";
        }
        return null;
    }

    @Override
    public String getDefaultCatalogName() {
        //must have UC enabled for this, will not play with hive_metastore
        return "main";
    }

    @Override
    public String getDefaultSchemaName() {
        return "default";
    }

    @Override
    public boolean supportsInitiallyDeferrableColumns() {
        return false;
    }

    @Override
    public boolean supportsDropTableCascadeConstraints() {
        return false;
    }

    @Override
    public boolean supportsCatalogs() {
        return true;
    }

    @Override
    public boolean supportsCatalogInObjectName(Class<? extends DatabaseObject> type) {
        return false;
    }

    @Override
    public boolean supportsSequences() {
        return true;
    }

    @Override
    public boolean supportsTablespaces() {
        return false;
    }

    @Override
    public boolean supportsAutoIncrement() {
        return true;
    }

    @Override
    protected String getAutoIncrementClause(final String generationType, final Boolean defaultOnNull) {

        if (StringUtil.isEmpty(generationType)) {
            return super.getAutoIncrementClause();
        }

        String autoIncrementClause = "GENERATED %s AS IDENTITY"; // %s -- [ ALWAYS | BY DEFAULT ]
        return String.format(autoIncrementClause, generationType);
    }

    @Override
    protected String getAutoIncrementStartWithClause() {
        return "%d";
    }

    @Override
    protected String getAutoIncrementByClause() {
        return "%d";
    }

    @Override
    public boolean generateAutoIncrementStartWith(BigInteger startWith) {
        return true;
    }

    @Override
    public boolean generateAutoIncrementBy(BigInteger incrementBy) {
        return true;
    }

    @Override
    public boolean supportsRestrictForeignKeys() {
        return true;
    }

    @Override
    protected SqlStatement getConnectionSchemaNameCallStatement() {
        return new RawCallStatement("select current_schema()");
    }


    private Set<String> getDatabricksReservedWords() {

        Set<String> reservedWords = new HashSet<>();
        // Get Reserved words from: https://docs.databricks.com/sql/language-manual/sql-ref-reserved-words.html
        reservedWords.addAll(Arrays.asList("ANTI",
                "CROSS",
                "EXCEPT",
                "FULL",
                "INNER",
                "INTERSECT",
                "JOIN",
                "LATERAL",
                "LEFT",
                "MINUS",
                "NATURAL",
                "ON",
                "RIGHT",
                "SEMI",
                "USING",
                "UNION",
                "NULL",
                "DEFAULT",
                "TRUE",
                "FALSE",
                "LATERAL",
                "BUILTIN",
                "SESSION",
                "INFORMATION_SCHEMA",
                "SYS",
                "ALL",
                "ALTER",
                "AND",
                "ANY",
                "ARRAY",
                "AS",
                "AT",
                "AUTHORIZATION",
                "BETWEEN", "BOTH", "BY",
                "CASE", "CAST", "CHECK", "COLLATE", "COLUMN", "COMMIT", "CONSTRAINT", "CREATE", "CROSS", "CUBE", "CURRENT", "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_USER",
                "DELETE", "DESCRIBE", "DISTINCT", "DROP",
                "ELSE", "END", "ESCAPE", "EXCEPT", "EXISTS", "EXTERNAL", "EXTRACT",
                "FETCH", "FILTER", "FOR", "FOREIGN", "FROM", "FULL", "FUNCTION",
                "GLOBAL", "GRANT", "GROUP", "GROUPING",
                "HAVING",
                "IN", "INNER", "INSERT", "INTERSECT", "INTERVAL", "INTO", "IS",
                "JOIN",
                "LEADING", "LEFT", "LIKE", "LOCAL",
                "NATURAL", "NO", "NOT", "NULL",
                "OF", "ON", "ONLY", "OR", "ORDER", "OUT", "OUTER", "OVERLAPS",
                "PARTITION", "POSITION", "PRIMARY",
                "RANGE", "REFERENCES", "REVOKE", "RIGHT", "ROLLBACK", "ROLLUP", "ROW", "ROWS",
                "SELECT", "SESSION_USER", "SET", "SOME", "START",
                "TABLE", "TABLESAMPLE", "THEN", "TIME", "TO", "TRAILING", "TRUE", "TRUNCATE",
                "UNION", "UNIQUE", "UNKNOWN", "UPDATE", "USER", "USING",
                "VALUES",
                "WHEN", "WHERE", "WINDOW", "WITH"
                ));

        return reservedWords;
    }
}