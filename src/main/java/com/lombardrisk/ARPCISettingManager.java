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
import com.lombardrisk.Utils.Dom4jUtil;
import com.lombardrisk.Utils.FileUtil;
import com.lombardrisk.Utils.Helper;
import com.lombardrisk.pojo.ARPCISetting;
import com.lombardrisk.pojo.ExternalProject;
import com.lombardrisk.pojo.ZipSettings;


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
	/***
	 * 
	 * @param ids i.e. "masMetadata;maspack"
	 * @return
	 * @throws Exception 
	 */
	public static List<ARPCISetting> getARPCISettingList(String ids) throws Exception
	{
		List<ARPCISetting> arCIConfgList=new ArrayList<ARPCISetting>();
		try {
			if(ARPCISETTINGS!=null && ARPCISETTINGS.size()>0){
				
				if(StringUtils.isNotBlank(ids) && ids.startsWith("*")){ //get all ARPCISetting
					for(ARPCISetting arCIConfg:ARPCISETTINGS){
						arCIConfgList.add(reviseARPCISetting(arCIConfg));
					}
				}else if(StringUtils.isNotBlank(ids) && ids.contains(";")){
					String[] idarr=ids.split(";");
					Boolean flag=false;
					
					for(int i=0;i<idarr.length;i++){
						flag=false;
						for(ARPCISetting arCIConfg:ARPCISETTINGS){
							if(idarr[i].trim().equalsIgnoreCase(arCIConfg.getID())){
								flag=true;
								if(!arCIConfgList.contains(arCIConfg)){
									arCIConfgList.add(reviseARPCISetting(arCIConfg));
								}else{
									logger.warn("duplicated argument id="+idarr[i].trim());
								}
								break;
							}
						}
						if(!flag){
							logger.error("Not Exists id="+idarr[i].trim());
							logger.error("testudo's json might contains error, details see readme's json instruction.");
							throw new Exception("please check your json file.");
						}
					}
				}else{
					if(StringUtils.isBlank(ids)){
						logger.warn("argument id is not setted, get the fist by default in json.");
					}
					ARPCISetting arpci=getARPCISetting(ids);
					if(arpci==null){
						logger.error("Not Exists id="+ids);
					}else{
						arCIConfgList.add(arpci);
					}
				}
				
				return arCIConfgList;
			}
		} catch (SecurityException
				| IllegalArgumentException e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}
	
	private static ARPCISetting reviseARPCISetting(ARPCISetting arCIConfg){
		IReviseARPCISetting arcisetting=null;
		if(StringUtils.isBlank(System.getProperty(CMDL_ARPRUNONMAVEN))){
			arcisetting=new ReviseARPCISetting();
		}else{
			arcisetting=new MavenReviseARPCISetting();
		}
		return arcisetting.reviseARPCISetting(arCIConfg);
	}
	
}
