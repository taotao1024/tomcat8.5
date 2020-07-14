## tomcat8.5
Tomcat8.5 源码解析  
IDEA开发工具、JDK1.8、已解决中文乱码问题、500异常  
环境搭建参考 https://www.freesion.com/article/5781852074/   
Main Class:  
org.apache.catalina.startup.Bootstrap  
VM options:  
-Dcatalina.home=catalina-home
-Dcatalina.base=catalina-home
-Djava.endorsed.dirs=catalina-home/endorsed
-Djava.io.tmpdir=catalina-home/temp
-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager
-Djava.util.logging.config.file=catalina-home/conf/logging.properties
-Dfile.encoding=utf-8