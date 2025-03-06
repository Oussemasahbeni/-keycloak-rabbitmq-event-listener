package com.keycloak.events;

import com.rabbitmq.client.Channel;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;

import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

import static com.keycloak.events.UserSyncEventListenerProviderFactory.QUEUE_NAME;

public class UserSyncEventListenerProvider
        implements EventListenerProvider {


    private static final Logger log = Logger.getLogger(UserSyncEventListenerProvider.class);
    private final KeycloakSession session;
    private final RealmProvider model;
    private final Channel channel;


    public UserSyncEventListenerProvider(KeycloakSession session, Channel channel) {
        this.session = session;
        this.model = session.realms();
        this.channel = channel;
    }

    @Override
    public void onEvent(Event event) {

        if (EventType.REGISTER.equals(event.getType())) {
            log.infof("New %s Event", event.getType());
            log.infof("onEvent-> %s", toString(event));
            event.getDetails().forEach((key, value) -> log.infof("%s : %s", key, value));
            UserModel user = getUser(event);
            sendUserToQueue(user);
        }

        if(EventType.DELETE_ACCOUNT.equals(event.getType())) {
            log.info("User account deleted");
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {

    }


    private UserModel getUser(Event event) {
        RealmModel realm = this.model.getRealm(event.getRealmId());
       return  this.session.users().getUserById(realm, event.getUserId());
    }

    private void sendUserToQueue(UserModel user) {
        log.info("Sending user to queue");
        user.getGroupsStream().forEach(groupModel -> log.infof("Group: %s", groupModel.getName()));
        user.getRealmRoleMappingsStream().forEach(roleModel -> log.infof("Role: %s", roleModel.getName()));
        String message = """
                {
                    "id": "%s",
                    "email": "%s",
                    "username": "%s",
                    "firstName": "%s",
                    "lastName": "%s",
                    "enabled": %s,
                    "emailVerified": %s,
                }
                """.formatted(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.isEnabled(),
                user.isEmailVerified());

        try {
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
            log.info("User sync message published to queue: " + message);
        } catch (Exception e) {
            log.error("Failed to publish user sync message: " + e.getMessage());
        }
    }


    @Override
    public void close() {
    }


    private String toString(Event event) {
        StringJoiner joiner = new StringJoiner(", ");

        joiner.add("type=" + event.getType())
                .add("realmId=" + event.getRealmId())
                .add("clientId=" + event.getClientId())
                .add("userId=" + event.getUserId())
                .add("ipAddress=" + event.getIpAddress());

        if (event.getError() != null) {
            joiner.add("error=" + event.getError());
        }

        if (event.getDetails() != null) {
            event.getDetails().forEach((key, value) -> {
                if (value == null || !value.contains(" ")) {
                    joiner.add(key + "=" + value);
                } else {
                    joiner.add(key + "='" + value + "'");
                }
            });
        }

        return joiner.toString();
    }

    private String toString(AdminEvent event) {
        RealmModel realm = this.model.getRealm(event.getRealmId());
        UserModel newRegisteredUser = this.session.users().getUserById(realm, event.getAuthDetails().getUserId());

        StringJoiner joiner = new StringJoiner(", ");

        joiner.add("operationType=" + event.getOperationType())
                .add("realmId=" + event.getAuthDetails().getRealmId())
                .add("clientId=" + event.getAuthDetails().getClientId())
                .add("userId=" + event.getAuthDetails().getUserId());

        if (newRegisteredUser != null) {
            joiner.add("email=" + newRegisteredUser.getEmail())
                    .add("username=" + newRegisteredUser.getUsername())
                    .add("firstName=" + newRegisteredUser.getFirstName())
                    .add("lastName=" + newRegisteredUser.getLastName());
        }

        joiner.add("ipAddress=" + event.getAuthDetails().getIpAddress())
                .add("resourcePath=" + event.getResourcePath());

        if (event.getError() != null) {
            joiner.add("error=" + event.getError());
        }

        return joiner.toString();
    }

}