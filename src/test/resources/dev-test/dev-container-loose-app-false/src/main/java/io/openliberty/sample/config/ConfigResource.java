package io.openliberty.sample.config;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.core.MediaType;

import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.Json;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;

@RequestScoped
@Path("/config")
public class ConfigResource {

  @Inject
  private Config config;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public JsonObject getAllConfig() {
    JsonObjectBuilder builder = Json.createObjectBuilder();
    return builder.add("ConfigSources", sourceJsonBuilder())
                  .add("ConfigProperties", propertyJsonBuilder()).build();
  }

  public JsonObject sourceJsonBuilder() {
    JsonObjectBuilder sourcesBuilder = Json.createObjectBuilder();
    for (ConfigSource source : config.getConfigSources()) {
      sourcesBuilder.add(source.getName(), source.getOrdinal());
    }
    return sourcesBuilder.build();
  }

  public JsonObject propertyJsonBuilder() {
    JsonObjectBuilder propertiesBuilder = Json.createObjectBuilder();
    for (String name : config.getPropertyNames()) {
      if (name.contains("io_openliberty_sample")) {
        propertiesBuilder.add(name, config.getValue(name, String.class));
      }
    }
    return propertiesBuilder.build();
  }

}
