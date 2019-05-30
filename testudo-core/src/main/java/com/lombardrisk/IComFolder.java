package com.lombardrisk;

import org.apache.commons.lang3.StringUtils;

public interface IComFolder {

    //command-line arguments key
    String CMDL_ARPPROJECTFOLDER = "project";//project folder
    String CMDL_ARPCICONFG = "conf";
    String CMDL_ARPRODUCTID = "id";
    String CMDL_ARPCIPROC = "proc";
    String CMDL_ARPBUILDTYPE = "release";
    String CMDL_ARPRUNONJENKINS = "runOnJenkins";
    String CMDL_ARPRUNONMAVEN = "runMaven"; //[maven product solution]
    String JSON_FILENAME = "testudo.json";

    String FILE_SEPARATOR = "file.separator";
    String JSON_PATH = StringUtils.isBlank(System.getProperty(CMDL_ARPPROJECTFOLDER))
            ? JSON_FILENAME
            : System.getProperty(CMDL_ARPPROJECTFOLDER)
                    + System.getProperty(FILE_SEPARATOR) + JSON_FILENAME;

    //test data folders
    String SOURCE_FOLDER = "src" + System.getProperty(FILE_SEPARATOR);
    String DPM_PATH = "dpm" + System.getProperty(FILE_SEPARATOR);
    String META_PATH = "metadata" + System.getProperty(FILE_SEPARATOR);
    String MANIFEST_FILE = "manifest.xml";
    String INI_FILE_SUFFIX = "_FORM_META.ini";
    String PRODUCT_PROP_FILE = "package.properties";

    //product properties definition
    String PACKAGE_NAME_PREFIX = "package.name.prefix";
    String IMP_VERSION = "implementationVersion";
    String ACCESSFILE = "accessFile";
    String PREFIX = "prefix";

    //package things
    String PACKAGE_SUFFIX = ".zip";
    String PACKAGE_LRM_SUFFIX = ".lrm";
    String PACKAGE_LRM_SIGN_SUFFIX = "_sign.lrm";

    //user-defined, access database's table schema name
    String ACCESS_SCHEMA_INI = "ACCESS_FORM_META.ini";
}
