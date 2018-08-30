package com.lombardrisk.Utils;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lombardrisk.pojo.DatabaseServer;


public class DBInfo {
	private final static Logger logger = LoggerFactory.getLogger(DBInfo.class);
	private DBHelper dbHelper;
	private DBDriverType dbDriverFlag;
	public enum DBDriverType{ORACLE,SQLSERVER,ACCESSDB;}
	
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
	
	public void exportToSingle(String prefix,List<String> tableList,String exportPath,String iNIName)
	{
		if(tableList==null) return;
		if(StringUtils.isBlank(prefix)){prefix="";}
		//else{prefix=prefix.toLowerCase();}
		
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
				tab=tab.replace("#", "");
				String exportFullPath=exportPath+System.getProperty("file.separator")+tab+".csv";
				if(new File(exportFullPath).exists())
				{
					logger.warn("warn: duplicated ["+tab+"] in configuration file, overwriting existed one.");
					//continue;
				}
				logger.info("table["+tableName+"] is found. export metadata struct to "+iNIName);
				
				SQL="select * from \""+tableName+"\"";// 
				if(getDbDriverFlag()==DBDriverType.SQLSERVER || getDbDriverFlag()==DBDriverType.ACCESSDB){
					SQL="select * from "+tableName;
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
	
	public void exportToDivides(String prefix,List<String> tableList,String exportPath,String iNIName)
	{
		//Boolean flag=false;
		if(tableList==null || StringUtils.isBlank(exportPath)) return;
		if(StringUtils.isBlank(prefix)){prefix="";}
		
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
				tab=tab.replace("#", "");
				String subPath=new File(exportPath).getPath()+System.getProperty("file.separator")+tab;
				if(new File(subPath).exists())
				{
					logger.warn("warn: duplicated ["+tab+"], overwrite existed one.");
					//continue;
				}
				logger.info("table["+tableName+"] is found. export metadata struct to "+iNIName);
				SQL="select * from \""+tableName+"\" where rownum=1";// 
				if(getDbDriverFlag()==DBDriverType.SQLSERVER || getDbDriverFlag()==DBDriverType.ACCESSDB){
					SQL="select top 1 * from "+tableName;
				}
				dbHelper.exportToINI(tab,SQL, new File(exportPath).getPath()+System.getProperty("file.separator")+iNIName);
				SQL="select unique \"ReturnId\" from \""+tableName+"\"";
				if(getDbDriverFlag()==DBDriverType.SQLSERVER){
					SQL="select distinct \"ReturnId\" from \""+tableName+"\"";
				}else if(getDbDriverFlag()==DBDriverType.ACCESSDB){
					SQL="SELECT distinct ReturnId FROM "+tableName;
				}
				List<String> returnIds=queryRecords(SQL);
				if(returnIds!=null)
				{
					FileUtil.createDirectories(subPath);
					for(String returnId:returnIds)
					{
						if(StringUtils.isNotBlank(returnId) && returnId!="null")
						{
							logger.info("metadata exports to:"+tab+"_"+returnId+".csv");
							SQL="select * from \""+tableName+"\" where \"ReturnId\"='"+returnId+"'";
							if(getDbDriverFlag()==DBDriverType.SQLSERVER){
								SQL="select * from \""+tableName+"\" where \"ReturnId\"='"+returnId+"'";
							}else if(getDbDriverFlag()==DBDriverType.ACCESSDB){
								SQL="select * from "+tableName+" where CStr(ReturnId)=CStr('"+returnId+"')";
							}
							dbHelper.exportToCsv(SQL, subPath+System.getProperty("file.separator")+tab+"_"+returnId+".csv");
						}
					}
				}else
				{
					logger.warn("warn: table["+tableName+"] doesn't contains ReturnId field.");
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
			accdb.createAccessDB(dbHelper.getDatabaseServer().getSchema());
		}
		return flag;
	}
	
	/***
	 * according to returnId, search its return name and version in Rets table.
	 * this function works on access database.
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
			logger.error("this function works on access database.");
		}
		if(returnAndVer==null)returnAndVer="";
		
		return returnAndVer;
	}
	
	/**
	 * create table by schemaFullName which defined tableName, and import data which in csvPath to table.
	 * @param tableName
	 * @param csvPath
	 * @param schemaFullName
	 * @return
	 */
	public Boolean ImportCsvToAccess(String tableName, String csvPath, String schemaFullName)
	{
		Boolean flag=false;
		if(dbHelper.getDatabaseServer().getDriver().startsWith("access"))
		{
			dbHelper.connect();
			flag=FileUtil.search(schemaFullName, "["+tableName+"]");
			if(flag)
			{
				List<String> columns=FileUtil.searchTableDefinition(schemaFullName,tableName);
				if(columns!=null && columns.size()>0)
				{
					DBHelper.AccessdbHelper accdb=dbHelper.new AccessdbHelper();
					accdb.createAccessTable(tableName,columns);
					flag=accdb.importCsvToAccessDB(tableName,csvPath);
				}else{logger.error("error: invalid table definition ["+tableName+"]");}
			}
			
			dbHelper.close();
		}else
		{
			logger.error("this method works on access database.");
		}
		
		return flag;
	}

	public DBDriverType getDbDriverFlag() {
		return dbDriverFlag;
	}

	public void setDbDriverFlag(DBDriverType dbDriverFlag) {
		this.dbDriverFlag = dbDriverFlag;
	}
	

}
