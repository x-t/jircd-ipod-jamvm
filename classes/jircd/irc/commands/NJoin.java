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
public class NJoin implements Command {
	public void invoke(Source src, String[] params) {
		if (src instanceof Server) {
			String channame = params[0];
			if (Util.isChannelIdentifier(channame)) {
				Channel chan = Util.findChannel(channame);
				if (chan == null) {
					chan = new Channel(channame);
					jIRCd.addChannel(chan);
				}
				String nickList = params[1];
				int startPos = 0;
				int endPos = nickList.indexOf(',');
				while(endPos != -1) {
					addUser(chan, nickList.substring(startPos, endPos));
					startPos = endPos+1;
					endPos = nickList.indexOf(',', startPos);
				}
				addUser(chan, nickList.substring(startPos));
			} else {
				Message message = new Message(Constants.ERR_NOSUCHCHANNEL, src);
				message.appendParameter(channame);
				message.appendParameter("No such channel");
				src.send(message);
			}
		}
	}
	private static void addUser(Channel chan, String nick) {
		if(nick.startsWith("@@"))
			nick = nick.substring(2);
		else if(nick.charAt(0) == '@')
			nick = nick.substring(1);
		else if(nick.charAt(0) == '+')
			nick = nick.substring(1);
		chan.addUser(Util.findUser(nick));
	}
	public String getName() {
		return "NJOIN";
	}
	public int getMinimumParameterCount() {
		return 2;
	}
}
