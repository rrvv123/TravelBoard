package kr.ac.kopo.board;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class TravelBoardApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelBoardApplication.class, args);
    }

}
