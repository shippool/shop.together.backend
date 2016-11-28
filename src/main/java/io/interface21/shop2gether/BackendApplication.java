package io.interface21.shop2gether;

import java.util.Collections;

import org.ameba.annotation.EnableAspects;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableJpaRepositories
@EnableJpaAuditing
@EnableTransactionManagement
@EnableAspects
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@Bean
	CommandLineRunner clr(OwnerRepository repo, UserGroupRepository ugRepo) {
		return  args -> {
			Owner heiko = repo.save(new Owner("heiko", "4711", "heiko@home.com", true));
			UserGroup ug = ugRepo.save(new UserGroup(heiko, "Family", Collections.emptyList()));
			TextNote title = new TextNote("Title", "1 x Eggs", null, false);
			title.getSharedWith().add(ug);
			heiko.getItems().add(title);
			repo.save(heiko);
		};
	}
}
