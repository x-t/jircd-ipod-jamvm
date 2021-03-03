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

package jircd.ircx.commands;

import jircd.irc.*;

/**
 * @author markhale
 */
public class Whisper implements Command {
	public void invoke(Source src, String[] params) {
		String channame = params[0];
		Channel chan = Util.findChannel(channame);
		if (chan == null) {
			Message message = new Message(Constants.ERR_NOSUCHCHANNEL, src);
			message.appendParameter(channame);
			message.appendParameter("No such channel");
			src.send(message);
		} else {
			if (chan.isOn((User)src)) {
				String nickList = params[1];
				String msg = params[2];
				int startPos = 0;
				int endPos = nickList.indexOf(',');
				while(endPos != -1) {
					whisper(src, chan, nickList.substring(startPos, endPos), msg);
					startPos = endPos+1;
					endPos = nickList.indexOf(',', startPos);
				}
				whisper(src, chan, nickList.substring(startPos), msg);
			} else {
				Message message = new Message(Constants.ERR_NOTONCHANNEL, src);
				message.appendParameter(channame);
				message.appendParameter("You're not on that channel");
				src.send(message);
			}
		}
	}
	private static void whisper(Source src, Channel chan, String nick, String msg) {
		User target = Util.findUser(nick);
		if (target == null) {
			Message message = new Message(Constants.ERR_NOSUCHNICK, src);
			message.appendParameter(nick);
			message.appendParameter("No such nick");
			src.send(message);
		} else {
			if (chan.isOn(target)) {
				Message message = new Message(src, "WHISPER");
				message.appendParameter(msg);
				target.send(message);
			} else {
				Message message = new Message(Constants.ERR_USERNOTINCHANNEL, src);
				message.appendParameter(nick);
				message.appendParameter(chan.getName());
				message.appendParameter("They aren't on that channel");
				src.send(message);
			}
		}
	}
	public String getName() {
		return "WHISPER";
	}
	public int getMinimumParameterCount() {
		return 3;
	}
}
