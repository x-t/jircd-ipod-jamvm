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
public class Nick implements Command {
	private final jIRCd jircd;

	public Nick(jIRCd jircd) {
		this.jircd = jircd;
	}
	public void invoke(Source src, String[] params) {
		if (src instanceof User) {
			User user = (User) src;
			String newNick = params[0];
			if (Util.isNickName(newNick)) {
				User newUser = Util.findUser(newNick);
				if (newUser != null) {
					Message message = new Message(Constants.ERR_NICKNAMEINUSE, src);
					message.appendParameter(newNick);
					message.appendParameter("Nickname is already in use.");
					src.send(message);
				} else {
					user.changeNick(newNick);
				}
			} else {
				Message message = new Message(Constants.ERR_ERRONEUSNICKNAME, src);
				message.appendParameter(newNick);
				message.appendParameter("Erroneous nickname");
				src.send(message);
			}
		} else if (src instanceof Server) {
			if (params.length == 7) {
				String nick = params[0];
				String hopcount = params[1];
				String ident = params[2];
				String host = params[3];
				String token = params[4];
				String modes = params[5];
				String desc = params[6];
				User usr = new User(nick, ident, host, desc, (Server)src);
				jircd.thisServer.addUser(usr);
			} else {
				// too few parameters
				Message message = new Message(Constants.ERR_NEEDMOREPARAMS, src);
				message.appendParameter(getName());
				message.appendParameter("Not enough parameters");
				src.send(message);
			}
		} else if (src instanceof Unknown) {
			String newNick = params[0];
			if (Util.isNickName(newNick)) {
				if (Util.findUser(newNick) == null) {
					Unknown unknown = (Unknown) src;
					unknown.setNick(newNick);
					String[] userParams = unknown.getParameters();
					if (userParams != null) {
						// re-issue USER command
						Message message = new Message(null, "USER");
						message.appendParameter(userParams[0]);
						message.appendParameter(userParams[1]);
						message.appendParameter(userParams[2]);
						message.appendParameter(userParams[3]);
						CommandManager.invokeCommand(src, message);
					}
				} else {
					Message message = new Message(Constants.ERR_NICKNAMEINUSE, src);
					message.appendParameter(newNick);
					message.appendParameter("Nickname is already in use.");
					src.send(message);
				}
			} else {
				Message message = new Message(Constants.ERR_ERRONEUSNICKNAME, src);
				message.appendParameter(newNick);
				message.appendParameter("Erroneous nickname");
				src.send(message);
			}
		}
	}
	public String getName() {
		return "NICK";
	}
	public int getMinimumParameterCount() {
		return 1;
	}
}
