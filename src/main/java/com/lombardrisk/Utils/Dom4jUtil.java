package com.lombardrisk.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;





import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dom4jUtil {

	private final static Logger logger = LoggerFactory.getLogger(Dom4jUtil.class);
	
	
	
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
	
	
	//not tested
	private static String readSubElementAndUpdate(Element parentEle,String elementOrAttribute, String newValue)
	{
		boolean foundflag=false;
		String returnValue=null;
		if(elementOrAttribute.equalsIgnoreCase(parentEle.getName())){
			if(elementOrAttribute.equalsIgnoreCase("implementationVersion"))
			{
				String value=parentEle.getTextTrim();
				value=value.replaceAll("((?:\\d+\\.){2}\\d+).*", "$1."+newValue);
				parentEle.setText(value);
			}else{
				parentEle.setText(newValue);
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
