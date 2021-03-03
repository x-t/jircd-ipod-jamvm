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

import java.util.Iterator;

import jircd.jIRCd;
import jircd.irc.*;

/**
 * @author markhale
 */
public class Wallops implements Command {
	private final jIRCd jircd;

	public Wallops(jIRCd jircd) {
		this.jircd = jircd;
	}
	public void invoke(Source src, String[] params) {
		if(hasPermission(src)) {
			String msg = params[0];
			for(Iterator iter = jircd.thisServer.getUsers().iterator(); iter.hasNext(); ) {
				User user = (User) iter.next();
				if(user.isModeSet(User.UMODE_WALLOPS)) {
					Message message = new Message(src, "WALLOPS");
					message.appendParameter(msg);
					user.send(message);
				}
			}
		}
	}
	private boolean hasPermission(Source src) {
		if(src instanceof Server) {
			return true;
		} else if(src instanceof User) {
			User user = (User) src;
			return user.isModeSet(User.UMODE_OPER);
		} else {
			return false;
		}
	}
	public String getName() {
		return "WALLOPS";
	}
	public int getMinimumParameterCount() {
		return 1;
	}
}
