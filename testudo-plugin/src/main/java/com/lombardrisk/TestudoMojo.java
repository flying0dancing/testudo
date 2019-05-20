package com.lombardrisk;

import com.lombardrisk.status.BuildStatus;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.Properties;

public class TestudoMojo extends AbstractMojo {

    @SuppressWarnings("unused")
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @SuppressWarnings("unused")
    @Parameter
    private Boolean failBuild = true;

    public void execute() throws MojoFailureException {
        Properties properties = System.getProperties();
        addProperties(properties);
        System.setProperties(properties);
        Testudo.main(new String[]{});
        if (failBuild && BuildStatus.getInstance().hasErrors()) {
            throw new MojoFailureException("Build failed");
        }
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

    MavenProject getProject() {
        return project;
    }
}
