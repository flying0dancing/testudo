package com.lombardrisk;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.Properties;

@SuppressWarnings("unused")
@Mojo(name = "createPackage",
        defaultPhase = LifecyclePhase.COMPILE)
public class CreatePackageMojo extends TestudoMojo {

    /**
     * Parameter used at the suffix of the zip.
     */
    @Parameter(property = "createPackage.release", defaultValue = "SNAPSHOT")
    private String releaseParam;

    @Override
    void addProperties(final Properties properties) {
        super.addProperties(properties);
        properties.setProperty("release", releaseParam);
    }
}
