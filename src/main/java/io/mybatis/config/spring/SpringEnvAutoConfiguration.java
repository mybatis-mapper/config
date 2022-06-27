package io.mybatis.config.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringEnvAutoConfiguration {

  @Bean
  public SpringEnvUtil springEnvUtil() {
    return new SpringEnvUtil();
  }

}
