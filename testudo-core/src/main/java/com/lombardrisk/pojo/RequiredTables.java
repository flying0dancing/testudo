package com.lombardrisk.pojo;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;

public class RequiredTables {
	private static final Logger logger = LoggerFactory.getLogger(RequiredTables.class);
	private List<String> singles;
	private List<String> dividedByReturnIds;
	private List<String> excludeReturnIds;
	private String notes;
	
	public List<String> getSingles() {
		return singles;
	}
	public void setSingles(List<String> singles) {
		this.singles = singles;
	}
	public List<String> getDividedByReturnIds() {
		return dividedByReturnIds;
	}
	public void setDividedByReturnIds(List<String> dividedByReturnIds) {
		this.dividedByReturnIds = dividedByReturnIds;
	}
	public String getNotes() {
		return notes;
	}
	public void setNotes(String notes) {
		this.notes = notes;
	}
	  
	@Override
	public String toString()
	{
		try{
			return new GsonBuilder().setPrettyPrinting().create().toJson(this);
		}catch(Exception e){
			logger.error(e.getMessage());
			return "";
		}
	}
	public List<String> getExcludeReturnIds() {
		return excludeReturnIds;
	}
	public void setExcludeReturnIds(List<String> excludeReturnIds) {
		this.excludeReturnIds = excludeReturnIds;
	}
}
