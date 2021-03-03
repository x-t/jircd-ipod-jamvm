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

import jircd.jIRCd;
////import org.apache.log4j.Logger;

/**
 * An inbound socket connection to a server.
 * The Source of a Client is Unknown until it registers/logins.
 * @author thaveman
 * @author markhale
 */
public class Client extends Connection implements Runnable {
//	\^//private final Logger //logger = Logger.getLogger(getClass());

	private final Socket socket;
	private final BufferedReader input;
	private final BufferedWriter output;
	private final jIRCd jircd; // the server connected to
	private Source source;
	private long lastPing = 0; // millis
	private long lastPong = 0; // millis
	private long latency = 0; // millis
	private final long pingTimeout; // millis
	private volatile boolean dontDie = true;
	
	public Client(jIRCd jircd, Socket socket) throws IOException {
		this.jircd = jircd;
		this.socket = socket;
		input = new BufferedReader(new InputStreamReader(socket.getInputStream(), Constants.CHARSET), Constants.MAX_MESSAGE_SIZE);
		output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), Constants.CHARSET), Constants.MAX_MESSAGE_SIZE);
		pingTimeout = 1000 * Integer.parseInt(jircd.getProperty("jircd.ping.timeout", "120"));
		lastPing = System.currentTimeMillis();
		lastPong = lastPing;
		source = new Unknown(this, jircd.thisServer);
	}
	
	public Source getSource() {
		return source;
	}
	public String getHost() {
		return socket.getInetAddress().getHostName();
	}
	public String toString() {
		return "Client[" + source + ',' + socket.getInetAddress().getHostAddress() + ':' + socket.getPort() + ',' + socket.getClass().getName() + ']';
	}

	public void close() {
		dontDie = false;
		try {
			socket.close();
		} catch(IOException e) {
			//logger.debug("Exception on socket close", e);
		}
	}

	/**
	 * Pings this connection.
	 * Returns false on ping timeout.
	 */
	public synchronized boolean pingMe() {
		final long curTime = System.currentTimeMillis();
		if (lastPong >= lastPing) { // got a response previously
			if (curTime - lastPong > pingTimeout || lastPong == 0) { // more than timeout seconds? or newbie?
				lastPing = curTime;
				// send ping
				Server server = source.getServer();
				Message message = new Message(null, "PING");
				message.appendParameter(server.toString());
				source.send(message);
			}
		} else { // I have no response since the last ping
			if (curTime - lastPing > pingTimeout) { // more than timeout seconds since PING
				// should have had PONG a long time ago, timeout please!
				return false;
			}
		}
		return true;
	}

	public long idleMillis() {
		return (System.currentTimeMillis() - lastPong);
	}

	public synchronized String getID() {
		return source.toString();
	}

	public void run() {
		while (dontDie) {
			try {
				String inStr = input.readLine();
				//logger.debug("Message received from " + toString() + "\n\t" + inStr);
				if (inStr != null && inStr.length() > 0) {
					if (inStr.length() > Constants.MAX_MESSAGE_LENGTH)
						inStr = inStr.substring(0, Constants.MAX_MESSAGE_LENGTH); // max length per RFC

					// if there has been a response since the last PING, then don't try to ping again
					if (lastPing < lastPong)
						lastPong = System.currentTimeMillis();

					CommandManager.invokeCommand(source, new Message(inStr));
				} else if (inStr == null) {
					jircd.removeClient(this, "Connection reset by peer");
					return;
				}
			} catch(SocketException e) {
				jircd.removeClient(this, e.getMessage());
				return;
			} catch (Exception e) {
				//logger.warn("Exception occured in thread " + Thread.currentThread().toString(), e);
				return;
			}
		}
	}

	public void processPong() {
		lastPong = System.currentTimeMillis();
		latency = lastPong - lastPing;
	}

	/**
	 * Registers/logs-in using the specified source.
	 * This should be used by Command classes to register Source implementations.
	 * @return true on success
	 */
	public boolean login(Source newSource) {
		if(source instanceof Unknown) {
			if(newSource.getConnection() != this)
				throw new IllegalArgumentException("The connection of " + newSource.toString() + " must be " + toString() + " (it was " + newSource.getConnection().toString() + ")");
			if(newSource.getServer() != jircd.thisServer)
				throw new IllegalArgumentException("The server of " + newSource.toString() + " must be " + jircd.thisServer.toString() + " (it was " + newSource.getServer().toString() + ")");
			source = newSource;
			return true;
		} else {
			Message message = new Message(Constants.ERR_ALREADYREGISTRED, newSource);
			message.appendParameter("Unauthorized command (already registered)");
			newSource.send(message);
			return false;
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
