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
package org.openmrs.module.retrosecurity.api.impl;

import org.openmrs.api.impl.BaseOpenmrsService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.retrosecurity.api.RetroSecurityService;
import org.openmrs.module.retrosecurity.api.db.RetroSecurityDAO;

import org.openmrs.User;

import java.io.*;
import java.util.*;

/**
 * It is a default implementation of {@link RetroSecurityService}.
 */
public class RetroSecurityServiceImpl extends BaseOpenmrsService implements RetroSecurityService {
	
	protected final Log log = LogFactory.getLog(this.getClass());
	
	private RetroSecurityDAO dao;

	// the variable to show if glass is broken
	private boolean broken = false;
	private User user;

	static String modulePath = "/home/sep/Desktop/test_rep/retrosecurity";

	static String moduleSourcePath = modulePath + "/api/src/main/java/org/openmrs/module/retrosecurity";

	static String queryResultPath = moduleSourcePath + "/storage/qr.txt";
	static String derivationStoragePath = moduleSourcePath + "/storage/ds.txt";


	//private EngineCommunication ec;

	
	//method impls	
	public void setBroken(boolean broken) {
		this.broken = broken;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public boolean getBroken() {
		return this.broken;
	}

	public User getUser() {
		return this.user;
	}

	// break the glass method
	public void breakTheGlass() {
		if (!this.getBroken()) {
			this.setBroken(true);
		}
	}

	public void buildTheGlass(User user) {
		if (this.getBroken()) {
			this.setBroken(false);
		}
	}

	public String queryLog() throws IOException{
		return readFile(queryResultPath);
	}

	public String readDerivedLog() throws IOException{
		return readFile(derivationStoragePath);
	}

	private String readFile(String fileName) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		try {
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				sb.append("\n");
				line = br.readLine();
			}
			return sb.toString();
		} finally {
        		br.close();
    		}
	}
	
	/**
     * @param dao the dao to set
     */
    public void setDao(RetroSecurityDAO dao) {
	    this.dao = dao;
    }
    
    /**
     * @return the dao
     */
    public RetroSecurityDAO getDao() {
	    return dao;
    }
}

