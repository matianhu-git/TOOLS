package testProject;

import java.io.IOException;
import java.util.Scanner;

import org.apache.poi.util.StringUtil;

public class test001 {
	private static String execlPath = null;
	private static String jobId = null;
	private static Scanner scanner = null;

	public static void main(String[] args) {
		scanner = new Scanner(System.in);
		while (true) {
			try {
				int choice = scanner.nextInt();
				scanner.nextLine();

				switch (choice) {
				case 1:
					registerData();
					break;
				case 2:
					dbfromData();
					break;
				case 3:
					dbToData();
					break;
				case 4:
//					SQL文のチェック
					sqlCheck();
					break;
				case 5:
//					SQL文の作成
					sqlCreat();
					break;
				case 6:
//					dbdefの作成
					dbdefCreat();
					break;
				case 7:
//					fileparamsの作成
					filepParamsCreat();
					break;
				case 0:
					scanner.close();
					System.exit(0);
				case 11:
					setJobId();
					break;
				case 22:
					setExeclPath();
					break;
				default:

				}
			} catch (Exception e) {
				scanner.nextLine();
			}
		}
	}

	private static void setJobId() {
		System.out.println("jobId:");
		String lintString = scanner.next();
		jobId = lintString;
		System.out.println("jobId:" + jobId);
	}

	private static void setExeclPath() {
		System.out.println("execlPath:");
		String lintString = scanner.next();
		execlPath = lintString;
		System.out.println("execlPath:" + execlPath);
	}

	private static void registerData() throws IOException {

		// "JS_OVHD32060_010"
		if (StringUtil.isNotBlank(jobId)) {
			System.out.println("\n>>> テストデータ作成処理を開始します");
			Tool tool = new Tool(jobId);
			System.out.println("count:");
			int count = scanner.nextInt();
			System.out.println("count:" + count);
			String tables = ReadFileHelp.readTableFile(jobId);

			String sqlString = tool.createTestData(count, tables);
			PostgresHelp.executeSQL(sqlString);
			System.out.println("【完了】テストデータ作成処理が正常に終了しました");
		} else {
			System.out.println("必要なパラメータが空白です");
		}

	}

	private static void dbfromData() {

		if (StringUtil.isNotBlank(jobId) && StringUtil.isNotBlank(execlPath)) {
			System.out.println("\n>>> データ出力処理を開始します");
			Tool tool = new Tool(jobId);
			String tables = ReadFileHelp.readTableFile(jobId);
			tool.dbToExcel(execlPath, tables);
			System.out.println("【完了】データ出力処理が正常に終了しました");
		} else {
			System.out.println("必要なパラメータが空白です");
		}

	}

	private static void dbToData() throws Exception {
		if (StringUtil.isNotBlank(execlPath)) {
			System.out.println("\n>>> データ入力処理を開始します");
			String sqlString = ExcelHelper.excelToDB(execlPath);
			PostgresHelp.executeSQL(sqlString);

			System.out.println("【完了】データ入力処理が正常に終了しました");
		}else {
			System.out.println("必要なパラメータが空白です");
		}
		
	}
	
	private static void sqlCheck() throws Exception {
		if (StringUtil.isNotBlank(jobId)) {
			System.out.println("\n>>> SQL文のチェック処理を開始します");
			Tool tool = new Tool(jobId);
			String sqlContent = tool.processSqlComments();
			System.out.println(sqlContent);
			System.out.println("【完了】SQL文のチェック処理が正常に終了しました");
		}else {
			System.out.println("必要なパラメータが空白です");
		}
		
	}
	
	private static void sqlCreat() throws Exception {
		if (StringUtil.isNotBlank(jobId)) {
			System.out.println("\n>>> SQL文の作成処理を開始します");
			Tool tool = new Tool(jobId);
			String sqlContent = tool.creatSqlComments();
			System.out.println(sqlContent);
			System.out.println("【完了】SQL文の作成処理が正常に終了しました");
		}else {
			System.out.println("必要なパラメータが空白です");
		}
	}

	private static void dbdefCreat() throws Exception {
		if (StringUtil.isNotBlank(jobId)) {
			System.out.println("\n>>> dbdefの作成処理を開始します");
			
			ReadFileHelp.dbdefCreat(jobId);
			System.out.println("【完了】dbdefの作成処理が正常に終了しました");
		}else {
			System.out.println("必要なパラメータが空白です");
		}
	}
	
	private static void filepParamsCreat() {
		if (StringUtil.isNotBlank(jobId)) {
			System.out.println("\n>>> Cobolファイル構成データ作成処理を開始します");
			
			System.out.println("count:");
			int count = scanner.nextInt();
			System.out.println("count:" + count);
			
			CobolHelp.filepParamsCreat(execlPath,count);
			System.out.println("【完了】Cobolファイル構成データ作成処理が正常に終了しました");
		} else {
			System.out.println("必要なパラメータが空白です");
		}
	}
}
