/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/

package io.openliberty.sample.system;

import java.lang.management.ManagementFactory;

import jakarta.enterprise.context.RequestScoped;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import javax.management.ObjectName;
import javax.management.MBeanInfo;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServer;

@RequestScoped
@Path("/runtime")
public class SystemRuntime {
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public Response getRuntime() {
		String libertyVersion = getServerVersion();
		return Response.ok(libertyVersion).build();
	}

	String getServerVersion() {
        String version = null;
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName objName = new ObjectName("WebSphere:feature=kernel,name=ServerInfo");
			MBeanInfo beanInfo = mbs.getMBeanInfo(objName);

			for (MBeanAttributeInfo attr : beanInfo.getAttributes()) {
				if (attr.getName().equals("LibertyVersion")) {
					version = String.valueOf(mbs.getAttribute(objName, attr.getName()));
					break;
				}
			}
        } catch (Exception ex) {
            System.out.println("Unable to retrieve server version.");
        }
        return version;
    }
}
