package com.lombardrisk;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonSyntaxException;
import com.lombardrisk.pojo.ARPCISetting;
import com.lombardrisk.pojo.ExternalProject;
import com.lombardrisk.pojo.ZipSettings;
import com.lombardrisk.utils.Dom4jUtil;
import com.lombardrisk.utils.FileUtil;
import com.lombardrisk.utils.Helper;
import com.lombardrisk.utils.ReviseStrHelper;
/***
 * [maven product solution]
 * @author kun shen
 *
 */
public class MavenReviseARPCISetting implements IReviseARPCISetting, IComFolder{
	private final static Logger logger = LoggerFactory.getLogger(MavenReviseARPCISetting.class);
	private static String targetProjectPath=null;
	@Override
	public ARPCISetting reviseARPCISetting(ARPCISetting arCIConfg) {

		if(arCIConfg!=null){
			//revise "prefix", make sure it is lowercase
			String productPrefix=arCIConfg.getPrefix();
			if(StringUtils.isBlank(productPrefix)){
				//prefix must be set as a subfolder's name of project folder name
				throw new JsonSyntaxException("error: prefix is null, please set it value");
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
				//[maven product solution]change user.dir to project.dir
				projectPath=Helper.removeLastSlash(Helper.getParentPath(System.getProperty("project.dir")));
				productPath=projectPath+File.separator+arCIConfg.getPrefix();
				sourcePath=productPath+File.separator+SOURCE_FOLDER; //src/	
				metadataPath=sourcePath+META_PATH;
			}
			//[maven product solution]
			setTargetProjectPath(projectPath);
			//get target product path
			String targetProductPath=getTargetProjectPath()+File.separator+arCIConfg.getPrefix()+File.separator+"target";
			targetSrcPath=targetProductPath+File.separator+SOURCE_FOLDER;//current product(prefix)'s target source path
			metadataPath=targetSrcPath+META_PATH; //current product(prefix)'s target metadata path
			
			FileUtil.copyDirectory(sourcePath, targetSrcPath);
			
			arCIConfg.setMetadataPath(metadataPath);
			arCIConfg.setSrcPath(sourcePath);
			arCIConfg.setTargetSrcPath(targetSrcPath);
			FileUtil.createDirectories(arCIConfg.getMetadataPath());
			
			//revise "metadataStruct"
			String metadataStruct=arCIConfg.getMetadataStruct();
			if(StringUtils.isBlank(metadataStruct)){
				arCIConfg.setMetadataStruct(arCIConfg.getPrefix().toUpperCase()+INI_FILE_SUFFIX);
			}
			//revise "zipSettings"
			ZipSettings zipSetting=arCIConfg.getZipSettings();
			arCIConfg.setZipSettings(reviseZipSettings(zipSetting,sourcePath, targetSrcPath));
			
		}
		return arCIConfg;
	
	}

	public static String getTargetProjectPath() {
		return targetProjectPath;
	}

	public static void setTargetProjectPath(String targetProjectPatha) {
		targetProjectPath = targetProjectPatha;
	}

	public ZipSettings reviseZipSettings(ZipSettings zipSetting,String sourcePath,String targetSrcPath){
		if(zipSetting!=null){
			//revise "zipSettings"->"dpmFullPath"
			String dpmFullName=zipSetting.getDpmFullPath();
			FileUtil.createDirectories(targetSrcPath+DPM_PATH);
			if(StringUtils.isNotBlank(dpmFullName)){
				dpmFullName=ReviseStrHelper.defaultDpmFullName( dpmFullName, sourcePath, targetSrcPath,DPM_PATH);
			}else{	//need to do in maven solution
				String accdbFileNameInManifest=Dom4jUtil.updateElement(targetSrcPath+MANIFEST_FILE,ACCESSFILE ,null);
				dpmFullName=Helper.reviseFilePath(targetSrcPath+DPM_PATH+accdbFileNameInManifest);
				List<ExternalProject> externalProjects=zipSetting.getExternalProjects();
				if(externalProjects!=null && externalProjects.size()>0){
					for(ExternalProject externalpro:externalProjects){
						if(StringUtils.isNoneBlank(externalpro.getProject(),externalpro.getSrcFile()) ){
							String destDir=StringUtils.isBlank(externalpro.getDestDir())?targetSrcPath:
								Helper.reviseFilePath(targetSrcPath+File.separator+externalpro.getDestDir());
							FileUtil.copyExternalProject(Helper.reviseFilePath(Helper.getParentPath(System.getProperty("user.dir"))+
									externalpro.getProject()+File.separator+externalpro.getSrcFile()), destDir, externalpro.getUncompress());
							String dmpType=accdbFileNameInManifest.substring(accdbFileNameInManifest.lastIndexOf('.'));
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
			zipSetting.setDpmFullPath(dpmFullName);
			
			//revise "zipSettings"->"productProperties"
			String productPropsPath=zipSetting.getProductProperties();
			zipSetting.setProductProperties(ReviseStrHelper.revisePropsPath(getTargetProjectPath(),productPropsPath,PRODUCT_PROP_FILE));
		}
		return zipSetting;
	}
	

}
