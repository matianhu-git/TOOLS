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
        // Oracle JDBC �ڑ����
        String url = "jdbc:oracle:thin:@localhost:1521:ORCL"; // ���ۂ̐ڑ����ɕύX
        String user = "YOUR_USERNAME";
        String password = "YOUR_PASSWORD";

        // �e�[�u�������X�g�i���s����̓t�H�[������擾���Ă�OK�j
        String[] tableNames = {"EMPLOYEES", "DEPARTMENTS"};

        // �o�̓t�@�C���p�X
        String filePath = "dbdef.dft";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("�\��`�t�@�C��," + LocalDateTime.now() + ",Version = 1.0.0\n");

            try (Connection conn = DriverManager.getConnection(url, user, password)) {

                for (String tableName : tableNames) {

                    // === �@ �e�[�u���R�����g�擾 ===
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

                    // �e�[�u�����ƃR�����g���t�@�C���ɏo��
                    writer.write(String.format(":%s,%s,\n", tableName.toUpperCase(), tableComment));

                    // === �A �J������`�E�R�����g�擾 ===
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

                                // �J��������1�s�Ƃ��ďo��
                                String line = String.format("%s,%s,%s,%s",
                                        col0.toUpperCase(), col1, col2, col3);
                                writer.write(line + "\n");
                            }
                        }
                    }

                    writer.flush();
                }
            }

            System.out.println("�t�@�C�����쐬���܂���: " + filePath);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
