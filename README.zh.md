# spring-boot-starter-netty
[[English]](https://github.com/Leibnizhu/spring-boot-starter-netty/blob/master/README.md) [[中文]](https://github.com/Leibnizhu/spring-boot-starter-netty/blob/master/README.zh.md)  
## 简介
一个基于Netty(4.1.12.Final)实现的Spring Boot内置Servlet容器。  
本项目已经发布到Maven中央仓库，参见[The Central Repository](http://search.maven.org/#artifactdetails%7Cio.gitlab.leibnizhu%7Cspring-boot-starter-netty%7C1.0%7Cjar)。  


## Maven依赖
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
        <version>1.1</version>
    </dependency>
</dependencies>
```
2. 给Spring-Boot应用的入口类的`@SpringBootApplication`注解增加`scanBasePackages`属性，如下：  
```java
@SpringBootApplication(scanBasePackages = {"io.gitlab.leibnizhu", "your.package.name"})
@EnableScheduling
public class AwpApplication extends SpringBootServletInitializer {
    public AwpApplication() {
    }

    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(new Class[]{AwpApplication.class});
    }

    public static void main(String[] args) {
        SpringApplication.run(AwpApplication.class, args);
    }
}
```
3. 启动Spring-Boot应用。

## 代码设计分析的博文
陆续更新中……  
[基于Netty的Spring Boot内置Servlet容器的实现（一）](https://leibnizhu.github.io/p/%E5%9F%BA%E4%BA%8ENetty%E7%9A%84Spring-Boot%E5%86%85%E7%BD%AEServlet%E5%AE%B9%E5%99%A8%E7%9A%84%E5%AE%9E%E7%8E%B0%E4%B8%80/)  
[基于Netty的Spring Boot内置Servlet容器的实现（二）](https://leibnizhu.github.io/p/%E5%9F%BA%E4%BA%8ENetty%E7%9A%84Spring-Boot%E5%86%85%E7%BD%AEServlet%E5%AE%B9%E5%99%A8%E7%9A%84%E5%AE%9E%E7%8E%B0%E4%BA%8C/)  
[基于Netty的Spring Boot内置Servlet容器的实现（三）](https://leibnizhu.github.io/p/%E5%9F%BA%E4%BA%8ENetty%E7%9A%84Spring-Boot%E5%86%85%E7%BD%AEServlet%E5%AE%B9%E5%99%A8%E7%9A%84%E5%AE%9E%E7%8E%B0%E4%B8%89/)  
[基于Netty的Spring Boot内置Servlet容器的实现（四）](https://leibnizhu.github.io/p/%E5%9F%BA%E4%BA%8ENetty%E7%9A%84Spring-Boot%E5%86%85%E7%BD%AEServlet%E5%AE%B9%E5%99%A8%E7%9A%84%E5%AE%9E%E7%8E%B0%E5%9B%9B/)  
[基于Netty的Spring Boot内置Servlet容器的实现（五）](https://leibnizhu.github.io/p/%E5%9F%BA%E4%BA%8ENetty%E7%9A%84Spring-Boot%E5%86%85%E7%BD%AEServlet%E5%AE%B9%E5%99%A8%E7%9A%84%E5%AE%9E%E7%8E%B0%E4%BA%94/)

