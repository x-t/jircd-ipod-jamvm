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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import jircd.jIRCd;

/**
 * A server on a server.
 * @author thaveman
 * @author markhale
 */
public class Server extends Source {
	/** (String nickName, User user) */
	private final Map users = new HashMap();
	private final String hostname;
	private final String description;
	private final Server route;
	private final Connection connection; // the connection used if it's linked to me
	private final int token;
	
	public Server(String host, String description, Server route, Connection connection) {
		this.hostname = host;
		this.description = description;
		this.route = route;
		this.connection = connection;
		token = Math.abs(new Random().nextInt());
	}
	
	public String getNick() {
		return getHost();
	}
	/** ID */
	public String toString() {
		return getHost();
	}

	public Collection getUsers() {
		return Collections.unmodifiableCollection(users.values());
	}

	public Connection getConnection() {
		return connection;
	}
	/**
	 * Returns the server this server is connected to.
	 */
	public Server getServer() {
		return route;
	}
	
	public String getHost() {
		return hostname;
	}
	public String getDescription() {
		return description;
	}
	public int getToken() {
		return token;
	}

	public synchronized void addUser(User user) {
		users.put(user.getNick().toLowerCase(), user);
	}
	public synchronized User getUser(String nick) {
		return (User) users.get(nick.toLowerCase());
	}
	public synchronized void removeUser(User usr, String reason) {
		String nick = usr.getNick().toLowerCase();
		if(users.containsKey(nick)) {
			// first remove the user from any channels he/she may be in
			for(Iterator iter = usr.getChannels().iterator(); iter.hasNext();) {
				Channel channel = (Channel) iter.next();
				Message message = new Message(usr, "QUIT");
				message.appendParameter(reason);
				channel.send(message, usr);
				iter.remove();
				channel.removeUser(usr);
			}
			users.remove(nick);
		}
	}

	public synchronized void changeNick(User user, String oldnick, String newnick) {
		users.put(newnick.toLowerCase(), user);
		users.remove(oldnick.toLowerCase());
		Message message = new Message(user, "NICK");
		message.appendParameter(newnick);
		for(Iterator iter = users.values().iterator(); iter.hasNext();) {
			User iusr = (User) iter.next();
			iusr.send(message);
		}
	}

	/**
	 * Returns the number of visible users on this server.
	 */
	public synchronized int userCount() {
		int ucount = 0;
		for(Iterator iter = users.values().iterator(); iter.hasNext();) {
			User user = (User) iter.next();
			if (!(user.isModeSet(User.UMODE_INVISIBLE))) {
				ucount++;
			}
		}
		return ucount;
	}
	/**
	 * Returns the number of invisible users on this server.
	 */
	public synchronized int userCountInvisible() {
		int ucount = 0;
		for(Iterator iter = users.values().iterator(); iter.hasNext();) {
			User user = (User) iter.next();
			if (user.isModeSet(User.UMODE_INVISIBLE)) {
				ucount++;
			}
		}
		return ucount;
	}

	public void send(Message msg) {
		if (connection != null)
			connection.println(msg.toString());
		else
			route.connection.println(msg.toString());
	}
}
