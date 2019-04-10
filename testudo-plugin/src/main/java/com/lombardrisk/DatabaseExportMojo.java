package com.lombardrisk;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.Properties;

@SuppressWarnings("unused")
@Mojo(name = "databaseExport",
        defaultPhase = LifecyclePhase.COMPILE)
public class DatabaseExportMojo extends TestudoMojo {

    @Override
    void addProperties(final Properties properties) {
        super.addProperties(properties);
        properties.setProperty("proc", "1");
    }
}
