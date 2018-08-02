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


public class ARPPack implements IComFolder {

	private final static Logger logger = LoggerFactory.getLogger(ARPPack.class);
	
	public Boolean createNewDpm(DBInfo db)
	{
		Boolean flag=false;
		db.createAccessDB();
		return flag;
	}
	/***
	 * import all filtered csv files to accessdb
	 * @param db it's a access database, its location or schema should be get value from <I>json file</I>->"zipSettings"->"accessDBFullPath"
	 * @param csvParentPath it gets value from <I>json file</I> ->"exportPath"
	 * @param csvPaths it gets value from <I>json file</I>->"zipSettings"->"requiredCsvs"
	 * @param schemaFullName It's a configuration file, which contains all tables' definition.
	 * @return
	 */
	public Boolean importMetadataToDpm(DBInfo db,String csvParentPath,List<String> csvPaths, String schemaFullName)
	{
		return importMetadataToDpm(db.getDbHelper().getDatabaseServer().getSchema(),csvParentPath, csvPaths, schemaFullName);
	}
	
	/***
	 * import all filtered csv files to accessdb
	 * @param dbFullPath it should be get value from <I>json file</I>->"zipSettings"->"accessDBFullPath"
	 * @param csvParentPath it gets value from <I>json file</I> ->"exportPath"
	 * @param csvPaths it gets value from <I>json file</I>->"zipSettings"->"requiredCsvs"
	 * @param schemaFullName It's a configuration file, which contains all tables' definition.
	 * @return
	 */
	public Boolean importMetadataToDpm(String dbFullPath,String csvParentPath,List<String> csvPaths, String schemaFullName)
	{
		if(StringUtils.isBlank(csvParentPath)){return false;}
		if(csvPaths==null || csvPaths.size()<=0){return false;}
		Boolean flag=true;
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
					Boolean flagT=runCmdCommand(commons); 
					if(!flagT)flag=flagT;
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
		if(realCsvFullPaths.size()<=0){return false;}
		return flag;
	}
	
	public Boolean packageARProduct(String sourcePath,List<String> packFileNames, String propFullPath, String zipPath, String jenkinsVariable){
		if(StringUtils.isBlank(sourcePath)){return false;}
		Boolean flag=true;
		List<String> realFullPaths=new ArrayList<String>();
		for(String pathTmp:packFileNames){
			List<String> realFullPathsTmp=FileUtil.getFilesByFilter(sourcePath+pathTmp);
			if(realFullPathsTmp.size()<=0)
			{
				logger.error("error: invalid path ["+sourcePath+System.getProperty("file.separator")+pathTmp+"]");
				continue;
			}
			for(String pathTmp2:realFullPathsTmp){
				if(!realFullPaths.contains(pathTmp2)){
					realFullPaths.add(pathTmp2);
				}
			}
		}
		if(realFullPaths.size()<=0) return false;
		if(FileUtil.exists(propFullPath)){
			PropHelper.loading(propFullPath);
			//modify manifest.xml
			Dom4jUtil.updateElement(sourcePath+MANIFEST_FILE, IMP_VERSION, jenkinsVariable);
			Dom4jUtil.updateElement(sourcePath+MANIFEST_FILE, MAPPING_VERSION, PropHelper.getProperty(GEN_PRODUCT_DPM_VERSION));
			List<String> accdbfiles=FileUtil.getFilesByFilter(Helper.reviseFilePath(sourcePath+DPM_PATH+"*"+DPM_FILE_SUFFIX));
			if(accdbfiles.size()>0)
			{
				//String accdbFileName=FileUtil.getFileNameWithSuffix(dpmFullPath);
				String accdbFileName=FileUtil.getFileNameWithSuffix(accdbfiles.get(0));
				Dom4jUtil.updateElement(sourcePath+MANIFEST_FILE,ACCESSFILE ,accdbFileName);
			}
		}else{
			logger.warn("warn: cannot found file["+propFullPath+"]");
		}
		
		//zipped and lrm product
		String packageNamePrefix=PropHelper.getProperty(PACKAGE_NAME_PREFIX);
		String packageNameSuffix=PropHelper.getProperty(AR_INSTALLER_VERSION);
		String zipFileNameWithoutSuffix=packageNamePrefix+"for_AR_"+packageNameSuffix;
		String zipFullPathWithoutSuffix=zipPath+zipFileNameWithoutSuffix;
		if(StringUtils.isNoneBlank(packageNamePrefix,packageNameSuffix)){
			flag=FileUtil.ZipFiles(sourcePath, realFullPaths,Helper.reviseFilePath(zipFullPathWithoutSuffix+".zip"));
			if(!flag) return flag;
			String[] commons={"java","-jar",PropHelper.SCRIPT_LRM_PRODUCT,Helper.reviseFilePath(zipFullPathWithoutSuffix+".zip")};
			flag=runCmdCommand(commons); 
		}else{
			flag=false;
		}
		
		if(flag)
		{
			FileUtil.renameTo(zipFullPathWithoutSuffix+"_sign.lrm", zipFullPathWithoutSuffix+".lrm");
			logger.info("pass");
		}else{
			logger.error("fail");
		}
		
		return flag;
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
