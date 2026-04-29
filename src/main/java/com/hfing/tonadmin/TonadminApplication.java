package com.hfing.tonadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class TonadminApplication {

    public static void main(String[] args) {
        SpringApplication.run(TonadminApplication.class, args);
    }

}
