package com.lombardrisk.utils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lombardrisk.IComFolder;
import com.lombardrisk.pojo.DatabaseServer;
import com.lombardrisk.pojo.TableProps;


public class DBInfo  implements IComFolder{
	private final static Logger logger = LoggerFactory.getLogger(DBInfo.class);
	private DBHelper dbHelper;
	private DBDriverType dbDriverFlag;
	public enum DBDriverType{ORACLE,SQLSERVER,ACCESSDB;}
	private static Map<String,List<TableProps>> dbTableColumns=new HashMap<String,List<TableProps>>();
	
	public DBInfo(DatabaseServer databaseServer){
		setDbHelper(databaseServer);
		if(dbHelper.getDatabaseServer().getDriver().startsWith("ora")){
			setDbDriverFlag(DBDriverType.ORACLE);
		}else if(dbHelper.getDatabaseServer().getDriver().startsWith("sql")){
			setDbDriverFlag(DBDriverType.SQLSERVER);
		}else if(dbHelper.getDatabaseServer().getDriver().startsWith("access")){
			setDbDriverFlag(DBDriverType.ACCESSDB);
		}
	}
	
	public DBHelper getDbHelper() {
		return dbHelper;
	}

	private void setDbHelper(DatabaseServer databaseServer) {
		this.dbHelper =new DBHelper(databaseServer);
	}
	
	public Boolean executeSQL(String sql){
		dbHelper.connect();
		Boolean flag=dbHelper.addBatch(sql);
		dbHelper.close();
		
		return flag;
	}
	
	private String queryRecord(String sql)
	{
		dbHelper.connect();
		String rst = dbHelper.query(sql);
		dbHelper.close();
		return rst;
	}
	
	private List<String> queryRecords(String sql)
	{
		dbHelper.connect();
		List<String> rst = dbHelper.queryRecords(sql);
		dbHelper.close();
		return rst;
	}
	
	@SuppressWarnings("unused")
	private int update(String sql)
	{
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
	 * @param iNIName 
	 */
	public void exportToSingle(String prefix,List<String> tableList,String exportPath,String iNIName,List<String> excludeReturnIds)
	{
		if(StringUtils.isBlank(exportPath)) return;
		if(tableList==null || tableList.size()<=0) return;
		if(StringUtils.isBlank(prefix)){prefix="";}
		//else{prefix=prefix.toLowerCase();}
		String sqlCondition=combineSqlCondition(excludeReturnIds);
		logger.info("================= export single tables =================");
		for(String tab:tableList)
		{
			String SQL="select unique t.table_name from user_tab_cols t where lower(t.table_name)='"+tab.replace("#", prefix).toLowerCase()+"'";
			if(getDbDriverFlag()==DBDriverType.SQLSERVER){
				SQL="select name from sysobjects where xtype='u' and lower(name)=lower('"+tab.replace("#", prefix).toLowerCase()+"')";
			}else if(getDbDriverFlag()==DBDriverType.ACCESSDB){
				SQL="SELECT Name FROM sys.MSysObjects WHERE LCase(Name)='"+tab.replace("#", prefix).toLowerCase()+"'";
			}
			String tableName=queryRecord(SQL);
			if(StringUtils.isNotBlank(tableName))
			{
				logger.info("----------- "+tableName+" ----------- ");
				tab=tab.replace("#", "");
				String exportFullPath=exportPath+System.getProperty("file.separator")+tab+".csv";
				if(new File(exportFullPath).exists())
				{
					logger.warn("warn: duplicated ["+tab+"] in metadata, overwriting existed one.");
					//continue;
				}
				logger.info("export metadata struct to "+iNIName);
				
				
				if(getDbDriverFlag()==DBDriverType.SQLSERVER || getDbDriverFlag()==DBDriverType.ACCESSDB){
					SQL="select * from "+tableName+judgeReturnIdExist(tableName,sqlCondition);
				}else{
					SQL="select * from \""+tableName+"\""+judgeReturnIdExist(tableName,sqlCondition);// 
				}
				dbHelper.exportToINI(tab,SQL, new File(exportPath).getPath()+System.getProperty("file.separator")+iNIName);
				logger.info("metadata exports to:"+tab+".csv");
				dbHelper.exportToCsv(SQL, exportFullPath);
				
			}else
			{
				logger.warn("warn: table["+tab.replace("#", prefix)+"] doesn't exist.");
				//logger.debug(" Sql Statement:"+SQL);
			}
		}
		dbHelper.close();
	}
	
	
	/***
	 * export data in database into *.csv files
	 * @param prefix product prefix, use to replace # defined in tableList
	 * @param tableList defined in json file
	 * @param exportPath 
	 * @param iNIName 
	 * @param idOfDBAndTable this value starts with "#", following databaseServerAndTables's ID
	 */
	public void exportToSingle1(String prefix,List<String> tableList,String exportPath,String iNIName,List<String> excludeReturnIds,String idOfDBAndTable)
	{
		if(StringUtils.isBlank(exportPath)) return;
		if(tableList==null || tableList.size()<=0) return;
		if(StringUtils.isBlank(prefix)){prefix="";}
		//else{prefix=prefix.toLowerCase();}
		String sqlCondition=combineSqlCondition(excludeReturnIds);
		logger.info("================= export single tables =================");
		for(String tab:tableList)
		{
			String SQL="select unique t.table_name from user_tab_cols t where lower(t.table_name)='"+tab.replace("#", prefix).toLowerCase()+"'";
			if(getDbDriverFlag()==DBDriverType.SQLSERVER){
				SQL="select name from sysobjects where xtype='u' and lower(name)=lower('"+tab.replace("#", prefix).toLowerCase()+"')";
			}else if(getDbDriverFlag()==DBDriverType.ACCESSDB){
				SQL="SELECT Name FROM sys.MSysObjects WHERE LCase(Name)='"+tab.replace("#", prefix).toLowerCase()+"'";
			}
			String tableName=queryRecord(SQL);
			if(StringUtils.isNotBlank(tableName))
			{
				logger.info("----------- "+tableName+" ----------- ");
				tab=tab.replace("#", "");
				String exportFullPath=exportPath+System.getProperty("file.separator")+tab+idOfDBAndTable+".csv";
				if(new File(exportFullPath).exists())
				{
					logger.warn("warn: duplicated ["+tab+idOfDBAndTable+"] in metadata, overwriting existed one.");
				}
				logger.info("export metadata struct to "+iNIName);
				
				 
				if(getDbDriverFlag()==DBDriverType.SQLSERVER || getDbDriverFlag()==DBDriverType.ACCESSDB){
					SQL="select * from "+tableName+judgeReturnIdExist(tableName,sqlCondition);
				}else{
					SQL="select * from \""+tableName+"\""+judgeReturnIdExist(tableName,sqlCondition);//
				}
				dbHelper.exportToINI(tab+idOfDBAndTable,SQL, new File(exportPath).getPath()+System.getProperty("file.separator")+iNIName);
				logger.info("metadata exports to:"+tab+idOfDBAndTable+".csv");
				dbHelper.exportToCsv(SQL, exportFullPath);
				
			}else
			{
				logger.warn("warn: table["+tab.replace("#", prefix)+"] doesn't exist.");
				//logger.debug(" Sql Statement:"+SQL);
			}
		}
		dbHelper.close();
	}
	
	public String judgeReturnIdExist(String tableName,String sqlCondition){
		String exist=null;
		String SQL="";
		if(getDbDriverFlag()==DBDriverType.ORACLE){
			SQL="select column_name from user_tab_cols where table_name='"+tableName+"' and column_name='ReturnId'";
			exist=queryRecord(SQL);
		}else if(getDbDriverFlag()==DBDriverType.SQLSERVER){
			SQL="select b.name from sysobjects a, syscolumns b where a.xtype='u' and a.id=b.id and a.name='"+tableName+"' and b.name='ReturnId'";
			exist=queryRecord(SQL);
		}else if(getDbDriverFlag()==DBDriverType.ACCESSDB){
			SQL="SELECT * FROM "+tableName+" where false";
			if(StringUtils.isNotBlank(getDbHelper().getColumnType(SQL,"ReturnId"))){
				exist="ReturnId";
			}
		}
		
		if(StringUtils.isBlank(exist)){
			sqlCondition="";
		}
		return sqlCondition;
	}
	
	
	public String combineSqlCondition(List<String> excludeReturnIds){
		String sqlCondition="";
		if(excludeReturnIds!=null && excludeReturnIds.size()>0){
			sqlCondition=" where \"ReturnId\" not in (";
			if(getDbDriverFlag()==DBDriverType.ACCESSDB){
				sqlCondition=" where CStr(ReturnId) not in ("; 
			}
			for(int i=0;i<excludeReturnIds.size();i++){
				sqlCondition=sqlCondition+"'"+excludeReturnIds.get(i)+"',";
			}
			sqlCondition=sqlCondition.replaceAll(",$", ")");
		}
		return sqlCondition;
	}
	/***
	 * export data in database into *.csv files divided by field ReturnId
	 * @param prefix product prefix, use to replace # defined in tableList
	 * @param tableList defined in json file
	 * @param exportPath 
	 * @param iNIName 
	 */
	public void exportToDivides(String prefix,List<String> tableList,String exportPath,String iNIName,List<String> excludeReturnIds)
	{
		//Boolean flag=false;
		if(StringUtils.isBlank(exportPath)) return;
		if(tableList==null || tableList.size()<=0) return;
		if(StringUtils.isBlank(prefix)){prefix="";}
		String sqlCondition=combineSqlCondition(excludeReturnIds);
		
		logger.info("================= export tables need to be divided by ReturnId =================");
		for(String tab:tableList)
		{
			String SQL="select unique t.table_name from user_tab_cols t where lower(t.table_name)='"+tab.replace("#", prefix).toLowerCase()+"'";
			if(getDbDriverFlag()==DBDriverType.SQLSERVER){
				SQL="select name from sysobjects where xtype='u' and lower(name)=lower('"+tab.replace("#", prefix).toLowerCase()+"')";
			}else if(getDbDriverFlag()==DBDriverType.ACCESSDB){
				SQL="SELECT Name FROM sys.MSysObjects WHERE LCase(Name)='"+tab.replace("#", prefix).toLowerCase()+"'";
			}
			String tableName=queryRecord(SQL);
			if(StringUtils.isNotBlank(tableName))
			{
				logger.info("----------- "+tableName+" ----------- ");
				tab=tab.replace("#", "");
				String subPath=new File(exportPath).getPath()+System.getProperty("file.separator")+tab;
				
				logger.info("export metadata struct to "+iNIName);
				SQL="select * from \""+tableName+"\" where rownum=1";// 
				if(getDbDriverFlag()==DBDriverType.SQLSERVER || getDbDriverFlag()==DBDriverType.ACCESSDB){
					SQL="select top 1 * from "+tableName;
				}
				dbHelper.exportToINI(tab,SQL, new File(exportPath).getPath()+System.getProperty("file.separator")+iNIName);
				String typeReturnId=dbHelper.getColumnType(SQL, "ReturnId");
				SQL="select unique \"ReturnId\" from \""+tableName+"\""+sqlCondition;
				if(getDbDriverFlag()==DBDriverType.SQLSERVER){
					SQL="select distinct \"ReturnId\" from \""+tableName+"\""+sqlCondition;
				}else if(getDbDriverFlag()==DBDriverType.ACCESSDB){
					SQL="SELECT distinct ReturnId FROM "+tableName+sqlCondition;
				}
				List<String> returnIds=queryRecords(SQL);
				if(returnIds!=null)
				{
					FileUtil.createDirectories(subPath);
					for(String returnId:returnIds)
					{
						if(StringUtils.isNotBlank(returnId) &&  !returnId.equalsIgnoreCase("null"))
						{
							logger.info("metadata exports to:"+tab+"_"+returnId+".csv");
							SQL="select * from \""+tableName+"\" where \"ReturnId\"='"+returnId+"'";
							if(getDbDriverFlag()==DBDriverType.SQLSERVER){
								SQL="select * from \""+tableName+"\" where \"ReturnId\"='"+returnId+"'";
							}else if(getDbDriverFlag()==DBDriverType.ACCESSDB && typeReturnId.contains("VARCHAR")){
								SQL="select * from "+tableName+" where ReturnId='"+returnId+"'";
							}else if(getDbDriverFlag()==DBDriverType.ACCESSDB && !typeReturnId.contains("VARCHAR")){
								SQL="select * from "+tableName+" where ReturnId="+returnId;
							}
							dbHelper.exportToCsv(SQL, subPath+System.getProperty("file.separator")+tab+"_"+returnId+".csv");
						}
					}
				}else
				{
					logger.warn("warn: table["+tableName+"] doesn't contains any ReturnId.");
					//logger.debug(" Sql Statement:"+SQL);
				}
				
			}else
			{
				logger.warn("warn: table["+tab.replace("#", prefix)+"] doesn't exist.");
				//logger.debug(" Sql Statement:"+SQL);
			}
		}
		dbHelper.close();
	}
	
	
	/***
	 * export data in database into *.csv files divided by field ReturnId
	 * @param prefix product prefix, use to replace # defined in tableList
	 * @param tableList defined in json file
	 * @param exportPath 
	 * @param iNIName 
	 * @param idOfDBAndTable this value starts with "#", following databaseServerAndTables's ID
	 */
	public void exportToDivides1(String prefix,List<String> tableList,String exportPath,String iNIName,List<String> excludeReturnIds,String idOfDBAndTable)
	{
		//Boolean flag=false;
		if(StringUtils.isBlank(exportPath)) return;
		if(tableList==null || tableList.size()<=0) return;
		if(StringUtils.isBlank(prefix)){prefix="";}
		String sqlCondition=combineSqlCondition(excludeReturnIds);
		logger.info("================= export tables need to be divided by ReturnId =================");
		for(String tab:tableList)
		{
			String SQL="select unique t.table_name from user_tab_cols t where lower(t.table_name)='"+tab.replace("#", prefix).toLowerCase()+"'";
			if(getDbDriverFlag()==DBDriverType.SQLSERVER){
				SQL="select name from sysobjects where xtype='u' and lower(name)=lower('"+tab.replace("#", prefix).toLowerCase()+"')";
			}else if(getDbDriverFlag()==DBDriverType.ACCESSDB){
				SQL="SELECT Name FROM sys.MSysObjects WHERE LCase(Name)='"+tab.replace("#", prefix).toLowerCase()+"'";
			}
			String tableName=queryRecord(SQL);
			if(StringUtils.isNotBlank(tableName))
			{
				logger.info("----------- "+tableName+" ----------- ");
				tab=tab.replace("#", "");
				String subPath=new File(exportPath).getPath()+System.getProperty("file.separator")+tab;
				
				logger.info("export metadata struct to "+iNIName);
				SQL="select * from \""+tableName+"\" where rownum=1";// 
				if(getDbDriverFlag()==DBDriverType.SQLSERVER || getDbDriverFlag()==DBDriverType.ACCESSDB){
					SQL="select top 1 * from "+tableName;
				}
				dbHelper.exportToINI(tab+idOfDBAndTable,SQL, new File(exportPath).getPath()+System.getProperty("file.separator")+iNIName);
				String typeReturnId=dbHelper.getColumnType(SQL, "ReturnId");
				SQL="select unique \"ReturnId\" from \""+tableName+"\""+sqlCondition;
				if(getDbDriverFlag()==DBDriverType.SQLSERVER){
					SQL="select distinct \"ReturnId\" from \""+tableName+"\""+sqlCondition;
				}else if(getDbDriverFlag()==DBDriverType.ACCESSDB){
					SQL="SELECT distinct ReturnId FROM "+tableName+sqlCondition;
				}
				List<String> returnIds=queryRecords(SQL);
				if(returnIds!=null)
				{
					FileUtil.createDirectories(subPath);//with idOfDBAndTable or not
					for(String returnId:returnIds)
					{
						if(StringUtils.isNotBlank(returnId) && !returnId.equalsIgnoreCase("null"))
						{
							logger.info("metadata exports to:"+tab+idOfDBAndTable+"_"+returnId+".csv");
							SQL="select * from \""+tableName+"\" where \"ReturnId\"='"+returnId+"'";
							if(getDbDriverFlag()==DBDriverType.SQLSERVER){
								SQL="select * from \""+tableName+"\" where \"ReturnId\"='"+returnId+"'";
							}else if(getDbDriverFlag()==DBDriverType.ACCESSDB && typeReturnId.contains("VARCHAR")){
								SQL="select * from "+tableName+" where ReturnId='"+returnId+"'";
							}else if(getDbDriverFlag()==DBDriverType.ACCESSDB && !typeReturnId.contains("VARCHAR")){
								SQL="select * from "+tableName+" where ReturnId="+returnId;
							}
							//else if(getDbDriverFlag()==DBDriverType.ACCESSDB){
							//	SQL="select * from "+tableName+" where CStr(ReturnId)=CStr('"+returnId+"')";
							//}
							dbHelper.exportToCsv(SQL, subPath+System.getProperty("file.separator")+tab+idOfDBAndTable+"_"+returnId+".csv");
						}
					}
				}else
				{
					logger.warn("warn: table["+tableName+"] doesn't contains any ReturnId.");
					//logger.debug(" Sql Statement:"+SQL);
				}
				
			}else
			{
				logger.warn("warn: table["+tab.replace("#", prefix)+"] doesn't exist.");
				//logger.debug(" Sql Statement:"+SQL);
			}
		}
		dbHelper.close();
	}
	
	
	@Deprecated
	public void exportToCsv()
	{
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
	 * @return
	 */
	public Boolean createAccessDB()
	{
		Boolean flag=false;
		String dbFullName=dbHelper.getDatabaseServer().getSchema();
		/*if(new File(dbFullName).exists())
		{new File(dbFullName).delete();}*/
		if(!new File(dbFullName).exists()){
			DBHelper.AccessdbHelper accdb=dbHelper.new AccessdbHelper();
			flag=accdb.createAccessDB(dbHelper.getDatabaseServer().getSchema());
		}else{flag=true;}
		return flag;
	}
	
	/***
	 * according to returnId, search its return name and version in Rets table.
	 * this function should be worked on access database.
	 * @param tableName
	 * @param returnId
	 * @return returnName_returnVersion, return "" if error occurs.
	 */
	public String getReturnAndVersion(String returnId)
	{
		String returnAndVer="";
		String tableName="Rets";
		if(getDbDriverFlag()==DBDriverType.ACCESSDB)
		{
			dbHelper.connect();
			DBHelper.AccessdbHelper accdb=dbHelper.new AccessdbHelper();
			if(!accdb.accessTableExistence(tableName))
			{
				logger.error("cannot found "+tableName);
			}else{
				String SQL="SELECT Return & \"_v\" & Version AS Expr1 from ["+tableName+"] WHERE ReturnId="+returnId;
				returnAndVer=dbHelper.query(SQL);
			}
			
			dbHelper.close();
		}else
		{
			logger.error("this function should be worked on access database.");
		}
		if(returnAndVer==null)returnAndVer="";
		
		return returnAndVer;
	}
	
	/**
	 * create all tables which defined in the schemaFullName.
	 * @param schemaFullName
	 * @return
	 */
	public Boolean createAccessTables(String schemaFullName){
		Boolean flag=true;
		if(dbHelper.getDatabaseServer().getDriver().startsWith("access"))
		{
			dbHelper.connect();
			DBHelper.AccessdbHelper accdb=dbHelper.new AccessdbHelper();
			Map<String,List<String>> allTableDefs=FileUtil.getAllTableDefinitions(schemaFullName);
			for(String key:allTableDefs.keySet()){
				Boolean flagT=accdb.createAccessDBTable(key, allTableDefs.get(key));
				if(!flagT){flag=false;logger.error("error: fail to create table ["+key+"]");}
			}
			dbHelper.close();
		}else
		{
			logger.error("this method should be worked on access database.");
			flag=false;
		}
		return flag;
	} 
	/**
	 * create table by schemaFullName which defined tableName, and import data which in csvPath to table.
	 * @param tableName table name in access database
	 * @param tableNameWithDB the value which defined in schemaFullName, which can equal to tableName, or tableName plus databaseServerAndTables's ID
	 * @param csvPath
	 * @param schemaFullName
	 * @return
	 */
	public Boolean importCsvToAccess(String tableName,String tableNameWithDB, String csvPath, String schemaFullName)
	{
		Boolean flag=false;
		if(dbHelper.getDatabaseServer().getDriver().startsWith("access"))
		{
			dbHelper.connect();
			String userSchemaFullName=schemaFullName.replace(FileUtil.getFileNameWithSuffix(schemaFullName), ACCESS_SCHEMA_INI);
			List<TableProps> columns=null;
			columns=findDbTableColumns(tableName);
			if(columns==null){
				flag=FileUtil.search(userSchemaFullName,"["+tableName+"]");
				if(flag){
					columns=FileUtil.getMixedTablesDefinition(FileUtil.searchTablesDefinition(userSchemaFullName, tableName));
					setDbTableColumns(tableName,columns);
				}else{
					flag=FileUtil.search(schemaFullName, "["+tableNameWithDB+"]");
					//List<String> columns=FileUtil.searchTableDefinition(schemaFullName,tableNameWithDB);
					//List<String> columns=FileUtil.getMaxTablesDefinition(FileUtil.searchTablesDefinitionold(schemaFullName, tableName));
					columns=FileUtil.getMixedTablesDefinition(FileUtil.searchTablesDefinition(schemaFullName, tableName));
					setDbTableColumns(tableName,columns);
				}
			}
			if(columns!=null && columns.size()>0)
			{
				DBHelper.AccessdbHelper accdb=dbHelper.new AccessdbHelper();
				
				flag=accdb.createAccessDBTab(tableName, columns);
				if(flag){
					flag=accdb.importCsvToAccessDB(tableName,columns,csvPath);
					
				}else{logger.error("error: fail to create table ["+tableName+"]");}
			}else{
				logger.error("error: invalid table definition ["+tableName+"]");
				flag=false;
			}
			
			dbHelper.close();
		}else
		{
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

	public static Map<String,List<TableProps>> getDbTableColumns() {
		return dbTableColumns;
	}

	public static void setDbTableColumns(String tableName,List<TableProps> columns) {
		DBInfo.dbTableColumns.put(tableName, columns);
	}
	
	public static List<TableProps> findDbTableColumns(String tableName){
		if(! DBInfo.dbTableColumns.isEmpty()){
			for(String key:DBInfo.dbTableColumns.keySet()){
				if(key.equals(tableName)){
					return DBInfo.dbTableColumns.get(key);
				}
			}
		}
		return null;
	}

}
