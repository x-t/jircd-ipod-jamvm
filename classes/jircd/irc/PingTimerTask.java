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

package jircd.irc;

import java.util.Iterator;
import java.util.TimerTask;

import jircd.jIRCd;

/**
 * @author thaveman
 * @author markhale
 */
public class PingTimerTask extends TimerTask {
	private final jIRCd jircd;

	public PingTimerTask(jIRCd jircd) {
		this.jircd = jircd;
	}
	public void run() {
		// PING? PONG!
		synchronized(jircd.clients) {
			for(Iterator iter = jircd.clients.iterator(); iter.hasNext();) {
				Client client = (Client) iter.next();
				if(!client.pingMe()) {
					Source source = client.getSource();
					if(source instanceof User) {
						// should have had PONG a long time ago, timeout please!
						jircd.thisServer.removeUser((User)source, "Ping timeout");
						iter.remove();
						client.close();
					}
				}
			}
		}
	}
}
