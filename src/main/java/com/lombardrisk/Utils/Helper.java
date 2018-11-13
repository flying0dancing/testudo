package com.lombardrisk.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Helper {
	private final static Logger logger = LoggerFactory.getLogger(Helper.class);

	public static <T> Object filterListByID(List<T> list, Object value) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		return filterListBy(list,"getID",value);
	}
	
	public static <T> Object filterListByPrefix(List<T> list, Object value) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		return filterListBy(list,"getPrefix",value);
	}
	
	/**
     * get an generic object from a list by specified method
     *
     * @param list
     * @param by
     * @param value
     * @return an generic object
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
     */
    protected static <T> Object filterListBy(List<T> list, String by, Object value) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
    	if(list!=null && !list.isEmpty())
    	{
    		Method method=list.get(0).getClass().getDeclaredMethod(by);
    		for(T element:list)
    		{
    			if(method.invoke(element).equals(value))
    			{
    				return element;
    			}
    		}
    	}
    	return null;
    }
    
    public static String convertBlobToStr(Blob blob)
    {
    	try {
    		byte[] bytes=blob.getBytes(1l,(int)blob.length());
			return new String(bytes,"UTF-8");
			
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
    	
    	return null;
    }
    
    /**
     * revise/uniform file path separator, let file's separator followed OS's.
     * @param path
     * @return
     */
    public static String reviseFilePath(String path)
    {
    	if(StringUtils.isNotBlank(path)){
    		path=path.replace("\"", "");
    		
			if(System.getProperty("file.separator").equals("/")){
				path=path.replace("\\", "/");
				path=path.replaceAll("/+", "/");
			}else
			{
				path=path.replace("/", "\\");
				path=path.replaceAll("\\+", "\\");
			}
			if(path.contains(" "))
			{
				path="\""+path+"\"";
			}
		}
    	return path;
    }
    
    /***
     * remove last separator of path if it has.
     * @param path
     * @return
     */
    public static String removeLastSlash(String path)
	{
		if(StringUtils.isNotBlank(path)){
			path=path.replace("\"", "");
			if(path.endsWith("/") || path.endsWith("\\")){
				path=path.substring(0,path.length()-1);
			}
		}
		return path;
	}
    /***
     * get parent path, if it is the top folder, return itself
     * @param path
     * @return
     */
    public static String getParentPath(String path){
    	if(StringUtils.isNotBlank(path)){
    		path=removeLastSlash(path);
    		int lastSlash=path.lastIndexOf("\\")==-1?path.lastIndexOf("/"):path.lastIndexOf("\\");//get parent path
    		if(lastSlash>0)
    		{
    			path=path.substring(0,lastSlash)+System.getProperty("file.separator");
    		}else
    		{
    			path=path+System.getProperty("file.separator");
    		}
    		
    	}
    	return path;
    }

    public static void readme(String file){
    	try(InputStream is = ClassLoader.getSystemResourceAsStream(file)){
    		String line;

    		BufferedReader br = new BufferedReader(new InputStreamReader(is));
    		while ((line = br.readLine()) != null) {
    		    System.out.println(line);
    		}
    	} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    /**
	 * run external command
	 * @param commons
	 * @return
	 */
	public static Boolean runCmdCommand(String[] commons)
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
				logger.debug(str);
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
		if(flag){
			logger.info("cmd run OK.");
		}else{
			logger.error("cmd run failed.");
		}
		return flag;
	}

}
