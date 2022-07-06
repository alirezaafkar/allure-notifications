package guru.qa.allure.notifications.clients.skype;

import guru.qa.allure.notifications.chart.Chart;
import guru.qa.allure.notifications.clients.Notifier;
import guru.qa.allure.notifications.clients.skype.model.Attachment;
import guru.qa.allure.notifications.clients.skype.model.From;
import guru.qa.allure.notifications.clients.skype.model.SkypeMessage;
import guru.qa.allure.notifications.config.base.Base;
import guru.qa.allure.notifications.config.enums.Headers;
import guru.qa.allure.notifications.config.skype.Skype;
import guru.qa.allure.notifications.exceptions.MessageBuildException;
import guru.qa.allure.notifications.exceptions.MessagingException;
import guru.qa.allure.notifications.template.MarkdownTemplate;
import guru.qa.allure.notifications.util.ImageConverter;
import kong.unirest.Unirest;

import java.util.Collections;

public class SkypeClient implements Notifier {
    private final Base base;
    private final Skype skype;

    public SkypeClient(Base base, Skype skype) {
        this.base = base;
        this.skype = skype;
    }

    @Override
    public void sendText() throws MessagingException {
        Unirest.post("https://{url}/apis/v3/conversations/{conversationId}/activities")
                .routeParam("url", host())
                .routeParam("conversationId",
                        skype.getConversationId())
                .header("Content-Type", Headers.JSON.header())
                .header("Authorization", "Bearer " + token())
                .header("Host", host())
                .body(createSimpleMessage())
                .asString()
                .getBody();
    }

    @Override
    public void sendPhoto() throws MessagingException {
        Chart.createChart(base);

        Attachment attachment = Attachment.builder()
                .contentType("image/png")
                .name("chart.png")
                .contentUrl(contentUrl())
                .build();


        SkypeMessage body = createSimpleMessage();
        body.setAttachments(Collections.singletonList(attachment));

        Unirest.post("https://{url}/apis/v3/conversations/{conversationId}/activities")
                .routeParam("url", host())
                .routeParam("conversationId",
                        skype.getConversationId())
                .header("Content-Type", Headers.JSON.header())
                .header("Authorization", "Bearer " + token())
                .header("Host", host())
                .body(body)
                .asString()
                .getBody();
    }

    private SkypeMessage createSimpleMessage() throws MessageBuildException {
        From from = From.builder()
                .id(skype.getBotId())
                .name(skype.getBotName())
                .build();

        return SkypeMessage.builder()
                .type("message")
                .from(from)
                .text(new MarkdownTemplate(base).create())
                .build();
    }

    private String token() {
        return SkypeAuth.bearerToken(skype);
    }

    private String host() {
        return skype.getServiceUrl().substring(0, skype.getServiceUrl().contains("/")
                ? skype.getServiceUrl().indexOf("/") :
                skype.getServiceUrl().length());
    }

    private String contentUrl() {
        return String.join(",", "data:image/png;base64",
                ImageConverter.convertToBase64());
    }
}
