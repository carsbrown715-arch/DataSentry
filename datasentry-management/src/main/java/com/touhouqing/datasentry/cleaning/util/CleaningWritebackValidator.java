package com.touhouqing.datasentry.cleaning.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class CleaningWritebackValidator {

	private CleaningWritebackValidator() {
	}

	public static Map<String, ColumnMeta> loadColumnMeta(Connection connection, String tableName) {
		Map<String, ColumnMeta> metaMap = new HashMap<>();
		if (connection == null || tableName == null || tableName.isBlank()) {
			return metaMap;
		}
		try {
			DatabaseMetaData meta = connection.getMetaData();
			String catalog = connection.getCatalog();
			String schema = connection.getSchema();
			try (ResultSet rs = meta.getColumns(catalog, schema, tableName, null)) {
				while (rs.next()) {
					String name = rs.getString("COLUMN_NAME");
					int dataType = rs.getInt("DATA_TYPE");
					int columnSize = rs.getInt("COLUMN_SIZE");
					String typeName = rs.getString("TYPE_NAME");
					if (name != null && !name.isBlank()) {
						metaMap.put(name.toLowerCase(Locale.ROOT), new ColumnMeta(dataType, columnSize, typeName));
					}
				}
			}
		}
		catch (Exception e) {
			return metaMap;
		}
		return metaMap;
	}

	public static String validateValues(Map<String, ColumnMeta> metaMap, Map<String, Object> values) {
		if (metaMap == null || metaMap.isEmpty() || values == null || values.isEmpty()) {
			return null;
		}
		for (Map.Entry<String, Object> entry : values.entrySet()) {
			String column = entry.getKey();
			Object value = entry.getValue();
			if (column == null || value == null) {
				continue;
			}
			ColumnMeta meta = metaMap.get(column.toLowerCase(Locale.ROOT));
			if (meta == null) {
				continue;
			}
			if (isStringType(meta.dataType())) {
				String text = String.valueOf(value);
				if (meta.columnSize() > 0 && text.length() > meta.columnSize()) {
					return "Column " + column + " length exceeds limit " + meta.columnSize();
				}
			}
			else if (value instanceof String) {
				return "Column " + column + " expects non-string type";
			}
		}
		return null;
	}

	private static boolean isStringType(int dataType) {
		return dataType == Types.VARCHAR || dataType == Types.CHAR || dataType == Types.LONGVARCHAR
				|| dataType == Types.NVARCHAR || dataType == Types.NCHAR || dataType == Types.LONGNVARCHAR
				|| dataType == Types.CLOB || dataType == Types.NCLOB;
	}

	public record ColumnMeta(int dataType, int columnSize, String typeName) {
	}

}
