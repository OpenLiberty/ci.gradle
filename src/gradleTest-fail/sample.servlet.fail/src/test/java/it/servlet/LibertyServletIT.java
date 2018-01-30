package it.servlet;

import it.EndpointHelper;

import org.junit.Test;

public class LibertyServletIT extends EndpointHelper {

    @Test
    public void testDeployment() {
        testEndpoint("/servlet", "Hello, from a Servlet!");
    }
}