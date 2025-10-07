package testProject;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;

public class TableDefFileGenerator {

    public static void tabletToDefFile() {
        // Oracle JDBC 接続情報
        String url = "jdbc:oracle:thin:@localhost:1521:ORCL"; // 実際の接続情報に変更
        String user = "YOUR_USERNAME";
        String password = "YOUR_PASSWORD";

        // テーブル名リスト（改行や入力フォームから取得してもOK）
        String[] tableNames = {"EMPLOYEES", "DEPARTMENTS"};

        // 出力ファイルパス
        String filePath = "dbdef.dft";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("表定義ファイル," + LocalDateTime.now() + ",Version = 1.0.0\n");

            try (Connection conn = DriverManager.getConnection(url, user, password)) {

                for (String tableName : tableNames) {

                    // === ① テーブルコメント取得 ===
                    String tableComment = "";
                    String commentQuery1 =
                        "SELECT COMMENTS "
                      + "FROM USER_TAB_COMMENTS "
                      + "WHERE TABLE_NAME = ?";

                    try (PreparedStatement ps = conn.prepareStatement(commentQuery1)) {
                        ps.setString(1, tableName.toUpperCase());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                tableComment = rs.getString("COMMENTS");
                                if (tableComment == null) tableComment = "";
                            }
                        }
                    }

                    // テーブル名とコメントをファイルに出力
                    writer.write(String.format(":%s,%s,\n", tableName.toUpperCase(), tableComment));

                    // === ② カラム定義・コメント取得 ===
                    String commentQuery2 =
                        "SELECT "
                      + "  c.COLUMN_NAME, "
                      + "  (SELECT COMMENTS FROM USER_COL_COMMENTS "
                      + "   WHERE TABLE_NAME = c.TABLE_NAME "
                      + "     AND COLUMN_NAME = c.COLUMN_NAME) AS COLUMN_COMMENT, "
                      + "  c.DATA_TYPE, "
                      + "  CASE "
                      + "    WHEN c.DATA_TYPE LIKE 'VARCHAR%' THEN TO_CHAR(c.DATA_LENGTH) "
                      + "    WHEN c.DATA_TYPE LIKE 'NUMBER%' THEN "
                      + "         CASE WHEN c.DATA_PRECISION IS NOT NULL "
                      + "              THEN TO_CHAR(c.DATA_PRECISION) || '.' || NVL(c.DATA_SCALE, 0) "
                      + "              ELSE NULL END "
                      + "    ELSE NULL "
                      + "  END AS LENGTH "
                      + "FROM USER_TAB_COLUMNS c "
                      + "WHERE c.TABLE_NAME = ? "
                      + "ORDER BY c.COLUMN_ID";

                    try (PreparedStatement ps = conn.prepareStatement(commentQuery2)) {
                        ps.setString(1, tableName.toUpperCase());
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                String col0 = rs.getString("COLUMN_NAME");
                                String col1 = rs.getString("COLUMN_COMMENT");
                                String col2 = rs.getString("DATA_TYPE");
                                String col3 = rs.getString("LENGTH");

                                if (col1 == null) col1 = "";
                                if (col3 == null) col3 = "";

                                // カラム情報を1行として出力
                                String line = String.format("%s,%s,%s,%s",
                                        col0.toUpperCase(), col1, col2, col3);
                                writer.write(line + "\n");
                            }
                        }
                    }

                    writer.flush();
                }
            }

            System.out.println("ファイルを作成しました: " + filePath);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
