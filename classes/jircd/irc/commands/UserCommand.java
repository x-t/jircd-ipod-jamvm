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
public class UserCommand implements Command {
	private final jIRCd jircd;

	public UserCommand(jIRCd jircd) {
		this.jircd = jircd;
	}
	public void invoke(Source src, String[] params) {
		String nick = src.getNick();
		if(nick == null) {
			((Unknown)src).setParameters(params);
		} else {
			String username = params[0];
			String hostname = params[1];
			String servername = params[2];
			String desc = params[3];
			Server thisServer = src.getServer();
			Client client = (Client) src.getConnection();
			User user = new User(nick, username, client.getHost(), desc, thisServer, client);
			if(client.login(user)) {
				thisServer.addUser(user);

				Message message = new Message(Constants.RPL_WELCOME, src);
				message.appendParameter("Welcome to the " + jircd.getProperty("jircd.networkName") + " " + nick + "!" + username + "@" + client.getHost());
				src.send(message);

				message = new Message(Constants.RPL_YOURHOST, src);
				message.appendParameter("Your host is " + thisServer.getHost() + ", running version " + jIRCd.versionInfo());
				src.send(message);

				message = new Message(Constants.RPL_CREATED, src);
				message.appendParameter("This server was created ???");
				src.send(message);

				message = new Message(Constants.RPL_MYINFO, src);
				message.appendParameter(thisServer.getHost() + " " + jIRCd.versionInfo() + " - -");
				src.send(message);

				message = new Message(Constants.RPL_ISUPPORT, src);
				message.appendParameter("PREFIX=(ov)@+");
				message.appendParameter("CHANTYPES=#");
				message.appendParameter("NETWORK=" + jircd.getProperty("jircd.networkName"));
				message.appendParameter("are supported by this server");
				src.send(message);

				CommandManager.invokeCommand(src, new Message(thisServer, "LUSERS"));
				CommandManager.invokeCommand(src, new Message(thisServer, "MOTD"));
			}
		}
	}
	public String getName() {
		return "USER";
	}
	public int getMinimumParameterCount() {
		return 4;
	}
}
