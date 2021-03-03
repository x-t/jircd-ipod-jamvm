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
public class Notice implements Command {
	public void invoke(Source src, String[] params) {
		String msgdest = params[0];
		String msg = params[1];
		if (msgdest.charAt(0) == '#') {
			// message to channel
			Channel chan = Util.findChannel(msgdest);
			if (chan == null) {
				Message message = new Message(Constants.ERR_NOSUCHCHANNEL, src);
				message.appendParameter(msgdest);
				message.appendParameter("No such channel");
				src.send(message);
			} else {
				Message message = new Message(src, "NOTICE", chan);
				message.appendParameter(msg);
				chan.send(message, (User)src);
			}
		} else if (msgdest.charAt(0) == '&') {
			// message to local channel
			Message message = new Message(Constants.ERR_NOSUCHCHANNEL, src);
			message.appendParameter(msgdest);
			message.appendParameter("No such channel");
			src.send(message);
		} else {
			// message to user
			User luser = Util.findUser(msgdest);
			if (luser == null) {
				Message message = new Message(Constants.ERR_NOSUCHNICK, src);
				message.appendParameter(msgdest);
				message.appendParameter("No such nick");
				src.send(message);
			} else {
				Message message = new Message(src, "NOTICE", luser);
				message.appendParameter(msg);
				luser.send(message);
			}
		}
	}
	public String getName() {
		return "NOTICE";
	}
	public int getMinimumParameterCount() {
		return 2;
	}
}
