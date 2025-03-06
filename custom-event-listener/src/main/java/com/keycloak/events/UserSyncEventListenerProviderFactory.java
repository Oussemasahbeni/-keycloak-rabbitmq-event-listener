package com.keycloak.events;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class UserSyncEventListenerProviderFactory
        implements EventListenerProviderFactory {

  private static final String ID = "external-db-sync-rabbitmq";
  private static final Logger log = Logger.getLogger(UserSyncEventListenerProviderFactory.class);

  private ConnectionFactory connectionFactory;
  private Connection connection;
  private Channel channel;



  @Override
  public EventListenerProvider create(KeycloakSession keycloakSession) {
    checkConnectionAndChannel();
    return new UserSyncEventListenerProvider(keycloakSession,channel);
  }

  private synchronized void checkConnectionAndChannel() {
    try {
      if (connection == null || !connection.isOpen()) {
        this.connection = connectionFactory.newConnection();
      }
      if (channel == null || !channel.isOpen()) {
        channel = connection.createChannel();
      }
    }
    catch (IOException | TimeoutException e) {
      log.error("keycloak-to-rabbitmq ERROR on connection to rabbitmq", e);
    }
  }

  @Override
  public void init(Config.Scope scope) {

    this.connectionFactory = new ConnectionFactory();
    connectionFactory.setHost("rabbitmq"); // docker-compose service name
    connectionFactory.setPort(5672);
    connectionFactory.setUsername("admin");
    connectionFactory.setPassword("admin");
    connectionFactory.setAutomaticRecoveryEnabled(true);
  }

  @Override
  public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

  }

  @Override
  public void close() {
    try {
      channel.close();
      connection.close();
    }
    catch (IOException | TimeoutException e) {
      log.error("keycloak-to-rabbitmq ERROR on close", e);
    }
  }

  @Override
  public String getId() {
    return ID;
  }
}