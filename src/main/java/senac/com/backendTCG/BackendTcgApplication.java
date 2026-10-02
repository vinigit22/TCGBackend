package senac.com.backendTCG;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling liga o TorneioScheduler (encerramento de inscricoes e lembretes)
@EnableScheduling
@SpringBootApplication
public class BackendTcgApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendTcgApplication.class, args);
	}

}
