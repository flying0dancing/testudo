package com.lombardrisk.pojo;

import com.google.gson.GsonBuilder;
import com.lombardrisk.status.BuildStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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
    public String toString() {
        try {
            return new GsonBuilder().setPrettyPrinting().create().toJson(this);
        } catch (Exception e) {
            BuildStatus.getInstance().recordError();
            logger.error("Unable to display details of RequiredTables", e);
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
