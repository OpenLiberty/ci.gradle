package io.openliberty.sample.system;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SystemConfig {

  @Inject
  @ConfigProperty(name = "io_openliberty_sample_system_inMaintenance")
  Provider<Boolean> inMaintenance;


  public boolean isInMaintenance() {
    return inMaintenance.get();
  }
}
