/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/

package io.openliberty.sample.system;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@RequestScoped
@Path("/properties")
public class SystemResource {

	@Inject
	SystemConfig systemConfig;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Timed(name = "getPropertiesTime", description = "Time needed to get the properties of a system")
	@Counted(absolute = true, description = "Number of times the properties of a systems is requested")
	public Response getProperties() {
		if (!systemConfig.isInMaintenance()) {
			return Response.ok(System.getProperties()).build();
			//return Response.ok("{\"test\":\"abc\"}").build();
		} else {
			return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("ERROR: Service is currently in maintenance.")
					.build();
		}
	}
}
