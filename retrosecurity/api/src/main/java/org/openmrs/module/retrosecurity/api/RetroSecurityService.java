/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.retrosecurity.api;

import org.openmrs.api.OpenmrsService;
import org.springframework.transaction.annotation.Transactional;

import org.openmrs.User;

import java.io.*;
import java.util.*;


/**
 * This service exposes module's core functionality. It is a Spring managed bean which is configured in moduleApplicationContext.xml.
 * <p>
 * It can be accessed only via Context:<br>
 * <code>
 * Context.getService(RetroSecurityService.class).someMethod();
 * </code>
 * 
 * @see org.openmrs.api.context.Context
 */
@Transactional
public interface RetroSecurityService extends OpenmrsService {
     
	public void setBroken(boolean broken);
	public void setUser(User user);
	public boolean getBroken();
	public User getUser();
	public void breakTheGlass();
	public void buildTheGlass(User user);
	public String queryLog() throws IOException;
	public String readDerivedLog() throws IOException;

}
