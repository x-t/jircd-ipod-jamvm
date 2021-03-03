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

import java.util.Map;
import java.util.HashMap;

//import org.apache.log4j.Logger;

/**
 * @author thaveman
 * @author markhale
 */
public class CommandManager {
//	private static final Logger //logger = Logger.getLogger(CommandManager.class);

	/** (String name, Command cmd) */
	private static final Map commands = new HashMap();

	public static void addCommand(Command cmd) {
		commands.put(cmd.getName().toUpperCase(), cmd);
	}
	public static void removeCommand(String name) {
		commands.remove(name.toUpperCase());
	}
	public static void clearCommands() {
		commands.clear();
	}

	public static void invokeCommand(Source src, final Message message) {
		// determine sender
		if (src instanceof Server) { // received from a SERVER
			String fromName = message.getSender();
			if (fromName != null) {
				final int pos = fromName.indexOf('!');
				if (pos < 0) { // can't find *!*, so must be server
					src = Util.findServer(fromName);
				} else { // found a *!* in string, must be a user
					String usrName = fromName.substring(0, pos);
					src = Util.findUser(usrName);
				}
			}
		}

		// find command
		final String cmdName = message.getCommand();
		final Command command = (Command) commands.get(cmdName.toUpperCase());
		if (command == null) {
			// Unknown command
			Message errMessage = new Message(Constants.ERR_UNKNOWNCOMMAND, src);
			errMessage.appendParameter(cmdName);
			errMessage.appendParameter("Unknown command");
			src.send(errMessage);
			//logger.debug("Unknown command: " + message.toString());
			return;
		}

		String[] params = new String[message.getParameterCount()];
		for(int i=0; i<params.length; i++)
			params[i] = message.getParameter(i);

		if (params.length < command.getMinimumParameterCount()) {
			// too few parameters
			Message errMessage = new Message(Constants.ERR_NEEDMOREPARAMS, src);
			errMessage.appendParameter(cmdName);
			errMessage.appendParameter("Not enough parameters");
			src.send(errMessage);
			return;
		}
		// HERE WE GO!!!!!!!!!
		try {
			command.invoke(src, params);
		} catch (RuntimeException e) {
			//logger.warn("Error invoking method in " + command.getClass() + " for command " + cmdName, e);
		}
	}
}
