package com.lombardrisk.pojo;

import com.google.gson.GsonBuilder;
import com.lombardrisk.status.BuildStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBAndTables {

    private static final Logger logger = LoggerFactory.getLogger(DBAndTables.class);
    @SuppressWarnings("squid:S00116")
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
    public String toString() {
        try {
            return new GsonBuilder().setPrettyPrinting().create().toJson(this);
        } catch (Exception e) {
            BuildStatus.getInstance().recordError();
            logger.error("Unable to display details of DBAndTables", e);
            return "";
        }
    }
}
