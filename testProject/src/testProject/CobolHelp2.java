//package testProject;
//
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//import org.apache.poi.ss.usermodel.Cell;
//import org.apache.poi.ss.usermodel.CellType;
//import org.apache.poi.ss.usermodel.Row;
//import org.apache.poi.ss.usermodel.Sheet;
//import org.apache.poi.ss.usermodel.Workbook;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;
//
///**
// * COBOLファイル定義を解析し、Excelにデータを生成するユーティリティクラス
// */
//public class CobolHelp2 {
//
//    /**
//     * Excelファイルを解析し、COBOLの変数に対応するダミーデータを生成
//     *
//     * @param excelPath Excelファイルのパス
//     * @param count     生成するデータ件数（C列開始）
//     */
//    public static void filepParamsCreat(String excelPath, int count) {
//        System.out.println(">>> COBOL定義データ生成処理開始：" + excelPath);
//
//        try (FileInputStream fis = new FileInputStream(excelPath);
//             Workbook workbook = new XSSFWorkbook(fis)) {
//
//            Sheet sheet = workbook.getSheetAt(0); // 最初のシート
//
//            // Excelの全行を順番に処理
//            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
//                Row row = sheet.getRow(i);
//                if (row == null) continue;
//
//                Cell aCell = row.getCell(0); // A列
//                if (aCell == null) continue;
//
//                String cobolLine = aCell.getStringCellValue().trim();
//                if (cobolLine.isEmpty() || cobolLine.contains("REDEFINES") || cobolLine.contains("RENAMES")) {
//                    // 空行、REDEFINES、RENAMESはスキップ
//                    continue;
//                }
//
//                // PIC句を抽出 - 改善版
//                String picClause = extractPicClause(cobolLine);
//                if (picClause == null) continue;
//
//                CobolField field = parsePic(picClause);
//
//                // 変数名を抽出（改善版）
//                String varName = extractVariableNameImproved(cobolLine);
//                if (varName.isEmpty()) continue;
//
//                // B列: 変数名
//                Cell bCell = row.createCell(1, CellType.STRING);
//                bCell.setCellValue(varName);
//
//                // C列以降: count件のデータを生成
//                for (int j = 0; j < count; j++) {
//                    Cell dataCell = row.createCell(2 + j, CellType.STRING);
//
//                    // PIC型に応じて値を生成（改善版）
//                    String value = generateFieldValue(field, j + 1);
//                    dataCell.setCellValue(value);
//                }
//            }
//
//            // 上書き保存
//            try (FileOutputStream fos = new FileOutputStream(excelPath)) {
//                workbook.write(fos);
//            }
//
//            System.out.println("COBOLデータ生成完了：" + excelPath);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.out.println("エラー発生：" + e.getMessage());
//        }
//    }
//
//    /**
//     * PIC句を正確に抽出するメソッド
//     * "PIC 9(2) VALUE 20" → "PIC 9(2)" を抽出
//     */
//    private static String extractPicClause(String cobolLine) {
//        // PICから始まり、VALUEや次のキーワードの前までを抽出
//        Pattern picPattern = Pattern.compile("PIC\\s+S?[^\\s]+(\\s+[^\\s]+)*(?=\\s+(VALUE|REDEFINES|RENAMES|\\.|$))", Pattern.CASE_INSENSITIVE);
//        Matcher picMatcher = picPattern.matcher(cobolLine);
//        
//        if (picMatcher.find()) {
//            return picMatcher.group().trim();
//        }
//        
//        // 代替方法: 単純にPICからピリオドまでの部分を抽出
//        Pattern altPattern = Pattern.compile("PIC\\s+[^\\.]+");
//        Matcher altMatcher = altPattern.matcher(cobolLine.toUpperCase());
//        if (altMatcher.find()) {
//            return altMatcher.group().trim();
//        }
//        
//        return null;
//    }
//
//    /**
//     * 改善された変数名抽出メソッド
//     * COBOLの階層構造を考慮して変数名を正確に抽出
//     */
//    private static String extractVariableNameImproved(String cobolLine) {
//        // 行をトリミングして余分なスペースを除去
//        cobolLine = cobolLine.trim();
//        
//        // レベル番号を除去（01, 05, 10, 15など）
//        String lineWithoutLevel = cobolLine.replaceAll("^\\d+\\s+", "");
//        
//        // コメントや不要な部分を除去
//        lineWithoutLevel = lineWithoutLevel.split("\\s+PIC\\s+")[0];
//        lineWithoutLevel = lineWithoutLevel.split("\\s+VALUE\\s+")[0];
//        lineWithoutLevel = lineWithoutLevel.split("\\s+REDEFINES\\s+")[0];
//        lineWithoutLevel = lineWithoutLevel.split("\\s+RENAMES\\s+")[0];
//        
//        // 末尾のドットを除去
//        lineWithoutLevel = lineWithoutLevel.replaceAll("\\.$", "");
//        
//        // 変数名部分を抽出（WS-で始まる部分）
//        String[] parts = lineWithoutLevel.split("\\s+");
//        for (String part : parts) {
//            if (part.startsWith("WS-") && part.length() > 3) {
//                return part.toUpperCase();
//            }
//        }
//        
//        // WS-で始まらない場合、最後の有効な単語を変数名とする
//        for (int i = parts.length - 1; i >= 0; i--) {
//            if (!parts[i].isEmpty() && 
//                !parts[i].equals("PIC") && 
//                !parts[i].equals("VALUE") &&
//                !isNumeric(parts[i])) {
//                return parts[i].toUpperCase();
//            }
//        }
//        
//        return "";
//    }
//
//    /**
//     * 文字列が数値かどうかを判定
//     */
//    private static boolean isNumeric(String str) {
//        return str.matches("\\d+");
//    }
//
//    /**
//     * フィールドの型に基づいて値を生成
//     */
//    private static String generateFieldValue(CobolField field, int index) {
//        if ("数値".equals(field.getDataType())) {
//            return generateNumericValue(field, index);
//        } else if ("文字列".equals(field.getDataType())) {
//            return generateStringValue(field, index);
//        } else {
//            return ""; // 不明型は空白
//        }
//    }
//
//    /**
//     * 数値型の値を生成（ゼロ埋め対応）
//     */
//    private static String generateNumericValue(CobolField field, int index) {
//        if (field.getDecimalDigits() > 0) {
//            // 小数点ありの数値
//            int integerDigits = field.getTotalDigits() - field.getDecimalDigits();
//            String integerPart = String.format("%0" + integerDigits + "d", index);
//            
//            // 小数部分は固定値またはインデックスに基づく
//            int decimalValue = (index * 13) % (int)Math.pow(10, field.getDecimalDigits()); // 13は適当な素数
//            String decimalPart = String.format("%0" + field.getDecimalDigits() + "d", decimalValue);
//            
//            return integerPart + "." + decimalPart;
//        } else {
//            // 整数のみ
//            return String.format("%0" + field.getTotalDigits() + "d", index);
//        }
//    }
//
//    /**
//     * 文字列型の値を生成
//     */
//    private static String generateStringValue(CobolField field, int index) {
//        StringBuilder sb = new StringBuilder();
//        for (int i = 0; i < field.getLength(); i++) {
//            char c = (char) ('A' + ((index - 1 + i) % 26));
//            sb.append(c);
//        }
//        return sb.toString();
//    }
//
//    /**
//     * COBOL PIC句を解析して型情報を取得
//     */
//    private static CobolField parsePic(String picClause) {
//        CobolField field = new CobolField();
//        String clause = picClause.toUpperCase().trim();
//        field.setOriginalPic(clause);
//
//        // 符号付き判定
//        field.setSigned(clause.startsWith("PIC S"));
//
//        // COMP / COMP-3判定
//        boolean isComp3 = clause.contains("COMP-3");
//        boolean isComp = clause.contains("COMP") && !isComp3;
//        field.setComp3(isComp3);
//        field.setComp(isComp);
//
//        // PIC内容抽出 - 改善版
//        // "PIC 9(2)" から "9(2)" を抽出
//        // 修正: 括弧内の数字も含めて完全にマッチするように正規表現を改善
//        Matcher m = Pattern.compile("PIC\\s+S?([9XAZ\\*\\(\\)\\.V\\d]+(?:\\s+[9XAZ\\*\\(\\)\\.V\\d]+)*)").matcher(clause);
//        if (!m.find()) {
//            // 別の方法で試す: 単純にPIC以降を抽出
//            String core = clause.replaceFirst("^PIC\\s+S?", "").trim();
//            field = parseCorePic(core, field);
//            return field;
//        }
//
//        String core = m.group(1).trim();
//        System.out.println("DEBUG: PIC句 '" + clause + "' -> 核心部分 '" + core + "'"); // デバッグ用
//        return parseCorePic(core, field);
//    }
//
//    /**
//     * PICの核心部分を解析
//     */
//    private static CobolField parseCorePic(String core, CobolField field) {
//        // 整数/小数桁数計算
//        int totalDigits = 0;
//        int decimalDigits = 0;
//
//        if (core.contains("V")) {
//            // 小数点ありの場合: 9(3)V9(2) など
//            String[] parts = core.split("V");
//            totalDigits = extractDigitCountImproved(parts[0]);
//            decimalDigits = extractDigitCountImproved(parts[1]);
//        } else {
//            // 小数点なしの場合
//            totalDigits = extractDigitCountImproved(core);
//        }
//
//        field.setTotalDigits(totalDigits + decimalDigits);
//        field.setDecimalDigits(decimalDigits);
//
//        // 型分類
//        if (core.contains("X") || core.contains("A") || core.contains("G") || core.contains("Z") || core.contains("*")) {
//            field.setDataType("文字列");
//            field.setLength(extractLengthImproved(core));
//        } else if (core.contains("9")) {
//            field.setDataType("数値");
//            field.setLength(totalDigits + decimalDigits);
//        } else {
//            field.setDataType("不明");
//        }
//
//        System.out.println("DEBUG: 核心 '" + core + "' -> 型=" + field.getDataType() + 
//                          ", 長さ=" + field.getLength() + 
//                          ", 総桁数=" + field.getTotalDigits() + 
//                          ", 小数桁=" + field.getDecimalDigits()); // デバッグ用
//
//        return field;
//    }
//
//    /** 
//     * 改善された数字桁数抽出メソッド
//     * 9(2) や 999 などの形式を正しく処理
//     */
//    private static int extractDigitCountImproved(String part) {
//        int total = 0;
//        
//        // 9(2) の形式を処理
//        Matcher parenMatcher = Pattern.compile("9\\((\\d+)\\)").matcher(part);
//        while (parenMatcher.find()) {
//            total += Integer.parseInt(parenMatcher.group(1));
//        }
//        
//        // 単独の9を処理（括弧形式ではないもの）
//        String remaining = part.replaceAll("9\\(\\d+\\)", "");
//        int singleNines = remaining.replaceAll("[^9]", "").length();
//        total += singleNines;
//        
//        System.out.println("DEBUG: 桁数抽出 '" + part + "' -> " + total + "桁"); // デバッグ用
//        return total;
//    }
//
//    /** 
//     * 改善された文字列型長さ抽出
//     * X(2) や XXX などの形式を正しく処理
//     */
//    private static int extractLengthImproved(String core) {
//        int len = 0;
//        
//        // X(2) や A(3) などの形式を処理
//        Matcher parenMatcher = Pattern.compile("([AXZG\\*])\\((\\d+)\\)").matcher(core);
//        while (parenMatcher.find()) {
//            len += Integer.parseInt(parenMatcher.group(2));
//        }
//        
//        // 単独の文字を処理（括弧形式ではないもの）
//        String remaining = core.replaceAll("[AXZG\\*]\\(\\d+\\)", "");
//        int singleChars = remaining.replaceAll("[^AXZG\\*]", "").length();
//        len += singleChars;
//        
//        System.out.println("DEBUG: 長さ抽出 '" + core + "' -> " + len + "文字"); // デバッグ用
//        return len;
//    }
//}
//
///** COBOLフィールド情報クラス */
//class CobolField {
//    private String originalPic;
//    private String dataType;
//    private int length;
//    private int totalDigits;
//    private int decimalDigits;
//    private boolean signed;
//    private boolean comp;
//    private boolean comp3;
//    private int byteLength;
//
//    public String getOriginalPic() { return originalPic; }
//    public void setOriginalPic(String s) { this.originalPic = s; }
//    public String getDataType() { return dataType; }
//    public void setDataType(String s) { this.dataType = s; }
//    public int getLength() { return length; }
//    public void setLength(int i) { this.length = i; }
//    public int getTotalDigits() { return totalDigits; }
//    public void setTotalDigits(int i) { this.totalDigits = i; }
//    public int getDecimalDigits() { return decimalDigits; }
//    public void setDecimalDigits(int i) { this.decimalDigits = i; }
//    public boolean isSigned() { return signed; }
//    public void setSigned(boolean b) { this.signed = b; }
//    public boolean isComp() { return comp; }
//    public void setComp(boolean b) { this.comp = b; }
//    public boolean isComp3() { return comp3; }
//    public void setComp3(boolean b) { this.comp3 = b; }
//    public int getByteLength() { return byteLength; }
//    public void setByteLength(int i) { this.byteLength = i; }
//}