package com.lombardrisk.status;

public final class BuildStatus {

    private static final BuildStatus instance = new BuildStatus();

    public static BuildStatus getInstance() {
        return instance;
    }

    private BuildStatus() {
    }

    private boolean errors;

    public boolean hasErrors() {
        return errors;
    }

    public void recordError() {
        this.errors = true;
    }
}
