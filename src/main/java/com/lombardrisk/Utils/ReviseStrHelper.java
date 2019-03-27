package com.lombardrisk.Utils;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lombardrisk.IComFolder;

public class ReviseStrHelper {
	
	final static Logger logger=LoggerFactory.getLogger(ReviseStrHelper.class);
	private ReviseStrHelper(){}
	/**
	 * @param dpmFullName should not be null
	 * @return
	 */
	public static String defaultDpmFullName(String dpmFullName,String sourcePath,String targetSrcPath, String dpmPath){
		if(!dpmFullName.contains("/") && !dpmFullName.contains("\\")){
			//dpmFullName just a file name without path
			dpmFullName=targetSrcPath+dpmPath+dpmFullName;
		}else{
			dpmFullName=Helper.reviseFilePath(dpmFullName);
			String dpmPathTemp=Helper.getParentPath(dpmFullName);
			String dpmName=dpmFullName.replace(dpmPathTemp, "");
			//copy access file
			if(!dpmPathTemp.contains(sourcePath)){
				FileUtil.copyFileToDirectory(dpmFullName, targetSrcPath+dpmPath);
			}
			dpmFullName=targetSrcPath+dpmPath+dpmName;//remap its dpmFullName to target folder
		}
		return dpmFullName;
	}
	
	public static String revisePropsPath(String argetProjectPath, String productPropsPath,String product_prop_file){
		if(StringUtils.isNotBlank(productPropsPath)){
			if(!productPropsPath.contains("/") && !productPropsPath.contains("\\")){
				productPropsPath=Helper.reviseFilePath(argetProjectPath+File.separator+productPropsPath);
			}else{
				productPropsPath=Helper.reviseFilePath(productPropsPath);
			}
		}else{
			productPropsPath=Helper.reviseFilePath(argetProjectPath+File.separator+product_prop_file);
		}
		return productPropsPath;
	}
}
