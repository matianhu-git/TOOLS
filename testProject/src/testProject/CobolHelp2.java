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
// * COBOL�t�@�C����`����͂��AExcel�Ƀf�[�^�𐶐����郆�[�e�B���e�B�N���X
// */
//public class CobolHelp2 {
//
//    /**
//     * Excel�t�@�C������͂��ACOBOL�̕ϐ��ɑΉ�����_�~�[�f�[�^�𐶐�
//     *
//     * @param excelPath Excel�t�@�C���̃p�X
//     * @param count     ��������f�[�^�����iC��J�n�j
//     */
//    public static void filepParamsCreat(String excelPath, int count) {
//        System.out.println(">>> COBOL��`�f�[�^���������J�n�F" + excelPath);
//
//        try (FileInputStream fis = new FileInputStream(excelPath);
//             Workbook workbook = new XSSFWorkbook(fis)) {
//
//            Sheet sheet = workbook.getSheetAt(0); // �ŏ��̃V�[�g
//
//            // Excel�̑S�s�����Ԃɏ���
//            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
//                Row row = sheet.getRow(i);
//                if (row == null) continue;
//
//                Cell aCell = row.getCell(0); // A��
//                if (aCell == null) continue;
//
//                String cobolLine = aCell.getStringCellValue().trim();
//                if (cobolLine.isEmpty() || cobolLine.contains("REDEFINES") || cobolLine.contains("RENAMES")) {
//                    // ��s�AREDEFINES�ARENAMES�̓X�L�b�v
//                    continue;
//                }
//
//                // PIC��𒊏o - ���P��
//                String picClause = extractPicClause(cobolLine);
//                if (picClause == null) continue;
//
//                CobolField field = parsePic(picClause);
//
//                // �ϐ����𒊏o�i���P�Łj
//                String varName = extractVariableNameImproved(cobolLine);
//                if (varName.isEmpty()) continue;
//
//                // B��: �ϐ���
//                Cell bCell = row.createCell(1, CellType.STRING);
//                bCell.setCellValue(varName);
//
//                // C��ȍ~: count���̃f�[�^�𐶐�
//                for (int j = 0; j < count; j++) {
//                    Cell dataCell = row.createCell(2 + j, CellType.STRING);
//
//                    // PIC�^�ɉ����Ēl�𐶐��i���P�Łj
//                    String value = generateFieldValue(field, j + 1);
//                    dataCell.setCellValue(value);
//                }
//            }
//
//            // �㏑���ۑ�
//            try (FileOutputStream fos = new FileOutputStream(excelPath)) {
//                workbook.write(fos);
//            }
//
//            System.out.println("COBOL�f�[�^���������F" + excelPath);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.out.println("�G���[�����F" + e.getMessage());
//        }
//    }
//
//    /**
//     * PIC��𐳊m�ɒ��o���郁�\�b�h
//     * "PIC 9(2) VALUE 20" �� "PIC 9(2)" �𒊏o
//     */
//    private static String extractPicClause(String cobolLine) {
//        // PIC����n�܂�AVALUE�⎟�̃L�[���[�h�̑O�܂ł𒊏o
//        Pattern picPattern = Pattern.compile("PIC\\s+S?[^\\s]+(\\s+[^\\s]+)*(?=\\s+(VALUE|REDEFINES|RENAMES|\\.|$))", Pattern.CASE_INSENSITIVE);
//        Matcher picMatcher = picPattern.matcher(cobolLine);
//        
//        if (picMatcher.find()) {
//            return picMatcher.group().trim();
//        }
//        
//        // ��֕��@: �P����PIC����s���I�h�܂ł̕����𒊏o
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
//     * ���P���ꂽ�ϐ������o���\�b�h
//     * COBOL�̊K�w�\�����l�����ĕϐ����𐳊m�ɒ��o
//     */
//    private static String extractVariableNameImproved(String cobolLine) {
//        // �s���g���~���O���ė]���ȃX�y�[�X������
//        cobolLine = cobolLine.trim();
//        
//        // ���x���ԍ��������i01, 05, 10, 15�Ȃǁj
//        String lineWithoutLevel = cobolLine.replaceAll("^\\d+\\s+", "");
//        
//        // �R�����g��s�v�ȕ���������
//        lineWithoutLevel = lineWithoutLevel.split("\\s+PIC\\s+")[0];
//        lineWithoutLevel = lineWithoutLevel.split("\\s+VALUE\\s+")[0];
//        lineWithoutLevel = lineWithoutLevel.split("\\s+REDEFINES\\s+")[0];
//        lineWithoutLevel = lineWithoutLevel.split("\\s+RENAMES\\s+")[0];
//        
//        // �����̃h�b�g������
//        lineWithoutLevel = lineWithoutLevel.replaceAll("\\.$", "");
//        
//        // �ϐ��������𒊏o�iWS-�Ŏn�܂镔���j
//        String[] parts = lineWithoutLevel.split("\\s+");
//        for (String part : parts) {
//            if (part.startsWith("WS-") && part.length() > 3) {
//                return part.toUpperCase();
//            }
//        }
//        
//        // WS-�Ŏn�܂�Ȃ��ꍇ�A�Ō�̗L���ȒP���ϐ����Ƃ���
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
//     * �����񂪐��l���ǂ����𔻒�
//     */
//    private static boolean isNumeric(String str) {
//        return str.matches("\\d+");
//    }
//
//    /**
//     * �t�B�[���h�̌^�Ɋ�Â��Ēl�𐶐�
//     */
//    private static String generateFieldValue(CobolField field, int index) {
//        if ("���l".equals(field.getDataType())) {
//            return generateNumericValue(field, index);
//        } else if ("������".equals(field.getDataType())) {
//            return generateStringValue(field, index);
//        } else {
//            return ""; // �s���^�͋�
//        }
//    }
//
//    /**
//     * ���l�^�̒l�𐶐��i�[�����ߑΉ��j
//     */
//    private static String generateNumericValue(CobolField field, int index) {
//        if (field.getDecimalDigits() > 0) {
//            // �����_����̐��l
//            int integerDigits = field.getTotalDigits() - field.getDecimalDigits();
//            String integerPart = String.format("%0" + integerDigits + "d", index);
//            
//            // ���������͌Œ�l�܂��̓C���f�b�N�X�Ɋ�Â�
//            int decimalValue = (index * 13) % (int)Math.pow(10, field.getDecimalDigits()); // 13�͓K���ȑf��
//            String decimalPart = String.format("%0" + field.getDecimalDigits() + "d", decimalValue);
//            
//            return integerPart + "." + decimalPart;
//        } else {
//            // �����̂�
//            return String.format("%0" + field.getTotalDigits() + "d", index);
//        }
//    }
//
//    /**
//     * ������^�̒l�𐶐�
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
//     * COBOL PIC�����͂��Č^�����擾
//     */
//    private static CobolField parsePic(String picClause) {
//        CobolField field = new CobolField();
//        String clause = picClause.toUpperCase().trim();
//        field.setOriginalPic(clause);
//
//        // �����t������
//        field.setSigned(clause.startsWith("PIC S"));
//
//        // COMP / COMP-3����
//        boolean isComp3 = clause.contains("COMP-3");
//        boolean isComp = clause.contains("COMP") && !isComp3;
//        field.setComp3(isComp3);
//        field.setComp(isComp);
//
//        // PIC���e���o - ���P��
//        // "PIC 9(2)" ���� "9(2)" �𒊏o
//        // �C��: ���ʓ��̐������܂߂Ċ��S�Ƀ}�b�`����悤�ɐ��K�\�������P
//        Matcher m = Pattern.compile("PIC\\s+S?([9XAZ\\*\\(\\)\\.V\\d]+(?:\\s+[9XAZ\\*\\(\\)\\.V\\d]+)*)").matcher(clause);
//        if (!m.find()) {
//            // �ʂ̕��@�Ŏ���: �P����PIC�ȍ~�𒊏o
//            String core = clause.replaceFirst("^PIC\\s+S?", "").trim();
//            field = parseCorePic(core, field);
//            return field;
//        }
//
//        String core = m.group(1).trim();
//        System.out.println("DEBUG: PIC�� '" + clause + "' -> �j�S���� '" + core + "'"); // �f�o�b�O�p
//        return parseCorePic(core, field);
//    }
//
//    /**
//     * PIC�̊j�S���������
//     */
//    private static CobolField parseCorePic(String core, CobolField field) {
//        // ����/���������v�Z
//        int totalDigits = 0;
//        int decimalDigits = 0;
//
//        if (core.contains("V")) {
//            // �����_����̏ꍇ: 9(3)V9(2) �Ȃ�
//            String[] parts = core.split("V");
//            totalDigits = extractDigitCountImproved(parts[0]);
//            decimalDigits = extractDigitCountImproved(parts[1]);
//        } else {
//            // �����_�Ȃ��̏ꍇ
//            totalDigits = extractDigitCountImproved(core);
//        }
//
//        field.setTotalDigits(totalDigits + decimalDigits);
//        field.setDecimalDigits(decimalDigits);
//
//        // �^����
//        if (core.contains("X") || core.contains("A") || core.contains("G") || core.contains("Z") || core.contains("*")) {
//            field.setDataType("������");
//            field.setLength(extractLengthImproved(core));
//        } else if (core.contains("9")) {
//            field.setDataType("���l");
//            field.setLength(totalDigits + decimalDigits);
//        } else {
//            field.setDataType("�s��");
//        }
//
//        System.out.println("DEBUG: �j�S '" + core + "' -> �^=" + field.getDataType() + 
//                          ", ����=" + field.getLength() + 
//                          ", ������=" + field.getTotalDigits() + 
//                          ", ������=" + field.getDecimalDigits()); // �f�o�b�O�p
//
//        return field;
//    }
//
//    /** 
//     * ���P���ꂽ�����������o���\�b�h
//     * 9(2) �� 999 �Ȃǂ̌`���𐳂�������
//     */
//    private static int extractDigitCountImproved(String part) {
//        int total = 0;
//        
//        // 9(2) �̌`��������
//        Matcher parenMatcher = Pattern.compile("9\\((\\d+)\\)").matcher(part);
//        while (parenMatcher.find()) {
//            total += Integer.parseInt(parenMatcher.group(1));
//        }
//        
//        // �P�Ƃ�9�������i���ʌ`���ł͂Ȃ����́j
//        String remaining = part.replaceAll("9\\(\\d+\\)", "");
//        int singleNines = remaining.replaceAll("[^9]", "").length();
//        total += singleNines;
//        
//        System.out.println("DEBUG: �������o '" + part + "' -> " + total + "��"); // �f�o�b�O�p
//        return total;
//    }
//
//    /** 
//     * ���P���ꂽ������^�������o
//     * X(2) �� XXX �Ȃǂ̌`���𐳂�������
//     */
//    private static int extractLengthImproved(String core) {
//        int len = 0;
//        
//        // X(2) �� A(3) �Ȃǂ̌`��������
//        Matcher parenMatcher = Pattern.compile("([AXZG\\*])\\((\\d+)\\)").matcher(core);
//        while (parenMatcher.find()) {
//            len += Integer.parseInt(parenMatcher.group(2));
//        }
//        
//        // �P�Ƃ̕����������i���ʌ`���ł͂Ȃ����́j
//        String remaining = core.replaceAll("[AXZG\\*]\\(\\d+\\)", "");
//        int singleChars = remaining.replaceAll("[^AXZG\\*]", "").length();
//        len += singleChars;
//        
//        System.out.println("DEBUG: �������o '" + core + "' -> " + len + "����"); // �f�o�b�O�p
//        return len;
//    }
//}
//
///** COBOL�t�B�[���h���N���X */
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