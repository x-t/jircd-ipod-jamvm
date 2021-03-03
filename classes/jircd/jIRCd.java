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

package jircd;

import java.io.File;
import java.io.FilenameFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Timer;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;

import jircd.irc.*;
//import org.apache.log4j.Logger;

/**
 * @author thaveman
 * @author markhale
 */
public class jIRCd {
	//private final Logger //logger = Logger.getLogger(getClass());

	// version information
	public static final int VERSION_MAJOR = 0;
	public static final int VERSION_MINOR = 4;
	public static final int VERSION_PATCH = 0;
	public static final String VERSION_URL = "http://j-ircd.sourceforge.net/";

	// servers that exist anywhere on the network
	/** (String host, Server server) */
	public static final Map servers = new HashMap();

	// all channels on the network
	/** (String name, Channel channel) */
	public static final Map channels = new HashMap();

	// pointer to information about this server
	public Server thisServer;

	// server socket listeners
	/** set of Listener */
	private final Set listeners = new HashSet();

	// clients (including servers) that are connected to this servers
	/** set of Client */
	public final Set clients = Collections.synchronizedSet(new HashSet());

	// links to other servers
	/** set of Link */
	private final Set links = Collections.synchronizedSet(new HashSet());

	// configuration and informational information
	private long startTime;
	public final String configFile;
	private final Properties settings = new Properties();

	private final Timer timer = new Timer();

	public static void main(String[] args) {
		// program must be executed using: jircd.jIRCd <configuration file>
		if ((args == null) || (args.length < 1)) {
			System.err.println("Usage: jircd.jIRCd <configuration file>");
			System.exit(1);
		}
		
		String configFile = args[0];

		jIRCd jircd = new jIRCd(configFile);

		System.out.println();
		System.out.println("Welcome to jIRCd: The world's first full-featured multiplatform Java-powered IRC"
					+ " server. Created and maintained by Tyrel L. Haveman.");
		System.out.println("jIRCd uses a TCP protocol based on the Internet Relay Chat Protocol (RFC 1459), "
					+ "by Jarkko Oikarinen (May 1993). Portions may also be based on the IRC version 2 "
					+ "protocol (RFC 2810, RFC 2811, RFC 2812, RFC 2813) by C. Kalt (April 2000).");
		System.out.println("Please visit "+VERSION_URL+" for the latest information and releases.");
		System.out.println();
		System.out.println(versionInfo() + " starting...");
		
		// attempt to read the specified configuration file
		try {
			jircd.loadSettingsFile(configFile);
		} catch (IOException e) {
			System.err.println(e + " occured while reading configuration file.");
			System.exit(1);
		}
		
		System.out.println("Configuration file loaded for " + jircd.thisServer.getHost() + " - " + jircd.thisServer.getDescription());
		addServer(jircd.thisServer);

		jircd.loadPlugins();
		jircd.start();

		// now just hang out forever
		
		System.out.println("Press enter to terminate.");
		try {
			System.in.read();
		} catch (IOException e) {
			System.err.println(e + " occured while waiting for program termination.");
			System.exit(1);
		} finally {
			System.out.println("Shutting down...");
			jircd.shutdown();
		}
		//System.exit(0);
	}

	public static void addServer(Server cl) {
		servers.put(cl.getHost().toLowerCase(), cl);
	}
	public static void removeServer(Server ic) {
		servers.remove(ic);
	}
	
	public static void addChannel(Channel cl) {
		channels.put(cl.getName().toLowerCase(), cl);
	}
	public static void removeChannel(Channel ic) {
		channels.remove(ic.getName());
	}

	private jIRCd(String configFile) {
		this.configFile = configFile;
		settings.setProperty("jircd.version.name", versionInfo());
		settings.setProperty("jircd.version.url", VERSION_URL);
	}
	private void loadSettingsFile(String fileName) throws IOException {
		//logger.info("Reading configuration file...");

		settings.load(new FileInputStream(fileName));
		String tempHost = settings.getProperty("jircd.hostName", "dumb.admin");
		String tempDesc = settings.getProperty("jircd.description", "dumb.admin");
		thisServer = new Server(tempHost, tempDesc, null, null);

		String bind = settings.getProperty("jircd.bind.1");
		int n = 1;
		while(bind != null) {
			String ip;
			int port = Constants.DEFAULT_PORT;
			String factory = "default";
			final int portSeparator = bind.indexOf(':');
			final int factorySeparator = bind.lastIndexOf(',');
			if(portSeparator != -1) {
				ip = bind.substring(0, portSeparator);
				if(factorySeparator != -1) {
					port = Integer.parseInt(bind.substring(portSeparator+1, factorySeparator));
					factory = bind.substring(factorySeparator+1);
				} else {
					port = Integer.parseInt(bind.substring(portSeparator+1));
				}
			} else {
				ip = bind;
			}

			Listener listener;
			if(factory.equals("ssl"))
				listener = new SSLListener(this, ip, port);
			else
				listener = new Listener(this, ip, port);
			listeners.add(listener);
			n++;
			bind = settings.getProperty("jircd.bind."+n);
		}

		String oper = settings.getProperty("jircd.oper.1");
		n = 1;
		while(oper != null) {
			int pos1 = oper.indexOf(' ');
			int pos2 = oper.indexOf(' ', pos1+1);
			String name = oper.substring(0, pos1);
			String host = oper.substring(pos1+1, pos2);
			String pass = oper.substring(pos2+1);
			Operator.add(new Operator(name, host, pass));
			n++;
			oper = settings.getProperty("jircd.oper."+n);
		}
	}
	public void loadPlugins() {
		File pluginDir = new File("plugins");
		File[] jarFiles = pluginDir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".jar");
			}
		});
		for(int i=0; i<jarFiles.length; i++) {
			File jarFile = jarFiles[i];
			try {
				// create class loader for plugin
				URLClassLoader loader = URLClassLoader.newInstance(new URL[] {jarFile.toURL()}, Thread.currentThread().getContextClassLoader());

				JarFile jar = new JarFile(jarFile);
				//logger.info("Searching plugin "+jar.getName()+"...");
				Enumeration entries = jar.entries();
				while(entries.hasMoreElements()) {
					JarEntry entry = (JarEntry) entries.nextElement();
					String name = entry.getName();
					if(name.endsWith(".class")) {
						String className = name.substring(0, name.length()-6).replace('/', '.');
						try {
							Class cls = loader.loadClass(className);
							if(Command.class.isAssignableFrom(cls)) {
								Command command;
								try {
									command = (Command) cls.newInstance();
								} catch(InstantiationException ie) {
									Constructor cnstr = cls.getConstructor(new Class[] {jIRCd.class});
									command = (Command) cnstr.newInstance(new Object[] {this});
								}
								CommandManager.addCommand(command);
								//logger.info("...installed "+command.getName()+" ("+className+")");
							}
						} catch(Exception ex) {
							//logger.warn("Could not load class "+className, ex);
						}
					}
				}
			} catch(IOException ioe) {
				//logger.warn("Could not load plugin "+jarFile, ioe);
			}
		}
	}
	private void start() {
		// create all of the Listener objects to listen on all
		// ports specified in the configuration file
		//logger.info("Binding to port(s)...");

		for(Iterator iter = listeners.iterator(); iter.hasNext();) {
			Listener listener = (Listener) iter.next();
			if (listener.bind()) {
				new Thread(listener, listener.toString()).start(); // start that listening thread!
				//logger.info("..." + listener.toString() + "...");
			} else {
				iter.remove();
				//logger.warn("..." + listener.toString() + " (FAILED)...");
			}
		}
		//logger.info("...complete");

		startTime = System.currentTimeMillis();

		// start ping timer
		final long pingInterval = 1000 * Integer.parseInt(settings.getProperty("jircd.ping.interval", "5"));
		timer.schedule(new PingTimerTask(this), 0, pingInterval);
	}
	public void addClient(Client cl) {
		clients.add(cl);
		new Thread(cl, cl.toString()).start();
	}
	public void removeClient(Client ic, String reason) {
		if(clients.contains(ic)) {
			Source src = ic.getSource();
			if(src instanceof User)
				thisServer.removeUser((User)src, reason);
			clients.remove(ic);
		}
	}
	/**
	 * Add a link to another server.
	 */
	public void addLink(Link ln) {
		links.add(ln);
		new Thread(ln, ln.toString()).start();
	}
	/**
	 * Remove a link to another server.
	 */
	public void removeLink(Link ln) {
		links.remove(ln);
	}
	private void shutdown() {
		// stop ping timer
		timer.cancel();

		// broadcast shutdown notice
		for(Iterator iter = thisServer.getUsers().iterator(); iter.hasNext();) {
			User user = (User) iter.next();
			Message message = new Message(thisServer, "NOTICE", user);
			message.appendParameter("WARNING: Server shut down by local console.");
			user.send(message);
		}

		// stop listeners
		for(Iterator iter = listeners.iterator(); iter.hasNext();) {
			Listener listener = (Listener) iter.next();
			listener.close();
			iter.remove();
		}
		// disconnect clients
		synchronized(clients) {
			for(Iterator iter = clients.iterator(); iter.hasNext();) {
				Client client = (Client) iter.next();
				Source src = client.getSource();
				if(src instanceof User)
					thisServer.removeUser((User)src, "Server shutdown");
				client.close();
				iter.remove();
			}
		}
		// disconnect links
		synchronized(links) {
			for(Iterator iter = links.iterator(); iter.hasNext();) {
				Link link = (Link) iter.next();
				link.close();
				iter.remove();
			}
		}
	}

	public String getProperty(String key) {
		return settings.getProperty(key);
	}
	public String getProperty(String key, String defaultValue) {
		return settings.getProperty(key, defaultValue);
	}

	/**
	 * Returns the server uptime in milliseconds.
	 */
	public long uptimeMillis() {
		return (startTime == 0) ? 0 : (System.currentTimeMillis() - startTime);
	}

	public static String versionInfo() {
		return "jIRCd-" + VERSION_MAJOR + '.' + VERSION_MINOR + '.' + VERSION_PATCH;
	}
	
	public String toString() {
		return "jIRCd";
	}
}
