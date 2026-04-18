package com.draff1800.booking_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.draff1800.booking_service.messaging.RabbitMqMessagingTopology;

import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "app.messaging.rabbit.enabled", havingValue = "true")
public class RabbitMqConfig {

  @Bean
  public Declarables bookingMessagingDeclarables() {
    Queue bookingConfirmedQueue = new Queue(
      RabbitMqMessagingTopology.BOOKING_CONFIRMED_QUEUE,
      true,
      false,
      false,
      Map.of(
        "x-dead-letter-exchange", RabbitMqMessagingTopology.BOOKING_EVENTS_DLX,
        "x-dead-letter-routing-key", RabbitMqMessagingTopology.BOOKING_CONFIRMED_DLQ_ROUTING_KEY
      )
    );

    Queue bookingConfirmedDlq = new Queue(RabbitMqMessagingTopology.BOOKING_CONFIRMED_DLQ, true);

    TopicExchange bookingEventsExchange = new TopicExchange(RabbitMqMessagingTopology.BOOKING_DOMAIN_EVENTS_EXCHANGE, true, false);
    DirectExchange bookingEventsDlx = new DirectExchange(RabbitMqMessagingTopology.BOOKING_EVENTS_DLX, true, false);

    Binding mainBinding = BindingBuilder
      .bind(bookingConfirmedQueue)
      .to(bookingEventsExchange)
      .with(RabbitMqMessagingTopology.BOOKING_CONFIRMED_ROUTING_KEY);

    Binding dlqBinding = BindingBuilder
      .bind(bookingConfirmedDlq)
      .to(bookingEventsDlx)
      .with(RabbitMqMessagingTopology.BOOKING_CONFIRMED_DLQ_ROUTING_KEY);

    return new Declarables(bookingEventsExchange, bookingEventsDlx, bookingConfirmedQueue, bookingConfirmedDlq, mainBinding, dlqBinding);
  }

  @Bean
  public MessageConverter rabbitMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter rabbitMessageConverter) {
    RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(rabbitMessageConverter);
    return rabbitTemplate;
  }

  @Bean
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
    SimpleRabbitListenerContainerFactoryConfigurer configurer,
    ConnectionFactory connectionFactory,
    MessageConverter rabbitMessageConverter
  ) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    configurer.configure(factory, connectionFactory);
    factory.setMessageConverter(rabbitMessageConverter);
    factory.setDefaultRequeueRejected(false);
    factory.setAdviceChain(
      RetryInterceptorBuilder.stateless()
        .maxAttempts(3)
        .recoverer(new RejectAndDontRequeueRecoverer())
        .build()
    );
    return factory;
  }
}
