package bq.util;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.flogger.FluentLogger;

public class Config {

  static final YAMLMapper yamlMapper = new YAMLMapper();
  static FluentLogger logger = FluentLogger.forEnclosingClass();

  public static Optional<String> get(String key) {

    if (S.isBlank(key)) {
      return Optional.empty();
    }

    String envVal = System.getenv(key);
    if (S.isNotBlank(envVal)) {
      return Optional.of(envVal);
    }

    Optional<String> val = tryGet(key, new File(System.getProperty("user.home"), ".bq/config.yml"));
   if (val.isPresent()) {
     return val;
   }

   
   return val;
  }

  static Optional<String> tryGet(String key, File f) {
    if (f == null || !f.exists()) {
      return Optional.empty();
    }

    try {

      JsonNode n = yamlMapper.readTree(f);

      String val = n.path(key).asText(null);
      if (val != null) {
        return Optional.of(val);
      }

      return Optional.empty();
    } catch (IOException e) {

      throw new BqException(e);
    }
  }
}
