package com.lombardrisk;

import com.google.gson.JsonSyntaxException;
import com.lombardrisk.pojo.ARPCISetting;
import com.lombardrisk.pojo.ExternalProject;
import com.lombardrisk.pojo.TempACCESSDB;
import com.lombardrisk.pojo.ZipSettings;
import com.lombardrisk.status.BuildStatus;
import com.lombardrisk.utils.Dom4jUtil;
import com.lombardrisk.utils.FileUtil;
import com.lombardrisk.utils.Helper;
import com.lombardrisk.utils.ReviseStrHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class MavenReviseARPCISetting implements IReviseARPCISetting, IComFolder {

    private static final Logger logger = LoggerFactory.getLogger(MavenReviseARPCISetting.class);

    @Override
    public ARPCISetting reviseARPCISetting(ARPCISetting arCIConfg) {

        if (arCIConfg != null) {
            //revise "prefix", make sure it is lowercase
            String productPrefix = arCIConfg.getPrefix();
            if (StringUtils.isBlank(productPrefix)) {
                //prefix must be set as a subfolder's name of project folder name
                throw new JsonSyntaxException("error: prefix is null, please set it value");
            } else {
                arCIConfg.setPrefix(productPrefix.toLowerCase());
            }

            String metadataPath = arCIConfg.getMetadataPath();
            String projectPath;
            String productPath;
            String sourcePath;
            if (StringUtils.isNotBlank(metadataPath)) {
                metadataPath = Helper.reviseFilePath(metadataPath);
                sourcePath = Helper.getParentPath(metadataPath); //src/
                productPath = Helper.removeLastSlash(Helper.getParentPath(sourcePath));
                projectPath = Helper.removeLastSlash(Helper.getParentPath(productPath));
            } else {
                //[maven product solution]change user.dir to project.dir
                projectPath = Helper.removeLastSlash(Helper.getParentPath(System.getProperty("project.dir")));
                productPath = projectPath + File.separator + arCIConfg.getPrefix();
                sourcePath = productPath + File.separator + SOURCE_FOLDER; //src/
            }
            //[maven product solution]
            //get target product path
            String targetProductPath = projectPath + File.separator + arCIConfg.getPrefix() + File.separator + "target";
            String targetSrcPath = targetProductPath + File.separator + SOURCE_FOLDER;//current product(prefix)'s target source path

            FileUtil.copyDirectory(sourcePath, targetSrcPath);

            metadataPath = targetSrcPath + META_PATH; //current product(prefix)'s target metadata path
            arCIConfg.setMetadataPath(metadataPath);
            arCIConfg.setTargetSrcPath(targetSrcPath);
            FileUtil.createDirectories(arCIConfg.getMetadataPath());

            //revise "metadataStruct"
            String metadataStruct = arCIConfg.getMetadataStruct();
            if (StringUtils.isBlank(metadataStruct)) {
                arCIConfg.setMetadataStruct(arCIConfg.getPrefix().toUpperCase() + INI_FILE_SUFFIX);
            }
            //revise "zipSettings"
            ZipSettings zipSetting = arCIConfg.getZipSettings();
            arCIConfg.setZipSettings(reviseZipSettings(zipSetting, sourcePath, targetSrcPath));
        }
        return arCIConfg;
    }

    private ZipSettings reviseZipSettings(ZipSettings zipSetting, String sourcePath, String targetSrcPath) {
        if (zipSetting != null) {
            String targetProjectPath = Helper.getParentPath(Helper.getParentPath(sourcePath));
            String dpmFullName = zipSetting.getDpmFullPath();
            FileUtil.createDirectories(targetSrcPath + DPM_PATH);
            if (StringUtils.isNotBlank(dpmFullName)) {
                dpmFullName = ReviseStrHelper.defaultDpmFullName(dpmFullName, sourcePath, targetSrcPath, DPM_PATH);
            } else {
                String accdbFileNameInManifest = Dom4jUtil.updateElement(targetSrcPath + MANIFEST_FILE, ACCESSFILE, null);
                dpmFullName = Helper.reviseFilePath(targetSrcPath + DPM_PATH + accdbFileNameInManifest);
                copyExternalProjects(zipSetting, targetSrcPath, targetProjectPath, accdbFileNameInManifest);
            }
            zipSetting.setDpmFullPath(dpmFullName);
            String productPropsPath = zipSetting.getProductProperties();
            zipSetting.setProductProperties(ReviseStrHelper.revisePropsPath(targetProjectPath, productPropsPath, PRODUCT_PROP_FILE));
        }
        return zipSetting;
    }

    private void copyExternalProjects(
            final ZipSettings zipSetting,
            final String targetSrcPath,
            final String targetProjectPath,
            final String accdbFileNameInManifest) {
        List<ExternalProject> externalProjects = zipSetting.getExternalProjects();
        if (externalProjects != null && !externalProjects.isEmpty()) {
            for (ExternalProject externalpro : externalProjects) {
                if (StringUtils.isNoneBlank(externalpro.getProject(), externalpro.getSrcFile())) {
                    copyExternalProject(targetSrcPath, targetProjectPath, accdbFileNameInManifest, externalpro);
                } else {
                    BuildStatus.getInstance().recordError();
                    logger.error("externalProjects->project,srcFile cannot be null.");
                }
            }
        }
    }

    private void copyExternalProject(
            final String targetSrcPath,
            final String targetProjectPath,
            final String accdbFileNameInManifest,
            final ExternalProject externalpro) {
        String destDir = StringUtils.isBlank(externalpro.getDestDir()) ? targetSrcPath :
                Helper.reviseFilePath(targetSrcPath + File.separator + externalpro.getDestDir());
        String externalProjectParent = Helper.getParentPath(targetProjectPath);
        FileUtil.copyExternalProject(Helper.reviseFilePath(externalProjectParent +
                externalpro.getProject() + File.separator + externalpro.getSrcFile()), destDir, externalpro.getUncompress());
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
