package com.lombardrisk;

import com.google.gson.JsonSyntaxException;
import com.lombardrisk.pojo.ARPCISetting;
import com.lombardrisk.pojo.ExternalProject;
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

/**
 * run as a jar solution
 *
 * @author kun shen
 */
public class ReviseARPCISetting implements IReviseARPCISetting, IComFolder {

    private static final Logger logger = LoggerFactory.getLogger(ReviseARPCISetting.class);
    private static Boolean copyAllProductsInOneProject = true;
    private static String targetProjectPath = null;

    /**
     * adding default value for ARPCISetting
     */
    @Override
    public ARPCISetting reviseARPCISetting(ARPCISetting arCIConfg) {

        if (arCIConfg != null) {
            arCIConfg.setPrefix(revisePrefix(arCIConfg.getPrefix()));

            String metadataPath = arCIConfg.getMetadataPath();
            String projectPath;
            String productPath;//subfolder under project folder
            String targetSrcPath;
            String sourcePath;
            if (StringUtils.isNotBlank(metadataPath)) {
                metadataPath = Helper.reviseFilePath(metadataPath);
                sourcePath = Helper.getParentPath(metadataPath); //src/
                productPath = Helper.removeLastSlash(Helper.getParentPath(sourcePath));
                projectPath = Helper.removeLastSlash(Helper.getParentPath(productPath));
            } else {
                projectPath = Helper.getParentPath(System.getProperty("user.dir")) + System.getProperty(CMDL_ARPPROJECTFOLDER);
                productPath = projectPath + File.separator + arCIConfg.getPrefix();
                sourcePath = productPath + File.separator + SOURCE_FOLDER; //src/
                metadataPath = sourcePath + META_PATH;
            }

            if (StringUtils.isBlank(System.getProperty(CMDL_ARPRUNONJENKINS))) {
                //run on local machine
                if (getCopyAllProductsInOneProject()) {
                    setTargetProjectPath(FileUtil.createNewFileWithSuffix(projectPath, null, null));
                }
                //get target product path
                String targetProductPath = getTargetProjectPath() + File.separator + arCIConfg.getPrefix();
                targetSrcPath = targetProductPath + File.separator + SOURCE_FOLDER;//current product(prefix)'s target source path
                metadataPath = targetSrcPath + META_PATH; //current product(prefix)'s target metadata path
                if (StringUtils.isNotBlank(System.getProperty(CMDL_ARPRODUCTID)) && System.getProperty(CMDL_ARPRODUCTID).startsWith("*")) {
                    if (getCopyAllProductsInOneProject()) {
                        FileUtil.copyDirectory(projectPath, getTargetProjectPath());
                        setCopyAllProductsInOneProject(false);
                    }
                } else {
                    if (!FileUtil.exists(targetProductPath)) {
                        FileUtil.copyDirectory(productPath, targetProductPath);
                    }
                    setCopyAllProductsInOneProject(false);
                }
            } else {
                //run on Jenkins server
                setTargetProjectPath(projectPath);
                targetSrcPath = sourcePath;
            }

            arCIConfg.setMetadataPath(metadataPath);
            arCIConfg.setTargetSrcPath(targetSrcPath);
            FileUtil.createDirectories(arCIConfg.getMetadataPath());

            //revise "metadataStruct"
            String metadataStruct = arCIConfg.getMetadataStruct();
            if (StringUtils.isBlank(metadataStruct)) {
                arCIConfg.setMetadataStruct(arCIConfg.getPrefix().toUpperCase() + INI_FILE_SUFFIX);
            }
            arCIConfg.setZipSettings(reviseZipSettings(
                    arCIConfg.getZipSettings(),
                    sourcePath,
                    targetSrcPath));
        }
        return arCIConfg;
    }

    private String revisePrefix(final String productPrefix) {
        if (StringUtils.isBlank(productPrefix)) {
            //prefix must be set as a subfolder's name of project folder name
            throw new JsonSyntaxException("error: prefix is null, please set it value");
        }
        return productPrefix.toLowerCase();
    }

    private static String getTargetProjectPath() {
        return targetProjectPath;
    }

    private static void setTargetProjectPath(String targetProjectPatha) {
        targetProjectPath = targetProjectPatha;
    }

    private static Boolean getCopyAllProductsInOneProject() {
        return copyAllProductsInOneProject;
    }

    private static void setCopyAllProductsInOneProject(
            Boolean copyAllProductsInOneProjec) {
        ReviseARPCISetting.copyAllProductsInOneProject = copyAllProductsInOneProjec;
    }

    private ZipSettings reviseZipSettings(ZipSettings zipSetting, String sourcePath, String targetSrcPath) {
        if (zipSetting != null) {
            //revise "zipSettings"->"dpmFullPath"
            String dpmFullName = zipSetting.getDpmFullPath();
            FileUtil.createDirectories(targetSrcPath + DPM_PATH);
            if (StringUtils.isNotBlank(dpmFullName)) {
                dpmFullName = ReviseStrHelper.defaultDpmFullName(dpmFullName, sourcePath, targetSrcPath, DPM_PATH);
            } else {
                String accdbFileNameInManifest = Dom4jUtil.updateElement(targetSrcPath + MANIFEST_FILE, ACCESSFILE, null);
                dpmFullName = Helper.reviseFilePath(targetSrcPath + DPM_PATH + accdbFileNameInManifest);
                List<ExternalProject> externalProjects = zipSetting.getExternalProjects();
                if (externalProjects != null && externalProjects.size() > 0) {
                    for (ExternalProject externalpro : externalProjects) {
                        if (StringUtils.isNoneBlank(externalpro.getProject(), externalpro.getSrcFile())) {
                            String destDir = StringUtils.isBlank(externalpro.getDestDir()) ? targetSrcPath :
                                    Helper.reviseFilePath(targetSrcPath + File.separator + externalpro.getDestDir());
                            String externalProjectParent = Helper.getParentPath(Helper.getParentPath(Helper.getParentPath(sourcePath)));
                            FileUtil.copyExternalProject(Helper.reviseFilePath(externalProjectParent +
                                    externalpro.getProject() + File.separator + externalpro.getSrcFile()), destDir, externalpro.getUncompress());
                            String dmpType = accdbFileNameInManifest.substring(accdbFileNameInManifest.lastIndexOf('.'));
                            List<String> accdbfiles =
                                    FileUtil.getFilesByFilter(Helper.reviseFilePath(targetSrcPath + "/" + DPM_PATH + "*" + dmpType), null);
                            if (accdbfiles.size() > 0) {
                                String accdbFileName = FileUtil.getFileNameWithSuffix(accdbfiles.get(0));
                                if (!accdbFileName.equalsIgnoreCase(accdbFileNameInManifest)) {
                                    logger.info("Rename dpm name: " + accdbFileName + " to " + accdbFileNameInManifest);
                                    FileUtil.renameTo(accdbfiles.get(0), targetSrcPath + File.separator + DPM_PATH + accdbFileNameInManifest);
                                }
                            }
                        } else {
                            BuildStatus.getInstance().recordError();
                            logger.error("externalProjects->project,srcFile cannot be null.");
                        }
                    }
                }
            }
            zipSetting.setDpmFullPath(dpmFullName);

            //revise "zipSettings"->"productProperties"
            String productPropsPath = zipSetting.getProductProperties();
            zipSetting.setProductProperties(ReviseStrHelper.revisePropsPath(getTargetProjectPath(), productPropsPath, PRODUCT_PROP_FILE));
        }
        return zipSetting;
    }
}
