package it.io.openliberty.sample.health;

import java.util.HashMap;
import jakarta.json.JsonArray;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class HealthIT {

  private JsonArray servicesstatus;
  private static HashMap<String, String> dataWhenServicesUP, dataWhenServicesDown;

  static {
    dataWhenServicesUP = new HashMap<String, String>();
    dataWhenServicesDown = new HashMap<String, String>();
    dataWhenServicesUP.put("SystemResource", "UP");
    dataWhenServicesDown.put("SystemResource", "DOWN");
  }

  @Test
  public void testIfServicesAreUp() {
    servicesstatus = HealthUtilIT.connectToHealthEnpoint(200);
    checkServicesstatus(dataWhenServicesUP, servicesstatus);
  }

  @Test
  public void testIfServicesAreDown() {
    servicesstatus = HealthUtilIT.connectToHealthEnpoint(200);
    checkServicesstatus(dataWhenServicesUP, servicesstatus);
    HealthUtilIT.changeProperty(HealthUtilIT.INV_MAINTENANCE_FALSE, HealthUtilIT.INV_MAINTENANCE_TRUE);
    servicesstatus = HealthUtilIT.connectToHealthEnpoint(503);
    checkServicesstatus(dataWhenServicesDown, servicesstatus);
  }

  private void checkServicesstatus(HashMap<String, String> testData, JsonArray servicesstatus) {
    testData.forEach((service, expectedState) -> {
      assertEquals(expectedState, HealthUtilIT.getActualState(service, servicesstatus),
          "The state of " + service + " service is not matching the ");
    });

  }

  @AfterEach
  public void teardown() {
    HealthUtilIT.cleanUp();
  }

}
