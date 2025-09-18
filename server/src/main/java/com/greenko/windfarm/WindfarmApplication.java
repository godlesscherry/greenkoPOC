// path: server/src/main/java/com/greenko/windfarm/WindfarmApplication.java
package com.greenko.windfarm;

import com.greenko.windfarm.config.WindfarmProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(WindfarmProperties.class)
public class WindfarmApplication {
  public static void main(String[] args) {
    SpringApplication.run(WindfarmApplication.class, args);
  }
}
