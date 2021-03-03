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

package jircd.irc.commands;

import jircd.jIRCd;
import jircd.irc.*;

/**
 * @author markhale
 */
public class Quit implements Command {
	private final jIRCd jircd;

	public Quit(jIRCd jircd) {
		this.jircd = jircd;
	}
	public void invoke(Source src, String[] params) {
		String qm = "Quit";
		if (params != null && params.length > 0) {
			qm = "Quit: " + params[0];
		}
		Client client = (Client) src.getConnection();
		jircd.removeClient(client, qm);
		client.close();
	}
	public String getName() {
		return "QUIT";
	}
	public int getMinimumParameterCount() {
		return 0;
	}
}
