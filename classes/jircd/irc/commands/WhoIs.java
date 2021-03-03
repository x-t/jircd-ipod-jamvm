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

import jircd.irc.*;

/**
 * @author markhale
 */
public class WhoIs implements Command {
	public void invoke(Source src, String[] params) {
		if (params.length == 1) {
			String nick = params[0];
			User who = Util.findUser(nick);
			if (who == null) {
				Message message = new Message(Constants.ERR_NOSUCHNICK, src);
				message.appendParameter(nick);
				message.appendParameter("No such nickname");
				src.send(message);
			} else {
				String showhost;
				if (who.equals(src) || ((User)src).isModeSet(User.UMODE_OPER))
					showhost = who.getRealHost();
				else
					showhost = who.getHost();

				Message message = new Message(Constants.RPL_WHOISUSER, src);
				message.appendParameter(who.getNick());
				message.appendParameter(who.getIdent());
				message.appendParameter(who.getHost());
				message.appendParameter("*");
				message.appendParameter(who.getDescription());
				src.send(message);

				StringBuffer chanlist = new StringBuffer();
				for(Iterator iter = who.getChannels().iterator(); iter.hasNext();) {
					Channel chan = (Channel) iter.next();
					if (chan.isModeSet(Channel.CHANMODE_SECRET) || chan.isOn((User)src)) {
						chanlist.append(' ');
						if (chan.isOp(who)) chanlist.append("@");
						if (chan.isVoice(who)) chanlist.append("+");
						chanlist.append(chan.getName());
					}
				}
				if (chanlist.length() > 0) {
					message = new Message(Constants.RPL_WHOISCHANNELS, src);
					message.appendParameter(who.getNick());
					message.appendParameter(chanlist.toString().substring(1));
					src.send(message);
				}

				message = new Message(Constants.RPL_WHOISSERVER, src);
				message.appendParameter(who.getNick());
				message.appendParameter(who.getServer().getNick());
				message.appendParameter(who.getServer().getDescription());
				src.send(message);

				if (who.isModeSet(User.UMODE_OPER)) {
					message = new Message(Constants.RPL_WHOISOPERATOR, src);
					message.appendParameter(who.getNick());
					message.appendParameter("is an IRC Operator");
					src.send(message);
				}

				message = new Message(Constants.RPL_WHOISIDLE, src);
				message.appendParameter(who.getNick());
				message.appendParameter(Long.toString(((Client)who.getConnection()).idleMillis() / 1000));
				message.appendParameter(Long.toString(System.currentTimeMillis() / 1000));
				message.appendParameter("seconds idle, signon time");
				src.send(message);
			}
			Message message = new Message(Constants.RPL_ENDOFWHOIS, src);
			message.appendParameter(nick);
			message.appendParameter("End of /WHOIS list");
			src.send(message);
		} else {
			// find correct server and ask
		}
	}
	public String getName() {
		return "WHOIS";
	}
	public int getMinimumParameterCount() {
		return 1;
	}
}
