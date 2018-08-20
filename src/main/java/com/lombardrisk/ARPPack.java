package com.lombardrisk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lombardrisk.Utils.*;
import com.lombardrisk.pojo.DatabaseServer;


public class ARPPack implements IComFolder {

	private final static Logger logger = LoggerFactory.getLogger(ARPPack.class);
	
	public Boolean createNewDpm(DBInfo db)
	{
		Boolean flag=false;
		flag=db.createAccessDB();
		return flag;
	}
	/***
	 * import all filtered csv files to accessdb
	 * @param db it's a access database, its location or schema should be get value from <I>json file</I>->"zipSettings"->"accessDBFullPath"
	 * @param csvParentPath it gets value from <I>json file</I> ->"exportPath"
	 * @param csvPaths it gets value from <I>json file</I>->"zipSettings"->"requiredMetadata"
	 * @param schemaFullName It's a configuration file, which contains all tables' definition.
	 * @return return csv full paths
	 */
	public List<String> importMetadataToDpm(DBInfo db,String csvParentPath,List<String> csvPaths, String schemaFullName)
	{
		return importMetadataToDpm(db.getDbHelper().getDatabaseServer().getSchema(),csvParentPath, csvPaths, schemaFullName);
	}
	
	/***
	 * import all filtered csv files to accessdb
	 * @param dbFullPath it should be get value from <I>json file</I>->"zipSettings"->"accessDBFullPath"
	 * @param csvParentPath it gets value from <I>json file</I> ->"exportPath"
	 * @param csvPaths it gets value from <I>json file</I>->"zipSettings"->"requiredMetadata"
	 * @param schemaFullName It's a configuration file, which contains all tables' definition.
	 * @return return csv full paths, return null if error occurs.
	 */
	public List<String> importMetadataToDpm(String dbFullPath,String csvParentPath,List<String> csvPaths, String schemaFullName)
	{
		if(StringUtils.isBlank(csvParentPath)){return null;}
		if(csvPaths==null || csvPaths.size()<=0){return null;}
		List<String> realCsvFullPaths=new ArrayList<String>();
		for(String pathTmp:csvPaths)
		{
			List<String> realCsvFullPathsTmp=FileUtil.getFilesByFilter(csvParentPath+System.getProperty("file.separator")+pathTmp);
			if(realCsvFullPathsTmp.size()<=0)
			{
				logger.error("error: invalid path ["+csvParentPath+System.getProperty("file.separator")+pathTmp+"]");
				continue;
			}
			for(String pathTmp2:realCsvFullPathsTmp)
			{
				if(!realCsvFullPaths.contains(pathTmp2))
				{
					realCsvFullPaths.add(pathTmp2);
					logger.info("import dpm's file path:"+pathTmp2);
					String tableName=FileUtil.getFileNameWithoutSuffix(pathTmp2);
					Pattern p = Pattern.compile("(GridKey|GridRef|List|Ref|Sums|Vals|XVals)_.*", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
					Matcher m = p.matcher(tableName);
					if(m.find())
					{
						tableName=m.group(1);
					}
					String[] commons={"cscript",PropHelper.SCRIPT_GEN_DPM, Helper.reviseFilePath(schemaFullName), Helper.reviseFilePath(dbFullPath), Helper.reviseFilePath(pathTmp2), Helper.reviseFilePath(PropHelper.SCRIPT_PATH+"/log/GenerateProductDPM.log"), tableName};
					runCmdCommand(commons); 
					
					/*logger.info(String.join(" ", commons));
					
					try {
						Process process = Runtime.getRuntime().exec(commons);
						process.waitFor();
						int exitInt=process.exitValue();
						
						if(exitInt==0)
						{
							logger.info("pass");
						}else{
							flag=false;
							logger.error("fail");
						}
						
					} catch (InterruptedException |IOException e) {
						flag=false;
						logger.error(e.getMessage(),e);
					}*/
				}
			}
		}
		if(realCsvFullPaths.size()<=0){return null;}
		return realCsvFullPaths;
	}
	

	/***
	 * read csv file name's returnId, and then through dbFullName and its tableName, find its return name and version
	 * tableName rets which stored definition of all returns
	 * @param dbFullName full name of accessdb
	 * @param csvFullPaths 
	 * @return return a list of all returns' <I>name_version</I>, return null if error occurs.
	 */
	public List<String> getReturnNameAndVersions(String dbFullName,List<String> csvFullPaths)
	{
		if(csvFullPaths==null || csvFullPaths.size()<=0) return null;
		List<String> nameAndVers=new ArrayList<String>();
		
		DBInfo dbInfo=new DBInfo();
		dbInfo.setDbHelper(new DatabaseServer("accessdb","", dbFullName,"",""));
		for(String csvPath : csvFullPaths){
			String csvName=FileUtil.getFileNameWithoutSuffix(csvPath);
			if(!csvName.contains("_"))continue;
			String[] nameParts=csvName.split("_");
			String returnId=nameParts[1];
			String returnNameVer=dbInfo.getReturnAndVersion(returnId);
			if(!returnNameVer.equals("") && !nameAndVers.contains(returnNameVer)){
				nameAndVers.add(returnNameVer);
			}
		}
		if(nameAndVers.size()<=0) return null;
		return nameAndVers;
	}
	
	//TODO
	public Boolean execSQLs(String dbFullName,String sqlPath,List<String> sqlFileNames)
	{
		Boolean flag=true;
		if(sqlFileNames==null || sqlFileNames.size()<=0) return true;
		List<String> realFullPaths=getFileFullPaths(sqlPath, sqlFileNames);
		

		return flag;
	}
	
	/**
	 * 
	 * @param sourcePath should follow AR for product's folder structure
	 * @param packFileNames the file names which need to be packaged
	 * @param propFullPath the full path of package.properties, it should be get value from <I>json file</I>->"zipSettings"-> "productProperties"
	 * @param zipPath the path of package(.zip, .lrm)
	 * @param jenkinsVariable
	 * @return
	 */
	public Boolean packageARProduct(String sourcePath,List<String> packFileNames, String propFullPath, String zipPath, String jenkinsVariable){
		if(StringUtils.isBlank(sourcePath)){return false;}
		//execSQLs
		//TODO
		//get all packaged files
		String productPrefix=FileUtil.getFileNameWithSuffix(Helper.getParentPath(sourcePath)).toUpperCase();
		Boolean flag=true;
		List<String> realFullPaths=getFileFullPaths(sourcePath, packFileNames);
		if(realFullPaths==null) return false;
		
		//modify manifest.xml
		String packageVersion=Dom4jUtil.updateElement(sourcePath+MANIFEST_FILE, IMP_VERSION, jenkinsVariable);
		List<String> accdbfiles=FileUtil.getFilesByFilter(Helper.reviseFilePath(sourcePath+"/"+DPM_PATH+"*"+DPM_FILE_SUFFIX));
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
			flag=FileUtil.ZipFiles(sourcePath, realFullPaths,Helper.reviseFilePath(zipFullPathWithoutSuffix+".zip"));
			if(!flag) return flag;
			String[] commons={"java","-jar",PropHelper.SCRIPT_LRM_PRODUCT,Helper.reviseFilePath(zipFullPathWithoutSuffix+".zip")};
			flag=runCmdCommand(commons); 
		}else{
			flag=false;
		}
		
		if(flag)
		{
			logger.info("package named: "+zipFullPathWithoutSuffix+".zip/.lrm");
			FileUtil.renameTo(zipFullPathWithoutSuffix+"_sign.lrm", zipFullPathWithoutSuffix+".lrm");
			logger.info("package sucessfully.");
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
			
			List<String> realFullPathsTmp=FileUtil.getFilesByFilter(sourcePath+filter);
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
	
	
	private Boolean runCmdCommand(String[] commons)
	{
		Boolean flag=true;
		logger.info(String.join(" ", commons));
		try {
			Process process = Runtime.getRuntime().exec(commons);
			process.waitFor();
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

			String str=null;
			logger.debug("Here is the standard output of the command:");
			while((str=stdInput.readLine())!=null)
			{
				logger.info(str);
				if(str.toLowerCase().contains("error")) 
				{
					flag=false;
					break;
				}
			}
			logger.debug("Here is the standard error of the command (if any):");
			while((str=stdError.readLine())!=null)
			{
				logger.error(str);
				if(str.toLowerCase().contains("error")) 
				{
					flag=false;
					break;
				}
			}
			
		} catch (InterruptedException |IOException e) {
			flag=false;
			logger.error(e.getMessage(),e);
		} 
		return flag;
	}
}
