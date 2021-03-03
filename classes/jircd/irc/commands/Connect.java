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

import java.io.IOException;

import jircd.jIRCd;
import jircd.irc.*;

/**
 * @author markhale
 */
public class Connect implements Command {
	private final jIRCd jircd;

	public Connect(jIRCd jircd) {
		this.jircd = jircd;
	}
	public void invoke(Source src, String[] params) {
		final String host = params[0];
		final int port = params.length>1 ? Integer.parseInt(params[1]) : Constants.DEFAULT_PORT;
		User user = (User) src;
		if(hasPermission(user)) {
			try {
				Link link = new Link(jircd, host, port);
				jircd.addLink(link);
			} catch(IOException e) {
				Message message = new Message(Constants.ERR_NOSUCHSERVER, src);
				message.appendParameter(host);
				message.appendParameter("No such server");
				src.send(message);
			}
		} else {
			Message message = new Message(Constants.ERR_NOPRIVILEGES, src);
			message.appendParameter("Permission Denied- You're not an IRC operator");
			src.send(message);
		}
	}
	private boolean hasPermission(User user) {
		return user.isModeSet(User.UMODE_OPER);
	}
	public String getName() {
		return "CONNECT";
	}
	public int getMinimumParameterCount() {
		return 1;
	}
}
