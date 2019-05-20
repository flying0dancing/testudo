package com.lombardrisk;

import com.lombardrisk.pojo.DatabaseServer;
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

public class ARPPack implements IComFolder {

    private final static Logger logger = LoggerFactory.getLogger(ARPPack.class);

    public enum DBInfoSingle {
        INSTANCE;
        private DBInfo dbInfo;

        public DBInfo getDbInfo() {
            return dbInfo;
        }

        public void setDbInfo(String dbFullName) {
            this.dbInfo = new DBInfo(new DatabaseServer("accessdb", "", dbFullName, "", ""));
        }
    }

    /***
     * create a new access database(.accdb) if no existed, otherwise use the existed one.
     * @param dbFullName
     * @return
     */
    public Boolean createNewDpm(String dbFullName) {
        Boolean flag = false;
        DBInfoSingle.INSTANCE.setDbInfo(dbFullName);
        flag = DBInfoSingle.INSTANCE.getDbInfo().createAccessDB();
        return flag;
    }

    /***
     * import all filtered metadata (*.csv files) to access database
     * @param db it's a access database, its location or schema should be get value from <I>json file</I>->"zipSettings"->"dpmFullPath"
     * @param csvParentPath it gets value from <I>json file</I> ->"exportPath"
     * @param csvPaths it gets value from <I>json file</I>->"zipSettings"->"requiredMetadata"
     * @param schemaFullName It's a configuration file, which contains all tables' definition.
     * @return return metadata (*.csv files) full paths
     */
    public List<String> importMetadataToDpm(DBInfo db, String csvParentPath, List<String> csvPaths, String schemaFullName) {
        return importMetadataToDpm(csvParentPath, csvPaths, schemaFullName);
    }

    /***
     * import all filtered metadata (*.csv files) to access database
     * @param csvParentPath it gets value from <I>json file</I> ->"exportPath"
     * @param csvPaths it gets value from <I>json file</I>->"zipSettings"->"requiredMetadata"
     * @param schemaFullName It's a configuration file, which contains all tables' definition.
     * @return return metadata (*.csv files) full paths, return null if error occurs.
     */
    public List<String> importMetadataToDpm(String csvParentPath, List<String> csvPaths, String schemaFullName) {
        if (StringUtils.isBlank(csvParentPath)) {
            return null;
        }
        DBInfo dbInfo = DBInfoSingle.INSTANCE.getDbInfo();
        //dbInfo.createAccessTables(schemaFullName);
        if (csvPaths == null || csvPaths.size() <= 0) {
            return null;
        }
        List<String> realCsvFullPaths = new ArrayList<>();
        String name_returnId;
        String folderregex = FileUtil.getFolderRegex(csvParentPath);

        logger.info("================= import metadata into DPM =================");
        for (String pathTmp : csvPaths) {
            List<String> realCsvFullPathsTmp =
                    FileUtil.getFilesByFilter(Helper.reviseFilePath(csvParentPath + System.getProperty("file" +
                            ".separator") + pathTmp), null);
            if (realCsvFullPathsTmp.size() <= 0) {
                BuildStatus.getInstance().recordError();
                logger.error("error: invalid path [" + csvParentPath + System.getProperty("file.separator") + pathTmp + "]");
                continue;
            }
            for (String pathTmp2 : realCsvFullPathsTmp) {
                if (!realCsvFullPaths.contains(pathTmp2)) {
                    name_returnId = "";
                    realCsvFullPaths.add(pathTmp2);
                    logger.info("import metadata file:" + pathTmp2);
                    String tableNameWithDB = FileUtil.getFileNameWithoutSuffix(pathTmp2);
                    logger.debug("1 table name with DB {}", tableNameWithDB);
                    String tableName = tableNameWithDB.replaceAll("#.*?_", "_");
                    logger.debug("1 table name {}", tableName);

                    if (!tableName.contains("_")) {
                        tableName = tableNameWithDB.replaceAll("#.*", "");
                        logger.debug("2 table name {}", tableName);
                    }
                    Pattern p = Pattern.compile(folderregex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
                    Matcher m = p.matcher(tableName);
                    if (m.find()) {
                        tableName = m.group(1);
                        logger.debug("3 table name {}", tableName);
                        name_returnId = m.group(2);
                    }
                    tableNameWithDB = tableNameWithDB.replace(name_returnId, "");
                    if (name_returnId.equals("") && tableNameWithDB.contains("_")) {
                        tableName = tableNameWithDB.split("#")[0];
                        logger.debug("4 table name {}", tableName);
                    }
                    logger.debug("X table name {}", tableName);
                    Boolean flag = dbInfo.importCsvToAccess(tableName, tableNameWithDB,
                            Helper.reviseFilePath(pathTmp2), Helper.reviseFilePath(schemaFullName));
                    if (!flag) {
                        BuildStatus.getInstance().recordError();
                        logger.error("import metadata[" + pathTmp2 + "] to " + tableName + " fail.");
                    } else {
                        logger.info("import metadata[" + pathTmp2 + "] to " + tableName + " successfully.");
                    }
                }
            }
        }
        if (realCsvFullPaths.size() <= 0) {
            return null;
        }
        return realCsvFullPaths;
    }

    /***
     * read metadata (*.csv file) name's returnId, and then through dbFullName and its tableName(rets which stored definition of all returns), find its return name and version
     * @param csvFullPaths metadata (*.csv files) full paths
     * @return return a list of all returns' <I>name_version</I>, return null if error occurs.
     */
    public List<String> getReturnNameAndVersions(List<String> csvFullPaths) {
        if (csvFullPaths == null || csvFullPaths.size() <= 0) return null;
        List<String> nameAndVers = new ArrayList<String>();

        DBInfo dbInfo = DBInfoSingle.INSTANCE.getDbInfo();
        for (String csvPath : csvFullPaths) {
            String csvName = FileUtil.getFileNameWithoutSuffix(csvPath);
            if (!csvName.contains("_")) continue;
            String[] nameParts = csvName.split("_");
            if (!nameParts[1].matches("\\d+")) continue;
            String returnId = nameParts[1];
            String returnNameVer = dbInfo.getReturnAndVersion(returnId);
            if (!returnNameVer.equals("") && !nameAndVers.contains(returnNameVer)) {
                nameAndVers.add(returnNameVer);
            }
        }
        if (nameAndVers.size() <= 0) return null;
        return nameAndVers;
    }

    public Boolean execSQLs(String sourcePath, List<String> sqlFileNames, String excludeFileFilters) {
        Boolean flag = true;
        if (sqlFileNames == null || sqlFileNames.size() <= 0) return true;//means testudo.json doesn't provide sqlFiles.
        logger.info("================= execute SQLs =================");
        List<String> realFullPaths = getFileFullPaths(sourcePath, sqlFileNames, excludeFileFilters);
        if (realFullPaths == null || realFullPaths.size() <= 0) {
            BuildStatus.getInstance().recordError();
            logger.error("error: sqlFiles are invalid files or filters.");
            return false;//illegal, no invalid files need to execute if it set sqlFiles
        }
        DBInfo dbInfo = DBInfoSingle.INSTANCE.getDbInfo();
        for (String fileFullPath : realFullPaths) {
            logger.info("sql statements in file: " + fileFullPath);
            String fileContent = FileUtil.getFileContent1(fileFullPath);
            if (fileContent.contains(";")) {
                String[] sqlStatements = fileContent.split(";");
                for (String sql : sqlStatements) {
                    if (StringUtils.isNotBlank(sql)) {
                        logger.info("execute sql:" + sql.trim());
                        Boolean status = dbInfo.executeSQL(sql.trim());
                        if (!status) {
                            BuildStatus.getInstance().recordError();
                            logger.error("execute failed.");
                            flag = false;
                        } else {
                            logger.info("execute OK.");
                        }
                    }
                }
            } else if (StringUtils.isNotBlank(fileContent)) {
                logger.info("execute sql:" + fileContent);
                Boolean status = dbInfo.executeSQL(fileContent.trim());
                if (!status) {
                    BuildStatus.getInstance().recordError();
                    logger.error("execute failed.");
                    flag = false;
                } else {
                    logger.info("execute OK.");
                }
            }
        }

        return flag;
    }

    /**
     * @param sourcePath    should follow AR for product's folder structure
     * @param packFileNames the file names which need to be packaged
     * @param propFullPath  the full path of package.properties, it should be get value from <I>json file</I>->"zipSettings"-> "productProperties"
     * @param zipPath       the path of package(.zip, .lrm)
     * @param buildType     blank(null) represents it is internal build, true represents it is release build
     * @return
     */
    public Boolean packageARProduct(String sourcePath, ZipSettings zipSet, String propFullPath, String zipPath, String buildType) {
        if (StringUtils.isBlank(sourcePath)) {
            return false;
        }
        logger.info("================= package files =================");
        //get all packaged files
        //String productPrefix=FileUtil.getFileNameWithSuffix(Helper.getParentPath(sourcePath)).toUpperCase().replaceAll("\\(\\d+\\)", "");
        String productPrefix = Dom4jUtil.updateElement(sourcePath + MANIFEST_FILE, PREFIX, null);
        if (StringUtils.isBlank(productPrefix)) {
            productPrefix = FileUtil.getFileNameWithSuffix(Helper.getParentPath(sourcePath))
                    .toUpperCase().replaceAll("\\(\\d+\\)", "");
        }
        Boolean flag = true;
        List<String> packFileNames = zipSet.getZipFiles();
        List<String> realFullPaths = getFileFullPaths(sourcePath, packFileNames, zipSet.getExcludeFileFilters());
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
        if (pathsInManifest != null && pathsInManifest.size() > 0) {
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
        packageNamePrefix = StringUtils.isBlank(packageNamePrefix) ? productPrefix + "_" : packageNamePrefix + productPrefix + "_";
        String packageNameSuffix = null;//PropHelper.getProperty(AR_INSTALLER_VERSION);
        packageNameSuffix = StringUtils.isBlank(packageNameSuffix) ? "" : "_for_AR_v" + packageNameSuffix;
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
                logger.warn("warn: only package zip files, because file [" + PropHelper.SCRIPT_LRM_PRODUCT + "] doesn't exist.");
            }
            logger.info("package successfully.");
        } else {
            BuildStatus.getInstance().recordError();
            logger.error("error: package with failures.");
        }

        return flag;
    }

    /***
     * get all file's full path under sourcePath, with filters
     * @param sourcePath
     * @param filters
     * @return if get nothing or arguments contains blank argument, return null.
     */
    public List<String> getFileFullPaths(String sourcePath, List<String> filters) {
        if (filters == null || filters.size() <= 0) return null;
        if (StringUtils.isBlank(sourcePath)) return null;
        sourcePath = Helper.reviseFilePath(sourcePath + "/");
        List<String> realFilePaths = new ArrayList<String>();
        for (String filter : filters) {

            List<String> realFullPathsTmp = FileUtil.getFilesByFilter(Helper.reviseFilePath(sourcePath + filter), null);
            if (realFullPathsTmp.size() <= 0) {
                BuildStatus.getInstance().recordError();
                logger.error("error: cannot search [" + filter + "] under path [" + sourcePath + "]");
                continue;
            }
            for (String pathTmp : realFullPathsTmp) {
                if (!realFilePaths.contains(pathTmp)) {
                    realFilePaths.add(pathTmp);
                }
            }
        }
        if (realFilePaths.size() <= 0) return null;
        return realFilePaths;
    }

    /***
     * get all file's full path under sourcePath, with filters
     * @param sourcePath
     * @param filters
     * @return if get nothing or arguments contains blank argument, return null.
     */
    public List<String> getFileFullPaths(String sourcePath, List<String> filters, String excludeFilters) {
        if (filters == null || filters.size() <= 0) return null;
        if (StringUtils.isBlank(sourcePath)) return null;
        sourcePath = Helper.reviseFilePath(sourcePath + "/");
        List<String> realFilePaths = new ArrayList<String>();
        for (String filter : filters) {

            List<String> realFullPathsTmp = FileUtil.getFilesByFilter(Helper.reviseFilePath(sourcePath + filter), excludeFilters);
            if (realFullPathsTmp.size() <= 0) {
                BuildStatus.getInstance().recordError();
                logger.error("error: cannot search [" + filter + "] under path [" + sourcePath + "]");
                continue;
            }
            for (String pathTmp : realFullPathsTmp) {
                if (!realFilePaths.contains(pathTmp)) {
                    realFilePaths.add(pathTmp);
                }
            }
        }
        if (realFilePaths.size() <= 0) return null;
        return realFilePaths;
    }
}
