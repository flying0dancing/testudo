package com.lombardrisk.utils;

import com.lombardrisk.status.BuildStatus;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;

public final class SevenZipUtil {

    private SevenZipUtil(){}
    private static final Logger logger = LoggerFactory.getLogger(SevenZipUtil.class);

    public static void openArchive(String archiveFilename) throws SevenZipException, FileNotFoundException {
        RandomAccessFile randomAccessFile=new RandomAccessFile(archiveFilename,"r");
        IInArchive inArchive=SevenZip.openInArchive(null,new RandomAccessFileInStream(randomAccessFile));
        inArchive.close();
    }

    public static void unzipDirWithPassword(final String sourceZipFile, String destinationDir, final String password){
        RandomAccessFile randomAccessFile=null;
        IInArchive inArchive=null;
        try{
            randomAccessFile=new RandomAccessFile(sourceZipFile,"r");
            inArchive=SevenZip.openInArchive(null,new RandomAccessFileInStream(randomAccessFile));
            ISimpleInArchive simpleInArchive = inArchive.getSimpleInterface();

            //if no existed create
            File destinationFolder = new File(destinationDir);
            if (!destinationFolder.exists()){
                new File(destinationDir).mkdirs();
            }
            for (final ISimpleInArchiveItem item : simpleInArchive.getArchiveItems()){
                final int[] hash = new int[] { 0 };
                if (!item.isFolder()) {
                    ExtractOperationResult result;
                    final long[] sizeArray = new long[1];
                    if (item.getPath().indexOf(File.separator) > 0) {
                        String path = destinationDir + File.separator
                                + item.getPath().substring(0, item.getPath().lastIndexOf(File.separator));
                        File folderExisting = new File(path);
                        if (!folderExisting.exists()){
                            new File(path).mkdirs();
                        }
                    }
                    OutputStream out = new FileOutputStream(destinationDir + File.separator + item.getPath());
                    result = item.extractSlow(new ISequentialOutStream(){
                        public int write(final byte[] data) throws SevenZipException {
                            try{
                                out.write(data);
                            }catch (Exception e){
                                BuildStatus.getInstance().recordError();
                                logger.error(e.getMessage());
                            }
                            hash[0] |= Arrays.hashCode(data);
                            sizeArray[0] += data.length;
                            return data.length;
                        }
                    }, password);
                    out.close();
                    if (result == ExtractOperationResult.OK) {
                        logger.info(String.format("%9X | %10s | %s", //
                                hash[0], sizeArray[0], item.getPath()));
                    }else {
                        BuildStatus.getInstance().recordError();
                        logger.error("Error extracting item: " + result);
                    }
                }
            }


        }catch (Exception e){
            BuildStatus.getInstance().recordError();
            logger.error(e.getMessage());
        }finally {
            if(inArchive!=null){
                try {
                    inArchive.close();
                }catch (SevenZipException e) {
                    BuildStatus.getInstance().recordError();
                    logger.error(e.getMessage());
                }
            }
            if (randomAccessFile != null) {
                try {
                    randomAccessFile.close();
                }catch (IOException e){
                    BuildStatus.getInstance().recordError();
                    logger.error(e.getMessage());
                }
            }
        }

    }

    public static void unzipDirWithPasswordOld( final String sourceZipFile ,
                                      final String destinationDir , final String password ) {
        RandomAccessFile randomAccessFile = null;
        IInArchive inArchive = null;
        try {
            randomAccessFile = new RandomAccessFile(sourceZipFile, "r");
            inArchive = SevenZip.openInArchive(
                    null, // autodetect archive type
                    new RandomAccessFileInStream(randomAccessFile));

            // Getting simple interface of the archive inArchive
            ISimpleInArchive simpleInArchive = inArchive.getSimpleInterface();

            for (final ISimpleInArchiveItem item : simpleInArchive.getArchiveItems()) {
                final int[] hash = new int[]{0};
                if (!item.isFolder()) {
                    ExtractOperationResult result;
                    result = item.extractSlow(new ISequentialOutStream() {
                        public int write(final byte[] data) throws SevenZipException {
                            try {
                                if (item.getPath().indexOf(File.separator) > 0) {
                                    String path = destinationDir + File.separator + item.getPath()
                                            .substring(0, item.getPath().lastIndexOf(File.separator));
                                    File folderExisting = new File(path);
                                    if (!folderExisting.exists())
                                        new File(path).mkdirs();
                                }
                                if (!new File(destinationDir + File.separator + item.getPath()).exists()) {
                                    new File(destinationDir).createNewFile();
                                }
                                OutputStream out =
                                        new FileOutputStream(destinationDir + File.separator + item.getPath());
                                out.write(data);
                                out.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            hash[0] |= Arrays.hashCode(data);
                            return data.length; // Return amount of proceed data
                        }
                    }, password); /// password.
                    if (result == ExtractOperationResult.OK) {
                        System.out.println(String.format("%9X | %s",
                                hash[0], item.getPath()));
                    } else {
                        System.err.println("Error extracting item: " + result);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inArchive != null) {
                try {
                    inArchive.close();
                } catch (SevenZipException e) {
                    System.err.println("Error closing archive: " + e);
                    e.printStackTrace();
                }
            }
            if (randomAccessFile != null) {
                try {
                    randomAccessFile.close();
                } catch (IOException e) {
                    System.err.println("Error closing file: " + e);
                    e.printStackTrace();
                }
            }
        }
    }
    public static void main(String[] args){
        long begin=System.currentTimeMillis();
        String sourceZipFile ="E:\\ComplianceProduct\\ECR_CI\\ecrxbrl\\ci-script.7z";
        /*String destDir = "E:\\ComplianceProduct\\ECR_CI\\xbrl11";
        String password = "";
        unzipDirWithPasswordOld(sourceZipFile, destDir, password);
        logger.info("total time(sec):" + (System.currentTimeMillis() - begin) / 1000.00F);*/

        /*begin=System.currentTimeMillis();
        destDir = "E:\\ComplianceProduct\\ECR_CI\\xbrloldold";
        try {
            FileUtil.un7z(sourceZipFile,destDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("total time(sec):" + (System.currentTimeMillis() - begin) / 1000.00F);
        */

        //解压文件
        String unzipPath = "E:\\ComplianceProduct\\ECR_CI\\engli\\"; //解压目录
        SevenZipServer server = new SevenZipServer();
        System.out.println("---------------开始解压---------------------");
        server.extractZIP7Paral(sourceZipFile,unzipPath);
        System.out.println("---------------解压完成---------------------");
        logger.info("total time(sec):" + (System.currentTimeMillis() - begin) / 1000.00F);
    }

    public static void extractAccessTest(){
        long begin=System.currentTimeMillis();
        String sourceZipFile ="E:\\ComplianceProduct\\ECR_CI\\ecrxbrl\\ECR_FORM_META.7z";
        String destDir = "E:\\ComplianceProduct\\ECR_CI\\xbrl1";
        String password = "";
        unzipDirWithPassword(sourceZipFile, destDir, password);
        logger.info("total time(sec):" + (System.currentTimeMillis() - begin) / 1000.00F);

        begin=System.currentTimeMillis();
        destDir = "E:\\ComplianceProduct\\ECR_CI\\xbrlold";
        try {
            FileUtil.un7z(sourceZipFile,destDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("total time(sec):" + (System.currentTimeMillis() - begin) / 1000.00F);
    }
}
