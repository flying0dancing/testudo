package com.lombardrisk;

import org.apache.commons.lang3.StringUtils;

public interface IComFolder {
	
	//command-line arguments key
	String CMDL_ARPPRODUCTPREFIX="prefix";
	String CMDL_ARPCICONFG="conf";
	String CMDL_ARPRODUCTID="id";
	String CMDL_ARPCIPROC="proc";
	String CMDL_ARPBUILDTYPE="release";
		
	//String JSON_PATH="src/main/resources/exportSettings.json";
	String JSON_PATH=StringUtils.isBlank(System.getProperty(CMDL_ARPPRODUCTPREFIX))?"testudo.json":System.getProperty(CMDL_ARPPRODUCTPREFIX)+System.getProperty("file.separator")+"testudo.json";
	
	//test data folders
	String SOURCE_FOLDER="src"+System.getProperty("file.separator");
	String DPM_PATH="dpm"+System.getProperty("file.separator");
	String META_PATH="Metadata"+System.getProperty("file.separator");
	String FORMS_PATH="forms"+System.getProperty("file.separator");
	String TRANS_PATH="transforms"+System.getProperty("file.separator");
	String SQLS_PATH="sqls"+System.getProperty("file.separator");
	String MANIFEST_FILE="manifest.xml";
	String INI_FILE_SUFFIX="_FORM_META.ini";
	String DPM_FILE_SUFFIX="_FORM_META.accdb";
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
	

}
