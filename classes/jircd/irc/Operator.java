/*
 * jIRCd - Java Internet Relay Chat Daemon
 * Copyright 2003 Tyrel L. Haveman <tyrel@haveman.net>
 *
 * This file is part of jIRCd.
 *
 * jIRCd is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * jIRCd is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along
 * with jIRCd; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package jircd.irc;

import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Doesn't really describe the operator, just the line in the config file.
 * @author thaveman
 * @author markhale
 */
public final class Operator {
	/** set of Operator */
	public static final Set operators = Collections.synchronizedSet(new HashSet());

	private final byte[] pass;
	private final String name;
	private final String host;

	private static byte[] encrypt(String pass) {
		try {
			return MessageDigest.getInstance("SHA").digest(pass.getBytes());
		} catch(NoSuchAlgorithmException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	public static void add(Operator i) {
		operators.add(i);
	}

	public Operator(String n, String h, String p) {
		name = n;
		host = h;
		pass = encrypt(p);
	}
	public String getName() {
		return name;
	}
	public String getHost() {
		return host;
	}

	public boolean isGood(String nick, String password, String userhost) {
		if (nick.equalsIgnoreCase(name) && isGood(password) && Util.match(host,userhost))
			return true;
		else
			return false;
	}
	private boolean isGood(String password) {
		byte[] hash = encrypt(password);
		if(hash.length != pass.length)
			return false;
		for(int i=0; i<hash.length; i++) {
			if(hash[i] != pass[i])
				return false;
		}
		return true;
	}
}
