package net.pistonmaster.pistonqueue.data;

import java.io.IOException;
import java.util.Properties;

public class BuildData {
  public static final String VERSION;
  public static final String DESCRIPTION;
  public static final String URL;

  static {
    Properties properties = new Properties();
    try (var inputStream = BuildData.class.getClassLoader().getResourceAsStream("pistonqueue-build-data.properties")) {
      if (inputStream == null) {
        throw new IllegalStateException("Build data properties file not found");
      }

      properties.load(inputStream);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load build data properties", e);
    }

    VERSION = properties.getProperty("version");
    DESCRIPTION = properties.getProperty("description");
    URL = properties.getProperty("url");
  }
}
