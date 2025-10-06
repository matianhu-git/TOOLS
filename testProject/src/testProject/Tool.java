package testProject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class Tool {

	private String JobId;

	public Tool(String jobId) {
		this.JobId = jobId;
	}

	private Map<String, String> tableNameDictionary = new HashMap<>();
	private Map<String, String> columnNameDictionary = new HashMap<>();

	public void dbToExcel(String txtFilePass, String tids) {
		try {
			String outputPath = txtFilePass;
			String[] tableNames = tids.split(",");

			// Create workbook
			Workbook workbook = new XSSFWorkbook();
			int sheetIndex = workbook.getSheetIndex("DataTemplate");
			if (sheetIndex != -1) {
				workbook.removeSheetAt(sheetIndex);
			}
			Sheet worksheet = workbook.createSheet("DataTemplate");

			// Database connection
			try (Connection conn = PostgresHelp.getConnection()) {
				int rowNum = 0; // Current row number (0-based in POI)

				// Write "start" to first row
				Row row = worksheet.createRow(rowNum++);
				row.createCell(0).setCellValue("start");

				for (String table : tableNames) {
					String tableName = table.toLowerCase();

					// Get table comment
					String tableComment = "";
					String commentQuery = "SELECT obj_description(?::regclass) AS description;";
					try (PreparedStatement commentStmt = conn.prepareStatement(commentQuery)) {
						commentStmt.setString(1, tableName);
						try (ResultSet rs = commentStmt.executeQuery()) {
							if (rs.next()) {
								String result = rs.getString("description");
								if (result != null) {
									String[] parts = result.split("\\r?\\n");
									tableComment = parts[0];
								}
							}
						}
					}

					// Write table comment and name
					row = worksheet.createRow(rowNum++);
					row.createCell(1).setCellValue(tableComment + "(" + tableName + ")");

					// Get column names and comments
					String columnQuery = "SELECT a.attname AS column_name, "
							+ "(string_to_array(REPLACE(REPLACE(pg_catalog.col_description(c.oid, a.attnum), E'\\r\\n', E'\\n'), E'\\r', E'\\n'), E'\\n'))[1] AS column_comment "
							+ "FROM pg_catalog.pg_attribute a "
							+ "INNER JOIN pg_catalog.pg_class c ON c.oid = a.attrelid "
							+ "INNER JOIN information_schema.columns d ON a.attname = d.column_name AND c.relname = d.table_name "
							+ "WHERE c.relname = ? ORDER BY ordinal_position";

					List<String> columnNames = new ArrayList<>();
					List<String> columnComments = new ArrayList<>();
					boolean isOrder = false;

					try (PreparedStatement colStmt = conn.prepareStatement(columnQuery)) {
						colStmt.setString(1, tableName);
						try (ResultSet colRs = colStmt.executeQuery()) {
							while (colRs.next()) {
								String columnName = colRs.getString("column_name");
								String columnComment = colRs.getString("column_comment");
								if (columnComment == null)
									columnComment = "";

								columnNames.add(columnName);
								columnComments.add(columnComment);

								if ("coop_code".equals(columnName)) {
									isOrder = true;
								}
							}
						}
					}

					// Write column comments row
					row = worksheet.createRow(rowNum++);
					for (int i = 0; i < columnComments.size(); i++) {
						Cell cell = row.createCell(1 + i);
						cell.setCellValue(columnComments.get(i));
						setCellStyle(cell, workbook, true);
					}

					// Write column names row
					row = worksheet.createRow(rowNum++);
					for (int i = 0; i < columnNames.size(); i++) {
						Cell cell = row.createCell(1 + i);
						cell.setCellValue(columnNames.get(i));
						setCellStyle(cell, workbook, true);
					}

					// Get and write data rows
					String dataQuery = "SELECT * FROM " + tableName;
					if (isOrder) {
						dataQuery += " ORDER BY coop_code";
					}

					try (Statement dataStmt = conn.createStatement();
							ResultSet dataRs = dataStmt.executeQuery(dataQuery)) {

						ResultSetMetaData metaData = dataRs.getMetaData();
						int columnCount = metaData.getColumnCount();

						while (dataRs.next()) {
							row = worksheet.createRow(rowNum++);
							for (int i = 1; i <= columnCount; i++) {
								Object value = dataRs.getObject(i);
								Cell cell = row.createCell(i); // +1 because we start from column 2
								if (value != null) {
									cell.setCellValue(value.toString());
								}
								setCellStyle(cell, workbook, false);
							}
						}
					}

					// Add empty row
					rowNum++;
				}

				// Write "end" to last row
				row = worksheet.createRow(rowNum);
				row.createCell(0).setCellValue("end");

				// Save workbook
				try (FileOutputStream outputStream = new FileOutputStream(outputPath)) {
					workbook.write(outputStream);
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			// Show error message (implementation depends on your UI framework)
			// JOptionPane.showMessageDialog(null, ex.getMessage(), "Error",
			// JOptionPane.ERROR_MESSAGE);
		}
	}

	private void setCellStyle(Cell cell, Workbook workbook, boolean isBcolor) {
		CellStyle style = workbook.createCellStyle();

		// Set border
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);

		if (isBcolor) {
			// Set background color
			style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			style.setFillForegroundColor(new XSSFColor(new java.awt.Color(135, 231, 173), null));

			// Set font bold
			Font font = workbook.createFont();
			font.setBold(true);
			style.setFont(font);
		}

		// Set text format
		DataFormat format = workbook.createDataFormat();
		style.setDataFormat(format.getFormat("@"));

		cell.setCellStyle(style);
	}

	private void defCurrentLoad(String txtJobId, int index) {
		tableNameDictionary.clear();
		columnNameDictionary.clear();

		// Original DFT file path
		String inputFilePath = "C:\\ICF_AutoCapsule_disabled\\SDE13\\defDEF\\" + txtJobId + "\\dbdef_" + txtJobId
				+ ".dft";

		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(inputFilePath), Charset.defaultCharset()))) {

			String line;
			while ((line = reader.readLine()) != null) {
				if (line.contains("表定義ファイル")) {
					continue;
				}

				if (line.indexOf(":") > -1) {
					// Handle table definitions
					String[] parts = line.replace(":", "").split(",");
					String tableNameKey = parts[0 + index];
					String tableNameValue = parts.length > 1 ? parts[1 - index] : "";

					if (!tableNameDictionary.containsKey(tableNameKey)) {
						tableNameDictionary.put(tableNameKey, tableNameValue);
					}
				} else {
					// Handle column definitions
					String[] parts = line.split(",");
					String columnNameKey = parts[0 + index];
					String columnNameValue = parts.length > 1 ? parts[1 - index] : "";

					if (!columnNameDictionary.containsKey(columnNameKey)) {
						columnNameDictionary.put(columnNameKey, columnNameValue);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			// Handle file reading error appropriately
		}
	}

	public String createTestData(int count, String tids) {

		StringBuilder sb = new StringBuilder();

		try {
			defCurrentLoad(this.JobId, 0);

			String[] tableNames = tids.split(",");

			for (String item : tableNames) {
				if (item.trim().isEmpty())
					continue;

				String tableName = item.toLowerCase();
				String delete = String.format("DELETE FROM %s;", tableName);

				String insertInto = String.format("INSERT INTO %s(", tableName);
				String values1 = "VALUES (";
				String values2 = " (";
				String valueTemp = "";

				Map<Integer, String> valuesDic = new HashMap<>();
				for (int i = 1; i < count; i++) {
					valuesDic.put(i, values2);
				}

				// Create SQL query
				String sql = String.format(
						"SELECT column_name, data_type, character_maximum_length, numeric_precision, numeric_scale "
								+ "FROM information_schema.columns "
								+ "WHERE table_schema = 'public' AND table_name = '%s' " + "ORDER BY ordinal_position;",
						tableName);
				try (Connection conn = PostgresHelp.getConnection();
						Statement userStmt = conn.createStatement();
						ResultSet reader = userStmt.executeQuery(sql)) {

					while (reader.next()) {
						String column_name = reader.getString(1); // Get 1st column
						String data_type = reader.getString(2); // Get 2nd column
						int character_maximum_length = 0;

						try {
							character_maximum_length = reader.getInt(3); // Get 3rd column
							if (reader.wasNull())
								character_maximum_length = 0;
						} catch (SQLException e) {
							// Ignore
						}

						insertInto += column_name;
						insertInto += ",";

						if (!"trk_ymd -, trk_tme, trk_sha_id, trk_ip, trk_kno, ksn_ymd, ksn_tme, ksn_sha_id, ksn_ip, ksn_kno, ksn_tms"
								.contains(column_name)) {
							switch (data_type) {
							case "numeric":
								int numeric_precision = reader.getInt(4); // Get 4th column
								int numeric_scale = reader.getInt(5); // Get 5th column
								valueTemp = "";

								for (int i = 1; i <= numeric_precision - numeric_scale; i++) {
									if (String.valueOf(i).length() > 1) {
										valueTemp += String.valueOf(i).substring(1, 2);
									} else {
										if (numeric_precision == 1 && i > 9) {
											valueTemp += String.valueOf(i).substring(1, 2);
										} else {
											valueTemp += String.valueOf(i);
										}
									}
								}

								if (numeric_scale > 0) {
									valueTemp += ".";
									for (int i = 1; i <= numeric_scale; i++) {
										if (String.valueOf(i).length() > 1) {
											valueTemp += String.valueOf(i).substring(1, 2);
										} else {
											valueTemp += String.valueOf(i);
										}
									}
								}

								values1 += valueTemp;
								values1 += ",";

								if (numeric_scale > 0) {
									for (int i = 1; i < count; i++) {
										double value = Double.parseDouble(valueTemp) + i;
										valuesDic.put(i, valuesDic.get(i) + value + ",");
									}
								} else {
									for (int i = 1; i < count; i++) {
										if (valueTemp.length() >= 18) {
											int value = Integer.parseInt(valueTemp.substring(0, 10)) + i;
											valuesDic.put(i, valuesDic.get(i) + value + ",");
										} else {
											int value = Integer.parseInt(valueTemp) + i;

											if (numeric_precision == 1 && value > 9) {
												valuesDic.put(i,
														valuesDic.get(i) + String.valueOf(value).substring(1, 2) + ",");
											} else {
												if (String.valueOf(value).length() > numeric_precision) {
													valuesDic.put(i, valuesDic.get(i)
															+ String.valueOf(value).substring(0, numeric_precision)
															+ ",");
												} else {
													valuesDic.put(i, valuesDic.get(i) + value + ",");
												}
											}
										}
									}
								}
								break;

							default:
								switch (column_name) {
								case "coop_group_code":
									values1 += "'01',";
									for (int i = 1; i < count; i++) {
										valuesDic.put(i, valuesDic.get(i) + "'01',");
									}
									break;
								case "jnw_skbt_id":
									values1 += "'J_OVHD37040000@000',";
									for (int i = 1; i < count; i++) {
										valuesDic.put(i, valuesDic.get(i) + "'J_OVHD37040000@001',");
									}
									break;
								default:
									valueTemp = "";
									if (column_name.contains("kbn") && character_maximum_length < 10) {
										values1 += "'";
										for (int i = 1; i < count; i++) {
											valuesDic.put(i, valuesDic.get(i) + "'");
										}

										for (int i = 0; i < character_maximum_length; i++) {
											if (String.valueOf(i).length() > 1) {
												valueTemp += String.valueOf(i).substring(1, 2);
											} else {
												valueTemp += String.valueOf(i);
											}
										}

										for (int i = 1; i < count; i++) {
											try {
												int value = Integer.parseInt(valueTemp) + i;
												if (value < 10) {
													if (character_maximum_length > 1) {
														valuesDic.put(i, valuesDic.get(i) + "0" + value);
													} else {
														valuesDic.put(i, valuesDic.get(i) + value);
													}
												} else {
													if (character_maximum_length > 1) {
														if (String.valueOf(value).length() > character_maximum_length) {
															valuesDic.put(i, valuesDic.get(i) + String.valueOf(value)
																	.substring(String.valueOf(value).length() - 2));
														} else {
															valuesDic.put(i, valuesDic.get(i) + value);
														}
													} else {
														valuesDic.put(i, valuesDic.get(i)
																+ String.valueOf(value).substring(1, 2));
													}
												}
											} catch (Exception e) {
												throw e;
											}
										}

										values1 += valueTemp;
										values1 += "',";
										for (int i = 1; i < count; i++) {
											valuesDic.put(i, valuesDic.get(i) + "',");
										}
									} else if (column_name.contains("_date") && character_maximum_length == 8) {
										String dateStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
										values1 += String.format("'%s',", dateStr);
										for (int i = 1; i < count; i++) {
											valuesDic.put(i, valuesDic.get(i) + String.format("'%s',", dateStr));
										}
									} else if (column_name.contains("_date") && character_maximum_length == 6) {
										String dateStr = new SimpleDateFormat("yyyyMM").format(new Date());
										values1 += String.format("'%s',", dateStr);
										for (int i = 1; i < count; i++) {
											valuesDic.put(i, valuesDic.get(i) + String.format("'%s',", dateStr));
										}
									} else if (column_name.contains("_time") && character_maximum_length == 8) {
										String timeStr = new SimpleDateFormat("HH:mm:ss").format(new Date());
										values1 += String.format("'%s',", timeStr);
										for (int i = 1; i < count; i++) {
											valuesDic.put(i, valuesDic.get(i) + String.format("'%s',", timeStr));
										}
									} else if (column_name.contains("_time") && character_maximum_length == 6) {
										String timeStr = new SimpleDateFormat("HHmmss").format(new Date());
										values1 += String.format("'%s',", timeStr);
										for (int i = 1; i < count; i++) {
											valuesDic.put(i, valuesDic.get(i) + String.format("'%s',", timeStr));
										}
									} else if (column_name.contains("_time") && character_maximum_length == 4) {
										String timeStr = new SimpleDateFormat("HHmm").format(new Date());
										values1 += String.format("'%s',", timeStr);
										for (int i = 1; i < count; i++) {
											valuesDic.put(i, valuesDic.get(i) + String.format("'%s',", timeStr));
										}
									} else if (column_name.contains("_tme") && character_maximum_length == 8) {
										String timeStr = new SimpleDateFormat("HH:mm:ss").format(new Date());
										values1 += String.format("'%s',", timeStr);
										for (int i = 1; i < count; i++) {
											valuesDic.put(i, valuesDic.get(i) + String.format("'%s',", timeStr));
										}
									} else if (column_name.contains("_tme") && character_maximum_length == 6) {
										String timeStr = new SimpleDateFormat("HHmmss").format(new Date());
										values1 += String.format("'%s',", timeStr);
										for (int i = 1; i < count; i++) {
											valuesDic.put(i, valuesDic.get(i) + String.format("'%s',", timeStr));
										}
									} else if (column_name.contains("_tme") && character_maximum_length == 4) {
										String timeStr = new SimpleDateFormat("HHmm").format(new Date());
										values1 += String.format("'%s',", timeStr);
										for (int i = 1; i < count; i++) {
											valuesDic.put(i, valuesDic.get(i) + String.format("'%s',", timeStr));
										}
									} else {
										String cValue = "";
										String kanji = "";
										String kana = "";
										int n = character_maximum_length / 10;

										if (columnNameDictionary.containsKey(column_name.toUpperCase())) {
											cValue = columnNameDictionary.get(column_name.toUpperCase());
											if (cValue.contains("漢字")) {
												String result = cValue
														+ String.join("", Collections.nCopies(n, "漢字漢字漢字漢字漢字"));
												if (result.length() < character_maximum_length) {
													kanji = String.join("", Collections
															.nCopies(character_maximum_length - result.length(), "一"));
												} else {
													kanji = result.substring(0, character_maximum_length);
												}
											} else if (cValue.contains("カナ")) {
												String result = getRandomString("アイウエオカキクケコ", character_maximum_length);

												if (result.length() < character_maximum_length) {
													kana = String.join("",
															Collections.nCopies(character_maximum_length, "カ"));
												} else {
													kana = result.substring(0, character_maximum_length);
												}
											}
										}

										values1 += "'";
										for (int i = 1; i < count; i++) {
											valuesDic.put(i, valuesDic.get(i) + "'");
										}

										if (!kanji.isEmpty()) {
											valueTemp += kanji;
											for (int i = 1; i < count; i++) {
												kanji = kanji.substring(0, kanji.length() - 2)
														+ getRandomString("一二三四五六七八九十", 2);
												valuesDic.put(i, valuesDic.get(i) + kanji);
											}
										} else if (!kana.isEmpty()) {
											valueTemp += kana;
											for (int i = 1; i < count; i++) {
												kana = getRandomString(kana, kana.length());
												valuesDic.put(i, valuesDic.get(i) + kana);
											}
										} else {
											for (int i = 1; i <= character_maximum_length; i++) {
												if (String.valueOf(i).length() > 1) {
													valueTemp += String.valueOf(i).substring(1, 2);
												} else {
													valueTemp += String.valueOf(i);
												}
											}

											for (int i = 1; i < count; i++) {
												int value = 0;
												if (valueTemp.length() > 8) {
													try {
														value = Integer.parseInt(valueTemp
																.substring(valueTemp.length() - 8, valueTemp.length()))
																+ i;
														valuesDic.put(i,
																valuesDic.get(i)
																		+ valueTemp.substring(0, valueTemp.length() - 8)
																		+ value);
													} catch (Exception e) {
														throw e;
													}
												} else {
													try {
														value = Integer.parseInt(valueTemp) + i;
														if (character_maximum_length > 1) {
															if (String.valueOf(value)
																	.length() > character_maximum_length) {
																valuesDic.put(i, valuesDic.get(i)
																		+ String.valueOf(value).substring(
																				String.valueOf(value).length() - 2));
															} else {
																valuesDic.put(i, valuesDic.get(i) + value);
															}
														} else {
															if (value < 10) {
																valuesDic.put(i, valuesDic.get(i) + value);
															} else {
																valuesDic.put(i, valuesDic.get(i)
																		+ String.valueOf(value).substring(1, 2));
															}
														}
													} catch (Exception e) {
														throw e;
													}
												}
											}
										}

										values1 += valueTemp;
										values1 += "',";
										for (int i = 1; i < count; i++) {
											valuesDic.put(i, valuesDic.get(i) + "',");
										}
									}
									break;
								}
								break;
							}
						}
					}

					insertInto = insertInto.substring(0, insertInto.length() - 1) + ")";
					values1 += "TO_CHAR(CURRENT_TIMESTAMP, 'YYYYMMDD'), TO_CHAR(CURRENT_TIMESTAMP, 'HH24MISS'), 'batchUser', '192.168.11.12', 'J_OVHD37040000@000', TO_CHAR(CURRENT_TIMESTAMP, 'YYYYMMDD'), TO_CHAR(CURRENT_TIMESTAMP, 'HH24MISS'), 'batchUser', '192.168.11.12', 'J_OVHD37040000@000', 1)";

					for (int i = 1; i < count; i++) {
						valuesDic.put(i, valuesDic.get(i)
								+ "TO_CHAR(CURRENT_TIMESTAMP, 'YYYYMMDD'), TO_CHAR(CURRENT_TIMESTAMP, 'HH24MISS'), 'batchUser', '192.168.11.12', 'J_OVHD37040000@000', TO_CHAR(CURRENT_TIMESTAMP, 'YYYYMMDD'), TO_CHAR(CURRENT_TIMESTAMP, 'HH24MISS'), 'batchUser', '192.168.11.12', 'J_OVHD37040000@000', 1)");
					}

					sb.append(delete).append("\n");
					sb.append(insertInto).append("\n");
					sb.append(values1).append("\n");
					sb.append(",");

					for (int i = 1; i < count; i++) {
						sb.append(valuesDic.get(i)).append("\n");
						if (i == count - 1) {
							sb.append(";");
						} else {
							sb.append(",");
						}
					}
					sb.append("\n");

				}

			}
		} catch (Exception ex) {
			System.err.println("Error: " + ex.getMessage());
			ex.printStackTrace();
		}

		return sb.toString();
	}

	private String getRandomString(String input, int n) {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		try {
			Thread.sleep(2);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		StringBuilder result = new StringBuilder();

		// Randomly select characters and concatenate them into the result
		while (result.length() < n) {
			// Randomly select a character index
			int randomIndex = random.nextInt(input.length());

			// Add the randomly selected character
			result.append(input.charAt(randomIndex));
		}

		// Return the result (truncate if length > n to ensure length is n)
		return result.length() > n ? result.substring(0, n) : result.toString();
	}

	public String processSqlComments() {
		defCurrentLoad(this.JobId, 0);

		List<String> sqlLines = ReadFileHelp.readSqlFile();
		StringBuilder sb = new StringBuilder();

		for (String sqlLine : sqlLines) {
			String line = sqlLine.toUpperCase();
			String tempLine = line;
			Map<Integer, String> dicT = new HashMap<>();
			Map<Integer, String> dicC = new HashMap<>();

			List<String> tableNameList = tableNameDictionary.keySet().stream()
					.filter(x -> tempLine.contains(x))
					.sorted(Comparator.comparingInt(String::length).reversed())
					.collect(Collectors.toList());

			List<String> columnNameList = columnNameDictionary.keySet().stream()
					.filter(x -> tempLine.contains(x))
					.sorted(Comparator.comparingInt(String::length).reversed())
					.collect(Collectors.toList());

			List<String> tableNames = new ArrayList<>();
			for (String tableName : tableNameList) {
				if (line.contains(tableName) && !tableNames.contains(tableName)) {
					tableNames.add(tableName);
					line = line.replace(tableName, "");
				}
			}

			for (String tableName : tableNames) {
				int index = sqlLine.toUpperCase().indexOf(tableName);
				if (dicT.containsKey(index)) {
					String longest = tableNameList.stream().max(Comparator.comparingInt(String::length)).orElse("");
					dicT.put(index, tableNameDictionary.get(longest));
				} else {
					dicT.put(index, tableNameDictionary.get(tableName));
				}
			}

			List<String> columnNames = new ArrayList<>();
			for (String columnName : columnNameList) {
				if (line.contains(columnName) && !columnNames.contains(columnName)) {
					columnNames.add(columnName);
					line = line.replace(columnName, "");
				}
			}

			for (String columnName : columnNames) {
				int index = sqlLine.toUpperCase().indexOf(columnName);
				if (dicC.containsKey(index)) {
					String longest = columnNameList.stream().max(Comparator.comparingInt(String::length)).orElse("");
					dicC.put(index, columnNameDictionary.get(longest));
				} else {
					dicC.put(index, columnNameDictionary.get(columnName));
				}
			}

			List<String> sortedT = dicT.entrySet().stream()
					.sorted(Map.Entry.comparingByKey())
					.map(Map.Entry::getValue)
					.collect(Collectors.toList());

			List<String> sortedC = dicC.entrySet().stream()
					.sorted(Map.Entry.comparingByKey())
					.map(Map.Entry::getValue)
					.collect(Collectors.toList());

			String commentTContent = String.join(",", sortedT);
			String commentCContent = String.join(",", sortedC);

			if (!sortedT.isEmpty() || !sortedC.isEmpty()) {
				sb.append(String.format("%s -- %s %s%n", sqlLine, commentTContent, commentCContent));
			} else {
				sb.append(String.format("%s%n", sqlLine));
			}
		}

		return sb.toString();
	}

	public String creatSqlComments() {
		defCurrentLoad(this.JobId, 1);

		StringBuilder sb = new StringBuilder();

		List<String> sqlLines = ReadFileHelp.readSqlFile();

		for (String sqlLine : sqlLines) {
			String line = sqlLine.toUpperCase() + " ";
			line = line.replace(",", " ,");
			String tempLine = line;

			List<String> tableNameList = tableNameDictionary.keySet().stream()
					.filter(x -> tempLine.contains(x + " "))
					.sorted(Comparator.comparingInt(String::length).reversed())
					.collect(Collectors.toList());

			List<String> columnNameList = columnNameDictionary.keySet().stream()
					.filter(x -> tempLine.contains(x + " "))
					.sorted(Comparator.comparingInt(String::length).reversed())
					.collect(Collectors.toList());

			for (String tableName : tableNameList) {
				String replaceTarget = tableName + " ";
				String replaceValue = tableNameDictionary.get(tableName) + " ";
				line = line.replaceAll(replaceTarget, replaceValue);
			}

			for (String columnName : columnNameList) {
				String replaceTarget = columnName + " ";
				String replaceValue = columnNameDictionary.get(columnName) + " ";
				line = line.replace(replaceTarget, replaceValue);
				replaceTarget = columnName + ")";
				replaceValue = columnNameDictionary.get(columnName) + ")";
				line = line.replace(replaceTarget, replaceValue);
			}

			sb.append(line).append(System.lineSeparator());
		}

		return sb.toString();
	}

}
