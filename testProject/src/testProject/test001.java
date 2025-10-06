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
//					SQL���̃`�F�b�N
					sqlCheck();
					break;
				case 5:
//					SQL���̍쐬
					sqlCreat();
					break;
				case 6:
//					dbdef�̍쐬
					dbdefCreat();
					break;
				case 7:
//					fileparams�̍쐬
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
			System.out.println("\n>>> �e�X�g�f�[�^�쐬�������J�n���܂�");
			Tool tool = new Tool(jobId);
			System.out.println("count:");
			int count = scanner.nextInt();
			System.out.println("count:" + count);
			String tables = ReadFileHelp.readTableFile(jobId);

			String sqlString = tool.createTestData(count, tables);
			PostgresHelp.executeSQL(sqlString);
			System.out.println("�y�����z�e�X�g�f�[�^�쐬����������ɏI�����܂���");
		} else {
			System.out.println("�K�v�ȃp�����[�^���󔒂ł�");
		}

	}

	private static void dbfromData() {

		if (StringUtil.isNotBlank(jobId) && StringUtil.isNotBlank(execlPath)) {
			System.out.println("\n>>> �f�[�^�o�͏������J�n���܂�");
			Tool tool = new Tool(jobId);
			String tables = ReadFileHelp.readTableFile(jobId);
			tool.dbToExcel(execlPath, tables);
			System.out.println("�y�����z�f�[�^�o�͏���������ɏI�����܂���");
		} else {
			System.out.println("�K�v�ȃp�����[�^���󔒂ł�");
		}

	}

	private static void dbToData() throws Exception {
		if (StringUtil.isNotBlank(execlPath)) {
			System.out.println("\n>>> �f�[�^���͏������J�n���܂�");
			String sqlString = ExcelHelper.excelToDB(execlPath);
			PostgresHelp.executeSQL(sqlString);

			System.out.println("�y�����z�f�[�^���͏���������ɏI�����܂���");
		}else {
			System.out.println("�K�v�ȃp�����[�^���󔒂ł�");
		}
		
	}
	
	private static void sqlCheck() throws Exception {
		if (StringUtil.isNotBlank(jobId)) {
			System.out.println("\n>>> SQL���̃`�F�b�N�������J�n���܂�");
			Tool tool = new Tool(jobId);
			String sqlContent = tool.processSqlComments();
			System.out.println(sqlContent);
			System.out.println("�y�����zSQL���̃`�F�b�N����������ɏI�����܂���");
		}else {
			System.out.println("�K�v�ȃp�����[�^���󔒂ł�");
		}
		
	}
	
	private static void sqlCreat() throws Exception {
		if (StringUtil.isNotBlank(jobId)) {
			System.out.println("\n>>> SQL���̍쐬�������J�n���܂�");
			Tool tool = new Tool(jobId);
			String sqlContent = tool.creatSqlComments();
			System.out.println(sqlContent);
			System.out.println("�y�����zSQL���̍쐬����������ɏI�����܂���");
		}else {
			System.out.println("�K�v�ȃp�����[�^���󔒂ł�");
		}
	}

	private static void dbdefCreat() throws Exception {
		if (StringUtil.isNotBlank(jobId)) {
			System.out.println("\n>>> dbdef�̍쐬�������J�n���܂�");
			
			ReadFileHelp.dbdefCreat(jobId);
			System.out.println("�y�����zdbdef�̍쐬����������ɏI�����܂���");
		}else {
			System.out.println("�K�v�ȃp�����[�^���󔒂ł�");
		}
	}
	
	private static void filepParamsCreat() {
		if (StringUtil.isNotBlank(jobId)) {
			System.out.println("\n>>> Cobol�t�@�C���\���f�[�^�쐬�������J�n���܂�");
			
			System.out.println("count:");
			int count = scanner.nextInt();
			System.out.println("count:" + count);
			
			CobolHelp.filepParamsCreat(execlPath,count);
			System.out.println("�y�����zCobol�t�@�C���\���f�[�^�쐬����������ɏI�����܂���");
		} else {
			System.out.println("�K�v�ȃp�����[�^���󔒂ł�");
		}
	}
}
