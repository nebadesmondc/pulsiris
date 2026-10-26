package dev.pulsiris.registry;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import dev.pulsiris.IntegrationTest;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@IntegrationTest
@RecordApplicationEvents
class TargetApiTests {

	@Autowired
	MockMvcTester mvc;

	@Test
	void registersTargetWithDefaultsAndReturnsLocation() {
		MvcTestResult result = register("""
				{ "name": "%s", "endpointUrl": "https://shop.example.com/health" }
				""".formatted(uniqueName("Checkout")));

		assertThat(result).hasStatus(HttpStatus.CREATED).headers().containsHeader("Location");
		assertThat(result).bodyJson().extractingPath("$.intervalSeconds").isEqualTo(60);
		assertThat(result).bodyJson().extractingPath("$.timeoutMs").isEqualTo(5000);
		assertThat(result).bodyJson().extractingPath("$.httpMethod").isEqualTo("GET");
	}

	@Test
	void returnsRegisteredTargetById() {
		String name = uniqueName("Billing");
		MvcTestResult created = register("""
				{ "name": "%s", "endpointUrl": "https://billing.example.com/ready", "httpMethod": "HEAD" }
				""".formatted(name));
		String location = created.getResponse().getHeader("Location");

		MvcTestResult fetched = mvc.get().uri(location).exchange();

		assertThat(fetched).hasStatusOk();
		assertThat(fetched).bodyJson().extractingPath("$.name").isEqualTo(name);
		assertThat(fetched).bodyJson().extractingPath("$.httpMethod").isEqualTo("HEAD");
	}

	@Test
	void rejectsInvalidTargetWithProblemDetails() {
		MvcTestResult result = register("""
				{ "name": "", "endpointUrl": "ftp://files.example.com", "intervalSeconds": 5 }
				""");

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
			.hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(result).bodyJson().extractingPath("$.errors[*].field")
			.asArray()
			.containsExactly("name", "endpointUrl", "intervalSeconds");
	}

	@Test
	void rejectsDuplicateNameIgnoringCase() {
		String name = uniqueName("Search");
		register("""
				{ "name": "%s", "endpointUrl": "https://search.example.com" }
				""".formatted(name));

		MvcTestResult duplicate = register("""
				{ "name": "%s", "endpointUrl": "https://search2.example.com" }
				""".formatted(name.toUpperCase()));

		assertThat(duplicate).hasStatus(HttpStatus.CONFLICT);
	}

	@Test
	void returnsNotFoundForUnknownTarget() {
		assertThat(mvc.get().uri("/api/targets/{id}", UUID.randomUUID())).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void listsTargetsOnePageAtATime() {
		register("""
				{ "name": "%s", "endpointUrl": "https://a.example.com" }
				""".formatted(uniqueName("Alpha")));
		register("""
				{ "name": "%s", "endpointUrl": "https://b.example.com" }
				""".formatted(uniqueName("Beta")));

		MvcTestResult result = mvc.get().uri("/api/targets?page=0&size=1").exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.content").asArray().hasSize(1);
		assertThat(result).bodyJson().extractingPath("$.page.size").isEqualTo(1);
		assertThat(result).bodyJson().extractingPath("$.page.totalPages").asNumber().satisfies(
				pages -> assertThat(pages.intValue()).isGreaterThanOrEqualTo(2));
	}

	@Test
	void filtersByNameAndEnabledState() {
		String token = UUID.randomUUID().toString().substring(0, 8);
		register("""
				{ "name": "Inventory %s", "endpointUrl": "https://inv.example.com" }
				""".formatted(token));
		register("""
				{ "name": "Inventory %s staging", "endpointUrl": "https://inv-stg.example.com", "enabled": false }
				""".formatted(token));

		MvcTestResult result = mvc.get().uri("/api/targets?name={name}&enabled=false", token.toUpperCase()).exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.content[*].name")
			.asArray()
			.containsExactly("Inventory " + token + " staging");
	}

	@Test
	void capsPageSizeAtTheConfiguredMaximum() {
		assertThat(mvc.get().uri("/api/targets?size=500")).hasStatusOk()
			.bodyJson()
			.extractingPath("$.page.size")
			.isEqualTo(100);
	}

	@Test
	void rejectsSortingByUnsupportedProperty() {
		assertThat(mvc.get().uri("/api/targets?sort=endpointUrl,desc")).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.errors[0].field")
			.isEqualTo("sort");
	}

	@Test
	void publishesOpenApiDocumentation() {
		assertThat(mvc.get().uri("/v3/api-docs")).hasStatusOk()
			.bodyJson()
			.extractingPath("$.paths['/api/targets/{id}'].delete.summary")
			.isEqualTo("Remove a target");
	}

	@Test
	void updatesTargetSettings() {
		String location = register("""
				{ "name": "%s", "endpointUrl": "https://orders.example.com" }
				""".formatted(uniqueName("Orders"))).getResponse().getHeader("Location");
		String newName = uniqueName("Orders");

		MvcTestResult updated = mvc.put()
			.uri(location)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{ "name": "%s", "endpointUrl": "https://orders.example.com/v2", "intervalSeconds": 30 }
					""".formatted(newName))
			.exchange();

		assertThat(updated).hasStatusOk();
		assertThat(updated).bodyJson().extractingPath("$.name").isEqualTo(newName);
		assertThat(updated).bodyJson().extractingPath("$.intervalSeconds").isEqualTo(30);
		assertThat(mvc.get().uri(location)).bodyJson().extractingPath("$.intervalSeconds").isEqualTo(30);
	}

	@Test
	void rejectsUpdateThatTakesAnotherTargetsName() {
		String taken = uniqueName("Taken");
		register("""
				{ "name": "%s", "endpointUrl": "https://taken.example.com" }
				""".formatted(taken));
		String location = register("""
				{ "name": "%s", "endpointUrl": "https://other.example.com" }
				""".formatted(uniqueName("Other"))).getResponse().getHeader("Location");

		MvcTestResult result = mvc.put()
			.uri(location)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{ "name": "%s", "endpointUrl": "https://other.example.com" }
					""".formatted(taken))
			.exchange();

		assertThat(result).hasStatus(HttpStatus.CONFLICT);
	}

	@Test
	void removesTargetAndPublishesEvent(ApplicationEvents events) {
		String location = register("""
				{ "name": "%s", "endpointUrl": "https://legacy.example.com" }
				""".formatted(uniqueName("Legacy"))).getResponse().getHeader("Location");

		assertThat(mvc.delete().uri(location)).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(mvc.get().uri(location)).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(mvc.delete().uri(location)).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(events.stream(TargetRemoved.class))
			.singleElement()
			.satisfies(event -> assertThat(location).endsWith(event.targetId().toString()));
	}

	private MvcTestResult register(String json) {
		return mvc.post().uri("/api/targets").contentType(MediaType.APPLICATION_JSON).content(json).exchange();
	}

	private static String uniqueName(String prefix) {
		return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
	}

}
