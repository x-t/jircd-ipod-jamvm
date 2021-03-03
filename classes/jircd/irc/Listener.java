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

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import javax.net.ServerSocketFactory;

import jircd.jIRCd;
//import org.apache.log4j.Logger;

/**
 * @author thaveman
 * @author markhale
 */
public class Listener implements Runnable {
	private static final int BACKLOG = 16535;

	//private final Logger //logger = Logger.getLogger(getClass());

	private final jIRCd jircd;
	private final String boundIP;
	private final int boundPort;
	private ServerSocket socket;
	private volatile boolean dontDie = true;

	public Listener(jIRCd jircd, String ip, int port) {
		this.jircd = jircd;
		boundIP = ip;
		boundPort = port;
	}
	
	public String toString() {
		return getClass().getName() + '[' + boundIP + ':' + boundPort + ']';
	}

	public void run() {
		while (dontDie) {
			try {
				Socket newClient = socket.accept();
				jircd.addClient(new Client(jircd, newClient));
			} catch (IOException e) {
				//logger.warn("Error in thread " + Thread.currentThread().toString() + ": " + e.toString());
			}
		}
	}
	
	public boolean bind() {
		try {
			socket = new ServerSocket(boundPort, BACKLOG, InetAddress.getByName(boundIP));
		} catch (Exception e) {
			//logger.warn("Bind exception", e);
			return false;
		}
		return true;
	}
	public boolean bind(ServerSocketFactory factory) {
		try {
			socket = factory.createServerSocket(boundPort, BACKLOG, InetAddress.getByName(boundIP));
		} catch (Exception e) {
			//logger.warn("Bind exception", e);
			return false;
		}
		return true;
	}

	public void close() {
		dontDie = false;
		try {
			socket.close();
		} catch(IOException e) {}
	}
	
	public String getIP() {
		return boundIP;
	}
	
	public int getPort() {
		return boundPort;
	}
}
