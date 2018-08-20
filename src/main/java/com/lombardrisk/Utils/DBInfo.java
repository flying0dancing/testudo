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
	
	public DBHelper getDbHelper() {
		return dbHelper;
	}

	public void setDbHelper(DatabaseServer databaseServer) {
		this.dbHelper =new DBHelper(databaseServer);
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
		
		logger.info("export single tables.");
		for(String tab:tableList)
		{
			String SQL="select unique t.table_name from user_tab_cols t where lower(t.table_name)='"+tab.replace("#", prefix).toLowerCase()+"'";
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
				logger.debug("table["+tableName+"] is found. export metadata struct to "+iNIName);
				SQL="select * from \""+tableName+"\" where rownum=1";
				dbHelper.exportToINI(tab,SQL, new File(exportPath).getPath()+System.getProperty("file.separator")+iNIName);
				logger.info("metadata exports to:"+tab+".csv");
				SQL="select * from \""+tableName+"\"";
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
		//else{prefix=prefix.toLowerCase();}
		
		logger.info("export tables divided by field [ReturnId].");
		
		for(String tab:tableList)
		{
			String SQL="select unique t.table_name from user_tab_cols t where lower(t.table_name)='"+tab.replace("#", prefix).toLowerCase()+"'";
			String tableName=queryRecord(SQL);
			if(StringUtils.isNotBlank(tableName))
			{
				tab=tab.replace("#", "");
				String subPath=new File(exportPath).getPath()+System.getProperty("file.separator")+tab;
				if(new File(subPath).exists())
				{
					logger.warn("warn: duplicated ["+tab+"] in json, overwrite existed one.");
					//continue;
				}
				logger.debug("table["+tableName+"] is found. export metadata struct to "+iNIName);
				SQL="select * from \""+tableName+"\" where rownum=1";
				dbHelper.exportToINI(tab,SQL, new File(exportPath).getPath()+System.getProperty("file.separator")+iNIName);
				SQL="select unique \"ReturnId\" from \""+tableName+"\"";
				List<String> returnIds=queryRecords(SQL);
				if(returnIds!=null)
				{
					FileUtil.createDirectories(subPath);
					for(String returnId:returnIds)
					{
						if(StringUtils.isNotBlank(returnId) && returnId!="null")
						{
							logger.info("metadata exports to:"+tab+"_"+returnId+".csv");
							SQL="select unique * from \""+tableName+"\" where \"ReturnId\"='"+returnId+"'";
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
			dbHelper.createAccessDB(dbHelper.getDatabaseServer().getSchema());
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
		if(dbHelper.getDatabaseServer().getDriver().startsWith("access"))
		{
			dbHelper.connect();
			if(!dbHelper.accessTableExistence("Rets"))
			{
				logger.error("cannot found Rets");
			}else{
				String SQL="SELECT Return & \"_v\" & Version AS Expr1 from [Rets] WHERE ReturnId="+returnId;
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
					dbHelper.createAccessTable(tableName,columns);
					flag=dbHelper.importCsvToAccessDB(tableName,csvPath);
				}else{logger.error("error: invalid table definition ["+tableName+"]");}
			}
			
			dbHelper.close();
		}else
		{
			logger.error("this method works on access database.");
		}
		
		return flag;
	}
	
	/**
	 * get Regulator DESCRIPTION list
	 * @return
	 */
	public List<String> getRegulatorDescription()
	{
		String SQL="SELECT \"DESCRIPTION\" FROM \"CFG_INSTALLED_CONFIGURATIONS\" WHERE \"STATUS\"='A' ";
		return queryRecords(SQL);
	}
	
	/**
	 * get Regulator Prefix like HKMA/FED/MAS
	 * @author kun shen
	 * @param regulator
	 * @return
	 */
	public String getRegulatorPrefix(String regulator)
	{
		String SQL = "SELECT \"PREFIX\" FROM \"CFG_INSTALLED_CONFIGURATIONS\" WHERE lower(\"DESCRIPTION\")='" + regulator.toLowerCase() + "'  AND \"STATUS\"='A' ";
		return queryRecord(SQL);

	}
	
	
	/**
	 * get Regulator IDRange Start
	 * 
	 * @param regulator
	 * @return IDRangeStart
	 */
	public String getRegulatorIDRangeStart(String regulator)
	{
		String SQL = "SELECT \"ID_RANGE_START\" FROM \"CFG_INSTALLED_CONFIGURATIONS\" WHERE lower(\"DESCRIPTION\")='" + regulator.toLowerCase() + "'  AND \"STATUS\"='A' ";
		return queryRecord(SQL);

	}
	
	/**
	 * get Regulator IDRange End
	 * 
	 * @param Regulator
	 * @return IDRangeEnd
	 */
	public String getRegulatorIDRangEnd(String regulator)
	{
		String SQL = "SELECT \"ID_RANGE_END\" FROM \"CFG_INSTALLED_CONFIGURATIONS\" WHERE lower(\"DESCRIPTION\")='" + regulator.toLowerCase() + "' AND \"STATUS\"='A'  ";
		return queryRecord(SQL);
	}
	

	
	
	public void resetDeActivateDate()
	{
		//String ID_Start = getRegulatorIDRangeStart(regulator);
		//String ID_End = getRegulatorIDRangEnd(regulator);
		//String SQL="update \"CFG_RPT_Rets\" set \"DeActivateDate\" =null where ID BETWEEN "+ID_Start+" and "+ID_End+" and \"DeActivateDate\" is not null ";
		String SQL="update \"CFG_RPT_Rets\" set \"DeActivateDate\" =null where \"DeActivateDate\" is not null ";
		update(SQL);
	}

	


}
