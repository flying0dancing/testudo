package com.lombardrisk;

import org.apache.commons.lang3.StringUtils;

public interface IComFolder {
	
	//command-line arguments key
	String CMDL_ARPPROJECTFOLDER="project";//project folder
	String CMDL_ARPPRODUCTPREFIX="prefix";//subfolder under project folder, Deprecated
	String CMDL_ARPCICONFG="conf";
	String CMDL_ARPRODUCTID="id";
	String CMDL_ARPCIPROC="proc";
	String CMDL_ARPBUILDTYPE="release";
	String CMDL_ARPRUNONJENKINS="runOnJenkins";
    String CMDL_ARPRUNONMAVEN="runMaven";
	
	String JSON_FILENAME="testudo.json";	
	//String JSON_PATH="src/main/resources/exportSettings.json";
	String JSON_PATH=StringUtils.isBlank(System.getProperty(CMDL_ARPPROJECTFOLDER))?JSON_FILENAME:System.getProperty(CMDL_ARPPROJECTFOLDER)+System.getProperty("file.separator")+JSON_FILENAME;
	
	//test data folders
	String SOURCE_FOLDER="src"+System.getProperty("file.separator");
	String DPM_PATH="dpm"+System.getProperty("file.separator");
	String META_PATH="metadata"+System.getProperty("file.separator");
	String FORMS_PATH="forms"+System.getProperty("file.separator");
	String TRANS_PATH="transforms"+System.getProperty("file.separator");
	String SQLS_PATH="sqls"+System.getProperty("file.separator");
	String MANIFEST_FILE="manifest.xml";
	String INI_FILE_SUFFIX="_FORM_META.ini";
	String PRODUCT_PROP_FILE="package.properties";
	
	//product properties definition
	String OCELOT_CONFIG_SIGN_VERSION="ocelot.config.sign.version";
	String GEN_PRODUCT_DPM_VERSION="gen.product.dpm.version";
	String AR_INSTALLER_VERSION="ar.installer.version";
	String PACKAGE_NAME_PREFIX="package.name.prefix";
	String OCELOT_CONFIG_SIGN_JAR_WINDOWS="ocelot.config.sign.jar.windows";
	String OCELOT_CONFIG_SIGN_JAR_LINUX="ocelot.config.sign.jar.linux";
	
	//manifest.xml
	String IMP_VERSION="implementationVersion";
	String MAPPING_VERSION="mappingVersion";
	String ACCESSFILE="accessFile";
	String PREFIX="prefix";
	//package things
	String PACKAGE_SUFFIX=".zip";
	String PACKAGE_LRM_SUFFIX=".lrm";
	
	//user-defined, access database's table schema name
	String ACCESS_SCHEMA_INI="ACCESS_FORM_META.ini";
	

}
