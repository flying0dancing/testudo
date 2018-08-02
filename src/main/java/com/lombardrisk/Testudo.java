package com.lombardrisk;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lombardrisk.Utils.DBInfo;
import com.lombardrisk.Utils.FileUtil;
import com.lombardrisk.Utils.Helper;
import com.lombardrisk.pojo.ARPCISetting;


/**
 * Hello world!
 *
 */
public class Testudo implements IComFolder
{
	private final static Logger logger = LoggerFactory.getLogger(Testudo.class);
    public static void main( String[] args )
    {
    	long begin=System.currentTimeMillis();
    	long end=begin;
		logger.info("total time(sec):"+(end-begin)/1000.00F);
		logger.info("start:");
		Boolean flag=false;
		//read args from command-line
		if(args.length>0){
			for(String s:args){
				String[] argKeyValue=s.split("=");
				argKeyValue[0]=argKeyValue[0].replaceAll("^\\-D?(.*)$", "$1");
				System.setProperty(argKeyValue[0], argKeyValue[1]);
			}
			
		}
		/*else{
			HelperDoc();
			end=System.currentTimeMillis();
			logger.info("total time(sec):"+(end-begin)/1000.00F);
			return;
		}*/
		String prefix=System.getProperty(CMDL_ARPRODUCTID);
		String proc=System.getProperty(CMDL_ARPCIPROC);
		String arpbuild=System.getProperty(CMDL_ARPBUILD,"b000");
		System.out.println("productID"+System.getProperty(CMDL_ARPRODUCTID));
		System.out.println("proc"+proc);
		System.out.println("Var:"+arpbuild);
		if(StringUtils.isBlank(prefix)){
			logger.warn("argument productPrefix is not setted, get the fist one by default.");
		}
		
		ARPCISetting arSetting=ARPCISettingManager.getARPCISetting(prefix);
		if(arSetting!=null){
			logger.info(arSetting.toString());
			if(StringUtils.isBlank(proc))
			{
				logger.warn("argument proc is not setted, run 2 by default.");
				proc="2";
			}
			if(proc.equals("1"))
			{
				DBInfo db=new DBInfo();
				db.setDbHelper(arSetting.getDatabaseServer());
				
				String iniFullName=arSetting.getMetadataPath()+System.getProperty("file.separator")+arSetting.getMetadataStruct();
				FileUtil.createNew(iniFullName);
				db.exportToDivides(arSetting.getPrefix(),arSetting.getRequiredTables().getDividedByReturnIds(),arSetting.getMetadataPath(),arSetting.getMetadataStruct());
				db.exportToSingle(arSetting.getPrefix(),arSetting.getRequiredTables().getSingles(),arSetting.getMetadataPath(),arSetting.getMetadataStruct());
				
				
			}else if(proc.equals("2")){
				ARPPack azipFile=new ARPPack();
				flag=azipFile.importMetadataToDpm(arSetting.getZipSettings().getDpmFullPath(),arSetting.getMetadataPath(),arSetting.getZipSettings().getRequiredMetadata(),arSetting.getMetadataPath()+System.getProperty("file.separator")+arSetting.getMetadataStruct());
				flag=azipFile.packageARProduct(arSetting.getSrcPath(), arSetting.getZipSettings().getZipFiles(), arSetting.getZipSettings().getProductProperties(), Helper.getParentPath(arSetting.getSrcPath()), arpbuild);
				
			}else if(proc.equalsIgnoreCase("all")){
				DBInfo db=new DBInfo();
				db.setDbHelper(arSetting.getDatabaseServer());
				
				String iniFullName=arSetting.getMetadataPath()+System.getProperty("file.separator")+arSetting.getMetadataStruct();
				FileUtil.createNew(iniFullName);
				db.exportToDivides(arSetting.getPrefix(),arSetting.getRequiredTables().getDividedByReturnIds(),arSetting.getMetadataPath(),arSetting.getMetadataStruct());
				db.exportToSingle(arSetting.getPrefix(),arSetting.getRequiredTables().getSingles(),arSetting.getMetadataPath(),arSetting.getMetadataStruct());
				
				ARPPack azipFile=new ARPPack();
				flag=azipFile.importMetadataToDpm(arSetting.getZipSettings().getDpmFullPath(),arSetting.getMetadataPath(),arSetting.getZipSettings().getRequiredMetadata(),arSetting.getMetadataPath()+System.getProperty("file.separator")+arSetting.getMetadataStruct());
				flag=azipFile.packageARProduct(arSetting.getSrcPath(), arSetting.getZipSettings().getZipFiles(), arSetting.getZipSettings().getProductProperties(), Helper.getParentPath(arSetting.getSrcPath()), arpbuild);
				
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
    	Helper.readme("readme.md");
	}
}
