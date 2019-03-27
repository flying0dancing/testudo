package com.lombardrisk;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonSyntaxException;
import com.lombardrisk.Utils.Dom4jUtil;
import com.lombardrisk.Utils.FileUtil;
import com.lombardrisk.Utils.Helper;
import com.lombardrisk.pojo.ARPCISetting;
import com.lombardrisk.pojo.ExternalProject;
import com.lombardrisk.pojo.ZipSettings;

public class MavenReviseARPCISetting implements IReviseARPCISetting, IComFolder{
	private final static Logger logger = LoggerFactory.getLogger(MavenReviseARPCISetting.class);
	private static String targetProjectPath=null;
	@Override
	public ARPCISetting reviseARPCISetting(ARPCISetting arCIConfg) {

		if(arCIConfg!=null){
			//revise "prefix", make sure it is lowercase
			String productPrefix=arCIConfg.getPrefix();
			if(StringUtils.isBlank(productPrefix)){
				throw new JsonSyntaxException("error: prefix is null, please set it value");//prefix must be set as a subfolder's name of project folder name
				
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
				projectPath=Helper.removeLastSlash(Helper.getParentPath(System.getProperty("project.dir")));//[maven product solution]change user.dir to project.dir
				productPath=projectPath+File.separator+arCIConfg.getPrefix();
				sourcePath=productPath+File.separator+SOURCE_FOLDER; //src/	
				metadataPath=sourcePath+META_PATH;
			}
			//[maven product solution]
			targetProjectPath=projectPath;
			//get target product path
			String targetProductPath=targetProjectPath+File.separator+arCIConfg.getPrefix()+File.separator+"target";
			targetSrcPath=targetProductPath+File.separator+SOURCE_FOLDER;//current product(prefix)'s target source path
			metadataPath=targetSrcPath+META_PATH; //current product(prefix)'s target metadata path
			//if(FileUtil.exists(targetProductPath)){
			//	FileUtil.deleteDirectory(targetProductPath);
			//}
			FileUtil.copyDirectory(sourcePath, targetProductPath);
			
			
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
					String accdbFileNameInManifest=Dom4jUtil.updateElement(targetSrcPath+MANIFEST_FILE,ACCESSFILE ,null);
					//dpmFullName=Helper.reviseFilePath(targetSrcPath+DPM_PATH+arCIConfg.getPrefix().toUpperCase()+DPM_FILE_SUFFIX);
					dpmFullName=Helper.reviseFilePath(targetSrcPath+DPM_PATH+accdbFileNameInManifest);
					List<ExternalProject> externalProjects=zipSetting.getExternalProjects();
					if(externalProjects!=null && externalProjects.size()>0){
						for(ExternalProject externalpro:externalProjects){
							if(StringUtils.isNoneBlank(externalpro.getProject(),externalpro.getSrcFile()) ){
								String destDir=StringUtils.isBlank(externalpro.getDestDir())?targetSrcPath:Helper.reviseFilePath(targetSrcPath+File.separator+externalpro.getDestDir());
								FileUtil.copyExternalProject(Helper.reviseFilePath(Helper.getParentPath(System.getProperty("user.dir"))+externalpro.getProject()+File.separator+externalpro.getSrcFile()), destDir, externalpro.getUncompress());
								String dmpType=accdbFileNameInManifest.substring(accdbFileNameInManifest.lastIndexOf("."));
								List<String> accdbfiles=FileUtil.getFilesByFilter(Helper.reviseFilePath(targetSrcPath+"/"+DPM_PATH+"*"+dmpType),null);
								if(accdbfiles.size()>0){
									String accdbFileName=FileUtil.getFileNameWithSuffix(accdbfiles.get(0));
									if(!accdbFileName.equalsIgnoreCase(accdbFileNameInManifest)){
										logger.info("Rename dpm name: "+ accdbFileName +" to "+accdbFileNameInManifest);
										FileUtil.renameTo(accdbfiles.get(0), targetSrcPath+File.separator+DPM_PATH+accdbFileNameInManifest);
									}
								}
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
