package cn.exitcode.richpeasants.ingest.config;

import cn.exitcode.richpeasants.ingest.mq.IngestMqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableRabbit
public class IngestRabbitConfig {

    @Bean
    public MessageConverter ingestJacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter ingestJacksonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(ingestJacksonMessageConverter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            MessageConverter ingestJacksonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(ingestJacksonMessageConverter);
        // 失败不重回主队列，走 DLQ，避免毒消息死循环
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    public DirectExchange documentIngestExchange() {
        return new DirectExchange(IngestMqConstants.EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange documentIngestDlx() {
        return new DirectExchange(IngestMqConstants.DLX, true, false);
    }

    @Bean
    public Queue documentIngestQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", IngestMqConstants.DLX);
        args.put("x-dead-letter-routing-key", IngestMqConstants.DLQ);
        return QueueBuilder.durable(IngestMqConstants.QUEUE).withArguments(args).build();
    }

    @Bean
    public Queue documentIngestDlq() {
        return QueueBuilder.durable(IngestMqConstants.DLQ).build();
    }

    @Bean
    public Binding documentIngestBinding(Queue documentIngestQueue, DirectExchange documentIngestExchange) {
        return BindingBuilder.bind(documentIngestQueue)
                .to(documentIngestExchange)
                .with(IngestMqConstants.ROUTING_KEY);
    }

    @Bean
    public Binding documentIngestDlqBinding(Queue documentIngestDlq, DirectExchange documentIngestDlx) {
        return BindingBuilder.bind(documentIngestDlq)
                .to(documentIngestDlx)
                .with(IngestMqConstants.DLQ);
    }
}
