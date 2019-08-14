package com.lombardrisk.utils;

import com.lombardrisk.IComFolder;
import com.lombardrisk.pojo.ExternalProject;
import com.lombardrisk.pojo.TempACCESSDB;
import com.lombardrisk.status.BuildStatus;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public final class ExternalProjectUtil implements IComFolder {
    private ExternalProjectUtil(){}
    private static final Logger logger = LoggerFactory.getLogger(ExternalProjectUtil.class);

    public static void copyExternalProject(final List<ExternalProject> externalProjects,final String targetSrcPath){

        if (externalProjects != null && externalProjects.size() > 0) {

            String externalProjectParent= getExternalProjectParent(targetSrcPath);

            for (ExternalProject externalpro : externalProjects) {
                if (StringUtils.isNoneBlank(externalpro.getProject(), externalpro.getSrcFile())) {
                    String destDir = StringUtils.isBlank(externalpro.getDestDir()) ? targetSrcPath :
                            Helper.reviseFilePath(targetSrcPath + File.separator + externalpro.getDestDir());

                    FileUtil.copyExternalProject(Helper.reviseFilePath(externalProjectParent +
                            externalpro.getProject() + File.separator + externalpro.getSrcFile()), destDir, externalpro.getUncompress());
                } else {
                    BuildStatus.getInstance().recordError();
                    logger.error("externalProjects->project,srcFile cannot be null.");
                }
            }

            searchDPMAndRename(targetSrcPath);
        }
    }

    private static String getExternalProjectParent(final String targetSrcPath){
        String temp=Helper.removeLastSlash(Helper.getParentPath(targetSrcPath));
        if(temp.endsWith("target")){
            temp=Helper.getParentPath(temp);
        }
        return Helper.getParentPath(Helper.getParentPath(temp));
    }

    private static void searchDPMAndRename(final String targetSrcPath) {
        String accdbFileNameInManifest = Dom4jUtil.updateElement(targetSrcPath + MANIFEST_FILE, ACCESSFILE, null);
        String dmpType = accdbFileNameInManifest.substring(accdbFileNameInManifest.lastIndexOf('.'));
        List<String> accdbfiles = FileUtil.getFilesByFilter(Helper.reviseFilePath(targetSrcPath + "/" + DPM_PATH + "*" + dmpType), null,false);
        if (!accdbfiles.isEmpty()) {
            String accdbFileName = FileUtil.getFileNameWithSuffix(accdbfiles.get(0));
            TempACCESSDB.INSTANCE.initial(accdbfiles.get(0),accdbFileName);
            if (accdbFileName.equalsIgnoreCase(accdbFileNameInManifest)) {
                String draftName="drft"+dmpType;
                logger.info("Rename dpm name: {} to {}", accdbFileName, draftName);
                FileUtil.renameTo(accdbfiles.get(0), targetSrcPath + File.separator + DPM_PATH + draftName);
                TempACCESSDB.INSTANCE.initial(targetSrcPath + File.separator + DPM_PATH + draftName,draftName);
            }
        }

    }

}
