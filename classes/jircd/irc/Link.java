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
import java.io.InputStreamReader;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Random;
import java.util.Iterator;

//import org.apache.log4j.Logger;
import jircd.jIRCd;

/**
 * An outbound socket connection to another server.
 * @author thaveman
 * @author markhale
 */
public class Link extends Connection implements Runnable {
	//private final Logger //logger = Logger.getLogger(getClass());

	private final jIRCd jircd;
	private final Socket socket;
	private final BufferedReader input;
	private final BufferedWriter output;
	private final int token;
	private Server server; // the server on the other end of this link
	private volatile boolean dontDie = true;

	public Link(jIRCd jircd, String host, int port) throws IOException {
		this.jircd = jircd;
		socket = new Socket(host, port);
		input = new BufferedReader(new InputStreamReader(socket.getInputStream(), Constants.CHARSET), Constants.MAX_MESSAGE_SIZE);
		output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), Constants.CHARSET), Constants.MAX_MESSAGE_SIZE);
		token = Math.abs(new Random().nextInt());
		println("PASS "+"linkPassword"+" 0210 IRC|");
		println("SERVER "+jircd.thisServer.getHost()+" 0 "+token+" :"+jircd.thisServer.getDescription());
	}

	public void run() {
		String password = null;
		while(dontDie) {
			try {
				String inStr = input.readLine();
				//logger.debug("Message received from " + toString() + "\n\t" + inStr);
				if (inStr != null && inStr.length() > 0) {
					if (inStr.length() > Constants.MAX_MESSAGE_LENGTH)
						inStr = inStr.substring(0, Constants.MAX_MESSAGE_LENGTH); // max len per RFC
					final Message inMessage = new Message(inStr);
					final String cmd = inMessage.getCommand();
					if (cmd.equalsIgnoreCase("PASS")) {
						password = inMessage.getParameter(0);
					} else if (cmd.equalsIgnoreCase("SERVER")) {
						loginServer(inMessage.getParameter(0), "My description", password);
						password = null;
					} else if (Character.isDigit(cmd.charAt(0))) {
						// numeric reply
					} else {
						CommandManager.invokeCommand(server, inMessage);
					}
				} else if (inStr == null) {
					jircd.removeLink(this);
					return;
				}
			} catch(SocketException e) {
				jircd.removeLink(this);
				return;
			} catch (Exception e) {
				//logger.warn("Exception occured in thread " + Thread.currentThread().toString(), e);
				return;
			}
		}
	}

	public void close() {
		dontDie = false;
		try {
			socket.close();
		} catch(IOException e) {
			//logger.debug("Exception on socket close", e);
		}
	}

	private void loginServer(String host, String desc, String password) {
		if(server != null) {
			println(':' + jircd.thisServer.getHost() + " 462 :You may not reregister");
		} else {
			server = new Server(host, desc, jircd.thisServer, this);
			jIRCd.addServer(server);
			for(Iterator iter = jircd.thisServer.getUsers().iterator(); iter.hasNext(); ) {
				User usr = (User) iter.next();
				Message message = new Message(null, "NICK");
				message.appendParameter(usr.getNick());
				message.appendParameter("0");
				message.appendParameter(usr.getIdent());
				message.appendParameter(usr.getRealHost());
				message.appendParameter(Integer.toString(jircd.thisServer.getToken()));
				message.appendParameter(usr.getModesList());
				message.appendParameter(usr.getDescription());
				server.send(message);
			}
			for(Iterator iter = jIRCd.channels.values().iterator(); iter.hasNext(); ) {
				Channel chan = (Channel) iter.next();
				Message message = new Message(server.getServer(), "NJOIN");
				message.appendParameter(chan.getName());
				message.appendParameter(chan.getNamesList());
				server.send(message);
			}
		}
	}

	public void println(String text) {
		// Do not use PrintWriter.println() since that depends on the system property line.separator,
		// which may not be "\r\n".
		try {
			output.write(text);
			output.write("\r\n");
			output.flush();
			//logger.debug("Message sent to " + toString() + "\n\t" + text);
		} catch(IOException e) {
			//logger.debug("Exception occurred while sending message", e);
		}
	}
}
