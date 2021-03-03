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

import jircd.irc.*;

/**
 * @author markhale
 */
public class Away implements Command {
	public void invoke(Source src, String[] params) {
		User user = (User) src;
		if(params.length > 0) {
			user.setAwayMessage(params[0]);
			Message message = new Message(Constants.RPL_NOWAWAY, src);
			message.appendParameter("You have been marked as being away");
			src.send(message);
		} else {
			user.setAwayMessage(null);
			Message message = new Message(Constants.RPL_UNAWAY, src);
			message.appendParameter("You are no longer marked as being away");
			src.send(message);
		}
	}
	public String getName() {
		return "AWAY";
	}
	public int getMinimumParameterCount() {
		return 0;
	}
}
