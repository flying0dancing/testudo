package com.lombardrisk.utils;

import com.lombardrisk.IComFolder;
import com.lombardrisk.pojo.DatabaseServer;
import com.lombardrisk.pojo.TableProps;
import com.lombardrisk.pojo.DivideTableFieldList;
import com.lombardrisk.status.BuildStatus;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DBInfo implements IComFolder {

    private final static Logger logger = LoggerFactory.getLogger(DBInfo.class);
    private DBHelper dbHelper;
    private DBDriverType dbDriverFlag;

    public enum DBDriverType {
        ORACLE,
        SQLSERVER,
        ACCESSDB;
    }

    private static Map<String, List<TableProps>> dbTableColumns = new HashMap<String, List<TableProps>>();

    public DBInfo(DatabaseServer databaseServer) {
        setDbHelper(databaseServer);
        if (dbHelper.getDatabaseServer().getDriver().startsWith("ora")) {
            setDbDriverFlag(DBDriverType.ORACLE);
        } else if (dbHelper.getDatabaseServer().getDriver().startsWith("sql")) {
            setDbDriverFlag(DBDriverType.SQLSERVER);
        } else if (dbHelper.getDatabaseServer().getDriver().startsWith("access")) {
            setDbDriverFlag(DBDriverType.ACCESSDB);
        }
    }

    public DBHelper getDbHelper() {
        return dbHelper;
    }

    private void setDbHelper(DatabaseServer databaseServer) {
        this.dbHelper = new DBHelper(databaseServer);
    }

    public Boolean executeSQL(String sql) {
        dbHelper.connect();
        Boolean flag = dbHelper.addBatch(sql);
        dbHelper.close();

        return flag;
    }

    private String queryRecord(String sql) {
        dbHelper.connect();
        String rst = dbHelper.query(sql);
        dbHelper.close();
        return rst;
    }

    private List<String> queryRecords(String sql) {
        dbHelper.connect();
        List<String> rst = dbHelper.queryRecords(sql);
        dbHelper.close();
        return rst;
    }

    @SuppressWarnings("unused")
    private int update(String sql) {
        dbHelper.connect();
        int rst = dbHelper.update(sql);
        dbHelper.close();
        return rst;
    }


    /***
     * export data in database into *.csv files
     * @param prefix product prefix, use to replace # defined in tableList
     * @param tableList defined in json file
     * @param exportPath
     * @param iNIName table structures
     * @param idOfDBAndTable this value starts with "#", following databaseServerAndTables's ID
     */
    public void exportToSingle(
            String prefix,
            List<String> tableList,
            String exportPath,
            String iNIName,
            List<String> excludeReturnIds,
            String idOfDBAndTable) {
        if (StringUtils.isBlank(exportPath)) return;
        if (tableList == null || tableList.size() <= 0) return;
        String emptyStr="";
        if (StringUtils.isBlank(prefix)) {
            prefix = emptyStr;
        }
        if(StringUtils.isBlank(idOfDBAndTable)){
            idOfDBAndTable=emptyStr;
        }
        String fileSeparator=System.getProperty("file.separator");
        String sharpFlag="#";
        String sQL,sqlCondition,dividedTableField,tabName,tableName,metadataName,exportFullPath;
        String selectSQL="select * from ";
        String quoteFlag="\"";
        String csvSuffix=".csv";
        logger.info("================= export single tables =================");
        for (String tab : tableList) {
            tabName=tab.replace(sharpFlag, prefix);
            tableName = getTableNameFromDB(tabName);
            if (StringUtils.isNotBlank(tableName)) {
                dividedTableField=DivideTableFieldList.getDividedField(tableName);
                sqlCondition = combineSqlCondition(dividedTableField, excludeReturnIds);
                logger.info("----------- " + tableName + " ----------- ");
                tab = tab.replace(sharpFlag, emptyStr);
                metadataName= tab + idOfDBAndTable + csvSuffix;
                exportFullPath = exportPath + fileSeparator + metadataName;
                if (new File(exportFullPath).exists()) {
                    logger.warn("warn: duplicated [" + tab + idOfDBAndTable + "] in metadata, overwriting existed one.");
                }
                logger.info("export metadata struct to " + iNIName);

                if (getDbDriverFlag() == DBDriverType.SQLSERVER || getDbDriverFlag() == DBDriverType.ACCESSDB) {
                    sQL = selectSQL + tableName;
                } else {
                    sQL = selectSQL + quoteFlag + tableName + quoteFlag;
                }
                sQL=sQL+judgeReturnIdExist(tableName, dividedTableField, sqlCondition);

                dbHelper.exportToINI(tab + idOfDBAndTable, sQL, new File(exportPath).getPath() + fileSeparator + iNIName);
                logger.info("metadata exports to:" + metadataName);
                dbHelper.exportToCsv(sQL, exportFullPath);
            } else {
                logger.warn("warn: table[" + tabName + "] doesn't exist.");
            }
        }
        dbHelper.close();
    }

    public String judgeReturnIdExist(String tableName,String dividedField, String sqlCondition) {
        String exist = null;
        String SQL;
        if (getDbDriverFlag() == DBDriverType.ORACLE) {
            SQL = "select column_name from user_tab_cols where table_name='" + tableName + "' and column_name='"+dividedField+"'";
            exist = queryRecord(SQL);
        } else if (getDbDriverFlag() == DBDriverType.SQLSERVER) {
            SQL = "select b.name from sysobjects a, syscolumns b where a.xtype='u' and a.id=b.id and a.name='"
                    + tableName
                    + "' and b.name='"+dividedField+"'";
            exist = queryRecord(SQL);
        } else if (getDbDriverFlag() == DBDriverType.ACCESSDB) {
            SQL = "SELECT * FROM " + tableName + " where false";
            if (StringUtils.isNotBlank(getDbHelper().getColumnType(SQL, dividedField))) {
                exist = dividedField;
            }
        }

        if (StringUtils.isBlank(exist)) {
            sqlCondition = "";
        }
        return sqlCondition;
    }

    public String combineSqlCondition(String dividedField,List<String> excludeReturnIds) {
        String sqlCondition = "";
        if (excludeReturnIds != null && excludeReturnIds.size() > 0) {
            sqlCondition = " where \""+dividedField+"\" not in (";
            if (getDbDriverFlag() == DBDriverType.ACCESSDB) {
                sqlCondition = " where CStr("+dividedField+") not in (";
            }
            for (int i = 0; i < excludeReturnIds.size(); i++) {
                sqlCondition = sqlCondition + "'" + excludeReturnIds.get(i) + "',";
            }
            sqlCondition = sqlCondition.replaceAll(",$", ")");
        }
        return sqlCondition;
    }

    /***
     * export data in database into *.csv files divided by field ReturnId
     * @param prefix product prefix, use to replace # defined in tableList
     * @param tableList defined in json file
     * @param exportPath
     * @param iNIName
     * @param idOfDBAndTable this value starts with "#", following databaseServerAndTables's ID
     */
    public void exportToDivides(
            String prefix,
            List<String> tableList,
            String exportPath,
            String iNIName,
            List<String> excludeReturnIds,
            String idOfDBAndTable) {
        //Boolean flag=false;
        if (StringUtils.isBlank(exportPath)) return;
        if (tableList == null || tableList.size() <= 0) return;
        String emptyStr="";
        if (StringUtils.isBlank(prefix)) {
            prefix = emptyStr;
        }
        if(StringUtils.isBlank(idOfDBAndTable)){
            idOfDBAndTable=emptyStr;
        }
        String fileSeparator=System.getProperty("file.separator");
        String sharpFlag="#";
        String sQL,sqlCondition,dividedTableField,typeReturnId,tabName,tableName,metadataName;
        String selectSQL="select * from ";
        String quoteFlag="\"";
        String fromSQL=" from ";
        String selectDistinct="select distinct ";
        String whereSQL=" where ";
        String quoteSingleFlag="'";
        String equalFlag="=";
        String csvSuffix=".csv";
        String nullFlag="null";
        String varcharFlag="VARCHAR";
        String underlineFlag="_";
        String top1SQL="select top 1 * from ";
        String uniqueSQL="select unique ";
        logger.info("================= export tables need to be divided by ReturnId =================");
        for (String tab : tableList) {
            tabName=tab.replace(sharpFlag, prefix);
            tableName = getTableNameFromDB(tabName);
            if (StringUtils.isNotBlank(tableName)) {
                dividedTableField=DivideTableFieldList.getDividedField(tableName);
                sqlCondition = combineSqlCondition(dividedTableField, excludeReturnIds);
                logger.info("----------- " + tableName + " ----------- ");
                tab = tab.replace(sharpFlag, emptyStr);
                String subPath = new File(exportPath).getPath() + fileSeparator + tab;

                logger.info("export metadata struct to " + iNIName);
                sQL = selectSQL + quoteFlag + tableName + quoteFlag+" where rownum=1";//
                if (getDbDriverFlag() == DBDriverType.SQLSERVER || getDbDriverFlag() == DBDriverType.ACCESSDB) {
                    sQL = top1SQL + tableName;
                }
                dbHelper.exportToINI(tab + idOfDBAndTable, sQL, new File(exportPath).getPath() + fileSeparator + iNIName);
                typeReturnId = dbHelper.getColumnType(sQL, dividedTableField);
                sQL = uniqueSQL+ quoteFlag + dividedTableField + quoteFlag + fromSQL + quoteFlag+ tableName + quoteFlag + sqlCondition;
                if (getDbDriverFlag() == DBDriverType.SQLSERVER) {
                    sQL = selectDistinct +quoteFlag+ dividedTableField + quoteFlag + fromSQL + quoteFlag+ tableName + quoteFlag + sqlCondition;
                } else if (getDbDriverFlag() == DBDriverType.ACCESSDB) {
                    sQL = selectDistinct + dividedTableField + fromSQL + tableName + sqlCondition;
                }
                List<String> returnIds = queryRecords(sQL);
                if (returnIds != null) {
                    FileUtil.createDirectories(subPath);//with idOfDBAndTable or not
                    for (String returnId : returnIds) {
                        if (StringUtils.isNotBlank(returnId) && !returnId.equalsIgnoreCase(nullFlag)) {
                            metadataName= tab + idOfDBAndTable + underlineFlag + returnId + csvSuffix;
                            logger.info("metadata exports to:" + metadataName);
                            sQL = selectSQL + quoteFlag  + tableName + quoteFlag + whereSQL + quoteFlag + dividedTableField + quoteFlag +
                                    equalFlag+quoteSingleFlag + returnId + quoteSingleFlag;
                            if(getDbDriverFlag() == DBDriverType.ACCESSDB){
                                sQL=selectSQL + tableName + whereSQL + dividedTableField + equalFlag;
                                if(typeReturnId.contains(varcharFlag)){
                                    sQL = sQL + quoteSingleFlag + returnId + quoteSingleFlag;
                                }else{
                                    sQL = sQL + returnId;
                                }
                            }

                            dbHelper.exportToCsv(sQL,subPath + fileSeparator + metadataName);
                        }
                    }
                } else {
                    logger.warn("warn: table[" + tableName + "] doesn't contains any ReturnId.");
                }
            } else {
                logger.warn("warn: table[" + tabName + "] doesn't exist.");
            }
        }
        dbHelper.close();
    }

    @Deprecated
    public void exportToCsv() {
        dbHelper.connect();
        //String SQL="SELECT * FROM [DataScheduleView] ";
        //dbHelper.exportToCsv(SQL, "E:\\tmp2\\test.csv");
        //dbHelper.tset("E:\\abc\\fed\\meta.accdb","List");
        //dbHelper.importCsvToAccessDB("E:\\abc\\fed\\meta.accdb","List",true,"E:\\tmp2\\ECRList\\ECRList_360002.csv");
        //dbHelper.createAccessTable("E:\\abc\\fed\\meta.accdb","aa","E:\\tmp2\\ECRList\\ECRList_360002.csv");
        //dbHelper.aasdf("E:\\abc\\fed\\meta.accdb","aa");
        dbHelper.close();
    }

    /**
     * create a accessDatabase, no need to create a new one if existed.
     *
     * @return
     */
    public Boolean createAccessDB() {
        Boolean flag = false;
        String dbFullName = dbHelper.getDatabaseServer().getSchema();
		/*if(new File(dbFullName).exists())
		{new File(dbFullName).delete();}*/
        if (!new File(dbFullName).exists()) {
            DBHelper.AccessdbHelper accdb = dbHelper.new AccessdbHelper();
            flag = accdb.createAccessDB(dbHelper.getDatabaseServer().getSchema());
        } else {
            flag = true;
        }
        return flag;
    }

    /***
     * according to returnId, search its return name and version in Rets table.
     * this function should be worked on access database.
     * @param returnId
     * @return returnName_returnVersion, return "" if error occurs.
     */
    public String getReturnAndVersion(String returnId) {
        String returnAndVer = "";
        String tableName = "Rets";
        if (getDbDriverFlag() == DBDriverType.ACCESSDB) {
            dbHelper.connect();
            DBHelper.AccessdbHelper accdb = dbHelper.new AccessdbHelper();
            if (!accdb.accessTableExistence(tableName)) {
                BuildStatus.getInstance().recordError();
                logger.error("cannot found " + tableName);
            } else {
                String sQL = "SELECT Return & \"_v\" & Version AS Expr1 from [" + tableName + "] WHERE ReturnId=" + returnId;
                returnAndVer = dbHelper.query(sQL);
            }

            dbHelper.close();
        } else {
            BuildStatus.getInstance().recordError();
            logger.error("this function should be worked on access database.");
        }
        if (returnAndVer == null) returnAndVer = "";

        return returnAndVer;
    }

    /**
     * create all tables which defined in the schemaFullName.
     *
     * @param schemaFullName
     * @return
     */
    public Boolean createAccessTables(String schemaFullName) {
        Boolean flag = true;
        if (dbHelper.getDatabaseServer().getDriver().startsWith("access")) {
            dbHelper.connect();
            DBHelper.AccessdbHelper accdb = dbHelper.new AccessdbHelper();
            Map<String, List<String>> allTableDefs = FileUtil.getAllTableDefinitions(schemaFullName);
            for (String key : allTableDefs.keySet()) {
                Boolean flagT = accdb.createAccessDBTable(key, allTableDefs.get(key));
                if (!flagT) {
                    flag = false;
                    BuildStatus.getInstance().recordError();
                    logger.error("error: fail to create table [" + key + "]");
                }
            }
            dbHelper.close();
        } else {
            BuildStatus.getInstance().recordError();
            logger.error("this method should be worked on access database.");
            flag = false;
        }
        return flag;
    }

    /**
     * create table by schemaFullName which defined tableName, and import data which in csvPath to table.
     *
     * @param tableName       table name in access database
     * @param tableNameWithDB the value which defined in schemaFullName, which can equal to tableName, or tableName plus databaseServerAndTables's ID
     * @param csvPath
     * @param schemaFullName
     * @return
     */
    public Boolean importCsvToAccess(String tableName, String tableNameWithDB, String csvPath, String schemaFullName) {
        Boolean flag = false;
        if (dbHelper.getDatabaseServer().getDriver().startsWith("access")) {
            dbHelper.connect();
            String userSchemaFullName = schemaFullName.replace(
                    FileUtil.getFileNameWithSuffix(schemaFullName),
                    ACCESS_SCHEMA_INI);
            List<TableProps> columns = findDbTableColumns(tableName);
            logger.debug("initial columns {}", columns);
            if (columns == null) {
                if (FileUtil.search(userSchemaFullName, "[" + tableName + "]")) {
                    logger.debug("using user schema: {}", userSchemaFullName);
                    columns = FileUtil.getMixedTablesDefinition(FileUtil.searchTablesDefinition(
                            userSchemaFullName,
                            tableName));
                    logger.debug("user schema columns {}", columns);
                    setDbTableColumns(tableName, columns);
                } else {
                    logger.debug("using full schema: {}", schemaFullName);
                    FileUtil.search(schemaFullName, "[" + tableNameWithDB + "]");
                    columns = FileUtil.getMixedTablesDefinition(FileUtil.searchTablesDefinition(
                            schemaFullName,
                            tableName));
                    logger.debug("full schema columns {}", columns);
                    setDbTableColumns(tableName, columns);
                }
            }
            if (columns != null && columns.size() > 0) {
                DBHelper.AccessdbHelper accdb = dbHelper.new AccessdbHelper();

                flag = accdb.createAccessDBTab(tableName, columns);
                if (flag) {
                    flag = accdb.importCsvToAccessDB(tableName, columns, csvPath);
                } else {
                    BuildStatus.getInstance().recordError();
                    logger.error("error: fail to create table [" + tableName + "]");
                }
            } else {
                BuildStatus.getInstance().recordError();
                logger.error("error: invalid table definition [" + tableName + "]");
                flag = false;
            }
            dbHelper.close();
        } else {
            BuildStatus.getInstance().recordError();
            logger.error("this method should be worked on access database.");
        }
        return flag;
    }

    public DBDriverType getDbDriverFlag() {
        return dbDriverFlag;
    }

    public void setDbDriverFlag(DBDriverType dbDriverFlag) {
        this.dbDriverFlag = dbDriverFlag;
    }

    public static Map<String, List<TableProps>> getDbTableColumns() {
        return dbTableColumns;
    }

    public static void setDbTableColumns(String tableName, List<TableProps> columns) {
        DBInfo.dbTableColumns.put(tableName, columns);
    }

    public static List<TableProps> findDbTableColumns(String tableName) {
        if (!DBInfo.dbTableColumns.isEmpty()) {
            for (String key : DBInfo.dbTableColumns.keySet()) {
                if (key.equals(tableName)) {
                    return DBInfo.dbTableColumns.get(key);
                }
            }
        }
        return null;
    }

    public String getTableNameFromDB(String tab){
        String sQL = "select unique t.table_name from user_tab_cols t where lower(t.table_name)='" + tab + "'";
        if (getDbDriverFlag() == DBDriverType.SQLSERVER) {
            sQL = "select name from sysobjects where xtype='u' and lower(name)=lower('" + tab + "')";
        } else if (getDbDriverFlag() == DBDriverType.ACCESSDB) {
            sQL = "SELECT Name FROM sys.MSysObjects WHERE LCase(Name)='" + tab + "'";
        }
        return queryRecord(sQL);
    }
}
