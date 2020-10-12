
echo off

Rem For this to work, you need the Java bin\ directory to be in your path.
Rem You typically find Java here: C:\Program Files (x86)\Java\jre7
Rem So, this should be added to your path: C:\Program Files (x86)\Java\jre7\bin
java -cp .\deploy\MDMonitor-ueber-0.2.3.jar -Dlog4j.configurationFile=etc\log4j2.xml com.bormannqds.mds.tools.main.MDMonitor
