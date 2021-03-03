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
import java.util.Set;
import java.util.HashSet;

import jircd.jIRCd;

/**
 * A user on a server.
 * @author thaveman
 * @author markhale
 */
public class User extends Source {
	public static final int UMODE_INVISIBLE = 0x0001;
	public static final int UMODE_SNOTICE   = 0x0002;
	public static final int UMODE_WALLOPS   = 0x0004;
	public static final int UMODE_OPER      = 0x0008;
	public static final int UMODE_AWAY      = 0x0010;
	
	public static final int[] UMODEFLAGS
		= { 'i', UMODE_INVISIBLE,
			's', UMODE_SNOTICE,
			'w', UMODE_WALLOPS,
			'o', UMODE_OPER,
			'a', UMODE_AWAY };
	
	private String nickname;
	private final String ident;
	private final String hostname;
	private final String realhost;
	private final String description;
	private String awayMsg;
	private Server server;
	private final Connection connection; // used only for local users
	/** set of Channel */
	private final Set channels = new HashSet();
	private int modes = 0;
	
	public User(String nickname, String ident, String hostname, String description, Server server) {
		this(nickname, ident, hostname, description, server, null);
	}
	public User(String nickname, String ident, String hostname, String description, Server server, Connection connection) {
		this.nickname = nickname;
		this.ident = ident;
		this.hostname = maskHost(hostname);
		this.realhost = hostname;
		this.description = description;
		this.server = server;
		this.connection = connection;
	}
	
	private String maskHost(String host)
	{
		java.util.StringTokenizer st = new java.util.StringTokenizer(host,".");
		StringBuffer retval = new StringBuffer();
		// first see if it's an IP
		boolean isIp = true;
		String tok = st.nextToken();
		while (st.hasMoreTokens())
		{
			try
			{
				Integer.valueOf(tok);
			}
			catch (NumberFormatException e)
			{
				isIp = false;
				break;
			}
			tok = st.nextToken();
		}
		int p;
		String appendage = Integer.toHexString((int)(Math.random() * 15728639 + 1048576));
		if (isIp && host.indexOf('.') >= 0)
		{
			p = host.lastIndexOf('.');
			retval.append(host.substring(0,++p));
			retval.append(appendage);
		}
		else if (host.indexOf('.') >= 0)
		{
			p = host.indexOf('.');
			retval.append(appendage);
			retval.append(host.substring(p));
		}
		else
		{
			retval.append(appendage);
			retval.append(".host");
		}
		st = null;
		return retval.toString();
	}
	
	public void processModes(String modeString) { processModes(modeString,false); }
	
	public void processModes(String modeString, boolean isAllowed)
	{
		char[] letters = modeString.toCharArray();
		
		boolean adding = true; // are we adding modes (+) or subtracting (-)
		
		int addModes = 0;
		int delModes = 0;
		
		StringBuffer goodModes = new StringBuffer();
		
		for (int i = 0; i < letters.length; i++)
		{
			boolean doDo = false;
			
			switch(letters[i])
			{
			case '+':
				adding = true;
				goodModes.append('+');
				break;
			case '-':
				adding = false;
				goodModes.append('-');
				break;
				
			// add other processing here for modes that may not want to be
			// set under certain conditions, etc.
			case 'o': // user can't set himself +o, the server must do it
				if (!isAllowed && adding) break;
				else doDo = true;
			case 'a': // user can't set himself +/-a, the server must do it
				if (!isAllowed) break;
				else doDo = true;
			case ':':
				break;
				
			default:
				doDo = true;
			}
			
			if (doDo) {
				int modeCode = isMode(letters[i]);
				if (modeCode != 0) {
					goodModes.append(letters[i]);
					if (adding)
						addModes |= modeCode;
					else
						delModes |= modeCode;
				} else {
					//Invalid Mode Character Detected!
					Message message = new Message(Constants.ERR_UMODEUNKNOWNFLAG, this);
					message.appendParameter("Unknown MODE flag");
					send(message);
				}
			}
		}
		setMode(addModes);
		unsetMode(delModes);
		
		if (goodModes.length() > 1) {
			Message message = new Message(server, "MODE", this);
			message.appendParameter(goodModes.toString());
			send(message);
		}
	}

	public synchronized String getModesList()
	{
		StringBuffer retval = new StringBuffer("+");
		
		for (int i = 0; i < UMODEFLAGS.length; i += 2)
		{
			if (isModeSet(UMODEFLAGS[i+1]))
				retval.append((char)UMODEFLAGS[i]);
		}
		
		return retval.toString();
	}
	
	private int isMode(char modeChar)
	{
		int retval = 0;
		for (int i = 0; i < UMODEFLAGS.length; i += 2)
		{
			if (UMODEFLAGS[i] == modeChar)
				retval = UMODEFLAGS[i + 1];
		}
		return retval;
	}
	
	private synchronized void setMode(int mode)
	{
		modes |= mode;
	}
	public synchronized boolean isModeSet(int mode)
	{
		return ((modes & mode) != 0);
	}
	private synchronized void unsetMode(int mode)
	{
		modes &= ~mode;
	}

	public void setAwayMessage(String msg) {
		awayMsg = msg;
		if(awayMsg != null)
			setMode(UMODE_AWAY);
		else
			unsetMode(UMODE_AWAY);
	}
	public String getAwayMessage() {
		return awayMsg;
	}

	public synchronized Set getChannels()
	{
		return channels;
	}

	public Connection getConnection()
	{
		return connection;
	}
	/** ID */
	public synchronized String toString()
	{
		return nickname + '!' + ident + '@' + hostname;
	}
	/**
	 * Returns the server this user is connected to.
	 */
	public Server getServer()
	{
		return server;
	}
	
	public synchronized String getNick()
	{
		return nickname;
	}
	
	public synchronized String getHost()
	{
		return hostname;
	}
	
	public synchronized String getRealHost()
	{
		return realhost;
	}

	public synchronized String getIdent()
	{
		return ident;
	}

	public synchronized String getDescription()
	{
		return description;
	}

	public synchronized void changeNick(String newnick) {
		server.changeNick(this, nickname, newnick);
		nickname = newnick;
	}
	
	protected synchronized void addChannel(Channel chan) {
		channels.add(chan);
	}

	protected synchronized void removeChannel(Channel chan) {
		channels.remove(chan);
	}

	public void send(Message msg) {
		if (connection != null)
			connection.println(msg.toString());
		else
			server.getConnection().println(msg.toString());
	}
}
