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


public class ARPCISettingManager implements IComFolder {
	

	private final static Logger logger = LoggerFactory.getLogger(ARPCISettingManager.class);
	private static Boolean hasLoaded=false;
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
	
	private static ARPCISetting reviseARPCISetting(ARPCISetting arCIConfg)
	{
		if(arCIConfg!=null){
			//revise "prefix"
			String productPrefix=arCIConfg.getPrefix();
			if(StringUtils.isBlank(productPrefix)){
				if(StringUtils.isBlank(System.getProperty(CMDL_ARPPRODUCTPREFIX))){
					throw new JsonSyntaxException("error: prefix is null, please set it value");
				}else
				{
					productPrefix=System.getProperty(CMDL_ARPPRODUCTPREFIX).toUpperCase();
					arCIConfg.setPrefix(productPrefix);
				}
			}
			
			//revise "metadataPath"
			String metadataPath=arCIConfg.getMetadataPath();
			String productPath=null;
			String targetProductPath=null;
			String targetSrcPath=null;
			String sourcePath=null;
			if(StringUtils.isNotBlank(metadataPath)){
				metadataPath=Helper.reviseFilePath(metadataPath);
				sourcePath=Helper.getParentPath(metadataPath); //src/
				productPath=Helper.removeLastSlash(Helper.getParentPath(sourcePath));
			}else{
				sourcePath=Helper.getParentPath(System.getProperty("user.dir"))+arCIConfg.getPrefix()+System.getProperty("file.separator")+SOURCE_FOLDER; //src/	
				metadataPath=sourcePath+META_PATH;
				productPath=Helper.getParentPath(System.getProperty("user.dir"))+arCIConfg.getPrefix();
			}
			if(StringUtils.isBlank(System.getProperty(CMDL_ARPRUNONJENKINS))){
				//run on local machine
				//get target product path
				targetProductPath=FileUtil.createNewFileWithSuffix(productPath,null,null);
				targetSrcPath=targetProductPath+System.getProperty("file.separator")+SOURCE_FOLDER;//target source path
				metadataPath=targetSrcPath+META_PATH; //target metadata path
				FileUtil.copyDirectory(sourcePath, targetSrcPath);
			}else{
				//run on Jenkins server
				targetSrcPath=sourcePath;
			}
			
			arCIConfg.setMetadataPath(metadataPath);
			arCIConfg.setSrcPath(sourcePath);
			arCIConfg.setTargetSrcPath(targetSrcPath);
			FileUtil.createDirectories(arCIConfg.getMetadataPath());
			
			//revise "metadataStruct"
			String metadataStruct=arCIConfg.getMetadataStruct();
			if(StringUtils.isBlank(metadataStruct)){
				arCIConfg.setMetadataStruct(arCIConfg.getPrefix()+INI_FILE_SUFFIX);
			}
			
			//revise "zipSettings"->"dpmFullPath"
			String dpmFullName=arCIConfg.getZipSettings().getDpmFullPath();
			if(StringUtils.isNotBlank(dpmFullName)){
				FileUtil.createDirectories(targetSrcPath+DPM_PATH);
				if(!dpmFullName.contains("/") && !dpmFullName.contains("\\")){
					//dpmFullName just a file name without path
					dpmFullName=targetSrcPath+DPM_PATH+dpmFullName;
				}else{
					dpmFullName=Helper.reviseFilePath(dpmFullName);
					String dpmPathTemp=Helper.getParentPath(dpmFullName);
					String dpmName=dpmFullName.replace(dpmPathTemp, "");
					dpmFullName=targetSrcPath+DPM_PATH+dpmName;//remap its dpmFullName to target folder
				}
			}else{
				FileUtil.createDirectories(targetSrcPath+DPM_PATH);//FileUtil.createDirectories(Helper.reviseFilePath(commonFolder+DPM_PATH));
				dpmFullName=Helper.reviseFilePath(targetSrcPath+DPM_PATH+arCIConfg.getPrefix()+DPM_FILE_SUFFIX);
			}
			arCIConfg.getZipSettings().setDpmFullPath(dpmFullName);
			
			//revise "zipSettings"->"productProperties"
			String productPropsPath=arCIConfg.getZipSettings().getProductProperties();
			String upperComFolder=Helper.getParentPath(sourcePath);
			if(StringUtils.isNotBlank(productPropsPath)){
				if(!productPropsPath.contains("/") && !productPropsPath.contains("\\")){
					productPropsPath=Helper.reviseFilePath(upperComFolder+productPropsPath);
				}else{
					productPropsPath=Helper.reviseFilePath(productPropsPath);
				}
			}else{
				productPropsPath=Helper.reviseFilePath(upperComFolder+PRODUCT_PROP_FILE);
			}
			arCIConfg.getZipSettings().setProductProperties(productPropsPath);
			
		}
		return arCIConfg;
	}
	


}
