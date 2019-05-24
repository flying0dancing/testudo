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

    private final static Logger logger = LoggerFactory.getLogger(Testudo.class);

    public static void main(String[] args) {
        long begin = System.currentTimeMillis();
        long end = begin;
        logger.info("start running testudo:");
        logger.info("Log dir" + System.getProperty("log.dir"));
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

        String iDinJoson = System.getProperty(CMDL_ARPRODUCTID);
        String proc = System.getProperty(CMDL_ARPCIPROC);
		/*String arpbuildtype=System.getProperty(CMDL_ARPBUILDTYPE);
		System.out.println("product Folder"+System.getProperty(CMDL_ARPPRODUCTPREFIX));
		System.out.println("product ID"+System.getProperty(CMDL_ARPRODUCTID));
		System.out.println("process"+proc);
		System.out.println("build type:"+arpbuildtype);*/

        //if(StringUtils.isBlank(iDinJoson)){
        //	logger.warn("argument id is not setted, get the fist by default in json.");
        //}

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
                        HelperDoc();
                        BuildStatus.getInstance().recordError();
                        logger.error("testudo's json might contains error, details see readme's json instruction.");
                    }
                }
            }
        } catch (Exception e) {
            BuildStatus.getInstance().recordError();
            logger.error(e.getMessage());
        }

        end = System.currentTimeMillis();
        logger.info("total time(sec):" + (end - begin) / 1000.00F);
    }

    private static void HelperDoc() {
        BuildStatus.getInstance().recordError();
        logger.error("please see readme.md.");
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
        if (dbAndTables != null && dbAndTables.size() > 0) {
            DBAndTables dbAndTable = dbAndTables.get(0);
            DBInfo db = new DBInfo(dbAndTable.getDatabaseServer());
            db.exportToDivides(arSetting.getPrefix(),
                    dbAndTable.getRequiredTables().getDividedByReturnIds(),
                    arSetting.getMetadataPath(),
                    arSetting.getMetadataStruct(),
                    dbAndTable.getRequiredTables().getExcludeReturnIds(),
                    null);
            db.exportToSingle(arSetting.getPrefix(),
                    dbAndTable.getRequiredTables().getSingles(),
                    arSetting.getMetadataPath(),
                    arSetting.getMetadataStruct(),
                    dbAndTable.getRequiredTables().getExcludeReturnIds(),
                    null);

            for (int i = 1; i < dbAndTables.size(); i++) {
                dbAndTable = dbAndTables.get(i);
                db = new DBInfo(dbAndTable.getDatabaseServer());
                String idOfDBAndTable = "#" + dbAndTable.getID();
                db.exportToDivides(arSetting.getPrefix(),
                        dbAndTable.getRequiredTables().getDividedByReturnIds(),
                        arSetting.getMetadataPath(),
                        arSetting.getMetadataStruct(),
                        dbAndTable.getRequiredTables().getExcludeReturnIds(),
                        idOfDBAndTable);
                db.exportToSingle(arSetting.getPrefix(),
                        dbAndTable.getRequiredTables().getSingles(),
                        arSetting.getMetadataPath(),
                        arSetting.getMetadataStruct(),
                        dbAndTable.getRequiredTables().getExcludeReturnIds(),
                        idOfDBAndTable);
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
        List<String> metadataPaths = azipFile.importMetadataToDpm(arSetting.getMetadataPath(),
                arSetting.getZipSettings().getRequiredMetadata(), iniFullName);
        if (metadataPaths != null) {
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
    }
}
