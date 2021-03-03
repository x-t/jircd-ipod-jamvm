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

/**
 * IRC message format.
 * @author markhale
 */
public class Message {
	private final String from;
	private final String command;
	private final String[] params = new String[Constants.MAX_MESSAGE_PARAMETERS];
	private int paramCount = 0;

	/**
	 * Parses a message string.
	 */
	public Message(String msg) {
		int startPos = 0;

		// parse prefix
		if(msg.charAt(0) == ':') {
			int endPos = msg.indexOf(' ', 2);
			from = msg.substring(1, endPos);
			startPos = endPos + 1;
		} else {
			from = null;
		}

		// parse command
		int endPos = msg.indexOf(' ', startPos);
		if(endPos == -1) {
			// no parameters
			command = msg.substring(startPos);
		} else {
			command = msg.substring(startPos, endPos);

			// parse parameters
			int trailingPos = msg.indexOf(" :", endPos);
			if(trailingPos == -1)
				trailingPos = msg.length();
			while(endPos != -1 && endPos < trailingPos) {
				startPos = endPos + 1;
				endPos = msg.indexOf(' ', startPos);
				if(endPos != -1) {
					params[paramCount] = msg.substring(startPos, endPos);
					paramCount++;
				}
			}
			if(endPos == -1 && startPos < msg.length()) { // ignore zero length parameters
				params[paramCount] = msg.substring(startPos);
				paramCount++;
			} else if(trailingPos+2 < msg.length()) { // ignore zero length parameters
				params[paramCount] = msg.substring(trailingPos+2);
				paramCount++;
			}
		}
	}
	/**
	 * Creates a message sent from a server.
	 * @param from can be null
	 */
	public Message(Source from, String command) {
		if(from != null)
			this.from = from.toString();
		else
			this.from = null;
		this.command = command;
	}
	/**
	 * Constructs a "numeric reply" type of message.
	 */
	public Message(Source from, String command, Source target) {
		this(from, command);
		appendParameter(target.getNick());
	}
	public Message(Source from, String command, Channel target) {
		this(from, command);
		appendParameter(target.getName());
	}
	/**
	 * Constructs a "numeric reply" type of message.
	 */
	public Message(String command, Source target) {
		this(target.getServer(), command, target);
	}
	public String getSender() {
		return from;
	}
	public String getCommand() {
		return command;
	}
	public void appendParameter(String param) {
		if(param != null && param.length() > 0) {
			params[paramCount] = param;
			paramCount++;
		}
	}
	public String getParameter(int n) {
		return params[n];
	}
	public int getParameterCount() {
		return paramCount;
	}
	public String toString() {
		StringBuffer buf = new StringBuffer();
		// append prefix
		if(from != null)
			buf.append(':').append(from).append(' ');

		// append command
		buf.append(command);

		// append parameters
		if(paramCount > 0) {
			final int lastParamIndex = paramCount - 1;
			for(int i=0; i<lastParamIndex; i++)
				buf.append(' ').append(params[i]);
			final String lastParam = params[lastParamIndex];
			// if the last parameter contains spaces or starts with a ':'
			if(lastParam.indexOf(' ') != -1 || lastParam.charAt(0) == ':')
				buf.append(" :").append(lastParam);
			else
				buf.append(' ').append(lastParam);
		}
		return buf.toString();
	}
}
