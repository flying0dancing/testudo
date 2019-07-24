package com.lombardrisk.utils;

import net.sf.sevenzipjbinding.ExtractAskMode;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IArchiveExtractCallback;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZipException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class SevenZipExtractCallback implements IArchiveExtractCallback {
    private final static int PRINT_SIZE=1000;
    private String path;
    private String packageName;
    private String unzipPath;
    private IInArchive inArchive;
    private OutputStream outputStream;
    private String fullItemName;
    private File fullItemFile;

    public SevenZipExtractCallback(IInArchive inArchive, String packageName, String unzipPath) {
        this.inArchive = inArchive;
        this.packageName=packageName;
        this.unzipPath = unzipPath;
    }

    public ISequentialOutStream getStream(int index, ExtractAskMode extractAskMode) throws SevenZipException {
        if(extractAskMode !=ExtractAskMode.EXTRACT){
            return null;
        }
        //progress display
        if(index%PRINT_SIZE==0){
            System.out.print(".");
        }

        this.path = inArchive.getStringProperty(index, PropID.PATH);
        Boolean isFolder=(Boolean) inArchive.getProperty(index,PropID.IS_FOLDER);

        this.fullItemName = unzipPath+path;
        this.fullItemFile=new File(fullItemName);
        if(isFolder){
            createDirectory(fullItemFile);
            return null;
        }

        Long size=(Long)inArchive.getProperty(index,PropID.SIZE);
        if(size==0){
            FileUtil.createNew(fullItemName);
            return null;
        }

        createDirectory(fullItemFile.getParentFile());

        try{
            outputStream=new FileOutputStream(fullItemFile);
        } catch (FileNotFoundException e) {
            throw new SevenZipException("Error opening file: "
                    + fullItemName);
        }
        return new ISequentialOutStream() {

            public int write(byte[] data) throws SevenZipException {
                try {
                    outputStream.write(data);
                } catch (Exception e) {
                    throw new SevenZipException("Error writing to file: "
                            + fullItemName);
                }
                return data.length;
            }
        };
    }

    public void prepareOperation(ExtractAskMode arg0) throws SevenZipException {
    }

    public void setOperationResult(ExtractOperationResult extractOperationResult) throws SevenZipException {
        closeOutputStream();
        if (extractOperationResult != ExtractOperationResult.OK) {
            StringBuilder sb = new StringBuilder();
            sb.append("decompress ").append(packageName).append(" file ").append(path).append("fail!");
            throw new SevenZipException("Error "+sb.toString());
        }
    }

    public void setTotal(long l) throws SevenZipException {

    }

    public void setCompleted(long l) throws SevenZipException {

    }

    private  void createNewFile(File file) throws SevenZipException{
        if(!file.exists()){
            try {
                if(!file.createNewFile()){
                    throw new SevenZipException("Error creating new file: "
                            + file.getAbsolutePath());
                }
            } catch (IOException e) {
                throw new SevenZipException("Error creating new file: "
                        + file.getAbsolutePath());
            }
        }
    }
    private void createDirectory(File file) throws SevenZipException {
        if(!file.exists()){
            if(!file.mkdirs()){
                throw new SevenZipException("Error creating directory: "
                        + file.getAbsolutePath());
            }
        }
    }

    private void closeOutputStream() throws SevenZipException {
        if(outputStream!=null){
            try{
                outputStream.close();
                outputStream=null;
            } catch (IOException e) {
                throw new SevenZipException("Error closing file: "
                        + fullItemName);
            }
        }
    }


}
