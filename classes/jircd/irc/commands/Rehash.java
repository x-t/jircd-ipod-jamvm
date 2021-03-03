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
public class Rehash implements Command {
	private final jIRCd jircd;

	public Rehash(jIRCd jircd) {
		this.jircd = jircd;
	}
	public void invoke(Source src, String[] params) {
		User user = (User) src;
		if(hasPermission(user)) {
			Message msg = new Message(Constants.RPL_REHASHING, src);
			msg.appendParameter(jircd.configFile);
			msg.appendParameter("Rehashing");
			src.send(msg);
			CommandManager.clearCommands();
			jircd.loadPlugins();
		} else {
			Message msg = new Message(Constants.ERR_NOPRIVILEGES, src);
			msg.appendParameter("Permission Denied- You're not an IRC operator");
			src.send(msg);
		}
	}
	private boolean hasPermission(User user) {
		return user.isModeSet(User.UMODE_OPER);
	}
	public String getName() {
		return "REHASH";
	}
	public int getMinimumParameterCount() {
		return 0;
	}
}
