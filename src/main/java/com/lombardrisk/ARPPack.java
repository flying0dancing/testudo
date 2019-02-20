package com.lombardrisk;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lombardrisk.Utils.*;
import com.lombardrisk.pojo.DatabaseServer;
import com.lombardrisk.pojo.ZipSettings;


public class ARPPack implements IComFolder {

	private final static Logger logger = LoggerFactory.getLogger(ARPPack.class);
	
	public enum DBInfoSingle{
		INSTANCE;
		private DBInfo dbInfo;
		
		public DBInfo getDbInfo() {
			return dbInfo;
		}
		public void setDbInfo(String dbFullName) {
			this.dbInfo = new DBInfo(new DatabaseServer("accessdb","", dbFullName,"",""));
		}
	}
	
	/***
	 * create a new access database(.accdb) if no existed, otherwise use the existed one.
	 * @param dbFullName
	 * @return
	 */
	public Boolean createNewDpm(String dbFullName)
	{
		Boolean flag=false;
		DBInfoSingle.INSTANCE.setDbInfo(dbFullName);
		flag=DBInfoSingle.INSTANCE.getDbInfo().createAccessDB();
		return flag;
	}
	/***
	 * import all filtered metadata (*.csv files) to access database
	 * @param db it's a access database, its location or schema should be get value from <I>json file</I>->"zipSettings"->"dpmFullPath"
	 * @param csvParentPath it gets value from <I>json file</I> ->"exportPath"
	 * @param csvPaths it gets value from <I>json file</I>->"zipSettings"->"requiredMetadata"
	 * @param schemaFullName It's a configuration file, which contains all tables' definition.
	 * @return return metadata (*.csv files) full paths
	 */
	public List<String> importMetadataToDpm(DBInfo db,String csvParentPath,List<String> csvPaths, String schemaFullName)
	{
		return importMetadataToDpm(db.getDbHelper().getDatabaseServer().getSchema(),csvParentPath, csvPaths, schemaFullName);
	}
	
	/***
	 * import all filtered metadata (*.csv files) to access database
	 * @param dbFullPath it should be get value from <I>json file</I>->"zipSettings"->"dpmFullPath"
	 * @param csvParentPath it gets value from <I>json file</I> ->"exportPath"
	 * @param csvPaths it gets value from <I>json file</I>->"zipSettings"->"requiredMetadata"
	 * @param schemaFullName It's a configuration file, which contains all tables' definition.
	 * @return return metadata (*.csv files) full paths, return null if error occurs.
	 */
	public List<String> importMetadataToDpm(String dbFullPath,String csvParentPath,List<String> csvPaths, String schemaFullName)
	{
		if(StringUtils.isBlank(csvParentPath)){return null;}
		DBInfo dbInfo=DBInfoSingle.INSTANCE.getDbInfo();
		//dbInfo.createAccessTables(schemaFullName);
		if(csvPaths==null || csvPaths.size()<=0){return null;}
		List<String> realCsvFullPaths=new ArrayList<String>();
		String name_returnId;
		logger.info("================= import metadata into DPM =================");
		for(String pathTmp:csvPaths)
		{
			List<String> realCsvFullPathsTmp=FileUtil.getFilesByFilter(Helper.reviseFilePath(csvParentPath+System.getProperty("file.separator")+pathTmp),null);
			if(realCsvFullPathsTmp.size()<=0)
			{
				logger.error("error: invalid path ["+csvParentPath+System.getProperty("file.separator")+pathTmp+"]");
				continue;
			}
			for(String pathTmp2:realCsvFullPathsTmp)
			{
				if(!realCsvFullPaths.contains(pathTmp2))
				{
					name_returnId="";
					realCsvFullPaths.add(pathTmp2);
					logger.info("import dpm's file path:"+pathTmp2);
					String tableNameWithDB=FileUtil.getFileNameWithoutSuffix(pathTmp2);
					String tableName=tableNameWithDB.replaceAll("#.*?_", "_");
					if(!tableName.contains("_")){
						tableName=tableNameWithDB.replaceAll("#.*", "");
					}
					Pattern p = Pattern.compile("(GridKey|GridRef|List|Ref|Sums|Vals|XVals)(_.*)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
					Matcher m = p.matcher(tableName);
					if(m.find())
					{
						tableName=m.group(1);
						name_returnId=m.group(2);
					}
					/*String[] commons={"cscript",PropHelper.SCRIPT_GEN_DPM, Helper.reviseFilePath(schemaFullName), Helper.reviseFilePath(dbFullPath), Helper.reviseFilePath(pathTmp2), Helper.reviseFilePath(PropHelper.SCRIPT_PATH+"/log/GenerateProductDPM.log"), tableName};
					Helper.runCmdCommand(commons);*/
					tableNameWithDB=tableNameWithDB.replace(name_returnId, "");
					if(name_returnId.equals("") && tableNameWithDB.contains("_")){
						tableName=tableNameWithDB.split("#")[0];
					}
					Boolean flag=dbInfo.importCsvToAccess(tableName,tableNameWithDB, Helper.reviseFilePath(pathTmp2), Helper.reviseFilePath(schemaFullName));
					if(!flag){
						logger.error("import csv["+pathTmp2+"] to "+tableName+" fail.");
					}
					
				}
			}
		}
		if(realCsvFullPaths.size()<=0){return null;}
		return realCsvFullPaths;
	}
	

	/***
	 * read metadata (*.csv file) name's returnId, and then through dbFullName and its tableName(rets which stored definition of all returns), find its return name and version
	 * @param dbFullName full name of accessdb
	 * @param csvFullPaths metadata (*.csv files) full paths
	 * @return return a list of all returns' <I>name_version</I>, return null if error occurs.
	 */
	public List<String> getReturnNameAndVersions(String dbFullName,List<String> csvFullPaths)
	{
		if(csvFullPaths==null || csvFullPaths.size()<=0) return null;
		List<String> nameAndVers=new ArrayList<String>();
		
		//DBInfo dbInfo=new DBInfo(new DatabaseServer("accessdb","", dbFullName,"",""));
		DBInfo dbInfo=DBInfoSingle.INSTANCE.getDbInfo();
		for(String csvPath : csvFullPaths){
			String csvName=FileUtil.getFileNameWithoutSuffix(csvPath);
			if(!csvName.contains("_"))continue;
			String[] nameParts=csvName.split("_");
			if(!nameParts[1].matches("\\d+"))continue;
			String returnId=nameParts[1];
			String returnNameVer=dbInfo.getReturnAndVersion(returnId);
			if(!returnNameVer.equals("") && !nameAndVers.contains(returnNameVer)){
				nameAndVers.add(returnNameVer);
			}
		}
		if(nameAndVers.size()<=0) return null;
		return nameAndVers;
	}
	
	
	public Boolean execSQLs(String dbFullName,String sourcePath,List<String> sqlFileNames,String excludeFileFilters)
	{
		Boolean flag=true;
		if(sqlFileNames==null || sqlFileNames.size()<=0) return true;//means testudo.json doesn't provide sqlFiles.
		logger.info("================= execute SQLs =================");
		List<String> realFullPaths=getFileFullPaths(sourcePath, sqlFileNames,excludeFileFilters);
		if(realFullPaths==null || realFullPaths.size()<=0) {
			logger.error("error: sqlFiles are invalid files or filters.");
			return false;//illegal, no invalid files need to execute if it set sqlFiles
		}
		//DBInfo dbInfo=new DBInfo(new DatabaseServer("accessdb","", dbFullName,"",""));
		DBInfo dbInfo=DBInfoSingle.INSTANCE.getDbInfo();
		for(String fileFullPath:realFullPaths){
			logger.info("sql statements in file: "+fileFullPath);
			String fileContent=FileUtil.getFileContent1(fileFullPath);
			if(fileContent.contains(";")){
				String[] sqlStatements=fileContent.split(";");
				for(String sql:sqlStatements){
					if(StringUtils.isNotBlank(sql)){
						logger.info("execute sql:"+sql.trim());
						Boolean status=dbInfo.executeSQL(sql.trim());
						if(!status){
							logger.error("execute failed.");
							flag=false;
						}else{logger.info("execute OK.");}
					}
				}
			}else if(StringUtils.isNotBlank(fileContent)){
				logger.info("execute sql:"+fileContent);
				Boolean status=dbInfo.executeSQL(fileContent.trim());
				if(!status){
					logger.error("execute failed.");
					flag=false;
				}else{logger.info("execute OK.");}
			}
		}
		
		return flag;
	}
	
	/**
	 * 
	 * @param sourcePath should follow AR for product's folder structure
	 * @param packFileNames the file names which need to be packaged
	 * @param propFullPath the full path of package.properties, it should be get value from <I>json file</I>->"zipSettings"-> "productProperties"
	 * @param zipPath the path of package(.zip, .lrm)
	 * @param buildType blank(null) represents it is internal build, true represents it is release build
	 * @return
	 */
	public Boolean packageARProduct(String sourcePath,ZipSettings zipSet, String propFullPath, String zipPath, String buildType){
		if(StringUtils.isBlank(sourcePath)){return false;}
		logger.info("================= package files =================");
		//get all packaged files
		String productPrefix=FileUtil.getFileNameWithSuffix(Helper.getParentPath(sourcePath)).toUpperCase().replaceAll("\\(\\d+\\)", "");
		Boolean flag=true;
		List<String> packFileNames=zipSet.getZipFiles();
		List<String> realFullPaths=getFileFullPaths(sourcePath, packFileNames,zipSet.getExcludeFileFilters());
		if(realFullPaths==null){
			logger.error("error: zipFiles are invalid files or filters.");
			return false;
		}
		String arpbuild=null;
		if(StringUtils.isBlank(buildType)){
			arpbuild=String.valueOf(System.currentTimeMillis());
		}
		realFullPaths.addAll(Dom4jUtil.getPathFromElement(sourcePath+MANIFEST_FILE,sourcePath));
		//modify manifest.xml
		String packageVersion=Dom4jUtil.updateElement(sourcePath+MANIFEST_FILE, IMP_VERSION, arpbuild);
		List<String> accdbfiles=FileUtil.getFilesByFilter(Helper.reviseFilePath(sourcePath+"/"+DPM_PATH+"*"+DPM_FILE_SUFFIX),null);
		if(accdbfiles.size()>0)
		{
			String accdbFileName=FileUtil.getFileNameWithSuffix(accdbfiles.get(0));
			Dom4jUtil.updateElement(sourcePath+MANIFEST_FILE,ACCESSFILE ,accdbFileName);
		}
		
		if(FileUtil.exists(propFullPath)){
			PropHelper.loading(propFullPath);
			//modify manifest.xml
			Dom4jUtil.updateElement(sourcePath+MANIFEST_FILE, MAPPING_VERSION, PropHelper.getProperty(GEN_PRODUCT_DPM_VERSION));
			
		}else{
			logger.warn("warn: cannot found file ["+propFullPath+"]");
		}
		
		//zipped and lrm product
		String packageNamePrefix=PropHelper.getProperty(PACKAGE_NAME_PREFIX);
		packageNamePrefix=StringUtils.isBlank(packageNamePrefix)?"ARfor"+productPrefix+"_":packageNamePrefix;
		String packageNameSuffix=null;//PropHelper.getProperty(AR_INSTALLER_VERSION);
		packageNameSuffix=StringUtils.isBlank(packageNameSuffix)?"":"_for_AR_v"+packageNameSuffix;
		String zipFileNameWithoutSuffix=packageNamePrefix+"v"+packageVersion+packageNameSuffix;
		String zipFullPathWithoutSuffix=Helper.reviseFilePath(zipPath+"/"+zipFileNameWithoutSuffix);
		if(StringUtils.isNotBlank(zipFileNameWithoutSuffix)){
			flag=FileUtil.zipFilesAndFolders(sourcePath, realFullPaths,Helper.reviseFilePath(zipFullPathWithoutSuffix+PACKAGE_SUFFIX));
			if(!flag) return flag;
			String[] commons={"java","-jar",PropHelper.SCRIPT_LRM_PRODUCT,Helper.reviseFilePath(zipFullPathWithoutSuffix+PACKAGE_SUFFIX)};
			flag=Helper.runCmdCommand(commons); 
		}else{
			flag=false;
		}
		
		if(flag)
		{
			logger.info("package named: "+zipFullPathWithoutSuffix+PACKAGE_SUFFIX);
			logger.info("package named: "+zipFullPathWithoutSuffix+PACKAGE_LRM_SUFFIX);
			FileUtil.renameTo(zipFullPathWithoutSuffix+"_sign.lrm", zipFullPathWithoutSuffix+PACKAGE_LRM_SUFFIX);
			logger.info("package successfully.");
		}else{
			logger.error("error: package with failures.");
		}
		
		return flag;
	}
	
	/***
	 * get all file's full path under sourcePath, with filters
	 * @param sourcePath
	 * @param filters
	 * @return if get nothing or arguments contains blank argument, return null.
	 */
	public List<String> getFileFullPaths(String sourcePath, List<String> filters)
	{
		if(filters==null || filters.size()<=0) return null;
		if(StringUtils.isBlank(sourcePath)) return null;
		sourcePath=Helper.reviseFilePath(sourcePath+"/");
		List<String> realFilePaths=new ArrayList<String>();
		for(String filter:filters){
			
			List<String> realFullPathsTmp=FileUtil.getFilesByFilter(Helper.reviseFilePath(sourcePath+filter),null);
			if(realFullPathsTmp.size()<=0)
			{
				logger.error("error: cannot search ["+filter+"] under path ["+sourcePath+"]");
				continue;
			}
			for(String pathTmp:realFullPathsTmp){
				if(!realFilePaths.contains(pathTmp)){
					realFilePaths.add(pathTmp);
				}
			}
		}
		if(realFilePaths.size()<=0) return null;
		return realFilePaths;
	}
	
	/***
	 * get all file's full path under sourcePath, with filters
	 * @param sourcePath
	 * @param filters
	 * @return if get nothing or arguments contains blank argument, return null.
	 */
	public List<String> getFileFullPaths(String sourcePath, List<String> filters,String excludeFilters)
	{
		if(filters==null || filters.size()<=0) return null;
		if(StringUtils.isBlank(sourcePath)) return null;
		sourcePath=Helper.reviseFilePath(sourcePath+"/");
		List<String> realFilePaths=new ArrayList<String>();
		for(String filter:filters){
			
			List<String> realFullPathsTmp=FileUtil.getFilesByFilter(Helper.reviseFilePath(sourcePath+filter),excludeFilters);
			if(realFullPathsTmp.size()<=0)
			{
				logger.error("error: cannot search ["+filter+"] under path ["+sourcePath+"]");
				continue;
			}
			for(String pathTmp:realFullPathsTmp){
				if(!realFilePaths.contains(pathTmp)){
					realFilePaths.add(pathTmp);
				}
			}
		}
		if(realFilePaths.size()<=0) return null;
		return realFilePaths;
	}
}
