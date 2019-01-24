package com.lombardrisk.Utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.healthmarketscience.jackcess.ColumnBuilder;
import com.healthmarketscience.jackcess.Cursor;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;
import com.healthmarketscience.jackcess.TableBuilder;
import com.healthmarketscience.jackcess.util.ImportUtil;
import com.healthmarketscience.jackcess.util.ImportUtil.Builder;
import com.lombardrisk.pojo.DatabaseServer;
import com.lombardrisk.pojo.TableProps;

public class DBHelper {

	private final static Logger logger = LoggerFactory.getLogger(DBHelper.class);
	private String dbmsDriver;
	private Connection conn = null;
	private DatabaseServer databaseServer;
	
	public DBHelper(DatabaseServer databaseServer)
	{
		this.databaseServer=databaseServer;
		
		String dbms=this.databaseServer.getDriver().toLowerCase();
		
		String[] hostsid=null;
		String host=null, sid=null;
		if(StringUtils.isNotBlank(this.databaseServer.getHost()))
		{
			hostsid=this.databaseServer.getHost().split("@|\\\\|#");
			host=hostsid[0];
			sid=hostsid[1];
		}
		//sql database
		if(StringUtils.isNotBlank(dbms) && dbms.startsWith("sql"))
		{
			dbmsDriver="net.sourceforge.jtds.jdbc.Driver";
			if(StringUtils.isBlank(this.databaseServer.getUsername())){
				this.databaseServer.setUsername("sa");
			}
			if(StringUtils.isBlank(this.databaseServer.getPassword())){
				this.databaseServer.setPassword("password");
			}
			if(StringUtils.isBlank(this.databaseServer.getUrl())){
				if(hostsid.length==2){
					this.databaseServer.setUrl(String.format("jdbc:jtds:sqlserver://%s:%s/%s;instance=%s", host, "1433", this.databaseServer.getSchema(),sid));
				}else
				{
					this.databaseServer.setUrl(String.format("jdbc:jtds:sqlserver://%s:%s/%s", host, "1433", this.databaseServer.getSchema()));
				}
			}
		}
		//oracle database
		if(StringUtils.isNotBlank(dbms) && dbms.startsWith("ora"))
		{
			dbmsDriver="oracle.jdbc.driver.OracleDriver";
			if(StringUtils.isBlank(this.databaseServer.getUsername())){
				this.databaseServer.setUsername(this.databaseServer.getSchema());
			}
			if(StringUtils.isBlank(this.databaseServer.getPassword())){
				this.databaseServer.setPassword("password");
			}
			if(StringUtils.isBlank(this.databaseServer.getUrl())){
				this.databaseServer.setUrl(String.format("jdbc:oracle:thin:@%s:%s:%s", host, "1521", sid));
			}
		}
		//access database
		if(StringUtils.isNotBlank(dbms) && dbms.startsWith("access"))
		{
			dbmsDriver="net.ucanaccess.jdbc.UcanaccessDriver";
			if(StringUtils.isBlank(this.databaseServer.getUrl())){
				this.databaseServer.setUrl(String.format("jdbc:ucanaccess://%s;memory=true;sysSchema=TRUE;columnOrder=DISPLAY", this.databaseServer.getSchema()));
			}
		}
	}

	public DatabaseServer getDatabaseServer() {
		return databaseServer;
	}
	public void setDatabaseServer(DatabaseServer databaseServer) {
		this.databaseServer = databaseServer;
	}

	public Boolean connect()
	{
		if (getConn() != null) return false;
		Boolean flag=false;
		flag=DbUtils.loadDriver(dbmsDriver);
		try
		{
			if(StringUtils.isNoneBlank( this.databaseServer.getUsername(),this.databaseServer.getPassword())){
				setConn(DriverManager.getConnection(this.databaseServer.getUrl(), this.databaseServer.getUsername(), this.databaseServer.getPassword()));
			}else{
				setConn(DriverManager.getConnection(this.databaseServer.getUrl()));
			}
			
		}
		catch (SQLException e)
		{
			logger.error("Database connection failed!");
			logger.error(e.getMessage(),e);
			flag=false;
		}
		
		return flag;
	}

	public void close()
	{
		try
		{
			if(getConn()!=null)
			{
				DbUtils.close(getConn());
				setConn(null);
			}
		}catch (SQLException e)
		{
			logger.error("Database close failed!");
			logger.error(e.getMessage(),e);
		}
	}
	
	

	public String query(String sql)
	{
		if (getConn() == null)
			return null;
		logger.debug("Sql Statement: [" + sql + "]");
		String value = null;
		try
		{
			ResultSet rs = null;
			Statement stmt = getConn().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);//
			rs = stmt.executeQuery(sql);
			if(rs.getRow()>=0)
			{
				ResultSetMetaData rsmd = rs.getMetaData();
				while (rs.next())
				{
					String type = rsmd.getColumnClassName(1).toString();
					if (type.equals("oracle.jdbc.OracleClob"))
						value = rs.getClob(1).getSubString((long) 1, (int) rs.getClob(1).length());
					else if (type.equals("java.math.BigDecimal"))
						value = String.valueOf(rs.getBigDecimal(1));
					else
						value = rs.getString(1);
				}
			}
				
		}
		catch (SQLException e)
		{
			logger.error("SQLException in [" + sql + "]");
			logger.error(e.getMessage(),e);
			value = null;
		}
		return value;
	}
	

	public List<String> queryRecords(String sql)
	{
		if (getConn() == null)
			return null;
		logger.debug("Sql Statement: [" + sql + "]");
		ArrayList<String> rst =null;
		try
		{
			rst=new ArrayList<String>();
			ResultSet rs = null;
			Statement stmt = getConn().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery(sql);
			ResultSetMetaData rsmd = rs.getMetaData();
			while (rs.next())
			{
				String type = rsmd.getColumnClassName(1).toString();
				if (type.equals("oracle.jdbc.OracleClob"))
					rst.add(rs.getClob(1).getSubString((long) 1, (int) rs.getClob(1).length()));
				else if (type.equals("java.math.BigDecimal"))
					rst.add(String.valueOf(rs.getBigDecimal(1)));
				else
					rst.add(rs.getString(1));
			}
		}
		catch(IndexOutOfBoundsException e)
		{
			logger.error("ResultSet is null in [" + sql + "]");
			logger.error(e.getMessage(),e);
			rst=null;
		}
		catch (SQLException e)
		{
			logger.error("SQLException in [" + sql + "]");
			logger.error(e.getMessage(),e);
			rst=null;
		}
		

		return rst;
	}
	
	/**
	 * convert database's data type into ini's data type
	 * @param columnTypeName
	 * @param precision number
	 * @param scale
	 * @return
	 */
	public String convertTypeStr(String columnTypeName, int precision, int scale)
	{
		String type=null;
		columnTypeName=columnTypeName.toUpperCase();
		if(columnTypeName.contains("VARCHAR")){
			if(precision>255){
				type=" LONGTEXT";
			}else{
				type=" VARCHAR("+String.valueOf(precision)+")";
			}
		}else 
		if(columnTypeName.contains("CLOB") || columnTypeName.contains("TEXT") || columnTypeName.contains("MEMO") || columnTypeName.contains("HYPERLINK")){
			type=" LONGTEXT";
		}else 
		if(columnTypeName.contains("DATE") || columnTypeName.contains("TIME")){
			type=" DATE";
		}else 
		if(columnTypeName.contains("BIT") ||columnTypeName.contains("BOOLEAN")){//|| (columnTypeName.contains("NUMBER") && precision==1 && scale==0) in oracle is bit
			type=" BOOLEAN";
		}else 
		if(columnTypeName.contains("INT")){
			type=" LONG";
			/*if(precision<=16){
				type=" INTEGER";
			}else{
				type=" LONG";
			}*/
		}else 
		if(columnTypeName.contains("NUMBER")){
			if(scale==0){
				/*if(1==precision){
					type=" BOOLEAN";
				}else if(1<precision && precision<=38){
					type=" INTEGER";
				}else{
					type=" LONG";
				}*/
				if(1==precision){
					type=" BOOLEAN";
				}else{
					type=" LONG";
				}
			}else if(scale<10){
				type=" DOUBLE";
			}else{
				if(precision>28){
					type=" DOUBLE";
				}else{
					type=" NUMERIC("+String.valueOf(precision)+","+String.valueOf(scale)+")";
				}
				//type=" NUMERIC("+String.valueOf(precision)+","+String.valueOf(scale)+")";
			}
			
		}else 
		if(columnTypeName.contains("REAL") ||columnTypeName.contains("SINGLE")){
			type=" SINGLE";
		}else 
		if(columnTypeName.contains("FLOAT") ||columnTypeName.contains("DOUBLE")){
			type=" DOUBLE";
		}else 
		if(columnTypeName.contains("DECIMAL") || columnTypeName.contains("NUMERIC")){
			if(precision>28){
				type=" DOUBLE";
			}else{
				type=" NUMERIC("+String.valueOf(precision)+","+String.valueOf(scale)+")";
			}
			
		}else{
			if(precision>255){
				type=" LONGTEXT";
			}else{
				type=" VARCHAR("+String.valueOf(precision)+")";
			}
			
		}
		return type;
	}
	

	public void exportToINI(String tableName,String sql,String fileFullName)
	{
		if (getConn() == null) {connect();}
		try{
			Statement state=getConn().createStatement();
			ResultSet rest=state.executeQuery(sql);
			ResultSetMetaData rsmd=rest.getMetaData();
			
			logger.debug("No of columns in the table:"+ rsmd.getColumnCount());
			StringBuffer strBuf=new StringBuffer();
			//csv struct
			strBuf.append("["+tableName+"]"+System.getProperty("line.separator"));
			for(int i=1;i<=rsmd.getColumnCount();i++)
			{
				strBuf.append("col"+String.valueOf(i)+"="+rsmd.getColumnName(i)+convertTypeStr(rsmd.getColumnTypeName(i),rsmd.getPrecision(i),rsmd.getScale(i))+(rsmd.isNullable(i)==0?"":" Nullable")+System.getProperty("line.separator"));
				//strBuf.append("col"+String.valueOf(i)+"="+rsmd.getColumnName(i)+","+rsmd.getColumnClassName(i)+","+rsmd.getColumnTypeName(i)+","+rsmd.getPrecision(i)+","+rsmd.getScale(i)+","+(rsmd.isNullable(i)==0?"":" Nullable")+System.getProperty("line.separator"));
				
			}
			FileUtil.updateContent(fileFullName, "["+tableName+"]", strBuf.toString());

		}catch(SQLException e)
		{
			logger.error("error: SQLException in [" + sql + "]");
			logger.error(e.getMessage(),e);
		}
		catch(Exception e){
			logger.error("error: Exception in [" + sql + "]");
			logger.error(e.getMessage(),e);
		}finally{
			close();
		}
		
	}
	
	public Boolean existColumn(String sql,String column)
	{
		Boolean flag=false;
		if (getConn() == null) {connect();}
		try{
			Statement state=getConn().createStatement();
			ResultSet rest=state.executeQuery(sql);
			ResultSetMetaData rsmd=rest.getMetaData();
			
			for(int i=1;i<=rsmd.getColumnCount();i++)
			{
				if(rsmd.getColumnName(i).equalsIgnoreCase(column)){
					flag=true;
					break;
				}
			}
		}catch(SQLException e)
		{
			logger.error("error: SQLException in [" + sql + "]");
			logger.error(e.getMessage(),e);
		}
		catch(Exception e){
			logger.error("error: Exception in [" + sql + "]");
			logger.error(e.getMessage(),e);
		}finally{
			close();
		}
		return flag;
	}
	/**
	 * execute sql and get result into fileFullName(comma limit), if fileFullName exists, overwrite it.
	 * @param sql
	 * @param fileFullName
	 */
	public void exportToCsv(String sql,String fileFullName)
	{
		if (getConn() == null){connect();}
		FileWriter csvName=null;
		BufferedWriter bufOutFile=null;
		try{
			Statement state=getConn().createStatement();
			ResultSet rest=state.executeQuery(sql);
			ResultSetMetaData rsmd=rest.getMetaData();
			
			csvName=new FileWriter(fileFullName);
			bufOutFile=new BufferedWriter(csvName);
			logger.debug("start \"export to csv\"");
			StringBuffer strBuf=new StringBuffer();
			//csv header
			int col;
			for(col=1;col<rsmd.getColumnCount();col++)
			{
				strBuf.append("\""+rsmd.getColumnName(col)+"\",");
			}
			strBuf.append("\""+rsmd.getColumnName(col)+"\"");
			
			bufOutFile.append(strBuf);
			bufOutFile.append(System.getProperty("line.separator"));
			//csv data
			String value=null;
			while(rest.next()){

				for(col=1;col<=rsmd.getColumnCount();col++)
				{
					//logger.info(rsmd.getColumnName(col)+" : "+rsmd.getColumnClassName(col));
					String classvar=rsmd.getColumnClassName(col);
					@SuppressWarnings("unused")
					String colTypevar=rsmd.getColumnTypeName(col);
					if(classvar.contains("Blob"))
					{
						value=Helper.convertBlobToStr(rest.getBlob(col));
					}else if(classvar.contains("Timestamp"))
					{
						value=StringUtils.isBlank(rest.getString(col))?"":rest.getString(col);
						value=value.replaceAll("(\")", "\"$1").replaceAll("\\.*", "");
					}else if(classvar.contains("Decimal")){
						
						value=StringUtils.isBlank(rest.getString(col))?"":rest.getBigDecimal(col).toPlainString();
						
					}else if(classvar.contains("Int") || classvar.contains("Boolean")){
						
						value=StringUtils.isBlank(rest.getString(col))?"":rest.getString(col).replaceAll("(\")", "\"$1");
						
					}else {
						
						value=StringUtils.isBlank(rest.getString(col))?"":"\""+rest.getString(col).replaceAll("(\")", "\"$1")+"\"";
					}
					
					if(col!=rsmd.getColumnCount())
					{
						bufOutFile.append(value+",");
					}else{
						bufOutFile.append(value);
					}
				}
				
				bufOutFile.append(System.getProperty("line.separator"));
				bufOutFile.flush();
			}
			logger.info("export to csv completely.");
		}catch(SQLException e)
		{
			logger.error("error: SQLException in [" + sql + "]");
			logger.error(e.getMessage(),e);
		}
		catch(Exception e){
			logger.error("error: Exception in [" + sql + "]");
			logger.error(e.getMessage(),e);
		}finally{
			close();
			try {
				bufOutFile.close();
				csvName.close();
			} catch (IOException e) {
				logger.error("error: fail to closing file handlers.");
				logger.error(e.getMessage(),e);
			}
		}
		
	}
	
	public Boolean addBatch(String sql){
		Boolean flag=false;
		if (getConn() == null)
			return flag;
		try {
			getConn().setAutoCommit(false);
			Statement statement=getConn().createStatement();
			String sqlLow=sql.trim().toLowerCase().replaceAll("(\\s)+", "$1");
			if(sqlLow.startsWith("update") || (sqlLow.contains("create") && sqlLow.contains("select") && sqlLow.contains("with"))){
				statement.executeUpdate(sql);
				flag=true;
			}else if(sqlLow.startsWith("alter")){
				String tableName="";
				String newTableName="";
				String regexDROP="alter\\s+TABLE\\s+\\[?(\\w+)\\]?\\s+rename\\s+to\\s+\\[?(\\w+)\\]?";
				Pattern pattern=Pattern.compile(regexDROP,Pattern.CASE_INSENSITIVE);
				Matcher match=pattern.matcher(sql);
				if(match.find()){
					tableName=match.group(1);
					newTableName=match.group(2);
				}
				DBHelper.AccessdbHelper accessDBH=this.new AccessdbHelper();
				flag=accessDBH.renameAccessDBTable( tableName,newTableName);
			}else if(sqlLow.startsWith("drop")){
				String tableName="";
				String regexDROP="DROP\\s+TABLE\\s+\\[?(\\w+)\\]?";
				Pattern pattern=Pattern.compile(regexDROP,Pattern.CASE_INSENSITIVE);
				Matcher match=pattern.matcher(sql);
				if(match.find()){
					tableName=match.group(1);
				}
				DBHelper.AccessdbHelper accessDBH=this.new AccessdbHelper();
				flag=accessDBH.deleteAccessDBTable(tableName);
			}else{
				statement.execute(sql);//result false is not quite sure
				flag=true;
			}
			getConn().commit();
		} catch (SQLException e) {
			logger.error("SQLException in [" + sql + "]");
			logger.error(e.getMessage(),e);
			flag=false;
		}
		return flag;
	}
	/**
	 * Execute an SQL INSERT, UPDATE, or DELETE query without replacement parameters
	 * @param sql
	 * @return The number of rows updated. if error occurs return 0;
	 */
	public int update(String sql)
	{
		if (getConn() == null)
			return 0;
		QueryRunner run = new QueryRunner();
		int result = 0;

		try
		{
			result = run.update(getConn(), sql);
		}
		catch (SQLException e)
		{
			logger.error("SQLException in [" + sql + "]");
			logger.error(e.getMessage(),e);
		}

		return result;
	}

	public void setConn(Connection conn)
	{
		this.conn = conn;
	}
	
	public Connection getConn()
	{
		return this.conn;
	}

	@Override
	protected void finalize() throws Throwable
	{
		close();
		super.finalize();
	}
	
	public class AccessdbHelper{
	
		/***
		 * existence of access table
		 * @param tableName
		 * @return
		 */
		public Boolean accessTableExistence(String tableName)
		{
			Boolean flag=false;
			try {
				String dbFullName=getDatabaseServer().getSchema();
				Database db=DatabaseBuilder.open(new File(dbFullName));

				if(db.getTable(tableName)!=null)
				{
					logger.debug("accessdb table["+tableName+"] already exists.");
					flag=true;
				}
				db.close();
				
			} catch (IOException e) {
				logger.error(e.getMessage(),e);
			}
			
			return flag;
		}
		private List<String> getIntersectNames(List<String> dbTabHeaders, List<String> csvHeaders){
			List<String> intersectNames=null;
			if(dbTabHeaders!=null && dbTabHeaders.size()>0 && csvHeaders!=null && csvHeaders.size()>0){
				intersectNames=new ArrayList<String>();
				List<String> largerOne=dbTabHeaders;
				List<String> smallOne=csvHeaders;
				if(dbTabHeaders.size()<csvHeaders.size()){
					largerOne=csvHeaders;
					smallOne=dbTabHeaders;
				}
				for(int i=0;i<smallOne.size();i++){
					if(largerOne.contains(smallOne.get(i))){
						intersectNames.add(smallOne.get(i));
					}
				}
			}
			return intersectNames;
		}
		private List<String> getIntersectNames(Set<String> dbTabHeaders, Set<String> csvHeaders){
			List<String> intersectNames=null;
			if(dbTabHeaders!=null && dbTabHeaders.size()>0 && csvHeaders!=null && csvHeaders.size()>0){
				intersectNames=new ArrayList<String>();
				Set<String> largerOne=dbTabHeaders;
				Set<String> smallOne=csvHeaders;
				if(dbTabHeaders.size()<csvHeaders.size()){
					largerOne=csvHeaders;
					smallOne=dbTabHeaders;
				}
				for(String small:smallOne){
					if(largerOne.contains(small)){
						intersectNames.add(small);
					}
				}
			}
			
			return intersectNames;
		}
		
		/**
		 * import csv to access table
		 * @param tableName
		 * @param importCsvFullName
		 * @return
		 */
		@SuppressWarnings("unchecked")
		public Boolean importCsvToAccessDB(String tableName,List<TableProps> columns,String importCsvFullName)
		{
			Boolean flag=false;
			try {
				String dbFullName=getDatabaseServer().getSchema();
				Database db=DatabaseBuilder.open(new File(dbFullName));
				Builder builder=new ImportUtil.Builder(db, tableName);
				if(db.getTable(tableName)!=null)
				{
					/*List<ColumnImpl> dbTableColumns=(List<ColumnImpl>) db.getTable(tableName).getColumns();
					for(int i=0;i<dbTableColumns.size();i++){
						
					}*/
					db.close();
					logger.debug("accessdb table["+tableName+"] already exists.");
					List<String> dbTableColumns=new ArrayList<String>();
					Map<String,String> columnsDefs=new HashMap<String,String>(); // key: columns's name, value: column's type
					String type;
					for(int j=0;j<columns.size();j++){
						dbTableColumns.add(columns.get(j).getName());
						type=columns.get(j).getTypeSize();
						if(type.contains("(")){
							type=type.substring(0,type.indexOf("("));
						}
						columnsDefs.put(columns.get(j).getName(), type);
					}
					//TODO
					if(StringUtils.isNoneBlank(importCsvFullName,tableName)){
						//
						Reader in = new FileReader(importCsvFullName);
						CSVParser parser = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
						List<String> finalHeaders=getIntersectNames(dbTableColumns,new ArrayList<String>(parser.getHeaderMap().keySet()));
						if(finalHeaders==null || finalHeaders.size()==0){
							return flag;
						}
						int lineno=1;
						String header="";
						String line=null;
						String value=null;
						String sql;
						StringBuffer lineBuffer=new StringBuffer();
						String regex="((\\d+[\\-\\\\/]\\d+[\\-\\\\/]\\d+)(?: \\d+\\:\\d+\\:\\d+)?)";//re=",((\d+[\-\\\/]\d+[\-\\\/]\d+)(?: \d+\:\d+\:\d+)?)," match format of date time
						header="insert into ["+tableName+"] (";
						for(int i=0;i<finalHeaders.size();i++){
							header=header+" ["+finalHeaders.get(i)+"],";
						}
						header=header.replaceAll(",$", " ) values "); // like insert into [InstanceSets] ([InstSetId],[InstSetName] ) values 
						List<String> strTypes=new ArrayList<String>(Arrays.asList("MEMO","LONGTEXT","VARCHAR"));
						List<CSVRecord> records=parser.getRecords();
						for(CSVRecord record : records){
							line="(";
							for(int i=0;i<finalHeaders.size();i++){
								value=record.get(finalHeaders.get(i));
								type=columnsDefs.get(finalHeaders.get(i)).toUpperCase();
								if(StringUtils.isBlank(value)){
									value="null";
								}else if(strTypes.contains(type)){
									value=value.replaceAll("(\")", "\"$1");// some values are contains ", changed to ""
									value="\""+value+"\"";//adding "
									//value="\"\""+value+"\"\"";
								}else if(type.startsWith("DATE")){
									value=value.replaceAll(regex, "#$2#");
								}
								line=line+value+",";
							}
							line=line.replaceAll(",$", ")");
							lineBuffer.append(line+",");
							if(lineno%200==0){
								sql=header+lineBuffer.substring(0, lineBuffer.length()-1);
								lineBuffer.setLength(0);//clear
								logger.debug(sql);
								flag=addBatch(sql);
								if(!flag){logger.error("fail to import data into:"+tableName+" ( "+(lineno-100)+"-"+lineno+" )");break;}
							}
							lineno++;
						}
						parser.close();
						if(lineBuffer.length()>0){
							sql=header+lineBuffer.substring(0, lineBuffer.length()-1);
							logger.debug(sql);
							flag=addBatch(sql);
							if(!flag){
								if(lineno>=100){
									logger.error("fail to import data into:"+tableName+" ( "+(lineno-100)+"-"+lineno+" )");
								}else{
									logger.error("fail to import data into:"+tableName+" ( 0-"+lineno+" )");
								}
							}
						}
						if(lineno==1){
							flag=true;
						}
						
					}
						
				}else{
					logger.warn("accessdb table["+tableName+"] doesn't exist, import csv directly.");
					builder.setUseExistingTable(false);
					builder.setDelimiter(",").setHeader(true).importReader(new BufferedReader(new FileReader(new File(importCsvFullName))));
					flag=true;
					db.close();
				}
				
			} catch (IOException e) {
				logger.error(e.getMessage(),e);
			}
			return flag;
		}
		/**
		 * Deprecated
		 * import csv to access table
		 * @param tableName
		 * @param importCsvFullName
		 * @return
		 */
		public Boolean importCsvToAccessDB(String tableName,String importCsvFullName)
		{
			Boolean flag=false;
			BufferedReader bufReader=null;
			try {
				String dbFullName=getDatabaseServer().getSchema();
				Database db=DatabaseBuilder.open(new File(dbFullName));
				Builder builder=new ImportUtil.Builder(db, tableName);
				if(db.getTable(tableName)!=null)
				{
					db.close();
					logger.debug("accessdb table["+tableName+"] already exists.");
					
					//TODO
					if(StringUtils.isNoneBlank(importCsvFullName,tableName)){
						//
						bufReader=new BufferedReader(new FileReader(importCsvFullName));
						StringBuffer lineBuffer=new StringBuffer();
						String line=null;
						int lineno=1;
						String header="";
						String sql;
						while((line=bufReader.readLine())!=null){
							 header="insert into ["+tableName+"] ("+line.replaceAll("(^|,)\"", "$1[").replaceAll("\"(,|$)", "]$1")+" ) values ";
							 break;
						}
						while((line=bufReader.readLine())!=null)
						{
							lineno++;
							if(StringUtils.isBlank(line))continue;
							String regex="((\\d+[\\-\\\\/]\\d+[\\-\\\\/]\\d+)(?: \\d+\\:\\d+\\:\\d+)?)";//re=",((\d+[\-\\\/]\d+[\-\\\/]\d+)(?: \d+\:\d+\:\d+)?)," match format of date time
							line=line.replaceAll("(^|,)(,|$)", "$1null$2").replaceAll(regex, "#$2#").replaceAll("(^|,)(,|$)", "$1null$2");//TODO may contains risk in ,,
							lineBuffer.append("("+line+"),");
							if(lineno%200==0){
								sql=header+lineBuffer.substring(0, lineBuffer.length()-1);
								lineBuffer.setLength(0);//clear
								logger.debug(sql);
								flag=addBatch(sql);
								if(!flag){logger.error("fail to import data into:"+tableName+" ( "+(lineno-100)+"-"+lineno+" )");break;}
							}
						}
						if(lineBuffer.length()>0){
							sql=header+lineBuffer.substring(0, lineBuffer.length()-1);
							logger.debug(sql);
							flag=addBatch(sql);
							if(!flag){logger.error("fail to import data into:"+tableName+" ( "+(lineno-100)+"-"+lineno+" )");}
						}
						if(lineno==1){
							flag=true;
						}
						bufReader.close();
					}
						
				}else{
					logger.warn("accessdb table["+tableName+"] doesn't exist, import csv directly.");
					builder.setUseExistingTable(false);
					builder.setDelimiter(",").setHeader(true).importReader(new BufferedReader(new FileReader(new File(importCsvFullName))));
					flag=true;
					db.close();
				}
				
			} catch (IOException e) {
				logger.error(e.getMessage(),e);
			}
			return flag;
		}
		
		/**
		 * create access db
		 * @param dbFullName
		 */
		public Boolean createAccessDB(String dbFullName)
		{
			Boolean flag=false;
			try {
				Database db=DatabaseBuilder.create(Database.FileFormat.V2010, new File(dbFullName));
				db.close();
				flag=true;
				
			} catch (IOException e) {
				logger.error(e.getMessage(),e);
			} 
			return flag;
		}
		
		
		
		/***
		 * create access table
		 * 
		 * @param tableName
		 * @param tableDefinition
		 */
		@Deprecated
		public void createAccessTable(String tableName,List<String> tableDefinition)
		{
			try {
				if (getConn() == null){connect();}
				String dbFullName=getDatabaseServer().getSchema();
				Database db=DatabaseBuilder.open(new File(dbFullName));
				if(db.getTable(tableName)!=null)
				{
					logger.debug("accessdb table already exists.");
					return;
				}
				TableBuilder table=new TableBuilder(tableName);
				
				//table.addColumn(new ColumnBuilder("a").setSQLType(convertTypeStrToInt_AccessDB("INTEGER")).setLength(24).setPrecision(5));
				//table.addColumn(new ColumnBuilder("b").setSQLType(convertTypeStrToInt_AccessDB("VARCHAR(12)")).setLength(24));
				ColumnBuilder[] cols=new ColumnBuilder[tableDefinition.size()];
				for(String colStr : tableDefinition){
					int colIndex,colSize;
					String colName,colType,colNullable;
					Pattern p = Pattern.compile("col(\\d+)\\=([^\\s]+)\\s+([^\\s]+)(.*)", Pattern.CASE_INSENSITIVE);
					Matcher m = p.matcher(colStr);
					if(m.find())
					{
						//int groupCount=m.groupCount();
						colIndex=Integer.parseInt(m.group(1))-1;
						colName=m.group(2);
						cols[colIndex]=new ColumnBuilder(colName);
						if(m.group(3).contains("("))
						{
							int index=m.group(3).indexOf("(");
							colType=m.group(3).substring(0,index);
							colSize=Integer.parseInt(m.group(3).substring(index+1).replace(")", ""));
							cols[colIndex].setSQLType(convertTypeStrToInt_AccessDB(colType.toUpperCase()));
							cols[colIndex].setLengthInUnits(colSize);
						}else
						{
							cols[colIndex].setSQLType(convertTypeStrToInt_AccessDB(m.group(3).toUpperCase()));
						}
						//TODO to make some column not null
						colNullable=m.group(4);
						if(colNullable!=null && colNullable.trim().equalsIgnoreCase("Nullable")){
							//cols[colIndex].putProperty(PropertyMap.REQUIRED_PROP,false).putProperty(PropertyMap.ALLOW_ZERO_LEN_PROP, true);
							//PropertyMap a=new PropertyMapImpl();		
							//PropertyMap.Property prop=PropertyMapImpl.createProperty(PropertyMap.REQUIRED_PROP, null, true);
							
						}else
						{
							//cols[colIndex].putProperty(PropertyMap.REQUIRED_PROP,true).putProperty(PropertyMap.ALLOW_ZERO_LEN_PROP, false);
						}
					}
					
				}
				for(int i=0;i<cols.length;i++)
				{
					table.addColumn(cols[i]);
				}
				table.toTable(db);
				
				db.close();
			} catch (IOException e) {
				logger.error(e.getMessage(),e);
			} catch (SQLException e) {
				logger.error(e.getMessage(),e);
			}
		}
		
		
		/**
		 * create access table
		 * @param tableName
		 * @param tableDefinition
		 * @return
		 */
		@Deprecated
		public Boolean createAccessDBTable(String tableName,List<String> tableDefinition)
		{
			Boolean flag=false;
			try {
				flag=accessTableExistence(tableName);
				if(flag){
					return flag;
				}
				//generate sql statement
				StringBuilder sqlBuilder=new StringBuilder("CREATE TABLE ["+tableName+"] (");
				for(String str:tableDefinition){
					String[] strArr=str.split("\\=| ");

					if(strArr[2].equalsIgnoreCase("LONGTEXT")){
						strArr[2]="MEMO";
					}else if(strArr[2].equalsIgnoreCase("BOOLEAN")){
						strArr[2]="YESNO";//Optional
					}else if(strArr[2].equalsIgnoreCase("DATE")){
						strArr[2]="DATETIME";//Optional
					}else if(strArr[2].equalsIgnoreCase("DECIMAL")){
						strArr[2]="NUMERIC";//Optional
					}
					if(strArr.length==3){
						sqlBuilder.append("["+strArr[1]+"] "+strArr[2]+" NOT NULL,");
					}else if(strArr.length==4){
						sqlBuilder.append("["+strArr[1]+"] "+strArr[2]+",");
					}else {
						logger.warn("please check the column definition: "+str);
					}
				}
				String sql=sqlBuilder.deleteCharAt(sqlBuilder.length()-1).append(")").toString();
				flag=addBatch(sql);//create table
			} catch (Exception e) {
				logger.error(e.getMessage(),e);
			} 
			return flag;
		}
		
		/**
		 * create access table
		 * @param tableName
		 * @param tableDefinition
		 * @return
		 */
		public Boolean createAccessDBTab(String tableName,List<TableProps> tableDefinition)
		{
			Boolean flag=false;
			try {
				flag=accessTableExistence(tableName);
				if(flag){
					return flag;
				}
				//generate sql statement
				StringBuilder sqlBuilder=new StringBuilder("CREATE TABLE ["+tableName+"] (");
				for(TableProps props:tableDefinition){
					if(props.getTypeSize().equalsIgnoreCase("LONGTEXT")){
						props.setTypeSize("MEMO");
					}else if(props.getTypeSize().equalsIgnoreCase("BOOLEAN")){
						props.setTypeSize("YESNO");//Optional
					}else if(props.getTypeSize().equalsIgnoreCase("DATE")){
						props.setTypeSize("DATETIME");
					}else if(props.getTypeSize().equalsIgnoreCase("DECIMAL")){
						props.setTypeSize("NUMERIC");
					}
					sqlBuilder.append("["+props.getName()+"] "+props.getTypeSize()+props.getNullable()+",");
				}
				String sql=sqlBuilder.deleteCharAt(sqlBuilder.length()-1).append(")").toString();
				flag=addBatch(sql);//create table
			} catch (Exception e) {
				logger.error(e.getMessage(),e);
			} 
			return flag;
		}
		
		public Boolean deleteAccessDBTable(String tableName){
			Boolean flag=false;
			if(StringUtils.isBlank(tableName)){
				logger.info("table name is blank, please check sql syntax.");
				return flag;
			}
			try{
				String dbFullName=getDatabaseServer().getSchema();
				Database db=DatabaseBuilder.open(new File(dbFullName));
				Table sysTable=db.getSystemTable("MSysObjects");
				Cursor sysCursor=sysTable.getDefaultCursor();
				Map<String,Object> findCriteria=new HashMap<String,Object>();
				findCriteria.put("Name", tableName);
				findCriteria.put("Type", (short)1);
				
				if(sysCursor.findFirstRow(findCriteria)){
					sysTable.deleteRow(sysCursor.getCurrentRow());
					flag=true;
				}else{
					logger.info("table name ["+tableName+"] doesn't exist.");
				}
				db.close();
			}catch(Exception e){
				logger.error(e.getMessage(),e);
			}
			return flag;
		}
		public Boolean renameAccessDBTable(String tableName,String newTableName){
			Boolean flag=false;
			if(StringUtils.isBlank(tableName) || StringUtils.isBlank(newTableName)){
				logger.info("table name is blank, please check sql syntax.");
				return flag;
			}
			try{
				String dbFullName=getDatabaseServer().getSchema();
				Database db=DatabaseBuilder.open(new File(dbFullName));
				Table sysTable=db.getSystemTable("MSysObjects");
				Cursor sysCursor=sysTable.getDefaultCursor();
				Map<String,Object> findCriteria=new HashMap<String,Object>();
				findCriteria.put("Name", tableName);
				findCriteria.put("Type", (short)1);
				
				if(sysCursor.findFirstRow(findCriteria)){
					Row row=sysCursor.getCurrentRow();
					row.put("Name", newTableName);
					sysTable.updateRow(row);
					flag=true;
				}
				db.close();
			}catch(Exception e){
				logger.error(e.getMessage(),e);
			}
			return flag;
		}

		private int convertTypeStrToInt_AccessDB(String type)
		{
			if(StringUtils.isNotBlank(type))
			{
				if(type.startsWith("VARCHAR"))
				{
					return Types.NVARCHAR;
				}
				if(type.equalsIgnoreCase("LONGTEXT"))
				{
					return Types.NCLOB;
				}
				if(type.equalsIgnoreCase("DATE"))
				{
					return Types.DATE;
				}
				if(type.equalsIgnoreCase("LONG"))
				{
					return Types.INTEGER;
				}
				if(type.equalsIgnoreCase("INTEGER"))
				{
					return Types.INTEGER;
				}
				if(type.equalsIgnoreCase("SINGLE"))
				{
					return Types.INTEGER;
				}
				if(type.equalsIgnoreCase("DOUBLE"))
				{
					return Types.DOUBLE;
				}
				if(type.equalsIgnoreCase("DECIMAL"))
				{
					return Types.DECIMAL;
				}
				if(type.equalsIgnoreCase("BOOLEAN"))
				{
					return Types.BOOLEAN;
				}
				
			}
			return Types.NVARCHAR;
		}
	}
	
}
