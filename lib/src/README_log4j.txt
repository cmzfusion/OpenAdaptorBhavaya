This patched version of log4j 1.2.15 has a modification to the DOMConfigurator class which 
fixes a problem caused by the security patch applied to jdk 1.5.0_16 and 1.6.0_07

This patch prevented log4j from loading config xml files from a jar files under webstart.
For more details see:

http://www.objectdefinitions.com/odblog/2008/fix-for-log4j-bug-45704-failed-to-load-loggingxml-for-jre-150_16-and-webstart/
https://issues.apache.org/bugzilla/show_bug.cgi?id=45704

Note- this patch at least allows the load of a self-contained config.xml -
but it will still not work if the config.xml contains references to external entities with relative URL
e.g. <!ENTITY testEntity SYSTEM "relativeEntity.xml" >









