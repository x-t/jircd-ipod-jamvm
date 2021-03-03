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

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Date;

import jircd.jIRCd;

/**
 * IRC channel.
 * @author thaveman
 * @author markhale
 */
public class Channel {
	public static int CHANMODE_PRIVATE    = 0x0001;
	public static int CHANMODE_SECRET     = 0x0002;
	public static int CHANMODE_INVITEONLY = 0x0004;
	public static int CHANMODE_TOPICOPS   = 0x0008;
	public static int CHANMODE_NOEXTERNAL = 0x0010;
	public static int CHANMODE_MODERATED  = 0x0020;
	
	public static int CHANMODE_OPERATOR   = 0x0040;
	public static int CHANMODE_VOICE      = 0x0080;
	public static int CHANMODE_BAN        = 0x0100;
	
	public static int CHANMODE_LIMIT      = 0x0200;
	public static int CHANMODE_KEY        = 0x0400;
	
	public static final int[] MODEFLAGS
		= { 'p', CHANMODE_PRIVATE,
			's', CHANMODE_SECRET,
			'i', CHANMODE_INVITEONLY,
			't', CHANMODE_TOPICOPS,
			'n', CHANMODE_NOEXTERNAL,
			'm', CHANMODE_MODERATED,
	
			'o', CHANMODE_OPERATOR,
			'v', CHANMODE_VOICE,
			'b', CHANMODE_BAN,

			'l', CHANMODE_LIMIT,
			'k', CHANMODE_KEY
		  };

	private final String name;
	private int modes;
	private String topic = "";
	private String key; // null if none
	private int limit; // 0 if none
	/** (User user, Member member) */
	private final Map members = new HashMap();
	/** set of Bans */
	private final Set bans = new HashSet();

	/**
	 * Channel member.
	 */
	private class Member {
		private final User user;
		private int chanModes = 0;

		public Member(User user) {
			this.user = user;
		}
		public User getUser() {
			return user;
		}
		public synchronized boolean isChanOp() {
			return (chanModes & Channel.CHANMODE_OPERATOR) != 0;
		}
		public synchronized boolean setOp(boolean state) {
			if (state)
				chanModes |= Channel.CHANMODE_OPERATOR;
			else
				chanModes &= ~Channel.CHANMODE_OPERATOR;
			return isChanOp();
		}
		public synchronized boolean isChanVoice() {
			return (chanModes & Channel.CHANMODE_VOICE) != 0;
		}
		public synchronized boolean setVoice(boolean state) {
			if (state)
				chanModes |= Channel.CHANMODE_VOICE;
			else
				chanModes &= ~Channel.CHANMODE_VOICE;
			return isChanVoice();
		}
	}

	/**
	 * Channel ban.
	 */
	private class Ban {
		private final String mask;
		private final String who;
		private final long when;
		
		public Ban(String mask,String who) {
			this.mask = mask;
			this.who = who;
			this.when = System.currentTimeMillis();
		}
	}

	public Channel(String name) {
		this.name = name;
	}

	public Channel(String name,User firstUser) {
		this(name);
		joinUser(firstUser,null);
	}

	public String getName() {
		return name;
	}

	public Set getUsers() {
		return Collections.unmodifiableSet(members.keySet());
	}
	public int getCount() {
		return members.size();
	}
	
	public String getTopic() {
		return this.topic;
	}
	
	public boolean isOn(User usr) {
		return members.containsKey(usr);
	}
	
	private Member getMember(User usr) {
		return (Member) members.get(usr);
	}

	public synchronized void joinUser(User us, String[] params) {
		// check for bans
		for(Iterator iter = bans.iterator(); iter.hasNext();) {
			Ban ban = (Ban) iter.next();
			if(Util.match(ban.mask, us.toString())) {
				Message message = new Message(Constants.ERR_BANNEDFROMCHAN, us);
				message.appendParameter(name);
				message.appendParameter("Cannot join channel (+b)");
				us.send(message);
				return;
			}
		}
		// check for key
		if (this.key != null && !this.key.equals("")) {
			String providedKey = "";
			if (params.length > 1)
				providedKey = params[1];
			if (!providedKey.equals(this.key)) {
				Message message = new Message(Constants.ERR_BADCHANNELKEY, us);
				message.appendParameter(name);
				message.appendParameter("Cannot join channel (+k)");
				us.send(message);
				return;
			}
		}
		// check for member limit
		if (this.limit > 0) {
			if (members.size() >= this.limit) {
				Message message = new Message(Constants.ERR_CHANNELISFULL, us);
				message.appendParameter(name);
				message.appendParameter("Cannot join channel (+l)");
				us.send(message);
				return;
			}
		}
		// check for invite
		if (this.isModeSet(CHANMODE_INVITEONLY)) {
			Message message = new Message(Constants.ERR_INVITEONLYCHAN, us);
			message.appendParameter(name);
			message.appendParameter("Cannot join channel (+i)");
			us.send(message);
			return;
		}
		
		addUser(us);
		Message message = new Message(us, "JOIN", this);
		send(message);
		if (us.getConnection() != null) {
			sendNames(us);
			sendTopicInfo(us);
		}
	}

	public void addUser(User user) {
		user.addChannel(this);
		Member member = new Member(user);
		if(members.isEmpty())
			member.setOp(true);
		members.put(user, member);
	}

	public synchronized void addBan(String mask, String who) {
		bans.add(new Ban(mask,who));
	}
	
	public synchronized void listBans(User towho) {
		for(Iterator iter = bans.iterator(); iter.hasNext();) {
			Ban ban = (Ban) iter.next();
			Message message = new Message(Constants.RPL_BANLIST, towho);
			message.appendParameter(name);
			message.appendParameter(ban.mask);
			message.appendParameter(ban.who);
			message.appendParameter(new Date(ban.when).toString());
			towho.send(message);
		}
		Message message = new Message(Constants.RPL_ENDOFBANLIST, towho);
		message.appendParameter(name);
		message.appendParameter("End of channel ban list");
		towho.send(message);
	}
	
	public synchronized boolean removeBan(String mask)
	{
		for(Iterator iter = bans.iterator(); iter.hasNext();) {
			Ban ban = (Ban) iter.next();
			if(ban.mask.equals(mask)) {
				bans.remove(ban);
				return true;
			}
		}
		return false;
	}

	public synchronized void removeUser(User usr) {
		members.remove(usr);
		usr.removeChannel(this);

		if(members.isEmpty()) {
			jIRCd.removeChannel(this);
		}
	}
	
	public synchronized boolean isOp(User usr) {
		Member member = getMember(usr);
		return (member != null && member.isChanOp());
	}
	
	public synchronized boolean isVoice(User usr) {
		Member member = getMember(usr);
		return (member != null && member.isChanVoice());
	}

	public synchronized void sendTopicInfo(User usr)
	{
		if (topic.length() == 0) {
			Message message = new Message(Constants.RPL_NOTOPIC, usr);
			message.appendParameter(name);
			message.appendParameter("No topic is set");
			usr.send(message);
		} else {
			Message message = new Message(Constants.RPL_TOPIC, usr);
			message.appendParameter(name);
			message.appendParameter(topic);
			usr.send(message);
		}
	}

	public synchronized String getNamesList() {
		StringBuffer sb = new StringBuffer();

		for(Iterator iter = members.values().iterator(); iter.hasNext();) {
			Member member = (Member) iter.next();
			if (member.isChanOp())
				sb.append(",@");
			else if (member.isChanVoice())
				sb.append(",+");
			else
				sb.append(',');
			sb.append(member.getUser().getNick());
		}

		return sb.toString().substring(1); // get rid of leading comma
	}

	public synchronized void sendNames(User usr) {
		StringBuffer sb = new StringBuffer();

		for(Iterator iter = members.values().iterator(); iter.hasNext();) {
			Member member = (Member) iter.next();
			if (member.isChanOp())
				sb.append(" @");
			else if (member.isChanVoice())
				sb.append(" +");
			else
				sb.append(' ');
			sb.append(member.getUser().getNick());
		}
		
		String ournames = sb.toString().substring(1); // get rid of leading space ' '

		String chanPrefix = "=";
		if(isModeSet(CHANMODE_SECRET))
			chanPrefix = "@";
		else if(isModeSet(CHANMODE_PRIVATE))
			chanPrefix = "*";

		Message message = new Message(Constants.RPL_NAMREPLY, usr);
		message.appendParameter(chanPrefix);
		message.appendParameter(name);
		message.appendParameter(ournames);
		usr.send(message);

		message = new Message(Constants.RPL_ENDOFNAMES, usr);
		message.appendParameter(name);
		message.appendParameter("End of /NAMES list");
		usr.send(message);
	}

	/**
	 * Sends a message to this channel, excluding a specified user.
	 */
	public synchronized void send(Message message, User userExcluded) {
		for(Iterator iter = members.keySet().iterator(); iter.hasNext();) {
			User user = (User) iter.next();
			if (!(user.equals(userExcluded))) {
				user.send(message);
			}
		}
	}
	/**
	 * Sends a message to this channel.
	 */
	public synchronized void send(Message message) {
		for(Iterator iter = members.keySet().iterator(); iter.hasNext();) {
			User user = (User) iter.next();
			user.send(message);
		}
	}

	public void setTopic(Source sender, String newTopic) {
		topic = newTopic;
		Message message = new Message(sender, "TOPIC", this);
		message.appendParameter(topic);
		send(message);
	}

	public void processModes(User sender, String modeString, String[] modeParams) {
		if (modeString.equals("+b") && modeParams.length == 0) {
			this.listBans(sender);
			return;
		}

		char[] letters = modeString.toCharArray();

		boolean adding = true; // are we adding modes (+) or subtracting (-)

		int addModes = 0;
		int delModes = 0;

		StringBuffer goodModes = new StringBuffer();
		String[] goodParams = new String[modeParams.length];
		int goodParamsCount = 0;

		int n = 0; // modeParams index

		for (int i = 0; i < letters.length; i++) {
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

			case 'l':
				if (adding) {
					if (n >= modeParams.length) break;
					try {
						int tryLimit = Integer.parseInt(modeParams[n]);
						limit = tryLimit;
						goodParams[goodParamsCount] = modeParams[n];
						goodParamsCount++;
						doDo = true;
					} catch(NumberFormatException nfe) {
					} finally {
						n++; // move on to the next parameter
					}
				} else {
					limit = 0;
					doDo = true;
				}
				break;
			case 'k':
				if (adding) {
					if (n >= modeParams.length) break;
					String tryKey = modeParams[n];
					n++;
					if (Util.isIRCString(tryKey)) {
						key = tryKey;
						goodParams[goodParamsCount] = tryKey;
						goodParamsCount++;
						doDo = true;
					}
				} else {
					if (n >= modeParams.length) break;
					String tryKey = modeParams[n];
					n++;
					if (key.equalsIgnoreCase(tryKey)) {
						key = null;
						goodParams[goodParamsCount] = tryKey;
						goodParamsCount++;
						doDo = true;
					}
				}
				break;
			case 'o':
				if (n >= modeParams.length) break;
				String opName = modeParams[n];
				n++;
				User opWho = Util.findUser(opName);
				if (opWho != null) {
					Member opMe = this.getMember(opWho);
					if (opMe != null) {
						doDo = true;
						goodParams[goodParamsCount] = opName;
						goodParamsCount++;
						if (adding) {
							opMe.setOp(true);
						} else {
							opMe.setOp(false);
						}
					} else {
						Message message = new Message(Constants.ERR_USERNOTINCHANNEL, sender);
						message.appendParameter(opName);
						message.appendParameter(name);
						message.appendParameter("They aren't on that channel");
						sender.send(message);
					}
				} else {
					Message message = new Message(Constants.ERR_NOSUCHNICK, sender);
					message.appendParameter(opName);
					message.appendParameter("No such nick");
					sender.send(message);
				}
				break;
			case 'b':
				if (n >= modeParams.length) break;
				String banMask = modeParams[n];
				n++;
				if (adding) {
					this.addBan(banMask, sender.getNick());
					doDo = true;
					goodParams[goodParamsCount] = banMask;
					goodParamsCount++;
				} else {
					if (this.removeBan(banMask)) {
						doDo = true;
						goodParams[goodParamsCount] = banMask;
						goodParamsCount++;
					}
					else break;
				}
				break;
			case 'v':
				if (n >= modeParams.length) break;
				String vName = modeParams[n];
				n++;
				User vWho = Util.findUser(vName);
				if (vWho != null) {
					Member vMe = this.getMember(vWho);
					if (vMe != null) {
						doDo = true;
						goodParams[goodParamsCount] = vName;
						goodParamsCount++;
						if (adding)
							vMe.setVoice(true);
						else
							vMe.setVoice(false);
					} else {
						Message message = new Message(Constants.ERR_USERNOTINCHANNEL, sender);
						message.appendParameter(vName);
						message.appendParameter(name);
						message.appendParameter("They aren't on that channel");
						sender.send(message);
					}
				} else {
					Message message = new Message(Constants.ERR_NOSUCHNICK, sender);
					message.appendParameter(vName);
					message.appendParameter("No such nick");
					sender.send(message);
				}
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
					Message message = new Message(Constants.ERR_UNKNOWNMODE, sender);
					message.appendParameter(Character.toString(letters[i]));
					message.appendParameter("is unknown mode char to me for "+name);
					sender.send(message);
				}
			}
		}
		
		setMode(addModes);
		unsetMode(delModes);
		
		if (goodModes.length() > 1)
		{
			Message message = new Message(sender, "MODE", this);
			message.appendParameter(goodModes.toString());
			for(int i=0; i<goodParamsCount; i++)
				message.appendParameter(goodParams[i]);
			send(message);
		}
	}

	public synchronized String getModesList()
	{
		StringBuffer retval = new StringBuffer("+");
		StringBuffer morestuff = new StringBuffer();
		
		for (int i = 0; i < MODEFLAGS.length; i += 2)
		{
			if (isModeSet(MODEFLAGS[i+1]))
			{
				retval.append((char)MODEFLAGS[i]);
				if (MODEFLAGS[i] == 'k') morestuff.append(' ').append(this.key);
				else if (MODEFLAGS[i] == 'l') morestuff.append(' ').append(this.limit);
			}
		}
		if (morestuff.length() > 0) retval.append(morestuff);
		return retval.toString();
	}
	
	private int isMode(char modeChar)
	{
		int retval = 0;
		for (int i = 0; i < MODEFLAGS.length; i += 2) {
			if (MODEFLAGS[i] == modeChar)
				retval = MODEFLAGS[i + 1];
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
}
