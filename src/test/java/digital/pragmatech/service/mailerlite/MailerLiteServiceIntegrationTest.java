package digital.pragmatech.service.mailerlite;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import digital.pragmatech.config.ApiConfiguration;
import digital.pragmatech.model.common.Subscriber;
import digital.pragmatech.model.mailerlite.MailerLiteGroup;
import digital.pragmatech.model.mailerlite.MailerLiteSubscriber;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
    "api.mailerlite.base-url=http://localhost:8090/api"
})
class MailerLiteServiceIntegrationTest {

    @Autowired
    private ApiConfiguration apiConfiguration;
    
    @Autowired
    private RestClient.Builder restClientBuilder;
    
    private WireMockServer wireMockServer;
    private MailerLiteService mailerLiteService;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(8090);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8090);
        
        // Configure test API token
        apiConfiguration.getMailerlite().setApiToken("test-token-123");
        // Override the base URL to use our WireMock server
        apiConfiguration.getMailerlite().setBaseUrl("http://localhost:8090/api");
        
        MailerLiteApiClient apiClient = new MailerLiteApiClient(restClientBuilder, apiConfiguration);
        mailerLiteService = new MailerLiteService(apiClient);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void testConnectionSuccess() {
        // Given
        stubFor(get(urlEqualTo("/api/me"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "data": {
                                    "id": "123456",
                                    "email": "test@example.com",
                                    "name": "Test User"
                                }
                            }
                            """)));

        // When
        boolean connected = mailerLiteService.testConnection();

        // Then
        assertThat(connected).isTrue();
    }

    @Test
    void createGroup() {
        // Given
        String groupName = "Test Group";
        stubFor(post(urlEqualTo("/api/groups"))
                .withRequestBody(containing("Test Group"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "data": {
                                    "id": "group123",
                                    "name": "Test Group",
                                    "active_count": 0,
                                    "sent_count": 0,
                                    "opens_count": 0,
                                    "clicks_count": 0,
                                    "unsubscribed_count": 0,
                                    "unconfirmed_count": 0,
                                    "bounced_count": 0,
                                    "junk_count": 0
                                }
                            }
                            """)));

        // When
        MailerLiteGroup group = mailerLiteService.createGroup(groupName);

        // Then
        assertThat(group).isNotNull();
        assertThat(group.getId()).isEqualTo("group123");
        assertThat(group.getName()).isEqualTo("Test Group");
        assertThat(group.getActiveCount()).isEqualTo(0);
    }

    @Test
    void getAllGroups() {
        // Given
        stubFor(get(urlEqualTo("/api/groups"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "data": [
                                    {
                                        "id": "group1",
                                        "name": "Newsletter Subscribers",
                                        "active_count": 150,
                                        "sent_count": 10,
                                        "opens_count": 120,
                                        "clicks_count": 45
                                    },
                                    {
                                        "id": "group2",
                                        "name": "VIP Customers",
                                        "active_count": 25,
                                        "sent_count": 5,
                                        "opens_count": 22,
                                        "clicks_count": 18
                                    }
                                ]
                            }
                            """)));

        // When
        List<MailerLiteGroup> groups = mailerLiteService.getAllGroups();

        // Then
        assertThat(groups).hasSize(2);
        
        MailerLiteGroup group1 = groups.get(0);
        assertThat(group1.getId()).isEqualTo("group1");
        assertThat(group1.getName()).isEqualTo("Newsletter Subscribers");
        assertThat(group1.getActiveCount()).isEqualTo(150);
        
        MailerLiteGroup group2 = groups.get(1);
        assertThat(group2.getId()).isEqualTo("group2");
        assertThat(group2.getName()).isEqualTo("VIP Customers");
        assertThat(group2.getActiveCount()).isEqualTo(25);
    }

    @Test
    void createOrUpdateSubscriber() {
        // Given
        Subscriber subscriber = Subscriber.builder()
                .email("john.doe@example.com")
                .firstName("John")
                .lastName("Doe")
                .status(Subscriber.SubscriberStatus.SUBSCRIBED)
                .build();

        stubFor(post(urlEqualTo("/api/subscribers"))
                .withRequestBody(containing("john.doe@example.com"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "data": {
                                    "id": "subscriber123",
                                    "email": "john.doe@example.com",
                                    "status": "active",
                                    "source": "api",
                                    "fields": {
                                        "name": "John",
                                        "last_name": "Doe"
                                    }
                                }
                            }
                            """)));

        // When
        MailerLiteSubscriber result = mailerLiteService.createOrUpdateSubscriber(subscriber);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("subscriber123");
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(result.getStatus()).isEqualTo("active");
        assertThat(result.getFields().get("name")).isEqualTo("John");
        assertThat(result.getFields().get("last_name")).isEqualTo("Doe");
    }

    @Test
    void assignSubscriberToGroup() {
        // Given
        String subscriberId = "subscriber123";
        String groupId = "group456";

        stubFor(post(urlEqualTo("/api/subscribers/" + subscriberId + "/groups/" + groupId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"success\": true}")));

        // When & Then - should not throw exception
        mailerLiteService.assignSubscriberToGroup(subscriberId, groupId);
        
        // Verify the request was made
        verify(postRequestedFor(urlEqualTo("/api/subscribers/" + subscriberId + "/groups/" + groupId)));
    }

    @Test
    void bulkImportSubscribers() {
        // Given
        List<Subscriber> subscribers = List.of(
                Subscriber.builder()
                        .email("user1@example.com")
                        .firstName("User")
                        .lastName("One")
                        .status(Subscriber.SubscriberStatus.SUBSCRIBED)
                        .build(),
                Subscriber.builder()
                        .email("user2@example.com")
                        .firstName("User")
                        .lastName("Two")
                        .status(Subscriber.SubscriberStatus.SUBSCRIBED)
                        .build()
        );
        String groupId = "group123";

        stubFor(post(urlEqualTo("/api/groups/" + groupId + "/import-subscribers"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "data": {
                                    "id": "import123",
                                    "status": "processing",
                                    "total": 2
                                }
                            }
                            """)));

        // When & Then - should not throw exception
        mailerLiteService.bulkImportSubscribers(subscribers, groupId);
        
        // Verify the request was made with correct data
        verify(postRequestedFor(urlEqualTo("/api/groups/" + groupId + "/import-subscribers"))
                .withRequestBody(containing("user1@example.com"))
                .withRequestBody(containing("user2@example.com")));
    }
}