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
public class Topic implements Command {
	public void invoke(Source src, String[] params) {
		String topicDest = params[0];
		Channel realDest = Util.findChannel(topicDest);
		if (realDest == null) {
			Message message = new Message(Constants.ERR_NOSUCHCHANNEL, src);
			message.appendParameter(topicDest);
			message.appendParameter("No such channel");
			src.send(message);
		} else {
			User user = (User) src;
			if (realDest.isOn(user)) {
				if (params.length > 1) {
					if (realDest.isOp(user) || !realDest.isModeSet(Channel.CHANMODE_TOPICOPS)) {
						realDest.setTopic(src,params[1]);
					} else {
						Message message = new Message(Constants.ERR_CHANOPRIVSNEEDED, src);
						message.appendParameter(topicDest);
						message.appendParameter("You're not channel operator");
						src.send(message);
					}
				} else {
					realDest.sendTopicInfo(user);
				}
			} else {
				Message message = new Message(Constants.ERR_NOTONCHANNEL, src);
				message.appendParameter(topicDest);
				message.appendParameter("You're not on that channel");
				src.send(message);
			}
		}
	}
	public String getName() {
		return "TOPIC";
	}
	public int getMinimumParameterCount() {
		return 1;
	}
}
