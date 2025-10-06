package testProject;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelHelper {

    public static String excelToDB(String filePath) throws Exception {
        StringBuilder sqlBuilder = new StringBuilder();
        String currentTableName = "";
        List<String> columnNames = new ArrayList<>();
        List<String> columnTypes = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(new File(filePath));
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet worksheet = workbook.getSheet("DataTemplate");
            if (worksheet == null) {
                throw new Exception("Worksheet 'DataTemplate' not found");
            }

            int rowCount = worksheet.getLastRowNum();

            for (int row = 0; row < rowCount; row++) {
                Row currentRow = worksheet.getRow(row);
                if (currentRow == null) continue;

                Cell firstCell = currentRow.getCell(0);
                String cellValue = firstCell != null ? firstCell.toString().trim().toLowerCase() : "";

                if (cellValue.equals("start")) {
                    // Do nothing for start
                } else if (cellValue.equals("end")) {
                    sqlBuilder.append(";\n");
                    break;
                } else {
                    Cell thirdCell = currentRow.getCell(2);
                    boolean tableNameFlag = (thirdCell == null || thirdCell.toString().trim().isEmpty());

                    if (tableNameFlag) {
                        Cell tableNameCell = currentRow.getCell(1);
                        if (tableNameCell == null || tableNameCell.toString().trim().isEmpty()) {
                            sqlBuilder.append(";\n");
                            continue;
                        }

                        String tableNameWithComment = tableNameCell.toString().trim();
                        currentTableName = extractTableName(tableNameWithComment);

                        // Get column names and types
                        columnNames = getTableColumns(currentTableName);
                        columnTypes = getColumnTypesFromDatabase(currentTableName, columnNames);

                        // Generate DELETE statement
                        String deleteSQL = generateDeleteSQL(currentTableName);
                        sqlBuilder.append(deleteSQL).append("\n");

                        // Prepare INSERT statement
                        sqlBuilder.append("INSERT INTO ").append(currentTableName)
                                  .append(" (").append(String.join(", ", columnNames)).append(") VALUES \n");

                        row += 2; // Skip next two rows
                    } else {
                        // Process data row
                        List<String> data = new ArrayList<>();
                        for (int col = 1; col <= columnNames.size(); col++) {
                            Cell cell = currentRow.getCell(col);
                            String value = (cell != null) ? cell.toString() : "";

                            String type = (col-1 < columnTypes.size()) ? columnTypes.get(col-1) : "unknown";
                            if (value.equalsIgnoreCase("NULL")) {
                                data.add("NULL");
                            } else {
                                switch (type) {
                                    case "numeric":
                                        data.add(value);
                                        break;
                                    default:
                                        data.add("'" + value + "'");
                                        break;
                                }
                            }
                        }

                        sqlBuilder.append("(").append(String.join(", ", data)).append(")");

                        // Check if next row has data
                        Row nextRow = worksheet.getRow(row + 1);
                        if (nextRow != null && nextRow.getCell(1) != null 
                            && !nextRow.getCell(1).toString().trim().isEmpty()) {
                            sqlBuilder.append(",\n");
                        } else {
                            sqlBuilder.append(";\n");
                        }
                    }
                }
            }
        }

        return sqlBuilder.toString();
    }

    private static List<String> getTableColumns(String tableName) throws SQLException {
        List<String> columns = new ArrayList<>();
        String query = "SELECT column_name FROM information_schema.columns " +
                       "WHERE table_schema = 'public' AND table_name = ? " +
                       "ORDER BY ordinal_position";

        try (Connection conn = PostgresHelp.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, tableName.toLowerCase());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                columns.add(rs.getString(1));
            }
        }
        return columns;
    }

    private static String extractTableName(String tableNameWithComment) {
        int startIndex = tableNameWithComment.indexOf('(');
        int endIndex = tableNameWithComment.indexOf(')');
        if (startIndex != -1 && endIndex != -1) {
            return tableNameWithComment.substring(startIndex + 1, endIndex);
        }
        return tableNameWithComment;
    }

    private static List<String> getColumnTypesFromDatabase(String tableName, List<String> columnNames) 
            throws SQLException {
        List<String> columnTypes = new ArrayList<>();
        String query = "SELECT column_name, data_type FROM information_schema.columns " +
                       "WHERE table_name = ? AND column_name = ?";

        try (Connection conn = PostgresHelp.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            for (String column : columnNames) {
                stmt.setString(1, tableName);
                stmt.setString(2, column);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    columnTypes.add(rs.getString("data_type"));
                } else {
                    columnTypes.add("unknown");
                }
            }
        }
        return columnTypes;
    }

    private static String generateDeleteSQL(String tableName) {
        return "DELETE FROM " + tableName + ";";
    }
}