package it;

import org.junit.Test;

public class ApplicationIT extends EndpointHelper {

    @Test
    public void testDeployment() {
        testEndpoint("/index.html", "<h1>Welcome to your Liberty Application</h1>");
    }

}