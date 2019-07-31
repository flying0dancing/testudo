package com.lombardrisk;

import com.lombardrisk.pojo.ARPCISetting;
import com.lombardrisk.pojo.DBAndTables;
import com.lombardrisk.status.BuildStatus;
import com.lombardrisk.utils.DBInfo;
import com.lombardrisk.utils.FileUtil;
import com.lombardrisk.utils.Helper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * start from this
 */
public class Testudo implements IComFolder {

    private static final Logger logger = LoggerFactory.getLogger(Testudo.class);
    private static final float MILLISECONDS_PER_SECOND = 60*1000.00F;

    public static void main(String[] args) {
        long begin = System.currentTimeMillis();

        logger.info("start running testudo:");
        logger.info("Log dir" + System.getProperty("log.dir"));

        convertArgumentsToSystemProperties(args);

        String iDinJoson = System.getProperty(CMDL_ARPRODUCTID);
        String proc = System.getProperty(CMDL_ARPCIPROC);

        List<ARPCISetting> arSettingList;
        try {
            arSettingList = ARPCISettingManager.getARPCISettingList(iDinJoson);
            if (arSettingList != null && arSettingList.size() > 0) {
                for (ARPCISetting arSetting : arSettingList) {
                    if (arSetting != null) {
                        logger.info(arSetting.toString());
                        if (arSetting.getDatabaseServerAndTables() != null && arSetting.getDatabaseServerAndTables().size() > 0) {
                            if (arSetting.getZipSettings() != null) {
                                jobproc(arSetting, proc);
                            } else {
                                jobproc(arSetting, "1");
                            }
                        } else if (arSetting.getZipSettings() != null) {
                            jobproc(arSetting, "2");
                        }
                    } else {
                        logger.error("please see readme.md.");
                        BuildStatus.getInstance().recordError();
                        logger.error("testudo's json might contains error, details see readme's json instruction.");
                    }
                }
            }
        } catch (Exception e) {
            BuildStatus.getInstance().recordError();
            logger.error(e.getMessage(), e);
        }
        long end = System.currentTimeMillis();
        logger.info("total time(min):" + (end - begin) / MILLISECONDS_PER_SECOND);
        Runtime.getRuntime().gc();
    }

    private static void convertArgumentsToSystemProperties(final String[] args) {
        //read args from command-line
        if (args.length > 0) {
            for (String s : args) {
                if (s.contains("=")) {
                    String[] argKeyValue = s.split("=");
                    argKeyValue[0] = argKeyValue[0].replaceAll("^\\-D?(.*)$", "$1");
                    System.setProperty(argKeyValue[0], argKeyValue[1]);
                } else {
                    s = s.replaceAll("^\\-D?(.*)$", "$1");
                    System.setProperty(s, "true");
                }
            }
        }
    }

    private static void jobproc(ARPCISetting arSetting, String proc) {
        if (StringUtils.isBlank(proc)) {
            logger.warn("argument proc is not setted, run 2 by default.");
            proc = "2";
        }
        if (proc.equals("1")) {
            readDBToMetadata(arSetting);
        } else if (proc.equals("2")) {
            packMetadataAndFiles(arSetting);
        } else if (proc.equalsIgnoreCase("all")) {

            readDBToMetadata(arSetting);
            packMetadataAndFiles(arSetting);
        } else {
            BuildStatus.getInstance().recordError();
            logger.error("argument proc is wrong, should be 1 or 2 or all, details see readme's [proc] instruction.");
        }
    }

    private static void readDBToMetadata(ARPCISetting arSetting) {
        String iniFullName =
                Helper.reviseFilePath(arSetting.getMetadataPath() + System.getProperty("file.separator") + arSetting.getMetadataStruct());
        FileUtil.createNew(iniFullName);
        List<DBAndTables> dbAndTables = arSetting.getDatabaseServerAndTables();
        List<String> tables,excludeReturnIds;
        String prefix,metadataPath,metadataStruct;
        if (dbAndTables != null && dbAndTables.size() > 0) {
            DBAndTables dbAndTable = dbAndTables.get(0);
            DBInfo db = new DBInfo(dbAndTable.getDatabaseServer());
            prefix=arSetting.getPrefix();
            metadataPath=arSetting.getMetadataPath();
            metadataStruct=arSetting.getMetadataStruct();

            excludeReturnIds=dbAndTable.getRequiredTables().getExcludeReturnIds();
            Helper.removeDuplicatedElements(excludeReturnIds);

            tables=dbAndTable.getRequiredTables().getDividedByReturnIds();
            Helper.removeDuplicatedElements(tables);
            db.exportToDivides(prefix,tables,metadataPath,metadataStruct,excludeReturnIds,null);

            tables=dbAndTable.getRequiredTables().getSingles();
            Helper.removeDuplicatedElements(tables);
            db.exportToSingle(prefix,tables,metadataPath,metadataStruct,excludeReturnIds,null);

            for (int i = 1; i < dbAndTables.size(); i++) {
                dbAndTable = dbAndTables.get(i);
                db = new DBInfo(dbAndTable.getDatabaseServer());
                String idOfDBAndTable = "#" + dbAndTable.getID();

                excludeReturnIds=dbAndTable.getRequiredTables().getExcludeReturnIds();
                Helper.removeDuplicatedElements(excludeReturnIds);

                tables=dbAndTable.getRequiredTables().getDividedByReturnIds();
                Helper.removeDuplicatedElements(tables);
                db.exportToDivides(prefix,tables,metadataPath,metadataStruct,excludeReturnIds,idOfDBAndTable);

                tables=dbAndTable.getRequiredTables().getSingles();
                Helper.removeDuplicatedElements(tables);
                db.exportToSingle(prefix,tables,metadataPath,metadataStruct,excludeReturnIds,idOfDBAndTable);
            }
        }
    }

    private static void packMetadataAndFiles(ARPCISetting arSetting) {
        String iniFullName =
                Helper.reviseFilePath(arSetting.getMetadataPath() + System.getProperty("file.separator") + arSetting.getMetadataStruct());
        ARPPack azipFile = new ARPPack();
        Boolean flag = azipFile.createNewDpm(arSetting.getZipSettings().getDpmFullPath());
        if (!flag) {
            BuildStatus.getInstance().recordError();
            logger.error("error: create access database unsuccessful.");
            return;
        }
        List<String> requiredMetadata=arSetting.getZipSettings().getRequiredMetadata();
        List<String> metadataPaths = azipFile.importMetadataToDpm(arSetting.getMetadataPath(),
                requiredMetadata, iniFullName);
        if (metadataPaths != null) {
            if(!requiredMetadata.contains("*.csv")){
                List<String> returnNameVers = azipFile.getReturnNameAndVersions(metadataPaths);
                if (returnNameVers != null) {
                    arSetting.getZipSettings().getZipFiles().addAll(returnNameVers);
                }
            }
            Boolean status = azipFile.execSQLs(arSetting.getTargetSrcPath(),
                    arSetting.getZipSettings().getSqlFiles(), arSetting.getZipSettings().getExcludeFileFilters());
            if (status) {
                azipFile.packageARProduct(arSetting.getTargetSrcPath(), arSetting.getZipSettings(), arSetting.getZipSettings().getProductProperties(),
                        Helper.getParentPath(arSetting.getTargetSrcPath()), System.getProperty(CMDL_ARPBUILDTYPE));
            }
        }else{
            BuildStatus.getInstance().recordError();
            logger.error("error: not found any requiredMetadata or import failures.");
        }

    }

}
