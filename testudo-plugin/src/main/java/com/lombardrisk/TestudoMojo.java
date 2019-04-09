package com.lombardrisk;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.Properties;

public class TestudoMojo extends AbstractMojo {

    @SuppressWarnings("unused")
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    public void execute() {
        Properties properties = System.getProperties();
        addProperties(properties);
        System.setProperties(properties);
        Testudo.main(new String[]{});
    }

    void addProperties(Properties properties) {
        properties.setProperty("runMaven", "true");
        properties.setProperty("project", project.getArtifactId());
        properties.setProperty("project.dir", getBaseDirectory());
        properties.setProperty("log.dir", getBaseDirectory() + "/target/logs");
        properties.setProperty("conf", getBaseDirectory() + "/testudo.json");
    }

    private String getBaseDirectory() {
        return project.getBasedir().toString();
    }
}
