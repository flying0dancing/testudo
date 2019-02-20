package com.lombardrisk.Utils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lombardrisk.pojo.TableProps;


public class FileUtil extends FileUtils{

	private final static Logger logger = LoggerFactory.getLogger(FileUtil.class);
	
	public static Boolean exists(String fileFullName){
		Boolean flag=false;
		
		if(StringUtils.isNotBlank(fileFullName)){
			File file=new File(fileFullName);
			flag=file.exists();
		}
		return flag;
	}
	
	/***
	 * Zipped folder which has multiple files along with sub folders, also can only zip folders.
	 * @param sourcePath toZipFileFullPaths's parent path, but not be included in zip file.
	 * @param toZipFileFullPaths many files's getAbsolutePath with name.
	 * @param zipFullName zip full path with name
	 */
	public static Boolean zipFilesAndFolders(String sourcePath, List<String> toZipFileFullPaths, String zipFullName){
		Boolean flag=true;
		byte[] buf = new byte[1024];
		try{
			int lastSlash=zipFullName.lastIndexOf("\\")==-1?zipFullName.lastIndexOf("/"):zipFullName.lastIndexOf("\\");
			String zipsPath=zipFullName.substring(0,lastSlash);//get zip's path
			File zipPathHd=new File(zipsPath);
			if(!zipPathHd.exists()){//if directories doesn't exist, create them
				zipPathHd.mkdirs();
			}
			File zipFullNameHd=new File(zipFullName);
			if(zipFullNameHd.exists())
			{
				zipFullNameHd.delete();
			}
			File sourcePathHd=new File(sourcePath);
			if(!sourcePathHd.exists()){
				logger.error("error:source path cannot be found ["+sourcePath+"]");
				return false;
			}
			sourcePath=sourcePathHd.getAbsolutePath();
			OutputStream os=new BufferedOutputStream(new FileOutputStream(zipFullName));
			ZipArchiveOutputStream zipOut=new ZipArchiveOutputStream(os);
			zipOut.setEncoding("UTF-8");
			for (String fileFullPath : toZipFileFullPaths){
				File fileHd=new File(fileFullPath);
				String toZipPath=fileFullPath.substring(sourcePath.length()+1);
				if(fileHd.isDirectory()){
					zipOut.putArchiveEntry(new ZipArchiveEntry(toZipPath+System.getProperty("file.separator")));
					zipOut.closeArchiveEntry();
				}
				if(fileHd.isFile()){
					FileInputStream in = new FileInputStream(fileHd);
					zipOut.putArchiveEntry(new ZipArchiveEntry(toZipPath));
					int len=0;
					while ((len = in.read(buf)) > 0)
					{
						zipOut.write(buf, 0, len);
					}
					zipOut.closeArchiveEntry();
					in.close();
				}
			}
			zipOut.close();
			os.close();
		}catch(Exception e){
			flag=false;
			logger.error(e.getMessage(),e);
		}
		return flag;
	}
	
	/***
	 * Zipped folder which has multiple files along with sub folders, also can only zip folders.
	 * this function use java's zip contains file name's encoding issue
	 * @param sourcePath toZipFileFullPaths's parent path, but not be included in zip file.
	 * @param toZipFileFullPaths many files's getAbsolutePath with name.
	 * @param zipFullName zip full path with name
	 */
	public static Boolean ZipFiles(String sourcePath, List<String> toZipFileFullPaths, String zipFullName)
	{
		Boolean flag=true;
		byte[] buf = new byte[1024];
		try
		{
			int lastSlash=zipFullName.lastIndexOf("\\")==-1?zipFullName.lastIndexOf("/"):zipFullName.lastIndexOf("\\");
			String zipsPath=zipFullName.substring(0,lastSlash);
			File zipPathHd=new File(zipsPath);
			if(!zipPathHd.exists()){
				zipPathHd.mkdirs();
			}
			File zipFullNameHd=new File(zipFullName);
			if(zipFullNameHd.exists())
			{
				zipFullNameHd.delete();
			}
			File sourcePathHd=new File(sourcePath);
			if(!sourcePathHd.exists()){
				logger.error("error:source path cannot be found ["+sourcePath+"]");
				return false;
			}
			sourcePath=sourcePathHd.getAbsolutePath();
			ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFullName));
			for (String fileFullPath : toZipFileFullPaths)
			{
				File fileHd=new File(fileFullPath);
				String toZipPath=fileFullPath.substring(sourcePath.length()+1);
				if(fileHd.isDirectory()){
					out.putNextEntry(new ZipEntry(toZipPath+System.getProperty("file.separator")));
					out.closeEntry();
				}else{
					FileInputStream in = new FileInputStream(fileHd);
					out.putNextEntry(new ZipEntry(toZipPath));
					int len;
					while ((len = in.read(buf)) > 0)
					{
						out.write(buf, 0, len);
					}
					out.closeEntry();
					in.close();
				}
				
			}
			out.close();
		}
		catch (IOException e)
		{
			flag=false;
			logger.error(e.getMessage(),e);
		}
		return flag;
	}
	
	public static void renameTo(String fileFullName,String destFullName)
	{
		if(StringUtils.isNoneBlank(fileFullName,destFullName)){
			File dest=new File(destFullName);
			if(dest.exists()){
				dest.delete();
			}
			File source=new File(fileFullName);
			if(source.exists()){
				source.renameTo(dest);
			}else
			{
				logger.error("error: failed to find source file["+fileFullName+"].");
			}
		}
	}
	/**
	 * if file doesn't existed, create a new one.
	 * @param fileFullName
	 */
	public static void createNew(String fileFullName)
	{
		if(StringUtils.isNotBlank(fileFullName)){
			try {
				File file=new File(fileFullName);
				if(!file.exists())
				{
					file.createNewFile();
				}
			} catch (IOException e) {
				logger.error("error: failed to create new file.");
				logger.error(e.getMessage(),e);
			}
		}
	}
	
	public static void createNewDelExisted(String fileFullName)
	{
		if(StringUtils.isNotBlank(fileFullName)){
			try {
				File file=new File(fileFullName);
				if(file.exists())
				{
					file.delete();
				}
				file.createNewFile();
			} catch (IOException e) {
				logger.error("error: failed to create new file.");
				logger.error(e.getMessage(),e);
			}
		}
	}
	
	public static void createDirectories(String folderPath)
	{
		if(StringUtils.isNotBlank(folderPath))
		{
			File folder = new File(folderPath);
			try
			{
				if(!folder.isDirectory())
				{
					logger.debug("create directories:"+folderPath);
					folder.mkdirs();
				}
			}catch(Exception e)
			{
				logger.error("error: failed to create directories.");
				logger.error(e.getMessage(),e);
			}
						
		}
	}
	
	public static void deleteDirectory(String folderPath)
	{
		if(StringUtils.isNotBlank(folderPath)){
			File folder = new File(folderPath);
			if(folder.exists()){
				deleteDirectory(folder);
			}
		}
	}
	public static void deleteDirectory(File folder)
	{
		if(folder.isDirectory())
		{
			File[] files=folder.listFiles();
			for(int i=0;i<files.length;i++)
			{
				deleteDirectory(files[i]);
			}
		}
		if(folder.exists()){folder.delete();}
		
	}
	
	/***
	 * get all file paths by filePath, maybe one file path return, or maybe more file paths return.
	 * @param filePath maybe contains "*"
	 * @return
	 */
	public static List<String> getFilesByFilter(String filePath,String excludeFilters)
	{
		List<String> filePaths=null;
		if(StringUtils.isNotBlank(filePath))
		{
			filePaths=new ArrayList<String>();
			listFilesByFilter(filePath,null,excludeFilters,filePaths);
		}
		return filePaths;
	}
	
	public static void listFilesByFilter(String filePath,String filterStr,String exfilterStr, List<String> filePaths)
	{
		File fileFullPath=new File(filePath);
		if(StringUtils.isBlank(filterStr))
		{
			filterStr="";
		}
		if(StringUtils.isBlank(exfilterStr))
		{
			exfilterStr="";
		}
		if(fileFullPath.exists())
		{
			if(fileFullPath.isDirectory())
			{
				File[] files=filterFilesAndSubFolders(fileFullPath,filterStr,exfilterStr);
				for(File file:files)
				{listFilesByFilter(file.getAbsolutePath(),filterStr,exfilterStr,filePaths);}
			}
			if(fileFullPath.isFile())
			{filePaths.add(fileFullPath.getAbsolutePath());}
		}else
		{
			filePath=filePath.replace("\"", "");
			if(filePath.endsWith("/") || filePath.endsWith("\\")){
				filePath=filePath.substring(0,filePath.length()-1);
			}
			int lastSlash=filePath.lastIndexOf("\\")==-1?filePath.lastIndexOf("/"):filePath.lastIndexOf("\\");
			String fileName=filePath.substring(lastSlash+1);
			File parentPath=new File(filePath.substring(0,lastSlash));//TODO maybe contains risk, for example see Helper.getParentPath
			if(parentPath.isDirectory())
			{
				File[] files=filterFilesAndSubFolders(parentPath,fileName,exfilterStr);
				for(File file:files)
				{listFilesByFilter(file.getAbsolutePath(),fileName,exfilterStr,filePaths);}
				
			}else{
				logger.error("error: invalid path["+filePath+"]");
			}
		}
		
	}
	
	private static File[] filterFilesAndSubFolders(File parentPath,String filterStr,String excludeFileStr)
	{
		final String[] fileters=filterStr.toLowerCase().split("\\*");
		final String[] exfileters=excludeFileStr.toLowerCase().replaceAll("^\\*(.*)", "$1").split(";");
		File[] files=parentPath.listFiles(new FilenameFilter(){
			@Override
			public boolean accept(File dir, String name) {
				boolean flag=true;
				if(new File(dir,name).isDirectory() && !name.startsWith(".")){
					return flag;
				}
				for(String filter:fileters)
				{
					if(StringUtils.isNotBlank(filter) && !name.toLowerCase().contains(filter)) {
						flag=false;
						break;
					}
				}
				if(flag){
					Boolean exflag=false;
					for(String exfilter:exfileters){
						if(StringUtils.isNotBlank(exfilter)){
							exflag=true;
							String[] subfilters=exfilter.split("\\*");
							for(String subexfilter:subfilters){
								if(StringUtils.isNotBlank(subexfilter) && !name.toLowerCase().contains(subexfilter)){
									exflag=false;
									break;
								}
							}
						}
						if(exflag)
						{
							flag=false;
							break;
						}
					}
				}
				return flag;
			}
		});
		return files;
	}
	
	/***
	 * get file name without suffix.
	 * @param fileFullName fullpath or filename
	 * @return
	 */
	public static String getFileNameWithoutSuffix(String fileFullName)
	{
		fileFullName=fileFullName.replace("\"", "");
		if(fileFullName.endsWith("/") || fileFullName.endsWith("\\")){
			fileFullName=fileFullName.substring(0,fileFullName.length()-1);
		}
		int lastDot=fileFullName.lastIndexOf(".");
		int lastSlash=fileFullName.lastIndexOf("\\")==-1?fileFullName.lastIndexOf("/"):fileFullName.lastIndexOf("\\");
		String fileName=fileFullName.substring(lastSlash+1,lastDot);
		return fileName;
	}
	/***
	 * get file name with suffix.
	 * @param fileFullName fullpath or filename
	 * @return
	 */
	public static String getFileNameWithSuffix(String fileFullName)
	{
		fileFullName=fileFullName.replace("\"", "");
		if(fileFullName.endsWith("/") || fileFullName.endsWith("\\")){
			fileFullName=fileFullName.substring(0,fileFullName.length()-1);
		}
		int lastSlash=fileFullName.lastIndexOf("\\")==-1?fileFullName.lastIndexOf("/"):fileFullName.lastIndexOf("\\");
		String fileName=fileFullName.substring(lastSlash+1);
		return fileName;
	}

	public static Boolean search(String fileFullName, String searchStr)
	{
		Boolean flag=false;
		logger.info("search "+searchStr+" in "+fileFullName);
		File filehd=new File(fileFullName);
		if(!filehd.exists() || !filehd.isFile()){
			logger.warn("File Not Found: "+fileFullName);
			return flag;
		}
		BufferedReader bufReader=null;
		try {
			bufReader=new BufferedReader(new FileReader(fileFullName));
			String line=null;
			while((line=bufReader.readLine())!=null){
				if(line.toLowerCase().equals(searchStr.toLowerCase()))
				{
					flag=true;
					logger.info("found "+searchStr+" in "+fileFullName);
					break;
				}
			}
			bufReader.close();
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage(),e);
		} catch (IOException e) {
			logger.error(e.getMessage(),e);
		}finally{
			try {
				bufReader.close();
			} catch (IOException e) {
				logger.error(e.getMessage(),e);
			}
		}
		return flag;
	}
	
	/**
	 * return table's definition in a INI file
	 * @param fileFullName
	 * @param tableName
	 * @return
	 */
	public static List<String> searchTableDefinition(String fileFullName,String tableName)
	{
		List<String> tableDefinition=null;
		BufferedReader bufReader=null;
		try{
			if(StringUtils.isNoneBlank(fileFullName,tableName)){
				//
				bufReader=new BufferedReader(new FileReader(fileFullName));
				String line=null;
				while((line=bufReader.readLine())!=null){
					if(StringUtils.equalsIgnoreCase(line, "["+tableName+"]")){
						tableDefinition=new ArrayList<String>();
						while((line=bufReader.readLine())!=null)
						{
							if(StringUtils.isBlank(line))continue;
							if(line.contains("[")){	break;}
							tableDefinition.add(line);
						}
						break;
					}
				}
				bufReader.close();
			}
		}catch(Exception e){
			logger.error(e.getMessage(),e);
		}
		return tableDefinition;
	}
	
	/**
	 * return table's definition in a INI file
	 * use other searchTablesDefinition method.
	 * @param fileFullName
	 * @param tableName
	 * @return
	 */
	@Deprecated
	public static List<List<String>> searchTablesDefinitionold(String fileFullName,String tableName)
	{
		List<List<String>> tablesDefinition=null;
		List<String> tableDefinition=null;
		BufferedReader bufReader=null;
		try{
			if(StringUtils.isNoneBlank(fileFullName,tableName)){
				tablesDefinition=new ArrayList<List<String>>();
				bufReader=new BufferedReader(new FileReader(fileFullName));
				String line=null;
				
				while((line=bufReader.readLine())!=null){
					if(StringUtils.equalsIgnoreCase(line, "["+tableName+"]") || StringUtils.startsWithIgnoreCase(line, "["+tableName+"#")){
						tableDefinition=new ArrayList<String>();
						while((line=bufReader.readLine())!=null)
						{
							if(StringUtils.isBlank(line))continue;
							if(line.contains("[")){	break;}
							tableDefinition.add(line);
						}
						tablesDefinition.add(tableDefinition);
					}
				}
				bufReader.close();
			}
		}catch(Exception e){
			logger.error(e.getMessage(),e);
		}
		return tablesDefinition;
	}
	/**
	 * return table's definition in a INI file
	 * @param fileFullName
	 * @param tableName
	 * @return
	 */
	public static List<List<TableProps>> searchTablesDefinition(String fileFullName,String tableName)
	{
		List<List<TableProps>> tablesDefinition=null;
		List<TableProps> tableColumns=null;
		BufferedReader bufReader=null;
		try{
			if(StringUtils.isNoneBlank(fileFullName,tableName)){
				tablesDefinition=new ArrayList<List<TableProps>>();
				bufReader=new BufferedReader(new FileReader(fileFullName));
				String line=null;
				String[] strArr=null;
				while((line=bufReader.readLine())!=null){
					if(StringUtils.equalsIgnoreCase(line, "["+tableName+"]") || StringUtils.startsWithIgnoreCase(line, "["+tableName+"#")){
						tableColumns=new ArrayList<TableProps>();
						while((line=bufReader.readLine())!=null)
						{
							if(StringUtils.isBlank(line))continue;
							if(line.contains("[")){	break;}
							//tableColumns.add(line);//
							strArr=line.split("\\=| ");
							if(strArr.length==3){
								tableColumns.add(new TableProps(strArr[1],strArr[2]," NOT NULL",strArr[0]));
							}else if(strArr.length==4){//strArr.length==4
								tableColumns.add(new TableProps(strArr[1],strArr[2],"",strArr[0]));
							}else{
								logger.warn("please check the column definition: "+line);
							}
							
						}
						tablesDefinition.add(tableColumns);
					}
				}
				bufReader.close();
			}
		}catch(Exception e){
			logger.error(e.getMessage(),e);
		}
		return tablesDefinition;
	}
	
	/***
	 * get the max columns' table definition.
	 * @param tablesDefinition
	 * @return
	 */
	public static List<String> getMaxTablesDefinition(List<List<String>> tablesDefinition){
		List<String> tableDefinition=null;
		if(tablesDefinition!=null && tablesDefinition.size()>0){
			int max=tablesDefinition.get(0).size();
			for(List<String> columns:tablesDefinition){
				if(columns.size()>max){
					max=columns.size();
				}
			}
			for(List<String> columns:tablesDefinition){
				if(columns.size()==max){
					tableDefinition=columns;
					break;
				}
			}
			
		}
		return tableDefinition;
	}
	/**
	 * get mixed columns from all same table's table definition
	 * @param tablesDefinition
	 * @return
	 */
	public static List<TableProps> getMixedTablesDefinition(List<List<TableProps>> tablesDefinition){
		List<TableProps> tableColumns=null;
		if(tablesDefinition!=null && tablesDefinition.size()>0){
			List<String> columnNames=new ArrayList<String>();
			tableColumns=tablesDefinition.get(0);//get first table's columns
			
			TableProps tableProps=null;
			//get first table columns' name
			for(int i=0;i<tableColumns.size();i++){
				columnNames.add(tableColumns.get(i).getName());
			}
			for(int index=1;index<tablesDefinition.size();index++){
				for(int col=0;col<tablesDefinition.get(index).size();col++){
					tableProps=tablesDefinition.get(index).get(col);
					if(!columnNames.contains(tableProps.getName())){
						columnNames.add(tableProps.getName());
						tableColumns.add(tableProps);
					}
				}
			}
			
		}
		return tableColumns;
	}
	/**
	 * if a file contains tableName, case insensitive, it will rewrite this table's definition at the end.
	 * @param fileFullName
	 * @param tableName
	 * @param addedContent
	 * @return
	 */
	public static Boolean updateContent(String fileFullName, String tableName,String addedContent)
	{
		Boolean flag=false;
		logger.info("update file: "+fileFullName);
		StringBuffer strBuffer=null;
		BufferedReader bufReader=null;
		try {
			strBuffer=new StringBuffer("");
			bufReader=new BufferedReader(new FileReader(fileFullName));
			String line=null;
			while((line=bufReader.readLine())!=null)
			{	//delete searched string and its sub fields
				if(line.toLowerCase().contains(tableName.toLowerCase()))
				{
					while((line=bufReader.readLine())!=null)
					{
						if(line.contains("[")){	break;}
						continue;
					}
					if(line==null){break;}
				}
				strBuffer.append(line+System.getProperty("line.separator"));
			}
			bufReader.close();
			
			BufferedWriter bufWriter=new BufferedWriter(new FileWriter(fileFullName));
			bufWriter.append(strBuffer);
			bufWriter.flush();
			bufWriter.append(addedContent);
			bufWriter.flush();
			bufWriter.close();
			
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage(),e);
		} catch (IOException e) {
			logger.error(e.getMessage(),e);
		}
		return flag;
	}
	
	/**
	 * get file's content, return a list
	 * @param fileFullName
	 * @return
	 */
	public static List<String> getFileContent(String fileFullName)
	{
		List<String> contents=new ArrayList<String>();
		BufferedReader bufReader=null;
		try{
			if(StringUtils.isNotBlank(fileFullName)){
				//
				bufReader=new BufferedReader(new FileReader(fileFullName));
				String line=null;
				while((line=bufReader.readLine())!=null){
					contents.add(line);
					System.out.println(line);
				}
				bufReader.close();
			}
		}catch(Exception e){
			logger.error(e.getMessage(),e);
		}
		return contents;
	}
	/**
	 * return a map, key is tablename, value is a List<noscript><</noscript>String> is definition of this table.
	 * @param fileFullName
	 * @return
	 */
	public static Map<String,List<String>> getAllTableDefinitions(String fileFullName){
		Map<String,List<String>> contents=new HashMap<String,List<String>>();
		BufferedReader bufReader=null;
		try{
			if(StringUtils.isNotBlank(fileFullName)){
				bufReader=new BufferedReader(new FileReader(fileFullName));
				String line=null;
				while((line=bufReader.readLine())!=null){
					line=line.trim();
					if(line.startsWith("[") && line.endsWith("]") ){
						String tableName=line.replaceAll("\\[|\\]", "");
						List<String> tableDefinition=searchTableDefinition(fileFullName,tableName);
						contents.put(tableName, tableDefinition);
					}
				}
				bufReader.close();
			}
		}catch(Exception e){
			logger.error(e.getMessage(),e);
		}
		return contents;
	}
	
	/**
	 * get content of fileFullName, return String
	 * @param fileFullName
	 * @return
	 */
	public static String getFileContent1(String fileFullName){
		StringBuffer contents=new StringBuffer();
		BufferedReader bufReader=null;
		try{
			if(StringUtils.isNotBlank(fileFullName)){
				//
				bufReader=new BufferedReader(new FileReader(fileFullName));
				String line=null;
				while((line=bufReader.readLine())!=null){
					contents.append(line);
					System.out.println(line);
				}
				bufReader.close();
			}
		}catch(Exception e){
			logger.error(e.getMessage(),e);
		}
		return contents.toString();
	}
	
	/***
	 * support folder and file, generate a new file name with path.
	 * @param fileFullName
	 * @param suffix can be null
	 * @param newFilePath can be null, get fileFullName's parent path
	 * @return new file full path with name
	 */
	public static String createNewFileWithSuffix(String fileFullName,String suffix,String newFilePath)
	{
		String newFileFullName=null;
		File file=new File(fileFullName);
		if(file.exists())
		{
			String fileName=file.getName();
			int count=1;
			String namePrefix=fileName, nameSuffix="";
			if(file.isFile() && fileName.contains(".")){
				namePrefix=fileName.substring(0, fileName.lastIndexOf("."));
				nameSuffix=fileName.replace(namePrefix, "");
			}
			//target newFilePath is null
			if(StringUtils.isBlank(newFilePath))
			{newFilePath=file.getPath().replace(namePrefix+nameSuffix, "");}
			if(StringUtils.isBlank(suffix))
			{
				newFileFullName=namePrefix+"("+String.valueOf(count)+")"+nameSuffix;
				while(new File(newFilePath+newFileFullName).exists())
				{
					count++;
					newFileFullName=namePrefix+"("+String.valueOf(count)+")"+nameSuffix;
				}
			}else
			{
				newFileFullName=namePrefix+suffix+nameSuffix;
				while(new File(newFilePath+newFileFullName).exists())
				{
					newFileFullName=namePrefix+suffix+"("+String.valueOf(count)+")"+nameSuffix;
					count++;
				}
			}
			newFileFullName=newFilePath+newFileFullName;
			logger.info("new file name is {}.",newFileFullName);
		}else
		{
			logger.error("argument:fileFullName[{}] doesn't exist.",fileFullName);
		}
		
		return newFileFullName;
	}
	
	public static void copyDirectory(String sourcePath, String destPath)
	{
		try
		{
			if(sourcePath!=null && destPath!=null)
			{
				File sourceFile=new File(sourcePath);
				File destFile=new File(destPath);
				copyDirectory(sourceFile,destFile);
			}
		
		}catch(Exception e)
		{logger.error(e.getMessage(),e);}

	}

}
