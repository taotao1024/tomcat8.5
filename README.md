## Tomcat8.5.X 源码解析  
IDEA开发工具、JDK1.8、已解决控制台中文乱码问题、500异常。 

## 注意事项 
环境搭建参考 https://www.freesion.com/article/5781852074/  
 
Main Class:   org.apache.catalina.startup.Bootstrap  

## 参数配置
VM options:  
-Dcatalina.home=catalina-home
-Dcatalina.base=catalina-home
-Djava.endorsed.dirs=catalina-home/endorsed
-Djava.io.tmpdir=catalina-home/temp
-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager
-Djava.util.logging.config.file=catalina-home/conf/logging.properties
-Dfile.encoding=utf-8