package com.lombardrisk.pojo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;

public class DBAndTables {
	private static final Logger logger = LoggerFactory.getLogger(DBAndTables.class);
	private String ID;
    private DatabaseServer databaseServer;
    private RequiredTables requiredTables;
    private String notes;
    public String getID() {
		return ID;
	}
	public void setID(String iD) {
		ID = iD;
	}
    public DatabaseServer getDatabaseServer() {
		return databaseServer;
	}
	public void setDatabaseServer(DatabaseServer databaseServer) {
		this.databaseServer = databaseServer;
	}
	public RequiredTables getRequiredTables() {
		return requiredTables;
	}
	public void setRequiredTables(RequiredTables requiredTables) {
		this.requiredTables = requiredTables;
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
}
