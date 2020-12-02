package org.folio.pv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ModPasswordValidatorApplication {

  public static void main(String[] args) {
    SpringApplication.run(ModPasswordValidatorApplication.class, args);
  }

}
