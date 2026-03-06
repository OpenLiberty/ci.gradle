package io.openliberty.sample.system;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class SystemHealth implements HealthCheck {
	
	@Inject
	SystemConfig systemConfig;
	
	public boolean isHealthy() {
	    if (systemConfig.isInMaintenance()) {
	      return false;
	    }
	     return true;
	  }
	
  @Override
  public HealthCheckResponse call() {
    if (!isHealthy()) {
      return HealthCheckResponse.named(SystemResource.class.getSimpleName())
    		  .withData("services","not available").down().build();
    }
    return HealthCheckResponse.named(SystemResource.class.getSimpleName())
            .withData("services","available").up().build();
  }
}
