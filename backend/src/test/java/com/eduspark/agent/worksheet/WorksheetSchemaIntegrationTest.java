package com.eduspark.agent.worksheet;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class WorksheetSchemaIntegrationTest {

  @Autowired private DataSource dataSource;

  @Test
  void worksheetTablesExistAfterFlywayMigration() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      DatabaseMetaData metaData = connection.getMetaData();

      assertThat(hasTable(metaData, "edu_document_chunk")).isTrue();
      assertThat(hasTable(metaData, "edu_worksheet")).isTrue();
      assertThat(hasTable(metaData, "edu_worksheet_export")).isTrue();
    }
  }

  @Test
  void worksheetTablesHaveUtf8mb4MigrationForMysql() throws Exception {
    String migration =
        Files.readString(
            Path.of("src/main/resources/db/migration/V5__convert_worksheet_tables_to_utf8mb4.sql"));

    assertThat(migration).contains("edu_document_chunk convert to character set utf8mb4");
    assertThat(migration).contains("edu_worksheet convert to character set utf8mb4");
    assertThat(migration).contains("edu_worksheet_export convert to character set utf8mb4");
  }

  @Test
  void worksheetTableHasGenerationRationaleColumn() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      DatabaseMetaData metaData = connection.getMetaData();

      assertThat(hasColumn(metaData, "edu_worksheet", "generation_rationale_json")).isTrue();
    }
  }

  private boolean hasTable(DatabaseMetaData metaData, String tableName) throws Exception {
    try (ResultSet resultSet = metaData.getTables(null, null, tableName, null)) {
      return resultSet.next();
    }
  }

  private boolean hasColumn(DatabaseMetaData metaData, String tableName, String columnName)
      throws Exception {
    try (ResultSet resultSet = metaData.getColumns(null, null, tableName, columnName)) {
      return resultSet.next();
    }
  }
}
