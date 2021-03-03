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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import jircd.jIRCd;

/**
 * @author thaveman
 * @author markhale
 */
public final class Util {
	private static final int MAX_NICK_LENGTH = 32;
	private static final int MAX_CHANNEL_NAME_LENGTH = 200;

	private Util() {}

	public static boolean isIRCString(String str) {
		final int len = str.length();
		for(int i=0; i < len; i++) {
			if (!isIRCCharacter(str.charAt(i))) return false;
		}
		return true;
	}
	private static boolean isIRCCharacter(char c) {
		return ((c >= 'A' && c <= '~') || (c >= '0' && c <= '9') || c == '-');
	}

	public static boolean isNickName(String name) {
		final int len = name.length();
		if(len > MAX_NICK_LENGTH) return false;
		for(int i=0; i < len; i++) {
			if (!isNickCharacter(name.charAt(i))) return false;
		}
		return true;
	}
	private static boolean isNickCharacter(char c) {
		return ((c >= 'A' && c <= '~') || (c >= '0' && c <= '9') || c == '-');
	}

	public static boolean isChannelIdentifier(String name) {
		final int len = name.length();
		if(len > MAX_CHANNEL_NAME_LENGTH) return false;
		if(!isChannelIdentifierStart(name.charAt(0))) return false;
		for(int i=1; i < len; i++)
			if(!isChannelIdentifierPart(name.charAt(i))) return false;
		return true;
	}
	private static boolean isChannelIdentifierStart(char c) {
		return (c == '#' || c == '&' || c == '+' || c == '!');
	}
	private static boolean isChannelIdentifierPart(char c) {
		return (c != ' ' && c != ',' && c != '\r' && c != '\n');
	}

	/**
	 * Returns the number of visible users on the network.
	 */
	public static int networkUserCount() {
		int temp = 0;
		for(Iterator iter = jIRCd.servers.values().iterator(); iter.hasNext();) {
			Server server = (Server) iter.next();
			temp += server.userCount();
		}
		return temp;
	}
	/**
	 * Returns the number of invisible users on the network.
	 */
	public static int networkUserCountInvisible() {
		int temp = 0;
		for(Iterator iter = jIRCd.servers.values().iterator(); iter.hasNext();) {
			Server server = (Server) iter.next();
			temp += server.userCountInvisible();
		}
		return temp;
	}

	public static User findUser(String name) {
		for(Iterator iter = jIRCd.servers.values().iterator(); iter.hasNext();) {
			Server server = (Server) iter.next();
			User user = server.getUser(name);
			if(user != null)
				return user;
		}
		return null;
	}

	public static Server findServer(String name) {
		return (Server) jIRCd.servers.get(name.toLowerCase());
	}

	public static Channel findChannel(String name) {
		return (Channel) jIRCd.channels.get(name.toLowerCase());
	}

	public static boolean match(String pattern, String text) {
		return matchWildcard(pattern, text);
	}
	private static boolean matchWildcard(String pattern, String text) {
		int patSize = pattern.length() - 1;
		int texSize = text.length() - 1;
		int patIndex = 0;
		int texIndex = 0;
		
		while (true)
		{
			if (patIndex > patSize) return (texIndex > texSize);
			
			if (pattern.charAt(patIndex) == '*')
			{
				patIndex++;
			
				if (patIndex > patSize) return true;
				
				while (pattern.charAt(patIndex) == '*')
					patIndex++;
			
				while (patIndex <= patSize && pattern.charAt(patIndex) == '?' && texIndex <= texSize)
				{	
					texIndex++;
					patIndex++;
				}
			
				if (patIndex > patSize) return false;
			
				if (pattern.charAt(patIndex) == '*') continue;
			
				while (texIndex <= texSize)
				{
					if (matchWildcard(pattern.substring(patIndex),text.substring(texIndex)))
						return true;
					else if (texIndex == texSize)
						return false;
					texIndex++;
				}
			}//end if
			if (texIndex > texSize) return true;
			if (patIndex <= patSize && pattern.charAt(patIndex) != '?' &&
				Character.toUpperCase(pattern.charAt(patIndex)) != Character.toUpperCase(text.charAt(texIndex)))
				return false;
			texIndex++;
			patIndex++;
		}
	}

	public static String[] loadTextFile(String filename, int maxLines) throws IOException {
		String[] tmpLines = new String[maxLines];
		BufferedReader file = new BufferedReader(new FileReader(filename));
		int n;
		try {
			String line = file.readLine();	
			for (n=0; line != null && n < tmpLines.length; n++) {
				tmpLines[n] = line;
				line = file.readLine();
			}
		} finally {		
			file.close();
		}

		String[] lines = new String[n];
		System.arraycopy(tmpLines, 0, lines, 0, n);
		return lines;
	}
}
