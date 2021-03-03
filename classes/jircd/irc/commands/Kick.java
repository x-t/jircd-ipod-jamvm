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
public class Kick implements Command {
	public void invoke(Source src, String[] params) {
		String channame = params[0];
		String username = params[1];
		if (Util.isChannelIdentifier(channame)) {
			// check channel exists
			Channel chan = Util.findChannel(channame);
			if (chan == null) {
				Message message = new Message(Constants.ERR_NOSUCHCHANNEL, src);
				message.appendParameter(channame);
				message.appendParameter("No such channel");
				src.send(message);
			} else {
				if(Util.isNickName(username)) {
					// check nick exists
					User user = Util.findUser(username);
					if(user == null) {
						Message message = new Message(Constants.ERR_NOSUCHNICK, src);
						message.appendParameter(username);
						message.appendParameter("No such nick");
						src.send(message);
					} else {
						if(chan.isOp((User)src)) {
							Message message = new Message(src, "KICK", chan);
							message.appendParameter(user.getNick());
							if(params.length == 3)
								message.appendParameter(params[2]);
							chan.send(message);
							chan.removeUser(user);
						} else {
							Message message = new Message(Constants.ERR_CHANOPRIVSNEEDED, src);
							message.appendParameter(channame);
							message.appendParameter("You're not channel operator");
							src.send(message);
						}
					}
				}
			}
		} else {
			Message message = new Message(Constants.ERR_NOSUCHCHANNEL, src);
			message.appendParameter(channame);
			message.appendParameter("No such channel");
			src.send(message);
		}
	}
	public String getName() {
		return "KICK";
	}
	public int getMinimumParameterCount() {
		return 2;
	}
}
