package com.lombardrisk.utils;

import com.lombardrisk.status.BuildStatus;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ZipUtil {
    private static final Logger logger = LoggerFactory.getLogger(ZipUtil.class);
    private static final int BUFFER_SIZE=2*1024;


    public static void toZip(String sourcePath, List<String> filters, String excludeFilters, String zipFullName,
                             boolean keepDirStructure) {
        long begin = System.currentTimeMillis();
        int lastSlash = zipFullName.lastIndexOf("\\") == -1 ? zipFullName.lastIndexOf("/") : zipFullName.lastIndexOf("\\");
        String zipsPath = zipFullName.substring(0, lastSlash);//get zip's path
        File zipPathHd = new File(zipsPath);
        if (!zipPathHd.exists()) {//if directories doesn't exist, create them
            zipPathHd.mkdirs();
        }
        File zipFullNameHd = new File(zipFullName);
        if (zipFullNameHd.exists()) {
            zipFullNameHd.delete();
        }
        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            BuildStatus.getInstance().recordError();
            logger.error("error:source path cannot be found [" + sourcePath + "]");
            return;
        }
        byte[] buf = new byte[BUFFER_SIZE];
        String filePathTmp;
        try(OutputStream out = new BufferedOutputStream(new FileOutputStream(zipFullName));
            ZipArchiveOutputStream zipOut = new ZipArchiveOutputStream(out)) {

            for(String filter:filters){
                filePathTmp=Helper.reviseFilePath(sourcePath + filter);
                File file=new File(filePathTmp);
                if(file.exists()){
                    compress(zipOut,buf,sourcePath.length(),file,null,excludeFilters, keepDirStructure);
                }else{
                    compress2(zipOut,buf,sourcePath,filter,excludeFilters,keepDirStructure);
                }

            }
            long end = System.currentTimeMillis();
            logger.info("compress time(sec):" + (end - begin) / 1000.00F);
        } catch (Exception e) {
            throw new RuntimeException("zip error from ZipUtils", e);
        }

    }

    private static void compress(final ZipArchiveOutputStream zos,final byte[] buf,final int startIndex,final File sourceFile,final String filter,
                                 final String exfilterStr,final boolean keepDirStructure ) throws Exception {

        String zipPath=sourceFile.getAbsolutePath().substring(startIndex);
        if (sourceFile.isFile()) {
            try (FileInputStream in = new FileInputStream(sourceFile)) {
                zos.putArchiveEntry(new ZipArchiveEntry(zipPath));
                int len;
                while ((len = in.read(buf)) > 0) {
                    zos.write(buf, 0, len);
                }
                zos.closeArchiveEntry();
            }
        } else {
            Boolean flag=true;
            File[] listFiles = FileUtil.filterFilesAndSubFolders(sourceFile, filter, exfilterStr);
            for(File file:listFiles){
                if(file.isFile()){
                    if (keepDirStructure && flag) {
                        //zip folder one time
                        zos.putArchiveEntry(new ZipArchiveEntry(zipPath + System.getProperty("file.separator")));
                        zos.closeArchiveEntry();
                        //System.out.println(file);
                        flag=false;
                    }
                }
                compress(zos,buf,startIndex,file,filter,exfilterStr,keepDirStructure);
            }
        }

    }

    private static void compress2(final ZipArchiveOutputStream zos,final byte[] buf,final String sourcePath,final String filter
            ,final String exfilterStr,final boolean keepDirStructure ) throws Exception {
        File parentPath = new File(sourcePath);
        if (parentPath.isDirectory()) {
            File[] files = FileUtil.filterFilesAndSubFolders(parentPath, filter, exfilterStr);
            for (File file : files) {
                compress(zos,buf,sourcePath.length(),file,filter,exfilterStr,keepDirStructure);
            }
        } else {
            BuildStatus.getInstance().recordError();
            logger.error("error: invalid path[" +sourcePath+filter + "]");
        }

    }

    public static void main(String[] args) throws Exception {
        List<String> filters=new ArrayList<>();
        filters.add("manifest.xml");
        filters.add("xbrl");
        filters.add("forms");
        //filters.add("FCPS");
        filters.add("dpm");
        String source="E:\\ComplianceProduct\\testudoProductDebug\\ecr\\target\\src\\";
        System.out.println(source.length());
        String outDir = "E:\\ComplianceProduct\\aaa.zip";
        ZipUtil.toZip(source,filters,".git;.gitkeep", outDir, true);
    }

}
