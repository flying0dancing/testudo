package com.lombardrisk;

public interface IComFolder {
	
	//String JSON_PATH="src/main/resources/exportSettings.json";
	String JSON_PATH="testudo.json";
	//test data folders
	String SOURCE_FOLDER="src/";
	String DPM_PATH="dpm/";
	String META_PATH="Metadata/";
	String FORMS_PATH="forms/";
	String TRANS_PATH="transforms/";
	String MANIFEST_FILE="manifest.xml";
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
	//command-line arguments key
	String CMDL_ARPCICONFG="conf";
	String CMDL_ARPRODUCTID="id";
	String CMDL_ARPCIPROC="proc";
	String CMDL_ARPBUILD="buildvar";

}
