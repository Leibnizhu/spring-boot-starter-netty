# spring-boot-starter-netty
## 简介
一个基于Netty实现的Spring Boot内置Servlet容器

## Maven依赖
先```mvn install```本项目，然后在使用的项目中加入以下依赖：  
```xml
<dependencies>
    <!-- 排除自带的内置Tomcat -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <exclusions>
            <exclusion>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-tomcat</artifactId>
            </exclusion>
        </exclusions>
    </dependency>
    <!-- 引入本项目 -->
    <dependency>
        <groupId>io.gitlab.leibnizhu</groupId>
        <artifactId>spring-boot-starter-netty</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

## 代码设计分析的博文
陆续更新中……  
[基于Netty的Spring Boot内置Servlet容器的实现（一）](http://leibnizhu.gitlab.io/2017/08/24/%E5%9F%BA%E4%BA%8ENetty%E7%9A%84Spring%20Boot%E5%86%85%E7%BD%AEServlet%E5%AE%B9%E5%99%A8%E7%9A%84%E5%AE%9E%E7%8E%B0%EF%BC%88%E4%B8%80%EF%BC%89/)  
[基于Netty的Spring Boot内置Servlet容器的实现（二）](http://leibnizhu.gitlab.io/2017/08/24/%E5%9F%BA%E4%BA%8ENetty%E7%9A%84Spring-Boot%E5%86%85%E7%BD%AEServlet%E5%AE%B9%E5%99%A8%E7%9A%84%E5%AE%9E%E7%8E%B0%EF%BC%88%E4%BA%8C%EF%BC%89/)  
[基于Netty的Spring Boot内置Servlet容器的实现（三）](http://leibnizhu.gitlab.io/2017/08/27/%E5%9F%BA%E4%BA%8ENetty%E7%9A%84Spring-Boot%E5%86%85%E7%BD%AEServlet%E5%AE%B9%E5%99%A8%E7%9A%84%E5%AE%9E%E7%8E%B0%EF%BC%88%E4%B8%89%EF%BC%89/)  

