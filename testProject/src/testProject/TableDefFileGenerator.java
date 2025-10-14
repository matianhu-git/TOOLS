package testProject;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class TableDefFileGenerator {
	
	// Oracle データベース接続情報（自分の環境に合わせて変更してください）
	private static String url = "jdbc:oracle:thin:@localhost:1521:XE";  // XE は Oracle Express Edition の例
	private static String user = "HR";                                 // ユーザー名（スキーマ名）
	private static String password = "your_password";                  // パスワード
	
	 // 「テーブル検索」ボタンが押された時の処理
    private static String onSearchTableNames() {
        
        // Oracle用SQL
        //   USER_TABLES：現在ログインしているユーザーのテーブル一覧
        //   ALL_TABLES ：指定したスキーマのテーブル一覧（owner指定が必要）
        String sql =
            "SELECT table_name " +
            "FROM all_tables " +
            "WHERE owner = ?";

        StringBuilder sb = new StringBuilder();

        // try-with-resources：自動でクローズされる
        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // スキーマ名を指定（Oracleでは通常大文字）
            stmt.setString(1, user.toUpperCase());

            // SQLを実行し、結果を取得
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // テーブル名を1行ずつ追加
                    sb.append(rs.getString("table_name")).append(",");
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        
        return sb.toString();
    }
    
    public static void tabletToDefFile() {

        // テーブル名リスト（改行や入力フォームから取得してもOK）
        String[] tableNames = onSearchTableNames().split(",");

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
