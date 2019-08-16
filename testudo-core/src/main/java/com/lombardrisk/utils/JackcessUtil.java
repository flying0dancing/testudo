package com.lombardrisk.utils;

import com.healthmarketscience.jackcess.ColumnBuilder;
import com.healthmarketscience.jackcess.DataType;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Table;
import com.healthmarketscience.jackcess.TableBuilder;
import com.healthmarketscience.jackcess.util.ImportFilter;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public final class JackcessUtil {
    private static final int TWO=2;
    private static final int TWO_HUNDRED=200;
    private JackcessUtil(){}
    public static List<ColumnBuilder> toColumns(ResultSetMetaData md) throws SQLException {
        List<ColumnBuilder> columns = new LinkedList();

        for(int i = 1; i <= md.getColumnCount(); ++i) {
            ColumnBuilder column = new ColumnBuilder(md.getColumnLabel(i));
            int lengthInUnits = md.getColumnDisplaySize(i);
            column.setSQLType(md.getColumnType(i), lengthInUnits);
            DataType type = column.getType();
            if (type.isTrueVariableLength() && !type.isLongValue()) {
                column.setLengthInUnits((short)lengthInUnits);
            }

            if (type.getHasScalePrecision()) {
                int scale = md.getScale(i);
                int precision = md.getPrecision(i);
                if (type.isValidScale(scale)) {
                    column.setScale((byte)scale);
                }

                if (type.isValidPrecision(precision)) {
                    column.setPrecision((byte)precision);
                }
            }

            columns.add(column);
        }

        return columns;
    }
    private static Table createUniqueTable(Database db, String name, List<ColumnBuilder> columns,
                                           ResultSetMetaData md, ImportFilter filter) throws IOException, SQLException {
        String baseName = name;
        int var6 = TWO;
        while (db.getTable(name) != null){
            name = baseName + var6++;
        }
        return (new TableBuilder(name)).addColumns(filter.filterColumns(columns, md)).toTable(db);
    }
    public static String importResultSet(ResultSet source, Database db, String name, ImportFilter filter,
                                         boolean useExistingTable) throws SQLException, IOException {
        ResultSetMetaData md = source.getMetaData();
        name = TableBuilder.escapeIdentifier(name);
        Table table = null;
        if (!useExistingTable || (table = db.getTable(name)) == null) {
            List<ColumnBuilder> columns = toColumns(md);
            table = createUniqueTable(db, name, columns, md, filter);
        }

        List<Object[]> rows = new ArrayList(TWO_HUNDRED);
        int numColumns = md.getColumnCount();

        while(source.next()) {
            Object[] row = new Object[numColumns];

            for(int i = 0; i < row.length; ++i) {
                row[i] = source.getObject(i + 1);
            }

            row = filter.filterRow(row);
            if (row != null) {
                rows.add(row);
                if (rows.size() == TWO_HUNDRED) {
                    table.addRows(rows);
                    rows.clear();
                }
            }
        }

        if (rows.size() > 0) {
            table.addRows(rows);
        }

        return table.getName();
    }
}
