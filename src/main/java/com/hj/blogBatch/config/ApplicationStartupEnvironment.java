package com.hj.blogBatch.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ApplicationStartupEnvironment implements ApplicationListener<ApplicationStartedEvent> {

    private final Environment env;

    @Autowired
    public ApplicationStartupEnvironment(Environment env) {
        this.env = env;
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        log.info("-------- env :"+env.toString());
       /*  String username = env.getProperty("test.username");
        String password = env.getProperty("test.password");
        System.out.println("username = " + username);
        System.out.println("password = " + password); */
    }

    public Environment getEnv() {
        return env;
    }

    
}
