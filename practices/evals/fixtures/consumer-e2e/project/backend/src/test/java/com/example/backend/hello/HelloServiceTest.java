package com.example.backend.hello;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

// This project's OWN test -- not part of any ax-transform-installed ArchUnit gate.
// verify-downstream.sh's A8 assertion (test-results/test/*.xml tests > 0) needs at
// least one project-owned test so a green `./gradlew test` cannot be vacuous.
class HelloServiceTest {

	private final HelloService helloService = new HelloService();

	@Test
	void greetsByName() {
		assertThat(helloService.greet("Ax")).isEqualTo("Hello, Ax!");
	}

}
