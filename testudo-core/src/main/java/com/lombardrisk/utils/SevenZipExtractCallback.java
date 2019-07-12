package com.lombardrisk.utils;

import net.sf.sevenzipjbinding.ExtractAskMode;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IArchiveExtractCallback;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZipException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;


public class SevenZipExtractCallback implements IArchiveExtractCallback {
    private int index;
    private String packageName;
    private String unzipPath;
    private IInArchive inArchive;
    //private static int printIndex=0;
    private int totalUnpackSize;

    public SevenZipExtractCallback(IInArchive inArchive, String packageName, String unzipPath, int totalUnpackSize) {
        this.inArchive = inArchive;
        this.packageName=packageName;
        this.unzipPath = unzipPath;
        this.totalUnpackSize=totalUnpackSize;
    }

    public ISequentialOutStream getStream(int index, ExtractAskMode extractAskMode) throws SevenZipException {
        if(extractAskMode !=ExtractAskMode.EXTRACT){
            return null;
        }
        //progress display
        if(index%1000==0){
            System.out.print(".");
        }
        this.index = index;
        String path = (String) inArchive.getProperty(index, PropID.PATH);
        Boolean isFolder=(Boolean) inArchive.getProperty(index,PropID.IS_FOLDER);
        Long size=(Long)inArchive.getProperty(index,PropID.SIZE);
        String fullItemName = unzipPath+path;
        if(isFolder){
            File fullItemFile=new File(fullItemName);
            if(!fullItemFile.exists()){
                fullItemFile.mkdirs();
                //System.out.println(fullItemName);
            }
            return null;
        }
        if(size==0){
            FileUtil.createNew(fullItemName);
            return null;
        }

        return new ISequentialOutStream() {
            public int write(byte[] data) throws SevenZipException {
                try {
                    File fullItemFile=new File(fullItemName);
                    File parent=fullItemFile.getParentFile();
                    if(!parent.exists()){
                        parent.mkdirs();
                    }
                    BufferedOutputStream fileOutputStream;
                    fileOutputStream= new BufferedOutputStream(new FileOutputStream(fullItemFile));
                    fileOutputStream.write(data);
                    fileOutputStream.flush();
                    fileOutputStream.close();
                    //System.out.println(fullItemName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return data.length;
            }
        };
    }

    public void prepareOperation(ExtractAskMode arg0) throws SevenZipException {
    }

    public void setOperationResult(ExtractOperationResult extractOperationResult) throws SevenZipException {
        /*String path = (String) inArchive.getProperty(index, PropID.PATH);
        boolean isFolder = (Boolean) inArchive.getProperty(index, PropID.IS_FOLDER);
        if (!isFolder) {
            if (extractOperationResult != ExtractOperationResult.OK) {
                StringBuilder sb = new StringBuilder();
                sb.append("decompress file ").append(path).append("fail!");
            }
        }*/
        String path = (String) inArchive.getProperty(index, PropID.PATH);
        if (extractOperationResult != ExtractOperationResult.OK) {
            StringBuilder sb = new StringBuilder();
            sb.append("decompress ").append(packageName).append(" file ").append(path).append("fail!");
        }

    }

    public void setTotal(long l) throws SevenZipException {

    }

    public void setCompleted(long l) throws SevenZipException {

    }

}
