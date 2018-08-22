package com.lombardrisk.Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.ColumnBuilder;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;
import com.healthmarketscience.jackcess.TableBuilder;
import com.healthmarketscience.jackcess.util.ImportUtil;
import com.healthmarketscience.jackcess.util.ImportUtil.Builder;
import com.lombardrisk.pojo.DatabaseServer;

public class DBHelper {

	private final static Logger logger = LoggerFactory.getLogger(DBHelper.class);
	private String dbmsDriver;
	private Connection conn = null;
	private DatabaseServer databaseServer;
	
	public DBHelper(DatabaseServer databaseServer)
	{
		this.databaseServer=databaseServer;
		if(StringUtils.isBlank(this.databaseServer.getPassword())){
			this.databaseServer.setPassword("password");
		}
		
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
			if(StringUtils.isBlank(this.databaseServer.getUrl())){
				this.databaseServer.setUrl(String.format("jdbc:oracle:thin:@%s:%s:%s", host, "1521", sid));
			}
		}
		//oracle database
		if(StringUtils.isNotBlank(dbms) && dbms.startsWith("access"))
		{
			dbmsDriver="net.ucanaccess.jdbc.UcanaccessDriver";
			if(StringUtils.isBlank(this.databaseServer.getUrl())){
				this.databaseServer.setUrl(String.format("jdbc:ucanaccess://%s;memory=false", this.databaseServer.getSchema()));
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
		if(flag)
		{
			try
			{
				setConn(DriverManager.getConnection(this.databaseServer.getUrl(), this.databaseServer.getUsername(), this.databaseServer.getPassword()));
			}
			catch (SQLException e)
			{
				logger.error("Database connection failed!");
				logger.error(e.getMessage(),e);
				flag=false;
			}
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
			Statement stmt = getConn().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
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
	
	public String convertTypeStr(String columnTypeName, int precision, int scale)
	{
		String type=null;
		columnTypeName=columnTypeName.toUpperCase();
		if(this.databaseServer.getDriver().startsWith("ora"))
		{
			if(columnTypeName.contains("LOB")){
				type=" LONGTEXT";
			}else if(columnTypeName.contains("DATE") || columnTypeName.contains("TIMESTAMP")){
				type=" DATE";
			}else if(columnTypeName.contains("NUMBER")){
				if(scale==0){
					type=" LONG";
				}else if(scale<=10){
					type=" DOUBLE";
				}else{
					type=" DECIMAL";
				}
				
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
			logger.info("start \"export to csv\"");
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
				for(col=1;col<rsmd.getColumnCount();col++)
				{
					//logger.info(rest.getString(col));
					if(rsmd.getColumnClassName(col).contains("Blob"))
					{
						value=Helper.convertBlobToStr(rest.getBlob(col));
					}else
					{
						value=rest.getNString(col);
					}
					if(value!=null)
					{
						if(rsmd.getColumnClassName(col).contains("Timestamp"))
						{
							bufOutFile.append(value.replaceAll("([\"])", "\"$1").replaceAll("\\.*", ""));
						}else if(rsmd.getColumnClassName(col).contains("Decimal")){
							bufOutFile.append(value.replaceAll("([\"])", "\"$1"));
						}else{
							bufOutFile.append("\"");
							bufOutFile.append(value.replaceAll("([\"])", "\"$1"));
							bufOutFile.append("\"");
						}
					}
					bufOutFile.append(",");
				}
				//last column
				if(rsmd.getColumnClassName(col).contains("Blob"))
				{
					value=Helper.convertBlobToStr(rest.getBlob(col));
				}else
				{
					value=rest.getNString(col);
				}
				if(value!=null)
				{
					if(rsmd.getColumnClassName(col).contains("Timestamp"))
					{
						bufOutFile.append(value.replaceAll("([\"])", "\"$1").replaceAll("\\.*", ""));
					}else if(rsmd.getColumnClassName(col).contains("Decimal")){
						bufOutFile.append(value.replaceAll("([\"])", "\"$1"));
					}else{
						bufOutFile.append("\"");
						bufOutFile.append(value.replaceAll("([\"])", "\"$1"));
						bufOutFile.append("\"");
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
		if (getConn() == null)
			return false;
		try {
			Statement statement=getConn().createStatement();
			statement.addBatch(sql);
			return true;
		} catch (SQLException e) {
			logger.error("SQLException in [" + sql + "]");
			logger.error(e.getMessage(),e);
			return false;
		}
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
		@Deprecated
		protected void tset(String dbFullName,String tableName)
		{
			try {
				Database db=DatabaseBuilder.open(new File(dbFullName));
				Table tab=db.getTable(tableName);
				for(Row row:tab)
				{
					for(Column col:tab.getColumns())
					{
						String colName=col.getName();
						Object value=row.get(colName);
						if(value==null)
						{
							value="null";
						}
						logger.debug("column "+colName+"("+col.getType()+")"+value+"("+value.getClass()+")");
						
					}
				}
				db.close();
			} catch (IOException e) {
				logger.error(e.getMessage(),e);
			}
			
		}
		
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
		/***
		 * import csv to access table
		 * @param tableName
		 * @param importCsvFullName
		 * @return
		 */
		public Boolean importCsvToAccessDB(String tableName,String importCsvFullName)
		{
			Boolean flag=false;
			try {
				Boolean useExistingTable=false;
				String dbFullName=getDatabaseServer().getSchema();
				Database db=DatabaseBuilder.open(new File(dbFullName));
				Builder builder=new ImportUtil.Builder(db, tableName);
				if(db.getTable(tableName)!=null)
				{
					logger.debug("accessdb table["+tableName+"] already exists.");
					useExistingTable=true;
				}
				builder.setUseExistingTable(useExistingTable);
				builder.setDelimiter(",").setHeader(true).importFile(new File(importCsvFullName));
				flag=true;
				db.close();
				
			} catch (IOException e) {
				logger.error(e.getMessage(),e);
			}
			return flag;
		}
		
		/**
		 * create access db
		 * @param dbFullName
		 */
		public void createAccessDB(String dbFullName)
		{
			try {
				Database db=DatabaseBuilder.create(Database.FileFormat.V2010, new File(dbFullName));
				db.close();
				
			} catch (IOException e) {
				logger.error(e.getMessage(),e);
			} 
		}
		
		/***
		 * create access table
		 * @param tableName
		 * @param tableDefinition
		 */
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
				
			}
			return Types.NVARCHAR;
		}
	}
	
}
