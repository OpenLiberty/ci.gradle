/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/

package it.io.openliberty.sample.health;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

public class HealthUtilIT {

  private static String port;
  private static String contextRoot;
  private static String baseUrl;
  private final static String HEALTH_ENDPOINT = "health";
  public static final String INV_MAINTENANCE_FALSE = "io_openliberty_sample_system_inMaintenance\":false";
  public static final String INV_MAINTENANCE_TRUE = "io_openliberty_sample_system_inMaintenance\":true";

  static {
    port = System.getProperty("http.port");
    contextRoot = System.getProperty("app.context.root");
    baseUrl = "http://localhost:" + port + contextRoot;
  }

  public static JsonArray connectToHealthEnpoint(int expectedResponseCode) {
    String healthURL = baseUrl + HEALTH_ENDPOINT;
    Client client = ClientBuilder.newClient();
    Response response = client.target(healthURL).request().get();
    assertEquals(expectedResponseCode, response.getStatus(), "Response code is not matching " + healthURL);
    JsonArray servicesstatus = response.readEntity(JsonObject.class).getJsonArray("checks");
    response.close();
    client.close();
    return servicesstatus;
  }

  public static String getActualState(String service, JsonArray servicesstatus) {
    String state = "";
    for (Object obj : servicesstatus) {
      if (obj instanceof JsonObject) {
        if (service.equals(((JsonObject) obj).getString("name"))) {
          state = ((JsonObject) obj).getString("status");
        }
      }
    }
    return state;
  }

  public static void changeProperty(String oldValue, String newValue) {
    try {
      String fileName = System.getProperty("user.dir").split("src")[0] + "/target/classes/META-INF/CustomConfigSource.json";
      BufferedReader reader = new BufferedReader(new FileReader(new File(fileName)));
      String line = "";
      String oldContent = "", newContent = "";
      while ((line = reader.readLine()) != null) {
        oldContent += line + "\r\n";
      }
      reader.close();
      newContent = oldContent.replaceAll(oldValue, newValue);
      FileWriter writer = new FileWriter(fileName);
      writer.write(newContent);
      writer.close();
      Thread.sleep(3500);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void cleanUp() {
    changeProperty(INV_MAINTENANCE_TRUE, INV_MAINTENANCE_FALSE);
  }

}
