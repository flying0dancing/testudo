package com.lombardrisk;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.lombardrisk.Utils.FileUtil;
import com.lombardrisk.Utils.Helper;
import com.lombardrisk.pojo.ARPCISetting;
import com.lombardrisk.pojo.ExternalProject;
import com.lombardrisk.pojo.ZipSettings;


public class ARPCISettingManager implements IComFolder {
	

	private final static Logger logger = LoggerFactory.getLogger(ARPCISettingManager.class);
	private static Boolean hasLoaded=false;
	private static Boolean copyAllProductsInOneProject=true;
	private static String targetProjectPath=null;
	private final static List<ARPCISetting> ARPCISETTINGS=loadJson(System.getProperty(CMDL_ARPCICONFG,JSON_PATH));
	
	
	public static synchronized List<ARPCISetting> loadJson(String file) 
	{
		try{
			if(!hasLoaded){
				java.lang.reflect.Type type=new TypeToken<List<ARPCISetting>>(){}.getType();
				//ARPCISetting exportSetting=new Gson().fromJson(new FileReader("src/main/resources/test.json"), ExportSetting.class);
				if( (System.getProperty("file.separator").equals("/") && !file.startsWith("/")) || (System.getProperty("file.separator").equals("\\") && !file.contains(":")) )
				{
					if(file.contains("/") || file.contains("\\")){
						file=Helper.reviseFilePath(Helper.getParentPath(System.getProperty("user.dir"))+file);
					}else{
						file=Helper.reviseFilePath(System.getProperty("user.dir")+"/"+file);
					}
				}
				
				File fileHd=new File(file);
				if(fileHd.exists())
				{
					List<ARPCISetting> settingList=new Gson().fromJson(new FileReader(fileHd), type);
					return settingList;
				}else{
					return new ArrayList<ARPCISetting>();
				}
				
			}else{
				return ARPCISETTINGS;
			}
		} catch (JsonIOException | JsonSyntaxException e) {
			logger.error(e.getMessage(), e);
			return new ArrayList<ARPCISetting>();
		}catch (FileNotFoundException e) {
			logger.error(e.getMessage(), e);
			return new ArrayList<ARPCISetting>();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return new ArrayList<ARPCISetting>();
		}
	}
	
	public static ARPCISetting getARPCISetting(String key)
	{
		try {
			if(ARPCISETTINGS!=null && ARPCISETTINGS.size()>0){
				ARPCISetting arCIConfg;
				if(StringUtils.isBlank(key)){
					arCIConfg=reviseARPCISetting(ARPCISETTINGS.get(0));
				}else{
					//arCIConfg=reviseARPCISetting((ARPCISetting)Helper.filterListByPrefix(ARPCISETTINGS, key));
					arCIConfg=reviseARPCISetting((ARPCISetting)Helper.filterListByID(ARPCISETTINGS, key));
				}
				return arCIConfg;
			}
		} catch (NoSuchMethodException | SecurityException
				| IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}
	
	public static List<ARPCISetting> getARPCISettingList()
	{
		List<ARPCISetting> arCIConfgList=new ArrayList<ARPCISetting>();
		try {
			if(ARPCISETTINGS!=null && ARPCISETTINGS.size()>0){
				for(ARPCISetting arCIConfg:ARPCISETTINGS){
					arCIConfgList.add(reviseARPCISetting(arCIConfg));
				}
				return arCIConfgList;
			}
		} catch (SecurityException
				| IllegalArgumentException e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}
	
	private static ARPCISetting reviseARPCISetting(ARPCISetting arCIConfg)
	{
		if(arCIConfg!=null){
			//revise "prefix", make sure it is lowercase
			String productPrefix=arCIConfg.getPrefix();
			if(StringUtils.isBlank(productPrefix)){
				throw new JsonSyntaxException("error: prefix is null, please set it value");//prefix must be set as a subfolder's name of project folder name
				/*if(StringUtils.isBlank(System.getProperty(CMDL_ARPPRODUCTPREFIX))){
					throw new JsonSyntaxException("error: prefix is null, please set it value");
				}else
				{
					productPrefix=System.getProperty(CMDL_ARPPRODUCTPREFIX).toLowerCase();
					arCIConfg.setPrefix(productPrefix);
				}*/
			}else{
				arCIConfg.setPrefix(productPrefix.toLowerCase());
			}
			
			//revise "metadataPath"
			String metadataPath=arCIConfg.getMetadataPath();
			String projectPath=null;
			String productPath=null;//subfolder under project folder
			String targetSrcPath=null;
			String sourcePath=null;
			if(StringUtils.isNotBlank(metadataPath)){
				metadataPath=Helper.reviseFilePath(metadataPath);
				sourcePath=Helper.getParentPath(metadataPath); //src/
				productPath=Helper.removeLastSlash(Helper.getParentPath(sourcePath));
				projectPath=Helper.removeLastSlash(Helper.getParentPath(productPath));
			}else{
				projectPath=Helper.getParentPath(System.getProperty("user.dir"))+System.getProperty(CMDL_ARPPROJECTFOLDER);
				productPath=projectPath+File.separator+arCIConfg.getPrefix();
				sourcePath=productPath+File.separator+SOURCE_FOLDER; //src/	
				metadataPath=sourcePath+META_PATH;
			}
			
			if(StringUtils.isBlank(System.getProperty(CMDL_ARPRUNONJENKINS))){
				//run on local machine
				if(copyAllProductsInOneProject){
					targetProjectPath=FileUtil.createNewFileWithSuffix(projectPath,null,null);
				}
				//get target product path
				String targetProductPath=targetProjectPath+File.separator+arCIConfg.getPrefix();
				targetSrcPath=targetProductPath+File.separator+SOURCE_FOLDER;//current product(prefix)'s target source path
				metadataPath=targetSrcPath+META_PATH; //current product(prefix)'s target metadata path
				if(StringUtils.isNotBlank(System.getProperty(CMDL_ARPRODUCTID)) && System.getProperty(CMDL_ARPRODUCTID).startsWith("*")){
					if(copyAllProductsInOneProject){
						FileUtil.copyDirectory(projectPath, targetProjectPath);
						copyAllProductsInOneProject=false;
					}
				}else{
					if(!FileUtil.exists(targetProductPath)){
						FileUtil.copyDirectory(productPath, targetProductPath);
					}
				}
			}else{
				//run on Jenkins server
				targetProjectPath=projectPath;
				targetSrcPath=sourcePath;
			}
			
			arCIConfg.setMetadataPath(metadataPath);
			arCIConfg.setSrcPath(sourcePath);
			arCIConfg.setTargetSrcPath(targetSrcPath);
			FileUtil.createDirectories(arCIConfg.getMetadataPath());
			
			//revise "metadataStruct"
			String metadataStruct=arCIConfg.getMetadataStruct();
			if(StringUtils.isBlank(metadataStruct)){
				//arCIConfg.setMetadataStruct(arCIConfg.getPrefix()+INI_FILE_SUFFIX);
				arCIConfg.setMetadataStruct(arCIConfg.getPrefix().toUpperCase()+INI_FILE_SUFFIX);
			}
			//revise "zipSettings"
			ZipSettings zipSetting=arCIConfg.getZipSettings();
			if(arCIConfg.getZipSettings()!=null){
				//revise "zipSettings"->"dpmFullPath"
				String dpmFullName=arCIConfg.getZipSettings().getDpmFullPath();
				FileUtil.createDirectories(targetSrcPath+DPM_PATH);
				if(StringUtils.isNotBlank(dpmFullName)){
					if(!dpmFullName.contains("/") && !dpmFullName.contains("\\")){
						//dpmFullName just a file name without path
						dpmFullName=targetSrcPath+DPM_PATH+dpmFullName;
					}else{
						dpmFullName=Helper.reviseFilePath(dpmFullName);
						String dpmPathTemp=Helper.getParentPath(dpmFullName);
						String dpmName=dpmFullName.replace(dpmPathTemp, "");
						//copy access file
						if(!dpmPathTemp.contains(sourcePath)){
							FileUtil.copyFileToDirectory(dpmFullName, targetSrcPath+DPM_PATH);
						}
						dpmFullName=targetSrcPath+DPM_PATH+dpmName;//remap its dpmFullName to target folder
					}
				}else{
					//dpmFullName=Helper.reviseFilePath(targetSrcPath+DPM_PATH+arCIConfg.getPrefix()+DPM_FILE_SUFFIX);
					dpmFullName=Helper.reviseFilePath(targetSrcPath+DPM_PATH+arCIConfg.getPrefix().toUpperCase()+DPM_FILE_SUFFIX);
					List<ExternalProject> externalProjects=zipSetting.getExternalProjects();
					if(externalProjects!=null && externalProjects.size()>0){
						for(ExternalProject externalpro:externalProjects){
							if(StringUtils.isNoneBlank(externalpro.getProject(),externalpro.getSrcFile()) ){
								String destDir=StringUtils.isBlank(externalpro.getDestDir())?targetSrcPath:Helper.reviseFilePath(targetSrcPath+File.separator+externalpro.getDestDir());
								FileUtil.copyExternalProject(Helper.reviseFilePath(Helper.getParentPath(System.getProperty("user.dir"))+externalpro.getProject()+File.separator+externalpro.getSrcFile()), destDir, externalpro.getUncompress());
							}else{
								logger.error("externalProjects->project,srcFile cannot be null.");
							}
						}
					}
				}
				arCIConfg.getZipSettings().setDpmFullPath(dpmFullName);
				
				//revise "zipSettings"->"productProperties"
				String productPropsPath=arCIConfg.getZipSettings().getProductProperties();
				if(StringUtils.isNotBlank(productPropsPath)){
					if(!productPropsPath.contains("/") && !productPropsPath.contains("\\")){
						productPropsPath=Helper.reviseFilePath(targetProjectPath+File.separator+productPropsPath);
					}else{
						productPropsPath=Helper.reviseFilePath(productPropsPath);
					}
				}else{
					productPropsPath=Helper.reviseFilePath(targetProjectPath+File.separator+PRODUCT_PROP_FILE);
				}
				arCIConfg.getZipSettings().setProductProperties(productPropsPath);
			}
			
		}
		return arCIConfg;
	}
	


}
