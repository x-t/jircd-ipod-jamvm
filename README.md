# jIRCd for JamVM 1.5.0

jIRCd 0.4.1-beta (2004) port for primitive (iPod-2G/iOS-4.2.1) JamVM (jamvm-1.5.0/java-5) systems.

## Changes

* Removes dependencies
* Disables logging

## Setting up/running

Binary included as `jircd.jar`, edit appropriate configs.

## launchd service

```
mv org.zxyz.jircd.plist /Library/LaunchDaemons/org.zxyz.jircd.plist
launchctl load /Library/LaunchDaemons/org.zxyz.jircd.plist
launchctl start org.zxyz.jircd
```

## Compiling

Preferably do it inside a container, like [apsl/java5](https://hub.docker.com/r/apsl/java5/dockerfile).

Use `ant` to build a binary.

Find it [here](https://apache.mirror.serveriai.lt//ant/binaries/apache-ant-1.9.16-bin.zip)
