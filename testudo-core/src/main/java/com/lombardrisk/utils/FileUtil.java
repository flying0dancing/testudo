package com.lombardrisk.utils;

import com.lombardrisk.pojo.TableProps;
import com.lombardrisk.status.BuildStatus;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public final class FileUtil {
    private static final String CharacterSet="UTF-8";
    private FileUtil() {
    }

    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);
    private static final int BUFFER_SIZE = 2048;
    private static final float MILLISECONDS_PER_SECOND = 1000.00F;

    public static boolean exists(final String fileFullName) {
        if (StringUtils.isNotBlank(fileFullName)) {
            File file = new File(fileFullName);
            return file.exists();
        }
        return false;
    }

    /***
     * Zipped folder which has multiple files along with sub folders, also can only zip folders.
     * @param sourcePath toZipFileFullPaths's parent path, but not be included in zip file.
     * @param toZipFileFullPaths many files's getAbsolutePath with name.
     * @param zipFullName zip full path with name
     */
    @SuppressWarnings("squid:S109")
    public static boolean zipFilesAndFolders(String sourcePath,final List<String> toZipFileFullPaths,final String zipFullName) {
        boolean flag = true;
        byte[] buf = new byte[1024];
        try {
            logger.info("start compressing");
            long begin=System.currentTimeMillis();
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
            File sourcePathHd = new File(sourcePath);
            if (!sourcePathHd.exists()) {
                BuildStatus.getInstance().recordError();
                logger.error("error:source path cannot be found [" + sourcePath + "]");
                return false;
            }
            sourcePath = sourcePathHd.getAbsolutePath();
            try (OutputStream os = new BufferedOutputStream(new FileOutputStream(zipFullName));
                 ZipArchiveOutputStream zipOut = new ZipArchiveOutputStream(os)) {
                zipOut.setEncoding(CharacterSet);
                ListIterator<String> listIterator = toZipFileFullPaths.listIterator();
                String fileFullPath;

                while (listIterator.hasNext()){
                    fileFullPath=listIterator.next();
                    File fileHd = new File(fileFullPath);
                    String toZipPath = fileFullPath.substring(sourcePath.length() + 1);
                    if (fileHd.isDirectory()) {
                        zipOut.putArchiveEntry(new ZipArchiveEntry(toZipPath + System.getProperty("file.separator")));
                        zipOut.closeArchiveEntry();
                    }
                    if (fileHd.isFile()) {
                        try (FileInputStream in = new FileInputStream(fileHd)) {
                            zipOut.putArchiveEntry(new ZipArchiveEntry(toZipPath));
                            int len;
                            while ((len = in.read(buf)) > 0) {
                                zipOut.write(buf, 0, len);
                            }
                            zipOut.closeArchiveEntry();
                        }
                    }

                }
            }
            logger.info("compression used time(sec):" + (System.currentTimeMillis() - begin) / MILLISECONDS_PER_SECOND);
        } catch (Exception e) {
            flag = false;
            BuildStatus.getInstance().recordError();
            logger.error(e.getMessage(), e);
        }
        return flag;
    }

    private static List<String> un7z1(File file, String destDir) throws IOException {
        List<String> fileNames = new ArrayList<>();
        try (SevenZFile sevenZFile = new SevenZFile(file)) {
            logger.info(file.getName()+"\textracting");
            SevenZArchiveEntry entry;

            long begin=System.currentTimeMillis();
            while ((entry = sevenZFile.getNextEntry()) != null) {
                fileNames.add(entry.getName());
                if (entry.isDirectory()) {
                    createDirectory(destDir, entry.getName());
                } else {
                    File tmpFile = new File(destDir + File.separator + entry.getName());
                    createDirectory(tmpFile.getParent() + File.separator, null);
                    try (FileOutputStream out = new FileOutputStream(tmpFile)) {
                        byte[] content = new byte[(int) entry.getSize()];
                        sevenZFile.read(content, 0, content.length);
                        out.write(content);
                    }catch (Exception e){
                        BuildStatus.getInstance().recordError();
                        logger.error(e.getMessage(), e);
                        throw e;
                    }
                }

            }
            logger.info("extraction time(sec):" + (System.currentTimeMillis() - begin) / MILLISECONDS_PER_SECOND);
        } catch (IOException e) {
            BuildStatus.getInstance().recordError();
            logger.error(e.getMessage(), e);
            throw e;
        }
        Runtime.getRuntime().gc();
        return fileNames;
    }

    @SuppressWarnings("squid:S109")
    private static List<String> unTar(InputStream inputStream, String destDir) throws IOException {
        List<String> fileNames = new ArrayList<>();

        try (TarArchiveInputStream tarIn = new TarArchiveInputStream(inputStream, BUFFER_SIZE)) {
            TarArchiveEntry entry;
            while ((entry = tarIn.getNextTarEntry()) != null) {
                fileNames.add(entry.getName());
                if (entry.isDirectory()) {
                    createDirectory(destDir, entry.getName());
                } else {
                    File tmpFile = new File(destDir + File.separator + entry.getName());
                    createDirectory(tmpFile.getParent() + File.separator, null);
                    try (OutputStream out = new FileOutputStream(tmpFile)) {
                        int length;
                        byte[] b = new byte[2048];
                        while ((length = tarIn.read(b)) != -1) {
                            out.write(b, 0, length);
                        }
                    }
                }
            }
        } catch (IOException e) {
            BuildStatus.getInstance().recordError();
            logger.error(e.getMessage(), e);
            throw e;
        }
        return fileNames;
    }

    private static List<String> un7z(String tarFile, String destDir) throws IOException {
        File file = new File(tarFile);
        return un7z(file, destDir);
    }

    private static List<String> un7z(File tarFile, String destDir) throws IOException {
        if (StringUtils.isBlank(destDir)) {
            destDir = tarFile.getParent();
        }
        destDir = destDir.endsWith(File.separator) ? destDir : destDir + File.separator;
        return un7z1(tarFile, destDir);
    }

    private static List<String> unTar(String tarFile, String destDir) throws IOException {
        File file = new File(tarFile);
        return unTar(file, destDir);
    }

    private static List<String> unTar(File tarFile, String destDir) throws IOException {
        if (StringUtils.isBlank(destDir)) {
            destDir = tarFile.getParent();
        }
        destDir = destDir.endsWith(File.separator) ? destDir : destDir + File.separator;
        return unTar(new FileInputStream(tarFile), destDir);
    }

    private static List<String> unTarBZip2(File tarFile, String destDir) throws IOException {
        if (StringUtils.isBlank(destDir)) {
            destDir = tarFile.getParent();
        }
        destDir = destDir.endsWith(File.separator) ? destDir : destDir + File.separator;
        return unTar(new BZip2CompressorInputStream(new FileInputStream(tarFile)), destDir);
    }

    private static List<String> unTarBZip2(String file, String destDir) throws IOException {
        File tarFile = new File(file);
        return unTarBZip2(tarFile, destDir);
    }

    private static List<String> unBZip2(String bzip2File, String destDir) throws IOException {
        File file = new File(bzip2File);
        return unBZip2(file, destDir);
    }

    private static List<String> unBZip2(File srcFile, String destDir) throws IOException {
        if (StringUtils.isBlank(destDir)) {
            destDir = srcFile.getParent();
        }
        destDir = destDir.endsWith(File.separator) ? destDir : destDir + File.separator;
        List<String> fileNames = new ArrayList<>();
        File destFile = new File(destDir, FilenameUtils.getBaseName(srcFile.toString()));
        fileNames.add(FilenameUtils.getBaseName(srcFile.toString()));

        try (InputStream is = new BZip2CompressorInputStream(new BufferedInputStream(new FileInputStream(srcFile), BUFFER_SIZE));
             OutputStream os = new BufferedOutputStream(new FileOutputStream(destFile), BUFFER_SIZE)) {
            IOUtils.copy(is, os);
        }
        return fileNames;
    }

    private static List<String> unGZ(String gzFile, String destDir) throws IOException {
        File file = new File(gzFile);
        return unGZ(file, destDir);
    }

    private static List<String> unGZ(File srcFile, String destDir) throws IOException {
        if (StringUtils.isBlank(destDir)) {
            destDir = srcFile.getParent();
        }
        destDir = destDir.endsWith(File.separator) ? destDir : destDir + File.separator;
        List<String> fileNames = new ArrayList<>();
        File destFile = new File(destDir, FilenameUtils.getBaseName(srcFile.toString()));
        fileNames.add(FilenameUtils.getBaseName(srcFile.toString()));

        try (InputStream is = new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(srcFile), BUFFER_SIZE));
             OutputStream os = new BufferedOutputStream(new FileOutputStream(destFile), BUFFER_SIZE)) {
            IOUtils.copy(is, os);
        }
        return fileNames;
    }

    private static List<String> unTarGZ(File tarFile, String destDir) throws IOException {
        if (StringUtils.isBlank(destDir)) {
            destDir = tarFile.getParent();
        }
        destDir = destDir.endsWith(File.separator) ? destDir : destDir + File.separator;
        return unTar(new GzipCompressorInputStream(new FileInputStream(tarFile)), destDir);
    }

    private static List<String> unTarGZ(String file, String destDir) throws IOException {
        File tarFile = new File(file);
        return unTarGZ(tarFile, destDir);
    }

    private static List<String> unZip(String zipfilePath, String destDir) throws IOException {
        File zipFile = new File(zipfilePath);
        if (destDir == null || destDir.equals("")) {
            destDir = zipFile.getParent();
        }
        destDir = destDir.endsWith(File.separator) ? destDir : destDir + File.separator;
        List<String> fileNames = new ArrayList<>();

        try (ZipArchiveInputStream is = new ZipArchiveInputStream(new BufferedInputStream(new FileInputStream(zipfilePath), BUFFER_SIZE))) {
            ZipArchiveEntry entry;
            while ((entry = is.getNextZipEntry()) != null) {
                fileNames.add(entry.getName());
                if (entry.isDirectory()) {
                    File directory = new File(destDir, entry.getName());
                    directory.mkdirs();
                } else {
                    try (OutputStream os = new BufferedOutputStream(new FileOutputStream(new File(destDir, entry.getName())), BUFFER_SIZE)) {
                        IOUtils.copy(is, os);
                    }
                }
            }
        } catch (Exception e) {
            BuildStatus.getInstance().recordError();
            logger.error(e.getMessage());
            throw e;
        }
        return fileNames;
    }

    private static List<String> unWar(String warPath, String destDir) {
        List<String> fileNames = new ArrayList<>();
        File warFile = new File(warPath);
        try (
                BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(warFile));
                ArchiveInputStream in = new ArchiveStreamFactory().createArchiveInputStream(ArchiveStreamFactory.JAR, bufferedInputStream)) {

            JarArchiveEntry entry;
            while ((entry = (JarArchiveEntry) in.getNextEntry()) != null) {
                fileNames.add(entry.getName());
                if (entry.isDirectory()) {
                    new File(destDir, entry.getName()).mkdir();
                } else {
                    OutputStream out = new BufferedOutputStream(new FileOutputStream(new File(destDir, entry.getName())), BUFFER_SIZE);
                    IOUtils.copy(in, out);
                    out.close();
                }
            }
        } catch (Exception e) {
            BuildStatus.getInstance().recordError();
            logger.error(e.getMessage(), e);
        }

        return fileNames;
    }

    private static void unCompress(final String compressFile,final String destDir) throws IOException {
        String upperName = compressFile.toUpperCase();
        if (upperName.endsWith(".ZIP")) {
            unZip(compressFile, destDir);
        } else if (upperName.endsWith(".TAR")) {
            unTar(compressFile, destDir);
        } else if (upperName.endsWith(".TAR.BZ2")) {
            unTarBZip2(compressFile, destDir);
        } else if (upperName.endsWith(".BZ2")) {
            unBZip2(compressFile, destDir);
        } else if (upperName.endsWith(".TAR.GZ")) {
            unTarGZ(compressFile, destDir);
        } else if (upperName.endsWith(".GZ")) {
            unGZ(compressFile, destDir);
        } else if (upperName.endsWith(".WAR")) {
            unWar(compressFile, destDir);
        } else if (upperName.endsWith(".7Z")) {
            un7z(compressFile, destDir);
        }
    }

    public static void renameTo(final String fileFullName,final String destFullName) {
        if (StringUtils.isNoneBlank(fileFullName, destFullName)) {
            File dest = new File(destFullName);
            if (dest.exists()) {
                dest.delete();
            }
            File source = new File(fileFullName);
            if (source.exists()) {
                source.renameTo(dest);
            } else {
                BuildStatus.getInstance().recordError();
                logger.error("error: failed to find source file[" + fileFullName + "].");
            }
        }
    }

    /**
     * Create a new file if it doesn't exist.
     */
    public static void createNew(final String fileFullName) {
        if (StringUtils.isNotBlank(fileFullName)) {
            try {
                File file = new File(fileFullName);
                if (!file.exists()) {
                    file.createNewFile();
                }
            } catch (IOException e) {
                BuildStatus.getInstance().recordError();
                logger.error("error: failed to create new file.");
                logger.error(e.getMessage(), e);
            }
        }
    }

    private static void createDirectory(final String outputDir,final String subDir) {
        File file = new File(outputDir);
        if (!(subDir == null || subDir.trim().equals(""))) {
            file = new File(outputDir + File.separator + subDir);
        }
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public static void createDirectories(String folderPath) {
        if (StringUtils.isNotBlank(folderPath)) {
            File folder = new File(folderPath);
            try {
                if (!folder.isDirectory()) {
                    logger.debug("create directories:" + folderPath);
                    folder.mkdirs();
                }
            } catch (Exception e) {
                BuildStatus.getInstance().recordError();
                logger.error("error: failed to create directories.");
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Copies a file to a directory preserving the file date.
     * <p>
     * This method copies the contents of the specified source file
     * to a file of the same name in the specified destination directory.
     * The destination directory is created if it does not exist.
     * If the destination file exists, then this method will overwrite it.
     */
    public static boolean copyFileToDirectory(String srcFileFullName,String destDirFullPath) {
        File srcFile = new File(srcFileFullName);
        File destDir = new File(destDirFullPath);
        try {
            if (srcFile.isFile() && StringUtils.isNotBlank(destDirFullPath)) {
                FileUtils.copyFileToDirectory(srcFile, destDir);
                return true;
            }
        } catch (IOException e) {
            BuildStatus.getInstance().recordError();
            logger.error(e.getMessage(), e);
        }
        return false;
    }

    /***
     * get all file paths by filePath, maybe one file path return, or maybe more file paths return.
     * @param filePath maybe contains "*"
     */
    public static List<String> getFilesByFilter(final String filePath,final String excludeFilters,
                                                final boolean keepDirStructure) {
        if (StringUtils.isNotBlank(filePath)) {
            return listFilesByFilter(filePath, null, excludeFilters,keepDirStructure);
        }
        return new ArrayList<>();
    }

    protected static Pair<File, String> splitFilePathIntoParentFileAndFileName(String filePathParam) {
        String filePath = filePathParam.replace("\"", "");
        if (filePath.endsWith("/") || filePath.endsWith("\\")) {
            filePath = filePath.substring(0, filePath.length() - 1);
        }

        int lastSlash = filePath.lastIndexOf("\\") == -1 ? filePath.lastIndexOf("/") : filePath.lastIndexOf("\\");
        File parentPath = new File(filePath.substring(0, lastSlash));
        String fileName = filePath.substring(lastSlash + 1);
        return Pair.of(parentPath, fileName);
    }

    private static List<String> listFilesByFilter(final String filePath,final String filterStr,
                                                  final String exfilterStr,final boolean keepDirStructure) {
        List<String> filePaths = new ArrayList<>();
        File fileFullPath = new File(filePath);
        /*if (StringUtils.isBlank(filterStr)) {
            filterStr = "";
        }
        if (StringUtils.isBlank(exfilterStr)) {
            exfilterStr = "";
        }*/
        if (fileFullPath.exists()) {
            if (fileFullPath.isDirectory()) {
                File[] files = filterFilesAndSubFolders(fileFullPath, filterStr, exfilterStr);
                Boolean flag=true;
                for (File file : files) {
                    if(file.isFile()){
                        if(keepDirStructure && flag){
                            filePaths.add(file.getParent());
                            flag=false;
                        }
                        filePaths.add(file.getAbsolutePath());
                    }else{
                        filePaths.addAll(listFilesByFilter(file.getAbsolutePath(), filterStr, exfilterStr,keepDirStructure));
                    }
                }
            }else{
                filePaths.add(filePath);
            }
        } else {
            Pair<File, String> pathParts = splitFilePathIntoParentFileAndFileName(filePath);
            String fileName = pathParts.getRight();
            File parentPath = pathParts.getLeft();

            if (parentPath.isDirectory()) {
                File[] files = filterFilesAndSubFolders(parentPath, fileName, exfilterStr);
                for (File file : files) {
                    if(file.isFile()){
                        filePaths.add(file.getAbsolutePath());
                    }else{
                        filePaths.addAll(listFilesByFilter(file.getAbsolutePath(), fileName, exfilterStr,keepDirStructure));
                    }
                }
            } else {
                BuildStatus.getInstance().recordError();
                logger.error("error: invalid path[" + filePath + "]");
            }
        }
        return filePaths;
    }

    protected static File[] filterFilesAndSubFolders(final File parentPath,final String filterStr,final String excludeFileStr) {
        final String[] filters = StringUtils.isBlank(filterStr)?null:filterStr.toLowerCase().split("\\*");
        final String[] exfilters = StringUtils.isBlank(excludeFileStr)?null:excludeFileStr.toLowerCase().replaceAll("^\\*(.*)", "$1").split(";");

        return parentPath.listFiles(new CustomFileNameFilter(filters, exfilters));
    }

    /***
     * get file name without suffix.
     * @param fileFullName fullpath or filename
     */
    public static String getFileNameWithoutSuffix(String fileFullName) {
        fileFullName = fileFullName.replace("\"", "");
        if (fileFullName.endsWith("/") || fileFullName.endsWith("\\")) {
            fileFullName = fileFullName.substring(0, fileFullName.length() - 1);
        }
        int lastDot = fileFullName.lastIndexOf(".");
        int lastSlash = fileFullName.lastIndexOf("\\") == -1 ? fileFullName.lastIndexOf("/") : fileFullName.lastIndexOf("\\");
        return fileFullName.substring(lastSlash + 1, lastDot);
    }

    /***
     * get file name with suffix.
     * @param fileFullName fullpath or filename
     */
    public static String getFileNameWithSuffix(String fileFullName) {
        fileFullName = fileFullName.replace("\"", "");
        if (fileFullName.endsWith("/") || fileFullName.endsWith("\\")) {
            fileFullName = fileFullName.substring(0, fileFullName.length() - 1);
        }
        int lastSlash = fileFullName.lastIndexOf("\\") == -1 ? fileFullName.lastIndexOf("/") : fileFullName.lastIndexOf("\\");
        return fileFullName.substring(lastSlash + 1);
    }


    public static boolean search(final String fileFullName,final String searchStr) {
        boolean flag = false;
        if (new File(fileFullName).isFile()) {
            logger.debug("search " + searchStr + " in " + fileFullName);
            try (BufferedReader bufReader = new BufferedReader(new InputStreamReader(new FileInputStream(fileFullName),CharacterSet))) {
                String line;
                while ((line = bufReader.readLine()) != null) {
                    if (line.equalsIgnoreCase(searchStr)) {
                        flag = true;
                        logger.info("found " + searchStr + " in " + fileFullName);
                        break;
                    }
                }
            } catch (IOException e) {
                BuildStatus.getInstance().recordError();
                logger.error(e.getMessage(), e);
            }
        }else{
            logger.warn("File Not Found: " + fileFullName);
        }

        return flag;
    }

    /**
     * return table's definition in a INI file
     */
    public static List<List<TableProps>> searchTablesDefinition(final String fileFullName,final String tableName) {
        List<List<TableProps>> tablesDefinition = null;
        List<TableProps> tableColumns;
        if (StringUtils.isNoneBlank(fileFullName, tableName)) {
            try (BufferedReader bufReader = new BufferedReader(new InputStreamReader(new FileInputStream(fileFullName),CharacterSet))) {
                tablesDefinition = new ArrayList<>();
                String line;
                while ((line = bufReader.readLine()) != null) {
                    if (StringUtils.equalsIgnoreCase(line, "[" + tableName + "]") || StringUtils.startsWithIgnoreCase(line, "[" + tableName + "#")) {
                        tableColumns = new ArrayList<>();
                        while ((line = bufReader.readLine()) != null) {
                            if (StringUtils.isBlank(line)) {
                                continue;
                            }
                            if (line.contains("[")) {
                                break;
                            }
                            String[] strArr = line.split("\\=| ");
                            if (strArr.length == 3) {
                                tableColumns.add(new TableProps(strArr[1], strArr[2], " NOT NULL", strArr[0]));
                            } else if (strArr.length == 4) {//strArr.length==4
                                tableColumns.add(new TableProps(strArr[1], strArr[2], "", strArr[0]));
                            } else {
                                logger.warn("please check the column definition: " + line);
                            }
                        }
                        tablesDefinition.add(tableColumns);
                    }
                }
            } catch (Exception e) {
                BuildStatus.getInstance().recordError();
                logger.error(e.getMessage(), e);
            }
        }
        return tablesDefinition;
    }

    /**
     * get mixed columns from all same table's table definition
     */
    public static List<TableProps> getMixedTablesDefinition(final List<List<TableProps>> tablesDefinition) {
        List<TableProps> tableColumns = null;
        if (tablesDefinition != null && tablesDefinition.size() > 0) {
            List<String> columnNames = new ArrayList<>();
            tableColumns = tablesDefinition.get(0);//get first table's columns
            if(tablesDefinition.size()==1){
                return tableColumns;
            }
            TableProps tableProps;
            //get first table columns' name
            for (TableProps tableColumn : tableColumns) {
                columnNames.add(tableColumn.getName());
            }
            for (int index = 1; index < tablesDefinition.size(); index++) {
                for (int col = 0; col < tablesDefinition.get(index).size(); col++) {
                    tableProps = tablesDefinition.get(index).get(col);
                    if (!columnNames.contains(tableProps.getName())) {
                        columnNames.add(tableProps.getName());
                        tableColumns.add(tableProps);
                    }
                }
            }
        }
        return tableColumns;
    }

    public static Map<String, List<TableProps>> getAllTablesMixedDefinition(final String fileFullName) {
        Map<String, List<TableProps>> mixedAllTables=null;
        List<String> tableNames=new ArrayList<>();
        List<TableProps> tableColumns;
        if (StringUtils.isNoneBlank(fileFullName)) {
            try (BufferedReader bufReader = new BufferedReader(new FileReader(fileFullName))) {

                String line;
                while ((line = bufReader.readLine()) != null) {
                    line=line.trim();
                    if(line.startsWith("[") && line.endsWith("]")){
                        String tableName=line.replaceAll("\\[|\\]", "");
                        if(tableName.contains("#")){
                            tableName=tableName.split("#")[0];
                        }
                        if(!tableNames.contains(tableName)){
                            tableNames.add(tableName);
                        }
                    }
                }
            } catch (Exception e) {
                BuildStatus.getInstance().recordError();
                logger.error(e.getMessage(), e);
            }
            for(String tableName:tableNames){
                tableColumns=getMixedTablesDefinition(searchTablesDefinition(fileFullName,tableName));
                mixedAllTables.put(tableName, tableColumns);
            }
        }
        return mixedAllTables;
    }
    /**
     * if a file contains tableName, case insensitive, it will rewrite this table's definition at the end.
     */
    public static void updateContent(final String fileFullName,final String tableName,final String addedContent) {
        logger.info("update file: " + fileFullName);
        StringBuffer strBuffer = new StringBuffer();
        try (BufferedReader bufReader = new BufferedReader(new InputStreamReader(new FileInputStream(fileFullName),CharacterSet))) {
            String line;
            while ((line = bufReader.readLine()) != null) {    //delete searched string and its sub fields
                if (line.toLowerCase().contains(tableName.toLowerCase())) {
                    while ((line = bufReader.readLine()) != null) {
                        if (line.contains("[")) {
                            break;
                        }
                    }
                    if (line == null) {
                        break;
                    }
                }
                strBuffer.append(line).append(System.getProperty("line.separator"));
            }
            bufReader.close();

            try (BufferedWriter bufWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileFullName),CharacterSet))) {

                bufWriter.append(strBuffer);
                bufWriter.flush();
                bufWriter.append(addedContent);
                bufWriter.flush();
            }
        } catch (IOException e) {
            BuildStatus.getInstance().recordError();
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * get content of fileFullName, return String
     */
    public static String getSQLContent(final String fileFullName) {
        StringBuilder contents = new StringBuilder();
        if (StringUtils.isNotBlank(fileFullName)) {
            try (BufferedReader bufReader = new BufferedReader(new InputStreamReader(new FileInputStream(fileFullName),CharacterSet))) {
                String line;
                while ((line = bufReader.readLine()) != null) {
                    line=line.trim();
                    if(line.equals("") || line.startsWith("#") || line.startsWith("--")|| line.startsWith("//")){
                        continue;
                    }else if(line.startsWith("/*")){
                        do{
                            if(line.trim().endsWith("*/")){
                                break;
                            }
                        }while ((line = bufReader.readLine()) != null);
                    }else{
                        contents.append(line);
                    }
                }
            } catch (Exception e) {
                BuildStatus.getInstance().recordError();
                logger.error(e.getMessage(), e);
            }
        }
        return contents.toString();
    }

    /***
     * support folder and file, generate a new file name with path.
     * @param fileFullName  the full file name
     * @param suffix can be null
     * @param newFilePath can be null, get fileFullName's parent path
     * @return new file full path with name
     */
    @SuppressWarnings("findbugs:DM_DEFAULT_ENCODING")
    public static String createNewFileWithSuffix(final String fileFullName,final String suffix,String newFilePath) {
        String newFileFullName = null;
        File file = new File(fileFullName);
        if (file.exists()) {
            String fileName = file.getName();
            int count = 1;
            String namePrefix = fileName;
            String nameSuffix = "";
            if (file.isFile() && fileName.contains(".")) {
                namePrefix = fileName.substring(0, fileName.lastIndexOf("."));
                nameSuffix = fileName.replace(namePrefix, "");
            }
            //target newFilePath is null
            if (StringUtils.isBlank(newFilePath)) {
                newFilePath = file.getPath().replace(namePrefix + nameSuffix, "");
            }
            if (StringUtils.isBlank(suffix)) {
                newFileFullName = namePrefix + "(" + count + ")" + nameSuffix;
                while (new File(newFilePath + newFileFullName).exists()) {
                    count++;
                    newFileFullName = namePrefix + "(" + count + ")" + nameSuffix;
                }
            } else {
                newFileFullName = namePrefix + suffix + nameSuffix;
                while (new File(newFilePath + newFileFullName).exists()) {
                    newFileFullName = namePrefix + suffix + "(" + count + ")" + nameSuffix;
                    count++;
                }
            }
            newFileFullName = newFilePath + newFileFullName;
            logger.info("new file name is {}.", newFileFullName);
        } else {
            BuildStatus.getInstance().recordError();
            logger.error("argument:fileFullName[{}] doesn't exist.", fileFullName);
        }

        return newFileFullName;
    }

    public static void copyDirectory(final String sourcePath,final String destPath) {
        try {
            if (sourcePath != null && destPath != null) {
                long begin=System.currentTimeMillis();
                File sourceFile = new File(sourcePath);
                File destFile = new File(destPath);
                createDirectories(destPath);
                FileUtils.copyDirectory(sourceFile, destFile);
                logger.info("copy folder time(sec):"+(System.currentTimeMillis()-begin)/1000.00F);
            }
        } catch (Exception e) {
            BuildStatus.getInstance().recordError();
            logger.error(e.getMessage(), e);
        }
    }

    public static void copyExternalProject(final String srcFile,final String destDir,final String uncompress) {
        File srcFileHd = new File(srcFile);
        if (srcFileHd.exists()) {
            createDirectories(destDir);
            if (srcFileHd.isDirectory()) {
                copyDirectory(srcFile, destDir);
            }
            if (srcFileHd.isFile()) {
                if (StringUtils.containsIgnoreCase("yes", uncompress)) {
                    try {
                        String var2 = System.getProperty("os.name").split(" ")[0];
                        if(var2.equalsIgnoreCase("windows")){
                            SevenZipServer.extractZIP7Parallel(srcFile,destDir+File.separator);
                        }else {
                            unCompress(srcFile, destDir);
                        }
                    } catch (Exception e) {
                        BuildStatus.getInstance().recordError();
                        logger.error(e.getMessage(), e);
                    }
                    Runtime.getRuntime().gc();
                } else {
                    copyFileToDirectory(srcFile, destDir);
                }
            }
        } else {
            BuildStatus.getInstance().recordError();
            logger.error("File Not Found: " + srcFile);
        }
    }

    public static List<String> getSubFolderNames(String path) {
        List<String> filenames = new ArrayList<>();
        if (StringUtils.isNotBlank(path)) {
            File parentPath = new File(path);
            if (parentPath.isDirectory()) {
                File[] files = parentPath.listFiles((dir, name) -> {
                    boolean flag = false;
                    if (new File(dir, name).isDirectory() && !name.startsWith(".")) {
                        flag = true;
                    }
                    return flag;
                });
                for (File file : files) {
                    filenames.add(file.getName());
                }
            } else {
                BuildStatus.getInstance().recordError();
                logger.error("File is not Directory: " + path);
            }
        }
        return filenames;
    }
    public static List<String> getSingleMetadataNames(String path) {
        List<String> filenames = new ArrayList<>();
        if (StringUtils.isNotBlank(path)) {
            File parentPath = new File(path);
            if (parentPath.isDirectory()) {
                File[] files = parentPath.listFiles((dir, name) -> {
                    boolean flag = false;
                    if (new File(dir, name).isFile() && name.toLowerCase().endsWith(".csv")) {
                        flag = true;
                    }
                    return flag;
                });
                String name;
                for (File file : files) {
                    name=file.getName();
                    name=name.substring(0,name.lastIndexOf("."));//may like Rets, Rets#AR
                    name=name.split("#")[0];
                    if(!filenames.contains(name)){
                        filenames.add(name);
                    }

                }
            } else {
                BuildStatus.getInstance().recordError();
                logger.error("File is not Directory: " + path);
            }
        }
        return filenames;
    }
    public static String getFolderRegex(final List<String> filenames) {
        //List<String> filenames = getSubFolderNames(path);
        StringBuilder names = new StringBuilder();
        if (filenames != null && filenames.size() > 0) {
            names.append("(");
            for (String filename : filenames
            ) {
                names.append(filename).append("|");
            }
            names.deleteCharAt(names.length() - 1);
            names.append(")(_.*)");
        } else {
            names.append("(GridKey|GridRef|List|Ref|Sums|Vals|XVals|RefReturns)(_.*)");
        }
        return names.toString();
    }

    public static void deleteFiles(final String path, final String excludeFilter){
        File file=new File(path);
        final String[] excludeName=new String[]{excludeFilter.toLowerCase()};
        if(file.exists()){
            deleteFiles(file,excludeName);
        }
    }
    public static void deleteFiles(final File file,final String[] excludeName){
        if(file.isDirectory()){
            File[] files=file.listFiles(new CustomFileNameFilter(null,excludeName));
            for(File file1:files){
                deleteFiles(file1,excludeName);
            }
        }
        if(file.isFile()){
            file.delete();
        }
    }
    private static class CustomFileNameFilter implements FilenameFilter {
        private static final String regex="\\*";
        private final String[] filters;
        private final String[] exFilters;

        public CustomFileNameFilter(final String[] filters,final String[] exFilters){

            this.filters = filters;
            this.exFilters = exFilters;
        }

        @Override
        public boolean accept(final File dir,final String name) {
            if (new File(dir, name).isDirectory() && !name.startsWith(".")) {
                return true;
            }
            if(ArrayUtils.isNotEmpty(filters)){
                for (String filter : filters) {
                    if (StringUtils.isNotBlank(filter) && !name.toLowerCase().contains(filter)) {
                        return false;
                    }
                }
            }
            return runExcludeFilters(name);
        }

        private boolean runExcludeFilters(final String name) {
            if(ArrayUtils.isNotEmpty(exFilters)){
                for (String exfilter : exFilters) {
                    boolean exflag = false;
                    if (StringUtils.isNotBlank(exfilter)) {
                        exflag = true;
                        String[] subfilters = exfilter.split(regex);
                        for (String subexfilter : subfilters) {
                            if (StringUtils.isNotBlank(subexfilter) && !name.toLowerCase().contains(subexfilter)) {
                                exflag = false;
                                break;
                            }
                        }
                    }
                    if (exflag) {
                        return false;
                    }
                }
            }
            return true;
        }
    }
}
