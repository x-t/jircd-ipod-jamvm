rem This script is intended for Windows systems,
rem but is easy to adapt to other operating systems.

@echo off
java -Dlog4j.configuration=file:log4j.xml -cp log4j.jar;jircd.jar jircd.jIRCd jircd.properties
rem SSL
rem java -Djavax.net.ssl.keyStore=jircd.jks -Djavax.net.ssl.keyStorePassword=passphrase -Dlog4j.configuration=file:log4j.xml -cp log4j.jar;jircd.jar jircd.jIRCd jircd.properties
