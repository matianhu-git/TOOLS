package testProject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

public class ReadFileHelp {
	
	private static Map<String, List<String>> tableDictionary = new HashMap<>();

	public static String readTableFile(String jobId) {
		String retString = "";
		String inputFilePath = "C:\\ICF_AutoCapsule_disabled\\SDE13\\defDEF\\" + jobId + "\\dbdef.txt";

		StringBuilder stringBuilder = new StringBuilder();
		try {
		    List<String> lines = Files.readAllLines(Paths.get(inputFilePath), Charset.defaultCharset());
		    for (String line : lines) {
		        stringBuilder.append(line).append(",");
		    }
		    retString = stringBuilder.toString() ;
		    retString= retString.substring(0, retString.length()-1);
		} catch (IOException e) {
		    e.printStackTrace();
		}
		return retString;
	}
	
	public static List<String> readSqlFile() {
		List<String> retStringList = new ArrayList<String>();
		String inputFilePath = "D:\\project\\testProject\\src\\files\\sqlInput.sql";

		try {
		    List<String> lines = Files.readAllLines(Paths.get(inputFilePath), Charset.defaultCharset());
		    for (String line : lines) {
		    	retStringList.add(line);
		    }
		} catch (IOException e) {
		    e.printStackTrace();
		}
		return retStringList;
	}

	public static void dbdefCreat(String jobId) {
	    try {
	        defTempleteLoad();

	        // 新しい DEF ファイルのパス
	        String outputFilePath = "C:\\ICF_AutoCapsule_disabled\\SDE13\\defDEF\\" 
	            + jobId + "\\dbdef_" + jobId + ".dft";

	        File outputFile = new File(outputFilePath);
	        if (outputFile.exists()) {
	            outputFile.delete();
	        }

	        // 保持する ID のリスト
	        String[] idsToKeep = readTableFile(jobId).split(",");

	        // ヘッダー行を保持
	        String header = "表定義ファイル,2024/09/27 16:00:49,Version=1.0.0";

	        // 新しい DEF ファイルを書き込む
	        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
	                new FileOutputStream(outputFilePath), Charset.forName("MS932")))) {
	            writer.write(header);
	            writer.newLine();

	            for (String item : idsToKeep) {
	                String key = tableDictionary.keySet().stream()
	                        .filter(x -> x.contains(item + ","))
	                        .findFirst()
	                        .orElse(null);

	                if (key != null && !key.isEmpty()) {
	                    List<String> filteredLines = tableDictionary.get(key);
	                    writer.write(key);
	                    writer.newLine();

	                    for (String line : filteredLines) {
	                        writer.write(line);
	                        writer.newLine();
	                    }
	                }
	            }
	        }

	        System.out.println("新しい .dft ファイルが保存されました: " + outputFilePath);

	    } catch (Exception ex) {
	        JOptionPane.showMessageDialog(null, ex.getMessage());
	    }
	}

	public static String detectCharsetFromBOM(File file) throws IOException {
	    try (FileInputStream is = new FileInputStream(file)) {
	        byte[] bom = new byte[4];
	        is.read(bom);

	        if ((bom[0] == (byte)0xEF && bom[1] == (byte)0xBB && bom[2] == (byte)0xBF)) {
	            return "UTF-8";
	        } else if ((bom[0] == (byte)0xFF && bom[1] == (byte)0xFE)) {
	            return "UTF-16LE";
	        } else if ((bom[0] == (byte)0xFE && bom[1] == (byte)0xFF)) {
	            return "UTF-16BE";
	        }
	    }
	    return "MS932"; // fallback 编码
	}

	private static void defTempleteLoad() {
	    tableDictionary.clear();

	    // 元の dft ファイルのパス
	    String inputFilePath = "C:\\ICF_AutoCapsule_disabled\\SDE13\\defDEF\\common\\dbdef.dft";

	    List<String> lines;
	    try {
	    	File file = new File(inputFilePath);
	    	String charsetName = detectCharsetFromBOM(file);
	    	System.out.println(charsetName);
	        lines = Files.readAllLines(Paths.get(inputFilePath), Charset.forName("UTF-8"));
	    } catch (IOException e) {
	        JOptionPane.showMessageDialog(null, "ファイル読み込みエラー: " + e.getMessage());
	        return;
	    }

	    List<String> lineList = new ArrayList<>();
	    String keyTmp = "";
	    int index = 0;

	    for (String line : lines) {
	        if (line.contains("表定義ファイル")) {
	            continue;
	        }

	        if (line.contains(":")) {
	            if (!tableDictionary.containsKey(line)) {
	                if (index == 0) {
	                    keyTmp = line;
	                    tableDictionary.put(keyTmp, null);
	                } else {
	                    tableDictionary.put(keyTmp, lineList);
	                    lineList = new ArrayList<>();
	                    keyTmp = line;
	                    tableDictionary.put(keyTmp, null);
	                }
	                index++;
	            }
	        } else {
	            lineList.add(line);
	        }
	    }

	    tableDictionary.put(keyTmp, lineList);
	}

}
