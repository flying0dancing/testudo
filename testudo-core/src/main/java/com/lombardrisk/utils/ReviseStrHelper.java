package com.lombardrisk.utils;

import java.io.File;
import org.apache.commons.lang3.StringUtils;


public final class ReviseStrHelper {
	private ReviseStrHelper(){
		throw new IllegalStateException("Utility class");
	}
	//only used on IReviseARPCISetting
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
	
	//only used on IReviseARPCISetting
	public static String revisePropsPath(String argetProjectPath, String productPropsPath,String productPropFile){
		if(StringUtils.isNotBlank(productPropsPath)){
			if(!productPropsPath.contains("/") && !productPropsPath.contains("\\")){
				productPropsPath=Helper.reviseFilePath(argetProjectPath+File.separator+productPropsPath);
			}else{
				productPropsPath=Helper.reviseFilePath(productPropsPath);
			}
		}else{
			productPropsPath=Helper.reviseFilePath(argetProjectPath+File.separator+productPropFile);
		}
		return productPropsPath;
	}
}
