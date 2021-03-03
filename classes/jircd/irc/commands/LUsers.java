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
public class LUsers implements Command {
	private static int maxLocal = 0;
	private static int maxGlobal = 0;

	public void invoke(Source src, String[] params) {
		if (params == null || params.length == 0) {
			Server server = src.getServer();
			int curLoc = server.userCount() + server.userCountInvisible();
			if (curLoc > maxLocal) maxLocal = curLoc;
			int curGlob = Util.networkUserCount() + Util.networkUserCountInvisible();
			if (curGlob > maxGlobal) maxGlobal = curGlob;

			Message message = new Message(Constants.RPL_LUSERCLIENT, src);
			message.appendParameter("There are " + Util.networkUserCount() + " users and " + Util.networkUserCountInvisible() + " invisible on " + jIRCd.servers.size() + " servers");
			src.send(message);

			message = new Message(Constants.RPL_LUSEROP, src);
			message.appendParameter("0");
			message.appendParameter("operator(s) online");
			src.send(message);

			message = new Message(Constants.RPL_LUSERCHANNELS, src);
			message.appendParameter(Integer.toString(jIRCd.channels.size()));
			message.appendParameter("channels formed");
			src.send(message);

			message = new Message(Constants.RPL_LUSERME, src);
			message.appendParameter("I have " + server.userCount() + " clients and " + 0 + " servers.");
			src.send(message);

			message = new Message("265", src);
			message.appendParameter("Current local users: " + curLoc + " Max: " + maxLocal);
			src.send(message);

			message = new Message("266", src);
			message.appendParameter("Current global users: " + curGlob + " Max: " + maxGlobal);
			src.send(message);
		} else {
			// find correct server and ask
		}
	}
	public String getName() {
		return "LUSERS";
	}
	public int getMinimumParameterCount() {
		return 0;
	}
}
