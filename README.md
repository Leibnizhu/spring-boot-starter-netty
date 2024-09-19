# spring-boot-starter-netty
[[English]](https://github.com/Leibnizhu/spring-boot-starter-netty/blob/master/README.md) [[中文]](https://github.com/Leibnizhu/spring-boot-starter-netty/blob/master/README.zh.md)  
## Introduction
*   This is a Spring Boot embedded servlet container project base on netty API.
*   Only supports Netty versions greater than 4.1.41.Final, you can specify
    the version number to be used in the pom.xml file of the project.

## Maven Dependencies
1. Clone project and install:
    ```shell
    $ mvn clean install
    ```

1. add the dependencies below to your maven project:
```xml
<dependencies>
    <!-- exludes embedded Tomcat -->
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
    <!-- include this netty servlet container -->
    <dependency>
        <groupId>io.gitlab.leibnizhu</groupId>
        <artifactId>spring-boot-starter-netty</artifactId>
        <version>1.1-RELEASE</version>
    </dependency>
    <!-- Specify Netty version. It is necessary to compatibility with other Netty-based dependencies -->
    <dependency>
        <groupId>io.netty</groupId>
        <artifactId>netty-all</artifactId>
        <version>4.1.113.Final</version>
    </dependency>
</dependencies>
```

## Blogs about how to design/code this project
Only Chineses, updating one after another……  
[基于Netty的Spring Boot内置Servlet容器的实现（一）](https://leibnizhu.github.io/p/%E5%9F%BA%E4%BA%8ENetty%E7%9A%84Spring-Boot%E5%86%85%E7%BD%AEServlet%E5%AE%B9%E5%99%A8%E7%9A%84%E5%AE%9E%E7%8E%B0%E4%B8%80/)  
[基于Netty的Spring Boot内置Servlet容器的实现（二）](https://leibnizhu.github.io/p/%E5%9F%BA%E4%BA%8ENetty%E7%9A%84Spring-Boot%E5%86%85%E7%BD%AEServlet%E5%AE%B9%E5%99%A8%E7%9A%84%E5%AE%9E%E7%8E%B0%E4%BA%8C/)  
[基于Netty的Spring Boot内置Servlet容器的实现（三）](https://leibnizhu.github.io/p/%E5%9F%BA%E4%BA%8ENetty%E7%9A%84Spring-Boot%E5%86%85%E7%BD%AEServlet%E5%AE%B9%E5%99%A8%E7%9A%84%E5%AE%9E%E7%8E%B0%E4%B8%89/)  
[基于Netty的Spring Boot内置Servlet容器的实现（四）](https://leibnizhu.github.io/p/%E5%9F%BA%E4%BA%8ENetty%E7%9A%84Spring-Boot%E5%86%85%E7%BD%AEServlet%E5%AE%B9%E5%99%A8%E7%9A%84%E5%AE%9E%E7%8E%B0%E5%9B%9B/)  
[基于Netty的Spring Boot内置Servlet容器的实现（五）](https://leibnizhu.github.io/p/%E5%9F%BA%E4%BA%8ENetty%E7%9A%84Spring-Boot%E5%86%85%E7%BD%AEServlet%E5%AE%B9%E5%99%A8%E7%9A%84%E5%AE%9E%E7%8E%B0%E4%BA%94/)
