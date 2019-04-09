package com.lombardrisk.pojo;

import java.lang.reflect.Field;

import org.apache.commons.lang.StringUtils;



public class TableProps {
	private String name;
	private String typeSize;
	private String nullable;
	private int order;
	public TableProps(String name,String typesize,String nullable,String order){
		int or=Integer.parseInt(order.replace("col", ""));
		this.setName(name);
		this.setTypeSize(typesize);
		this.setNullable(nullable);
		this.setOrder(or);
	}
	public TableProps(String name,String typesize,String nullable,int order){
		this.setName(name);
		this.setTypeSize(typesize);
		this.setNullable(nullable);
		this.setOrder(order);
	}
	@Override
	public String toString()
	{
		StringBuffer stringBuffer=new StringBuffer();
		Field[] fields=getClass().getDeclaredFields();
		for(Field field:fields)
		{
			try {
				String value=null;
				Object obj=field.get(this);
				if(obj==null || StringUtils.isBlank(obj.toString()))
				{continue;}
				else value=field.get(this).toString();
				stringBuffer.append(field.getName()+"[" + value+"] ");
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return stringBuffer.toString();
	}

	public String getTypeSize() {
		return typeSize;
	}

	public void setTypeSize(String typeSize) {
		this.typeSize = typeSize;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	

	public String getNullable() {
		return nullable;
	}

	public void setNullable(String nullable) {
		this.nullable = nullable;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	
}
