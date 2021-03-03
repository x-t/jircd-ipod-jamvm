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
import java.text.DecimalFormat;

import jircd.jIRCd;
import jircd.irc.*;

/**
 * @author markhale
 */
public class Stats implements Command {
	private static final DecimalFormat TWO_PLACES = new DecimalFormat("00");
	private final jIRCd jircd;

	public Stats(jIRCd jircd) {
		this.jircd = jircd;
	}
	public void invoke(Source src, String[] params) {
		String query = params[0];
		switch(query.charAt(0)) {
			case 'o':
				for(Iterator iter = Operator.operators.iterator(); iter.hasNext();) {
					Operator oper = (Operator) iter.next();
					Message message = new Message(Constants.RPL_STATSOLINE, src);
					message.appendParameter("O");
					message.appendParameter(oper.getHost());
					message.appendParameter("*");
					message.appendParameter(oper.getName());
					src.send(message);
				}
				break;
			case 'u':
				int uptimeSecs = (int) jircd.uptimeMillis()/1000;
				int days = uptimeSecs/(24*60*60);
				int hours = uptimeSecs/(60*60) - 24*days;
				int mins = uptimeSecs/60 - 60*(hours + 24*days);
				int secs = uptimeSecs - 60*(mins + 60*(hours + 24*days));
				Message message = new Message(Constants.RPL_STATSUPTIME, src);
				message.appendParameter("Server Up "+days+" days "+hours+':'+TWO_PLACES.format(mins)+':'+TWO_PLACES.format(secs));
				src.send(message);
				break;
		}
		Message message = new Message(Constants.RPL_ENDOFSTATS, src);
		message.appendParameter(query);
		message.appendParameter("End of /STATS report");
		src.send(message);
	}
	public String getName() {
		return "STATS";
	}
	public int getMinimumParameterCount() {
		return 1;
	}
}
