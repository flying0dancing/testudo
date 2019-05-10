package com.lombardrisk;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lombardrisk.pojo.ARPCISetting;
import com.lombardrisk.status.BuildStatus;
import com.lombardrisk.utils.Helper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public final class ARPCISettingManager implements IComFolder {

    private static final Logger logger = LoggerFactory.getLogger(ARPCISettingManager.class);

    private ARPCISettingManager() {
    }

    private static List<ARPCISetting> getAllSettings() {
        String propertyFile = System.getProperty(CMDL_ARPCICONFG, JSON_PATH);
        return loadJson(propertyFile);
    }

    private static synchronized List<ARPCISetting> loadJson(String file) {
        try {
            java.lang.reflect.Type type = new TypeToken<List<ARPCISetting>>() {
            }.getType();

            if ((System.getProperty("file.separator").equals("/") && !file.startsWith("/"))
                    || (System.getProperty("file.separator").equals("\\") && !file.contains(":"))) {
                if (file.contains("/") || file.contains("\\")) {
                    file = Helper.reviseFilePath(Helper.getParentPath(System.getProperty("user.dir")) + file);
                } else {
                    file = Helper.reviseFilePath(System.getProperty("user.dir") + "/" + file);
                }
            }

            File fileHd = new File(file);
            if (fileHd.exists()) {
                return new Gson().fromJson(new FileReader(fileHd), type);
            }
            return new ArrayList<>();
        } catch (Exception e) {
            BuildStatus.getInstance().recordError();
            logger.error(e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private static ARPCISetting getARPCISetting(final List<ARPCISetting> allSettings, String key) {
        try {
            if (allSettings != null && allSettings.size() > 0) {
                if (StringUtils.isBlank(key)) {
                    return reviseARPCISetting(allSettings.get(0));
                }
                return reviseARPCISetting((ARPCISetting) Helper.filterListByID(allSettings, key));
            }
        } catch (NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            BuildStatus.getInstance().recordError();
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    static List<ARPCISetting> getARPCISettingList(String ids) {
        List<ARPCISetting> allSettings = getAllSettings();
        try {
            if (allSettings != null && allSettings.size() > 0) {

                List<ARPCISetting> arCIConfgList = new ArrayList<>();
                if (StringUtils.isNotBlank(ids) && ids.startsWith("*")) { //get all ARPCISetting
                    reviseAllSettings(allSettings, arCIConfgList);
                } else if (StringUtils.isNotBlank(ids) && ids.contains(";")) {
                    splitAndReviseSetting(allSettings, ids, arCIConfgList);
                } else {
                    if (StringUtils.isBlank(ids)) {
                        logger.warn("argument id is not setted, get the fist by default in json.");
                    }
                    ARPCISetting arpci = getARPCISetting(allSettings, ids);
                    if (arpci == null) {
                        BuildStatus.getInstance().recordError();
                        logger.error("Not Exists id=" + ids);
                    } else {
                        arCIConfgList.add(arpci);
                    }
                }
                return arCIConfgList;
            }
        } catch (SecurityException
                | IllegalArgumentException e) {
            BuildStatus.getInstance().recordError();
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    private static void splitAndReviseSetting(
            final List<ARPCISetting> allSettings,
            final String ids,
            final List<ARPCISetting> arCIConfgList) {
        String[] idarr = ids.split(";");
        boolean flag;

        for (String id : idarr) {
            flag = false;
            for (ARPCISetting arCIConfg : allSettings) {
                if (id.trim().equalsIgnoreCase(arCIConfg.getID())) {
                    flag = true;
                    if (!arCIConfgList.contains(arCIConfg)) {
                        arCIConfgList.add(reviseARPCISetting(arCIConfg));
                    } else {
                        logger.warn("duplicated argument id=" + id.trim());
                    }
                    break;
                }
            }
            if (!flag) {
                BuildStatus.getInstance().recordError();
                logger.error("Not Exists id=" + id.trim());
                logger.error("testudo's json might contains error, details see readme's json instruction.");
                throw new IllegalStateException("please check your json file.");
            }
        }
    }

    private static void reviseAllSettings(final List<ARPCISetting> allSettings, final List<ARPCISetting> arCIConfgList) {
        for (ARPCISetting arCIConfg : allSettings) {
            arCIConfgList.add(reviseARPCISetting(arCIConfg));
        }
    }

    private static ARPCISetting reviseARPCISetting(ARPCISetting arCIConfg) {
        IReviseARPCISetting arcisetting;
        if (StringUtils.isBlank(System.getProperty(CMDL_ARPRUNONMAVEN))) {
            arcisetting = new ReviseARPCISetting();
        } else {
            arcisetting = new MavenReviseARPCISetting();
        }
        return arcisetting.reviseARPCISetting(arCIConfg);
    }
}
