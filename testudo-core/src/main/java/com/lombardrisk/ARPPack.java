package com.lombardrisk;

import com.lombardrisk.pojo.DatabaseServer;
import com.lombardrisk.pojo.TempACCESSDB;
import com.lombardrisk.pojo.ZipSettings;
import com.lombardrisk.status.BuildStatus;
import com.lombardrisk.utils.DBInfo;
import com.lombardrisk.utils.Dom4jUtil;
import com.lombardrisk.utils.FileUtil;
import com.lombardrisk.utils.Helper;
import com.lombardrisk.utils.PropHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ARPPack implements IComFolder {

    private final static Logger logger = LoggerFactory.getLogger(ARPPack.class);
    private final static String REGEX_1="#.*?_";
    private final static String UNDERLINE_1 ="_";
    private final static String REGEX_2="#.*";
    private final static String SHARP_1="#";
    private final static String BLANK="";

    
    private ARPPack(){}

    

    /***
     * import all filtered metadata (*.csv files) to access database
     * @param csvParentPath it gets value from <I>json file</I> ->"exportPath"
     * @param csvPaths it gets value from <I>json file</I>->"zipSettings"->"requiredMetadata"
     * @param schemaFullName It's a configuration file, which contains all tables' definition.
     * @return return metadata (*.csv files) full paths, return null if error occurs.
     */
    public static List<String> importMetadataToDpm(final String csvParentPath,final List<String> csvPaths, String schemaFullName,
													final ZipSettings zipSet) {
        if (StringUtils.isBlank(csvParentPath)) {
            return null;
        }
        Helper.removeDuplicatedElements(csvPaths);
        if (Helper.isEmptyList(csvPaths)) {
            return null;
        }
        schemaFullName=Helper.reviseFilePath(schemaFullName);
        String dbFullName=zipSet.getDpmFullPath();
        DBInfo dbInfo = new DBInfo(new DatabaseServer("accessdb", "", dbFullName, "", ""));
        dbInfo.getDbHelper().connect();
        List<String> subFolderNames=FileUtil.getSubFolderNames(csvParentPath);
        String folderRegex = FileUtil.getFolderRegex(subFolderNames);
        String userSchemaFullName=schemaFullName.replace(
                FileUtil.getFileNameWithSuffix(schemaFullName),
                ACCESS_SCHEMA_INI);
        dbInfo.setDefaultSchemaFullName(userSchemaFullName);
        dbInfo.setDefaultSchemaExist(userSchemaFullName);

        dbInfo.createAccessDBTable("Ref",schemaFullName);
        dbInfo.createAccessDBTable("GridRef",schemaFullName);

        List<String> realCsvFullPaths = new ArrayList<>();
        List<String> realNames=new ArrayList<>();
        List<String> realCsvFullPathsTmp;
       
        String tableName;
        Boolean flag=true;
        //long begin, end;
        logger.info("================= import metadata into DPM =================");
        for (String pathTmp : csvPaths) {
            realCsvFullPathsTmp =
                    FileUtil.getFilesByFilter(Helper.reviseFilePath(csvParentPath + System.getProperty("file" +
                            ".separator") + pathTmp), null,false);
            if (realCsvFullPathsTmp.size() <= 0) {
                logger.error("error: invalid path [" + csvParentPath + System.getProperty("file.separator") + pathTmp + "]");
            }else{

                for(String pathTmp2:realCsvFullPathsTmp){
                    realCsvFullPaths.add(pathTmp2);
                    tableName=getTableFromMetaName(pathTmp2,folderRegex);

                    if(!realNames.contains(tableName)){
                        dbInfo.createAccessDBTable(tableName,schemaFullName);
                        realNames.add(tableName);
                    }
                    //begin=System.currentTimeMillis();
                    flag = dbInfo.importCsvToAccess(tableName, Helper.reviseFilePath(pathTmp2));
                    //end=System.currentTimeMillis();
                    if (!flag) {
                        BuildStatus.getInstance().recordError();
                        logger.error("import [" + pathTmp2 + "] to " + tableName + " fail.");
                        break;
                    } else {
                        logger.info("import [" + pathTmp2 + "] to " + tableName + " successfully.");
                    }
                }
            }
            if(!flag){
                realCsvFullPaths=null;
                break;
            }
        }

        if (Helper.isEmptyList(realCsvFullPaths)) {
            dbInfo.getDbHelper().close();
            return null;
        }else {
            addReturnNamesAndVersionWrap(dbInfo,realCsvFullPaths,csvPaths,zipSet);
            String draftDBFullName=TempACCESSDB.INSTANCE.getDbFullName();
            if(StringUtils.isNotBlank(draftDBFullName)){
                dbInfo.getDbHelper().getAccdb().copyAccessDBTables(realNames,draftDBFullName);
                dbInfo.getDbHelper().close();

                logger.info("Rename dpm name: " + TempACCESSDB.INSTANCE.getName()+ " to " + FileUtil.getFileNameWithSuffix(dbFullName));
                FileUtil.renameTo(draftDBFullName, dbFullName);
                TempACCESSDB.INSTANCE.initial(null,null);//must set to null for all sub products running.
            }else {
                dbInfo.getDbHelper().close();
            }
			
        }
        Runtime.getRuntime().gc();
        return realCsvFullPaths;
    }


    private static void addReturnNamesAndVersionWrap(final DBInfo dbInfo, final List<String> realCsvFullPaths,
                                                     final List<String> csvPaths,final ZipSettings zipSet){
        if(!csvPaths.contains("*.csv")){
            List<String> returnNameVers=getReturnNameAndVersions(dbInfo,realCsvFullPaths);
            if(!Helper.isEmptyList(returnNameVers)){
                zipSet.getZipFiles().addAll(returnNameVers);
            }
        }
    }


    /***
     * read metadata (*.csv file) name's returnId, and then through dbFullName and its tableName(rets which stored definition of all returns), find its return name and version
     * @param csvFullPaths metadata (*.csv files) full paths
     * @return return a list of all returns' <I>name_version</I>, return null if error occurs.
     */
    public static List<String> getReturnNameAndVersions(final DBInfo dbInfo, final List<String> csvFullPaths) {
        if (Helper.isEmptyList(csvFullPaths)) {
            return null;
        }
        List<String> nameAndVers = new ArrayList<String>();

        Boolean flag=dbInfo.getDbHelper().getAccdb().accessTableExistence("Rets");
        if(flag){
            for (String csvPath : csvFullPaths) {
                String csvName = FileUtil.getFileNameWithoutSuffix(csvPath);
                if (!csvName.contains(UNDERLINE_1)) continue;
                String[] nameParts = csvName.split(UNDERLINE_1);
                if (!nameParts[1].matches("\\d+")) continue;
                String returnId = nameParts[1];
                String returnNameVer = dbInfo.getReturnAndVersion(returnId);
                if (!returnNameVer.equals(BLANK) && !nameAndVers.contains(returnNameVer)) {
                    nameAndVers.add(returnNameVer);
                }
            }
        }else{
            BuildStatus.getInstance().recordError();
            logger.error("cannot found Rets");
        }

        return nameAndVers;
    }

    public static Boolean execSQLs(final String dbFullName,final String sourcePath,final List<String> sqlFileNames,final String excludeFileFilters) {
        Boolean flag = true;
        if (Helper.isEmptyList(sqlFileNames)) {
            //means testudo.json doesn't provide sqlFiles.
            return flag;
        }
        logger.info("================= execute SQLs =================");
        List<String> realFullPaths = getFileFullPaths(sourcePath, sqlFileNames, excludeFileFilters,false);
        if (Helper.isEmptyList(realFullPaths)) {
            BuildStatus.getInstance().recordError();
            logger.error("error: sqlFiles are invalid files or filters.");
            return false;//illegal, no invalid files need to execute if it set sqlFiles
        }
        DBInfo dbInfo = new DBInfo(new DatabaseServer("accessdb", "", dbFullName, "", ""));
        dbInfo.getDbHelper().connect();
        Boolean status;
        for (String fileFullPath : realFullPaths) {
            logger.info("sql statements in file: " + fileFullPath);
            String fileContent = FileUtil.getSQLContent(fileFullPath);
            if (fileContent.contains(";")) {
                flag=execSQLs(dbInfo,fileContent);
            } else if (StringUtils.isNotBlank(fileContent)) {
                logger.info("execute sql:" + fileContent);
                status = dbInfo.executeSQL(fileContent.trim());
                if (!status) {
                    BuildStatus.getInstance().recordError();
                    logger.error("execute failed.");
                    flag = false;
                } else {
                    logger.info("execute OK.");
                }
            }
            if(!flag){
                break;
            }
        }
        dbInfo.getDbHelper().close();
        return flag;
    }

    private static Boolean execSQLs(final DBInfo dbInfo,final String fileContent){
        Boolean status=true;
        String[] sqlStatements = fileContent.split(";");
        for (String sql : sqlStatements) {
            if (StringUtils.isNotBlank(sql)) {
                logger.info("execute sql:" + sql.trim());
                status = dbInfo.executeSQL(sql.trim());
                if (!status) {
                    BuildStatus.getInstance().recordError();
                    logger.error("execute failed.");
                    break;
                } else {
                    logger.info("execute OK.");
                }
            }
        }
        return status;
    }
    /**
     * @param sourcePath    should follow AR for product's folder structure
     * @param zipSet
     * @param propFullPath  the full path of package.properties, it should be get value from <I>json file</I>->"zipSettings"-> "productProperties"
     * @param zipPath       the path of package(.zip, .lrm)
     * @param buildType     blank(null) represents it is internal build, true represents it is release build
     * @return
     */
    public static Boolean packageARProduct(String sourcePath, ZipSettings zipSet, String propFullPath, String zipPath, String buildType) {
        if (StringUtils.isBlank(sourcePath)) {
            return false;
        }
        logger.info("================= package files =================");
        //get all packaged files
        //String productPrefix=FileUtil.getFileNameWithSuffix(Helper.getParentPath(sourcePath)).toUpperCase().replaceAll("\\(\\d+\\)", "");
        String productPrefix = Dom4jUtil.updateElement(sourcePath + MANIFEST_FILE, PREFIX, null);
        if (StringUtils.isBlank(productPrefix)) {
            productPrefix = FileUtil.getFileNameWithSuffix(Helper.getParentPath(sourcePath))
                    .toUpperCase().replaceAll("\\(\\d+\\)", BLANK);
        }
        Boolean flag = true;
        List<String> packFileNames = zipSet.getZipFiles();
        long begin=System.currentTimeMillis();
        System.out.println("calculate compressed folders and files ");
        List<String> realFullPaths = getFileFullPaths(sourcePath, packFileNames, zipSet.getExcludeFileFilters(),true);
        System.out.println("calculate used time(sec):" + (System.currentTimeMillis() - begin) / MILLISECONDS_PER_SECOND);
        if (realFullPaths == null) {
            BuildStatus.getInstance().recordError();
            logger.error("error: zipFiles are invalid files or filters.");
            return false;
        }
        String arpbuild = null;
        if (StringUtils.isBlank(buildType)) {//not provided -Drelease
            arpbuild = String.valueOf(System.currentTimeMillis());
        } else {//provided -Drelease
            if (!buildType.equals("true")) {//provided like -Drelease=b7, true means only a flag -Drelease
                arpbuild = buildType;
            }
        }
        List<String> pathsInManifest = Dom4jUtil.getPathFromElement(sourcePath + MANIFEST_FILE, sourcePath);
        if (!Helper.isEmptyList(pathsInManifest)) {
            for (String pathtmp : pathsInManifest) {
                if (!realFullPaths.contains(pathtmp)) {
                    realFullPaths.add(pathtmp);
                }
            }
        }
        //modify implementationVersion in manifest.xml
        String packageVersion = Dom4jUtil.updateElement(sourcePath + MANIFEST_FILE, IMP_VERSION, arpbuild);

        if (FileUtil.exists(propFullPath)) {
            PropHelper.loading(propFullPath);
        } else {
            logger.warn("warn: cannot found file [" + propFullPath + "]");
        }

        //zipped and lrm product
        String packageNamePrefix = PropHelper.getProperty(PACKAGE_NAME_PREFIX);
        packageNamePrefix = StringUtils.isBlank(packageNamePrefix) ? productPrefix + UNDERLINE_1 : packageNamePrefix + productPrefix + UNDERLINE_1;
        String packageNameSuffix = null;//PropHelper.getProperty(AR_INSTALLER_VERSION);
        packageNameSuffix = StringUtils.isBlank(packageNameSuffix) ? BLANK : "_for_AR_v" + packageNameSuffix;
        String zipFileNameWithoutSuffix = packageNamePrefix + "v" + packageVersion + packageNameSuffix;
        String zipFullPathWithoutSuffix = Helper.reviseFilePath(zipPath + "/" + zipFileNameWithoutSuffix);

        if (StringUtils.isNotBlank(zipFileNameWithoutSuffix)) {
            flag = FileUtil.zipFilesAndFolders(sourcePath, realFullPaths, Helper.reviseFilePath(zipFullPathWithoutSuffix + PACKAGE_SUFFIX));
            if (!flag) return flag;
            if (new File(PropHelper.SCRIPT_LRM_PRODUCT).isFile() && StringUtils.isBlank(System.getProperty(CMDL_ARPRUNONMAVEN))) {
                String[] commons = {"java", "-jar", PropHelper.SCRIPT_LRM_PRODUCT, Helper.reviseFilePath(zipFullPathWithoutSuffix + PACKAGE_SUFFIX)};
                flag = Helper.runCmdCommand(commons);
            } else {
                logger.warn("warn: cannot found file [" + PropHelper.SCRIPT_LRM_PRODUCT + "]");
            }
        } else {
            flag = false;
        }

        if (flag) {
            logger.info("package named: " + zipFullPathWithoutSuffix + PACKAGE_SUFFIX);
            if (new File(zipFullPathWithoutSuffix + PACKAGE_LRM_SIGN_SUFFIX).isFile()) {
                logger.info("package named: " + zipFullPathWithoutSuffix + PACKAGE_LRM_SUFFIX);
                FileUtil.renameTo(zipFullPathWithoutSuffix + PACKAGE_LRM_SIGN_SUFFIX, zipFullPathWithoutSuffix + PACKAGE_LRM_SUFFIX);
            } else {
                logger.info("only generate .zip package.");
            }
            logger.info("package successfully.");
        } else {
            BuildStatus.getInstance().recordError();
            logger.error("error: package with failures.");
        }
        Runtime.getRuntime().gc();
        return flag;
    }


    /***
     * get all file's full path under sourcePath, with filters
     * @param sourcePath
     * @param filters
     * @return if get nothing or arguments contains blank argument, return null.
     */
    public static List<String> getFileFullPaths(final String sourcePath,final List<String> filters,
                                                final String excludeFilters,final Boolean keepDirStructure) {
        Helper.removeDuplicatedElements(filters);
        if (Helper.isEmptyList(filters) || StringUtils.isBlank(sourcePath)) {
            return null;
        }

        List<String> realFilePaths = new ArrayList<String>();
        List<String> realFullPathsTmp;
        String filePathTmp;
        for (String filter : filters) {
            filePathTmp=Helper.reviseFilePath(sourcePath + "/"+ filter);
            if(filter.equalsIgnoreCase("xbrl")){
                realFullPathsTmp = FileUtil.getFilesByFilter(filePathTmp, null,false);
            }else{
                realFullPathsTmp = FileUtil.getFilesByFilter(filePathTmp, excludeFilters,keepDirStructure);
            }
            if (realFullPathsTmp.size() <= 0) {
                //BuildStatus.getInstance().recordError(); //solve ARPA-72
                logger.warn("error: cannot search [" + filter + "] under path [" + sourcePath + "]");

            }else{
                for (String pathTmp : realFullPathsTmp) {
                    if (!realFilePaths.contains(pathTmp)) {
                        realFilePaths.add(pathTmp);
                    }
                }
            }
        }
        if (realFilePaths.size() <= 0) return null;
        return realFilePaths;
    }

    private static String getTableFromMetaName(final String pathTmp2,final String folderRegex){
        String name_returnId="";
        String tableNameWithDB = FileUtil.getFileNameWithoutSuffix(pathTmp2);
        logger.debug("1 table name with DB {}", tableNameWithDB);
        String tableName = tableNameWithDB.replaceAll(REGEX_1, UNDERLINE_1);
        logger.debug("1 table name {}", tableName);

        if (!tableName.contains("_")) {
            tableName = tableNameWithDB.replaceAll(REGEX_2, BLANK);
            logger.debug("2 table name {}", tableName);
        }
        Pattern p = Pattern.compile(folderRegex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher m = p.matcher(tableName);
        if (m.find()) {
            tableName = m.group(1);
            logger.debug("3 table name {}", tableName);
            name_returnId = m.group(2);
        }
        tableNameWithDB = tableNameWithDB.replace(name_returnId, BLANK);
        if (name_returnId.equals(BLANK) && tableNameWithDB.contains(UNDERLINE_1)) {
            tableName = tableNameWithDB.split(SHARP_1)[0];
            logger.debug("4 table name {}", tableName);
        }
        logger.debug("X table name {}", tableName);
        return tableName;
    }
}
