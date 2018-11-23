package com.lombardrisk;


import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lombardrisk.Utils.DBInfo;
import com.lombardrisk.Utils.FileUtil;
import com.lombardrisk.Utils.Helper;
import com.lombardrisk.pojo.ARPCISetting;
import com.lombardrisk.pojo.DBAndTables;


/**
 * start from this
 *
 */
public class Testudo implements IComFolder
{
	private final static Logger logger = LoggerFactory.getLogger(Testudo.class);
    public static void main( String[] args )
    {
    	long begin=System.currentTimeMillis();
    	long end=begin;
		logger.info("start running testudo:");
		//read args from command-line
		if(args.length>0){
			for(String s:args){
				if(s.contains("=")){
					String[] argKeyValue=s.split("=");
					argKeyValue[0]=argKeyValue[0].replaceAll("^\\-D?(.*)$", "$1");
					System.setProperty(argKeyValue[0], argKeyValue[1]);
				}else{
					s=s.replaceAll("^\\-D?(.*)$", "$1");
					System.setProperty(s,"true");
				}
				
			}
			
		}
		
		String iDinJoson=System.getProperty(CMDL_ARPRODUCTID);
		String proc=System.getProperty(CMDL_ARPCIPROC);
		/*String arpbuildtype=System.getProperty(CMDL_ARPBUILDTYPE);
		System.out.println("product Folder"+System.getProperty(CMDL_ARPPRODUCTPREFIX));
		System.out.println("product ID"+System.getProperty(CMDL_ARPRODUCTID));
		System.out.println("process"+proc);
		System.out.println("build type:"+arpbuildtype);*/
		
		if(StringUtils.isBlank(iDinJoson)){
			logger.warn("argument id is not setted, get the fist by default in json.");
		}
		
		ARPCISetting arSetting=ARPCISettingManager.getARPCISetting(iDinJoson);
		if(arSetting!=null){
			logger.info(arSetting.toString());
			if(StringUtils.isBlank(proc))
			{
				logger.warn("argument proc is not setted, run 2 by default.");
				proc="2";
			}

			if(proc.equals("1"))
			{
				readDBToMetadata(arSetting);
				
			}else if(proc.equals("2")){
				
				packMetadataAndFiles(arSetting);
				
			}else if(proc.equalsIgnoreCase("all")){
				
				readDBToMetadata(arSetting);
				packMetadataAndFiles(arSetting);
				
			}else
			{
				HelperDoc();
			}
		}else{
			HelperDoc();
		}
		end=System.currentTimeMillis();
		logger.info("total time(sec):"+(end-begin)/1000.00F);
		
    }
    
    private static void HelperDoc(){
    	//Helper.readme("readme.md");
    	logger.error("please see readme.md.");
	}
    
    private static void readDBToMetadata(ARPCISetting arSetting)
    {
    	String iniFullName=Helper.reviseFilePath(arSetting.getMetadataPath()+System.getProperty("file.separator")+arSetting.getMetadataStruct());
    	FileUtil.createNew(iniFullName);
		List<DBAndTables> dbAndTables=arSetting.getDatabaseServerAndTables();
		if(dbAndTables!=null && dbAndTables.size()>0){
			for(DBAndTables dbAndTable:dbAndTables){
				DBInfo db=new DBInfo(dbAndTable.getDatabaseServer());
				db.exportToDivides(arSetting.getPrefix(),dbAndTable.getRequiredTables().getDividedByReturnIds(),arSetting.getMetadataPath(),arSetting.getMetadataStruct());
				db.exportToSingle(arSetting.getPrefix(),dbAndTable.getRequiredTables().getSingles(),arSetting.getMetadataPath(),arSetting.getMetadataStruct());
				
			}
		}
    }
    
    private static void packMetadataAndFiles(ARPCISetting arSetting)
    {
    	String iniFullName=Helper.reviseFilePath(arSetting.getMetadataPath()+System.getProperty("file.separator")+arSetting.getMetadataStruct());
		if(FileUtil.exists(iniFullName)){
			ARPPack azipFile=new ARPPack();
			Boolean flag=azipFile.createNewDpm(arSetting.getZipSettings().getDpmFullPath());
			if(!flag){
				logger.error("error: create access database unsuccessful.");
				return;
			}
			List<String> metadataPaths=azipFile.importMetadataToDpm(arSetting.getZipSettings().getDpmFullPath(),arSetting.getMetadataPath(),arSetting.getZipSettings().getRequiredMetadata(),arSetting.getMetadataPath()+System.getProperty("file.separator")+arSetting.getMetadataStruct());
			if(metadataPaths!=null){
				List<String> returnNameVers=azipFile.getReturnNameAndVersions(arSetting.getZipSettings().getDpmFullPath(), metadataPaths);
				if(returnNameVers!=null){
					arSetting.getZipSettings().getZipFiles().addAll(returnNameVers);
				}
			}
			Boolean status=azipFile.execSQLs(arSetting.getZipSettings().getDpmFullPath(),arSetting.getTargetSrcPath(),arSetting.getZipSettings().getSqlFiles());
			if(status){
				azipFile.packageARProduct(arSetting.getTargetSrcPath(), arSetting.getZipSettings().getZipFiles(), arSetting.getZipSettings().getProductProperties(), Helper.getParentPath(arSetting.getTargetSrcPath()), System.getProperty(CMDL_ARPBUILDTYPE));
			}	
			
		}else{
			logger.error("error: invalid file["+iniFullName+"]");
		}
    }
  
}
