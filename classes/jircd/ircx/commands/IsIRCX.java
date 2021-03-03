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
public class IsIRCX implements Command {
	public void invoke(Source src, String[] params) {
		Message msg = new Message(jircd.ircx.Constants.IRCRPL_IRCX, src);
		msg.appendParameter("0");
		msg.appendParameter("0");
		msg.appendParameter("ANON");
		msg.appendParameter(Integer.toString(Constants.MAX_MESSAGE_LENGTH));
		msg.appendParameter("*");
		src.send(msg);
	}
	public String getName() {
		return "ISIRCX";
	}
	public int getMinimumParameterCount() {
		return 0;
	}
}
