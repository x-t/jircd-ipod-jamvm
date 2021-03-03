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
public class ServerCommand implements Command {
	public void invoke(Source src, String[] params) {
		String hostname = params[0];
		String hopcount = params[1];
		String token = params[2];
		String desc = params[3];
		Server thisServer = src.getServer();
		Client client = (Client) src.getConnection();
		Server server = new Server(hostname, desc, thisServer, client);
		if(client.login(server)) {
			jIRCd.addServer(server);

			Message message = new Message(null, "PASS");
			message.appendParameter("clientPassword");
			message.appendParameter("0210");
			message.appendParameter("IRC|");
			server.send(message);

			message = new Message(null, "SERVER");
			message.appendParameter(thisServer.getHost());
			message.appendParameter("0");
			message.appendParameter(Integer.toString(server.getToken()));
			message.appendParameter(thisServer.getDescription());
			server.send(message);

			for(Iterator iter = thisServer.getUsers().iterator(); iter.hasNext(); ) {
				User usr = (User) iter.next();
				message = new Message(null, "NICK");
				message.appendParameter(usr.getNick());
				message.appendParameter("0");
				message.appendParameter(usr.getIdent());
				message.appendParameter(usr.getRealHost());
				message.appendParameter(Integer.toString(thisServer.getToken()));
				message.appendParameter(usr.getModesList());
				message.appendParameter(usr.getDescription());
				server.send(message);
			}
			for(Iterator iter = jIRCd.channels.values().iterator(); iter.hasNext(); ) {
				Channel chan = (Channel) iter.next();
				message = new Message(server.getServer(), "NJOIN");
				message.appendParameter(chan.getName());
				message.appendParameter(chan.getNamesList());
				server.send(message);
			}
		}
	}
	public String getName() {
		return "SERVER";
	}
	public int getMinimumParameterCount() {
		return 4;
	}
}
