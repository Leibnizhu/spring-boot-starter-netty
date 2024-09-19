# spring-boot-starter-netty
[[English]](https://github.com/Leibnizhu/spring-boot-starter-netty/blob/master/README.md) [[中文]](https://github.com/Leibnizhu/spring-boot-starter-netty/blob/master/README.zh.md)  

## 简介
*   一个基于Netty实现的Spring Boot内置Servlet容器。
*   仅支持 Netty 4.1.41.Final 及以上版本，可以在项目pom.xml文件中指定使用的版本号。

## 构建
1. 获取工程并安装到本地：
    ```shell
    $ mvn clean install
    ```

1. 在你的Spring-Boot项目中加入以下依赖：  
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
        <version>1.2</version>
    </dependency>
    <!-- 指定项目使用的Netty版本。需要注意兼容其他基于Netty的依赖 -->
    <dependency>
        <groupId>io.netty</groupId>
        <artifactId>netty-all</artifactId>
        <version>4.1.113.Final</version>
    </dependency>
</dependencies>
```

## 代码设计分析的博文
陆续更新中……  
[基于Netty的Spring Boot内置Servlet容器的实现（一）](https://leibnizhu.github.io/p/%E5%9F%BA%E4%BA%8ENetty%E7%9A%84Spring-Boot%E5%86%85%E7%BD%AEServlet%E5%AE%B9%E5%99%A8%E7%9A%84%E5%AE%9E%E7%8E%B0%E4%B8%80/)  
[基于Netty的Spring Boot内置Servlet容器的实现（二）](https://leibnizhu.github.io/p/%E5%9F%BA%E4%BA%8ENetty%E7%9A%84Spring-Boot%E5%86%85%E7%BD%AEServlet%E5%AE%B9%E5%99%A8%E7%9A%84%E5%AE%9E%E7%8E%B0%E4%BA%8C/)  
[基于Netty的Spring Boot内置Servlet容器的实现（三）](https://leibnizhu.github.io/p/%E5%9F%BA%E4%BA%8ENetty%E7%9A%84Spring-Boot%E5%86%85%E7%BD%AEServlet%E5%AE%B9%E5%99%A8%E7%9A%84%E5%AE%9E%E7%8E%B0%E4%B8%89/)  
[基于Netty的Spring Boot内置Servlet容器的实现（四）](https://leibnizhu.github.io/p/%E5%9F%BA%E4%BA%8ENetty%E7%9A%84Spring-Boot%E5%86%85%E7%BD%AEServlet%E5%AE%B9%E5%99%A8%E7%9A%84%E5%AE%9E%E7%8E%B0%E5%9B%9B/)  
[基于Netty的Spring Boot内置Servlet容器的实现（五）](https://leibnizhu.github.io/p/%E5%9F%BA%E4%BA%8ENetty%E7%9A%84Spring-Boot%E5%86%85%E7%BD%AEServlet%E5%AE%B9%E5%99%A8%E7%9A%84%E5%AE%9E%E7%8E%B0%E4%BA%94/)

