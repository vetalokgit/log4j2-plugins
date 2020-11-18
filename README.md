"# log4j2-plugins" 
Simple log4j2 plugin that accumulates log output to RAM, than flushes to disk.
Put log4j2.xml to karaf/etc folder, built Logger.jar to karaf/system folder, and modify karaf/etc/startup.properties file accordingly 
(put startlevel=8).
The problem is that with karaf 4.0.3 (pax-logging 1.8.4) it works, but with karaf 4.2.10 (pax-logging 1.11.7) it produces duplicated archives.
If you add flush method after write, in RamFileAppender:
zipStream.write(dataToCompress);
zipStream.flush();
the problem should reproduce faster.
It is likely that there shod be some synchronization mechanism, but adding synchronized methods and variables did not help.