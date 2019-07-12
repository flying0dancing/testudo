package com.lombardrisk.utils;

import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.Callable;

public class SevenZipThreadTask implements Callable<Boolean> {
    private static final Logger logger = LoggerFactory.getLogger(SevenZipThreadTask.class);
    int[] in;
    String zipFile;
    String packageName;
    String unpackPath;
    int totalUnpackSize;

    public SevenZipThreadTask(int[] in, String zipFile, String packageName, String unpackPath,int totalUnpackSize){
        this.in=in;
        this.zipFile=zipFile;
        this.packageName=packageName;
        this.unpackPath=unpackPath;
        this.totalUnpackSize=totalUnpackSize;
    }

    @Override
    public Boolean call() {
        Boolean flag=false;
        IInArchive archive = null;
        RandomAccessFile randomAccessFile = null;
        try{
            randomAccessFile = new RandomAccessFile(zipFile, "r");
            archive = SevenZip.openInArchive(null,
                    new RandomAccessFileInStream(
                            randomAccessFile));
            archive.extract(in, false, new SevenZipExtractCallback(archive, packageName, unpackPath,totalUnpackSize));
            flag=true;
        }catch (FileNotFoundException e){
            logger.error(zipFile+"-FileNotFoundException occurs.");
        }catch (SevenZipException e){
            logger.error("SevenZipException occurs.");
        }finally {
            try {
                archive.close();
                randomAccessFile.close();
            }catch (IOException e){

            }
        }
        return flag;
    }
}
