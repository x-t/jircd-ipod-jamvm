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
public class Invite implements Command {
	public void invoke(Source src, String[] params) {
		String nickname = params[0];
		String channame = params[1];
		User luser = Util.findUser(nickname);
		if(luser == null) {
			Message message = new Message(Constants.ERR_NOSUCHNICK, src);
			message.appendParameter(nickname);
			message.appendParameter("No such nick");
			src.send(message);
		} else {
			String awayMsg = luser.getAwayMessage();
			if(awayMsg != null) {
				Message message = new Message(luser, Constants.RPL_AWAY, src);
				message.appendParameter(awayMsg);
				src.send(message);
			} else {
				Channel chan = Util.findChannel(channame);
				if(chan == null) {
					Message message = new Message(luser, Constants.RPL_INVITING, src);
					message.appendParameter(channame);
					message.appendParameter(nickname);
					luser.send(message);
				} else {
					if(chan.isOn(luser)) {
						Message message = new Message(Constants.ERR_USERONCHANNEL, src);
						message.appendParameter(nickname);
						message.appendParameter(channame);
						message.appendParameter("is already on channel");
						src.send(message);
					} else if(!chan.isOn((User)src)) {
						Message message = new Message(Constants.ERR_NOTONCHANNEL, src);
						message.appendParameter(channame);
						message.appendParameter("You're not on that channel");
						src.send(message);
					} else {
						if(chan.isModeSet(Channel.CHANMODE_INVITEONLY) && !chan.isOp((User)src)) {
							Message message = new Message(Constants.ERR_CHANOPRIVSNEEDED, src);
							message.appendParameter(channame);
							message.appendParameter("You're not channel operator");
							src.send(message);
						} else {
							Message message = new Message(src, Constants.RPL_INVITING, luser);
							message.appendParameter(channame);
							message.appendParameter(nickname);
							luser.send(message);
						}
					}
				}
			}
		}
	}
	public String getName() {
		return "INVITE";
	}
	public int getMinimumParameterCount() {
		return 2;
	}
}
