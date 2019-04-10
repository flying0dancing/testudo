package com.lombardrisk.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;



import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dom4jUtil {

	private final static Logger logger = LoggerFactory.getLogger(Dom4jUtil.class);
	
	/**
	 * get attribute path's value from manifest.xml
	 * @param xmlFileStr
	 * @param localPath
	 * @return
	 */
	public static List<String> getPathFromElement(String xmlFileStr,String localPath)
	{ 
		Document doc =null;
		List<String> paths=new ArrayList<String>();
	    try
	    {
	    	SAXReader reader=new SAXReader();
	    	File xmlFile=new File(xmlFileStr);
	    	Element root=null;
	    	if(!xmlFile.exists())
	    	{
	    		logger.error("error: invalid xml file["+xmlFileStr+"]");
	    		return paths;
	        	
	    	}else
	    	{
	    		doc=reader.read(xmlFile);
	    		root=doc.getRootElement();//list
	    	}
	    	String op=System.getProperty("file.separator");
	    	@SuppressWarnings("unchecked")
			List<Element> eltList=root.elements();
	    	for(Element element:eltList){
				if(element.getName().equalsIgnoreCase("assets")){
					List<String> manifestpaths=new ArrayList<String>();
					getPathsFromElement(element,"path","",manifestpaths);
					for(String path:manifestpaths){
			    		paths.add(Helper.reviseFilePath(localPath+path));
			    	}
					break;
				}
			}
	    	
	    	/*
	    	String formConfig=Helper.reviseFilePath(localPath+getPathFromElement(root, "formConfig","path"));//1 level
	    	if(StringUtils.isNotBlank(formConfig)){
	    		paths.add(formConfig);
	    		
	    		String arbitraryExcelExport=formConfig+op+getPathFromElement(root, "arbitraryExcelExport","path");//1-2 level
		    	List<String> tmppaths=Helper.getRelativePaths(formConfig+op,getPathFromElement(root, "presentationTemplate","path"));
		    	if(tmppaths!=null && tmppaths.size()>0){
		    		paths.addAll(tmppaths);
		    	}
		    	paths.addAll(Helper.getRelativePaths(formConfig+op,getPathFromElement(root, "excelExportTemplate","path")));
		    	paths.addAll(Helper.getRelativePaths(formConfig+op,getPathFromElement(root, "arbitraryExcelExport","path")));
		    	paths.addAll(Helper.getRelativePaths(arbitraryExcelExport+op,getPathFromElement(root, "arbitraryExcelExportTemplate","path")));
		    	paths.addAll(Helper.getRelativePaths(arbitraryExcelExport+op,getPathFromElement(root, "arbitraryExcelExportDescriptor","path")));
		    	paths.add(Helper.reviseFilePath(localPath+op+getPathFromElement(root, "transforms","path")));
		    	paths.add(Helper.reviseFilePath(localPath+op+getPathFromElement(root, "dpmConfig","path")));
	    	}*/
	    	
	    }catch(Exception e)
	    {
	    	logger.error(e.getMessage(),e);
	    } 
	    return paths;
	}
	/**
	 * this is for get all attribute path's values of manifest.xml's assets element, stored in pathList
	 * @param parentElt
	 * @param atrr the attribute name
	 * @param appendPath set it as "" if parentElt doesn't contains the attr, set it as attr's value if parentElt contains the attr
	 * @param pathList defined it before using this method
	 */
	private static void getPathsFromElement(Element parentElt,String atrr,String appendPath,List<String> pathList)
	{
		String returnValue=null;
		int attributeCount=parentElt.attributeCount();
		if(attributeCount>0){
			Attribute attribute=parentElt.attribute(atrr);
			if(attribute!=null){
				returnValue=attribute.getValue();
				if(StringUtils.isNotBlank(returnValue)){
					returnValue=returnValue.replace("\"", "");
					returnValue=returnValue.replaceAll("^(?:[\\/\\\\]+)?(.*?)(?:[\\/\\\\]+)?$", "$1");
		    		String[] pathsTmp=returnValue.split("[\\/\\\\]+");
					for(String returnPath:pathsTmp){
						if(StringUtils.isNotBlank(appendPath)){
							appendPath=appendPath+System.getProperty("file.separator")+returnPath;
							pathList.add(appendPath);
						}else{
							appendPath=returnPath;
							pathList.add(appendPath);
						}
					}
				}
			}
		}
		@SuppressWarnings("unchecked")
		List<Element> eltList=parentElt.elements();
		for(Element element:eltList){
			if(returnValue==null){
				getPathsFromElement(element,atrr,"",pathList);
			}else{
				getPathsFromElement(element,atrr,appendPath,pathList);
			}
		}
	}
	/**
	 * 1. get attr's value from which element(elementOrAttribute); 2. get first matched elementOrAttribute's text if attr is blank
	 * for example, getPathFromElement(root, "excelExportTemplate","path")
	 * @param parentEle
	 * @param elementOrAttribute
	 * @param atrr
	 * @return
	 */
	@Deprecated
	private static String getPathFromElement(Element parentEle,String elementOrAttribute,String atrr)
	{
		boolean foundflag=false;
		String returnValue=null;
		if(elementOrAttribute.equalsIgnoreCase(parentEle.getName())){
			foundflag=true;
			if(StringUtils.isBlank(atrr)){
				returnValue=parentEle.getTextTrim();
			}else{
				returnValue=parentEle.attributeValue(atrr);
			}
			return returnValue;
		}
		if(!foundflag && StringUtils.isBlank(atrr)){
			@SuppressWarnings("unchecked")
			Iterator<Attribute> attr=parentEle.attributeIterator();
			while(attr.hasNext())
			{
				Attribute attrNext= (Attribute)attr.next();
				if(elementOrAttribute.equalsIgnoreCase(attrNext.getName())){
					foundflag=true;
					returnValue=attrNext.getValue();
					break;
				}
			}
		}
		if(!foundflag){
			@SuppressWarnings("unchecked")
			Iterator<Element> it=parentEle.elementIterator();
			while(it.hasNext())
			{
				Element elementNext = (Element) it.next(); 
				returnValue=getPathFromElement(elementNext,elementOrAttribute,atrr);
				if(returnValue!=null) return returnValue;
			}
		}
		return returnValue;
	}
	
	public static String updateElement(String xmlFileStr, String elementOrAttribute, String newValue)
	{ 
		Document doc =null;
		String value="";
	    try
	    {
	    	SAXReader reader=new SAXReader();
	    	File xmlFile=new File(xmlFileStr);
	    	Element root=null;
	    	if(!xmlFile.exists())
	    	{
	    		logger.error("error: invalid xml file["+xmlFileStr+"]");
	    		return value;
	        	
	    	}else
	    	{
	    		doc=reader.read(xmlFile);
	    		root=doc.getRootElement();//list
	    	}
	    	value=readSubElementAndUpdate(root, elementOrAttribute, newValue);//not tested
	    	writeDocumentToXml(doc,xmlFileStr);
	    }catch(Exception e)
	    {
	    	logger.error(e.getMessage(),e);
	    } 
	    return value;
	}
	

	private static String readSubElementAndUpdate(Element parentEle,String elementOrAttribute, String newValue)
	{
		boolean foundflag=false;
		String returnValue=null;
		if(elementOrAttribute.equalsIgnoreCase(parentEle.getName())){
			if(elementOrAttribute.equalsIgnoreCase("implementationVersion"))
			{
				String value=parentEle.getTextTrim();
				//if newValue is null, minor build number adds 1, means release version.
				if(StringUtils.isNotBlank(newValue)){
					value=value.replaceAll("((?:\\d+\\.){2,}\\d+).*", "$1-"+newValue); //like 1.2.3.4-4687613176
				}else{
					String minorBuildNumber=value.replaceAll("(?:\\d+\\.){2,}(\\d+).*", "$1");
					int newBuildNumber=Integer.parseInt(minorBuildNumber)+1;
					value=value.replaceAll("((?:\\d+\\.){2,}).*", "$1"+newBuildNumber); //like 1.2.3.5,1.2.3.5-4457897 to 1.2.3.6
				}
				parentEle.setText(value);
			}else{
				if(StringUtils.isNotBlank(newValue)){
					parentEle.setText(newValue);
				}
			}
			foundflag=true;
			returnValue=parentEle.getTextTrim();
			return returnValue;
		}
		if(!foundflag){
			@SuppressWarnings("unchecked")
			Iterator<Attribute> attr=parentEle.attributeIterator();
			while(attr.hasNext())
			{
				Attribute attrNext= (Attribute)attr.next();
				if(elementOrAttribute.equalsIgnoreCase(attrNext.getName())){
					attrNext.setValue(newValue);
					foundflag=true;
					returnValue=attrNext.getValue();
					break;
				}
			}
		}
		if(!foundflag){
			@SuppressWarnings("unchecked")
			Iterator<Element> it=parentEle.elementIterator();
			while(it.hasNext())
			{
				Element elementNext = (Element) it.next(); 
				returnValue=readSubElementAndUpdate(elementNext,elementOrAttribute, newValue);
				if(returnValue!=null) return returnValue;
			}
		}
		return returnValue;
	}
	
	public static void writeDocumentToXml(Document doc,String xmlFileStr) 
	{ 
		try
		{
			OutputFormat format=OutputFormat.createPrettyPrint();
			format.setEncoding("UTF-8");
			File fileHandler=new File(xmlFileStr);
			FileOutputStream fileOutputStream=new FileOutputStream(fileHandler);
			OutputStreamWriter outputStreamWriter=new OutputStreamWriter(fileOutputStream);
			XMLWriter writer=new XMLWriter(outputStreamWriter,format);
			writer.write(doc);
			writer.flush();
			writer.close();
			outputStreamWriter.close();
			fileOutputStream.close();
			
		}catch(Exception e)
		{
			logger.error(e.getMessage(),e);
		}
		
	} 
	

}
