package digital.pragmatech.service.mailchimp;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import digital.pragmatech.config.ApiConfiguration;
import digital.pragmatech.model.mailchimp.MailchimpList;
import digital.pragmatech.model.mailchimp.MailchimpMember;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

@SpringBootTest
@TestPropertySource(properties = {"api.mailchimp.base-url=http://localhost:8089/3.0"})
class MailchimpServiceIntegrationTest {

  @Autowired private ApiConfiguration apiConfiguration;

  @Autowired private RestClient.Builder restClientBuilder;

  private WireMockServer wireMockServer;
  private MailchimpService mailchimpService;

  @BeforeEach
  void setUp() {
    wireMockServer = new WireMockServer(8089);
    wireMockServer.start();
    WireMock.configureFor("localhost", 8089);

    // Configure test API key
    apiConfiguration.getMailchimp().setApiKey("test-key-us1");
    apiConfiguration.getMailchimp().setDatacenter("us1");
    // Override the base URL to use our WireMock server
    apiConfiguration.getMailchimp().setBaseUrl("http://localhost:8089/3.0");

    MailchimpApiClient apiClient = new MailchimpApiClient(restClientBuilder, apiConfiguration);
    mailchimpService = new MailchimpService(apiClient);
  }

  @AfterEach
  void tearDown() {
    wireMockServer.stop();
  }

  @Test
  void testConnectionSuccess() {
    // Given
    stubFor(
        get(urlEqualTo("/3.0/"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                            {
                                "account_id": "test-account-123",
                                "account_name": "Test Account"
                            }
                            """)));

    // When
    boolean connected = mailchimpService.testConnection();

    // Then
    assertThat(connected).isTrue();
  }

  @Test
  void getAllLists() {
    // Given
    stubFor(
        get(urlMatching("/3.0/lists.*"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                            {
                                "lists": [
                                    {
                                        "id": "list123",
                                        "name": "Test Newsletter",
                                        "web_id": 123,
                                        "permission_reminder": "You are receiving this email because you signed up.",
                                        "use_archive_bar": true,
                                        "double_optin": false,
                                        "has_welcome": true,
                                        "marketing_permissions": false,
                                        "stats": {
                                            "member_count": 100,
                                            "unsubscribe_count": 5
                                        }
                                    }
                                ],
                                "total_items": 1
                            }
                            """)));

    // When
    List<MailchimpList> lists = mailchimpService.getAllLists();

    // Then
    assertThat(lists).hasSize(1);
    MailchimpList list = lists.get(0);
    assertThat(list.getId()).isEqualTo("list123");
    assertThat(list.getName()).isEqualTo("Test Newsletter");
    assertThat(list.getWebId()).isEqualTo(123);
    assertThat(list.isDoubleOptin()).isFalse();
    assertThat(list.isHasWelcome()).isTrue();
  }

  @Test
  void getAllMembers() {
    // Given
    String listId = "list123";
    stubFor(
        get(urlMatching("/3.0/lists/" + listId + "/members.*"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                            {
                                "members": [
                                    {
                                        "id": "member123",
                                        "email_address": "test@example.com",
                                        "unique_email_id": "unique123",
                                        "web_id": 456,
                                        "email_type": "html",
                                        "status": "subscribed",
                                        "merge_fields": {
                                            "FNAME": "John",
                                            "LNAME": "Doe"
                                        },
                                        "interests": {},
                                        "stats": {
                                            "avg_open_rate": 0.25,
                                            "avg_click_rate": 0.05
                                        },
                                        "language": "en",
                                        "vip": false,
                                        "source": "API",
                                        "tags_count": 2,
                                        "list_id": "list123"
                                    }
                                ],
                                "total_items": 1
                            }
                            """)));

    // When
    List<MailchimpMember> members = mailchimpService.getAllMembers(listId);

    // Then
    assertThat(members).hasSize(1);
    MailchimpMember member = members.get(0);
    assertThat(member.getId()).isEqualTo("member123");
    assertThat(member.getEmailAddress()).isEqualTo("test@example.com");
    assertThat(member.getStatus()).isEqualTo("subscribed");
    assertThat(member.getMergeFields().get("FNAME")).isEqualTo("John");
    assertThat(member.getMergeFields().get("LNAME")).isEqualTo("Doe");
    assertThat(member.isVip()).isFalse();
    assertThat(member.getTagsCount()).isEqualTo(2);
  }

  @Test
  void getAllTags() {
    // Given
    String listId = "list123";

    // Mock segments response
    stubFor(
        get(urlMatching("/3.0/lists/" + listId + "/segments.*"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                            {
                                "segments": [
                                    {
                                        "id": "segment1",
                                        "name": "VIP Customers"
                                    },
                                    {
                                        "id": "segment2",
                                        "name": "Newsletter Subscribers"
                                    }
                                ]
                            }
                            """)));

    // Mock interest categories response
    stubFor(
        get(urlMatching("/3.0/lists/" + listId + "/interest-categories.*"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                            {
                                "categories": [
                                    {
                                        "id": "category1",
                                        "title": "Product Interests"
                                    }
                                ]
                            }
                            """)));

    // Mock interests within category response
    stubFor(
        get(urlMatching("/3.0/lists/" + listId + "/interest-categories/category1/interests.*"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                            {
                                "interests": [
                                    {
                                        "id": "interest1",
                                        "name": "Electronics"
                                    },
                                    {
                                        "id": "interest2",
                                        "name": "Books"
                                    }
                                ]
                            }
                            """)));

    // When
    List<String> tags = mailchimpService.getAllTags(listId);

    // Then
    assertThat(tags)
        .containsExactlyInAnyOrder(
            "VIP Customers", "Newsletter Subscribers", "Product Interests", "Electronics", "Books");
  }
}
