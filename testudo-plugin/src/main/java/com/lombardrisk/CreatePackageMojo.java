package com.lombardrisk;

import com.lombardrisk.ocelot.config.ConfigPackageSIG;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

@SuppressWarnings("unused")
@Mojo(name = "createPackage", defaultPhase = LifecyclePhase.COMPILE)
public class CreatePackageMojo extends TestudoMojo {

    private static final Logger logger = LoggerFactory.getLogger(ConfigPackageSIG.class);

    /**
     * Parameter used at the suffix of the zip.
     */
    @Parameter(property = "createPackage.release", defaultValue = "SNAPSHOT")
    private String releaseParam;

    @Override
    public void execute() {
        super.execute();
        ConfigPackageSIG.main(new String[]{file()});
    }

    private String file(){
        String targetFolder = getProject().getBasedir() + "\\target";
        logger.debug("Target folder {}",targetFolder);
        try (Stream<Path> stream = Files.find(
                Paths.get(targetFolder), 1,
                (path, attr) ->
                        path.toString().endsWith(releaseParam+".zip"))) {
            Optional<Path> first = stream.findFirst();
            if (first.isPresent()){
                String zipFileName = first.get().getFileName().toString();
                logger.info("Found zip file to sign {}",zipFileName);
                return targetFolder + "\\"+zipFileName;
            } else{
                logger.info("No zip files found in target folder {}",targetFolder);
            }
        } catch (IOException e) {
            logger.error("Error finding zip file", e);
        }
        throw new IllegalStateException("Unable to find zip");
    }

    @Override
    void addProperties(final Properties properties) {
        super.addProperties(properties);
        properties.setProperty("release", releaseParam);
    }
}
