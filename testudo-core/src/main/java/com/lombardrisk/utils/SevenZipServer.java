package com.lombardrisk.utils;

import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SevenZipServer {
    private static final Logger logger = LoggerFactory.getLogger(SevenZipServer.class);

    /**
     *This method USES zip7 to decompress the file, need the parameter decompression file, unzip the path
     * Unpack The supported format is zip, rar,tar
     * @param zipFile
     * @param unpackPath
     */
    public boolean extractZIP7(String zipFile,String unpackPath ){
        IInArchive archive = null;
        RandomAccessFile randomAccessFile = null;
        boolean success = false;
        try {
            String packageName=FileUtil.getFileNameWithSuffix(zipFile);
            randomAccessFile = new RandomAccessFile(zipFile, "rw");
            archive = SevenZip.openInArchive(null,
                    new RandomAccessFileInStream(
                            randomAccessFile));
            int[] in = new int[archive.getNumberOfItems()];
            for(int i=0;i<in.length;i++){
                in[i] = i;
            }

            archive.extract(in, false, new SevenZipExtractCallback(archive, packageName, unpackPath, in.length));
            success = true;
        }catch (FileNotFoundException e){
            logger.error(zipFile+"-FileNotFoundException occurs: ");
            e.printStackTrace();
        }catch (SevenZipException e){
            logger.error("SevenZipException occurs: ");
            e.printStackTrace();
        }finally {
            try {
                archive.close();
                randomAccessFile.close();
            }catch (IOException e){

            }
        }
        return success;
    }

    public boolean extractZIP7Paral(String zipFile,String unpackPath ){
        long begin=System.currentTimeMillis();
        IInArchive archive = null;
        RandomAccessFile randomAccessFile = null;
        boolean success = false;
        try {
            String packageName=FileUtil.getFileNameWithSuffix(zipFile);
            System.out.print(packageName+"\textracting:");
            randomAccessFile = new RandomAccessFile(zipFile, "r");
            archive = SevenZip.openInArchive(null,
                    new RandomAccessFileInStream(
                            randomAccessFile));
            int size=archive.getNumberOfItems();
            List<Integer> itemsToExtract = new ArrayList<Integer>();
            Boolean isFolder=false;
            String path;
            File fullItemFile;
            for (int i = 0; i < size; i++) {
                isFolder=(Boolean)archive.getProperty(i, PropID.IS_FOLDER);
                if (isFolder) {
                    path = (String) archive.getProperty(i, PropID.PATH);
                    fullItemFile=new File(unpackPath+path);
                    if(!fullItemFile.exists()){
                        fullItemFile.mkdirs();
                    }
                }else{
                    itemsToExtract.add(Integer.valueOf(i));
                }
            }
            archive.close();
            randomAccessFile.close();
            int sizeRe=itemsToExtract.size();
            int size1=sizeRe/2;
            int size2=sizeRe-size1;
            int[] in1 = new int[size1];
            int[] in2=new int[size2];

            for(int i=0;i<size1;i++){
                in1[i] = itemsToExtract.get(i);
            }
            for(int i=0;i<size2;i++){
                in2[i]=itemsToExtract.get(size1+i);
            }
            success = true;
            List<Future<Boolean>> futures=new ArrayList<>();
            ExecutorService threadPool= Executors.newFixedThreadPool(2);
            futures.add(threadPool.submit(new SevenZipThreadTask(in1,zipFile,packageName,unpackPath,size)));
            futures.add(threadPool.submit(new SevenZipThreadTask(in2,zipFile,packageName,unpackPath,size)));
            for(int i=0;i<futures.size();i++){
                if(!futures.get(i).get()){
                    success =false;
                    break;
                }
            }
            threadPool.shutdown();
            //System.out.print("\b\b\b\b");
            System.out.println("100%");
        }catch (FileNotFoundException e){
            logger.error(zipFile+"-FileNotFoundException occurs: ");
            e.printStackTrace();
        }catch (SevenZipException e){
            logger.error("SevenZipException occurs: ");
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }finally {
            Runtime.getRuntime().gc();
            System.out.println("used time(sec):" + (System.currentTimeMillis() - begin) / 1000.00F);
        }
        return success;
    }
}
