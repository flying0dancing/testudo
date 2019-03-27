package com.lombardrisk.Utils;

import static java.lang.String.format;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PropHelper {
	private final static Logger logger = LoggerFactory.getLogger(PropHelper.class);
	private final static Properties props = new Properties();
	private static boolean hasLoaded = false;
	
	public static final String SCRIPT_PATH = getScriptPath(getProperty("script.path"),"ci-script");
	//public static final String SCRIPT_GEN_PRODUCT= getScriptGenProdPath(getProperty("script.zipProduct"));
	
	public static final String SCRIPT_LRM_PRODUCT= getScriptGenDPMPath("ocelot-config-sign-1.15.jar");//[maven product solution: change getProperty("script.lrmProduct") to this]
	
	
	private static void load(String file){
		try(InputStream is = ClassLoader.getSystemResourceAsStream(file)){
			if (is != null) {
                props.load(is);
                is.close();
            } else {
                logger.warn(format("%s was not provided", file));
            }
		}catch(IOException e){
			logger.error(file, e);
		}
	}
	@SuppressWarnings("unused")
	public static void loading(String file){
		try(InputStream is=new FileInputStream(file)){
			if(is !=null){
				props.load(is);
				is.close();
			}else{
				logger.warn(format("%s was not provided", file));
			}
		}catch(IOException e){
			logger.error(file, e);
		}
	}
	
	private static boolean load() {
        load(System.getProperty("testudo.prop", "testudo.properties"));
        return true;
    }
	
	public static String getProperty(String key) {
		if(!hasLoaded){
			hasLoaded = load();
		}
		
		return System.getProperty(key, props.getProperty(key));
	}
	
	
	private static String getScriptPath(String path,String folder)
	{
		if(StringUtils.isBlank(path))
		{
			path=System.getProperty("user.dir");
			path=Helper.removeLastSlash(path);
			
			path=Helper.getParentPath(path)+folder;
		}else{
			path=Helper.removeLastSlash(path);
		}
		
		return Helper.reviseFilePath(path);
	}
	
	private static String getScriptGenDPMPath(String value)
	{
		if(!value.contains("/") && !value.contains("\\")){
			value=SCRIPT_PATH+"/"+value;
		}
		value=Helper.reviseFilePath(value);
		
		return value;
	}
	
	@SuppressWarnings("unused")
	@Deprecated
	private static String getScriptGenProdPath(String value)
	{
		if(System.getProperty("file.separator").equals("/"))
		{
			value=value.substring(0,value.lastIndexOf("."))+".sh";
		}else{
			value=value.substring(0,value.lastIndexOf("."))+".bat";
		}
		value=getScriptGenDPMPath(value);//same operations with this function here
		return value;
	}
	

}
