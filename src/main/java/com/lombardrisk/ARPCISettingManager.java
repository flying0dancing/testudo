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
				if(!file.contains("\\") && !file.contains("/"))
				{
					file=Helper.reviseFilePath(System.getProperty("user.dir")+System.getProperty("file.separator")+file);
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
			//revise "metadataPath"
			String metadataPath=arCIConfg.getMetadataPath();
			String commonFolder=null;
			if(StringUtils.isNotBlank(metadataPath)){
				metadataPath=Helper.reviseFilePath(metadataPath);
				commonFolder=Helper.reviseFilePath(Helper.getParentPath(metadataPath)); //src/
			}else{
				commonFolder=Helper.getParentPath(Helper.removeLastSlash(System.getProperty("user.dir")))+arCIConfg.getPrefix()+SOURCE_FOLDER; //src/
				metadataPath=Helper.reviseFilePath(commonFolder+META_PATH);
			}
			FileUtil.createDirectories(arCIConfg.getMetadataPath());
			arCIConfg.setMetadataPath(metadataPath);
			arCIConfg.setSrcPath(commonFolder);
			//revise "zipSettings"->"dpmFullPath"
			String dpmFullName=arCIConfg.getZipSettings().getDpmFullPath();
			if(StringUtils.isNotBlank(dpmFullName)){
				if(!dpmFullName.contains("//") && !dpmFullName.contains("\\")){
					FileUtil.createDirectories(Helper.reviseFilePath(commonFolder+DPM_PATH));
					dpmFullName=Helper.reviseFilePath(commonFolder+DPM_PATH+dpmFullName);
				}else{
					FileUtil.createDirectories(Helper.getParentPath(dpmFullName));
					dpmFullName=Helper.reviseFilePath(dpmFullName);
				}
			}else{
				FileUtil.createDirectories(Helper.reviseFilePath(commonFolder+DPM_PATH));
				dpmFullName=Helper.reviseFilePath(commonFolder+DPM_PATH+arCIConfg.getPrefix()+DPM_FILE_SUFFIX);
			}
			arCIConfg.getZipSettings().setDpmFullPath(dpmFullName);
			
			//revise "zipSettings"->"productProperties"
			String productPropsPath=arCIConfg.getZipSettings().getProductProperties();
			String upperComFolder=Helper.getParentPath(commonFolder);
			if(StringUtils.isNotBlank(productPropsPath)){
				if(!productPropsPath.contains("//") && !productPropsPath.contains("\\")){
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
