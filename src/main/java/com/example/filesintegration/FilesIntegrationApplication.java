package com.example.filesintegration;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ImageBanner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.ftp.dsl.Ftp;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.ReflectionUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
public class FilesIntegrationApplication {

    private String ascii = "ascii";

    @Bean
    DefaultFtpSessionFactory ftpFileSessionFactory (
            @Value("${ftp.port:21}") int port,
            @Value("${ftp.username:test}") String username,
            @Value("${ftp.password:test}") String pw) {
        DefaultFtpSessionFactory ftpSessionFactory = new DefaultFtpSessionFactory();
        ftpSessionFactory.setPort(port);
        ftpSessionFactory.setPassword(pw);
        ftpSessionFactory.setUsername(username);

        return ftpSessionFactory;
    }

    @Bean
    Exchange exchange() {
        return ExchangeBuilder.directExchange(this.ascii).durable(true).build();
    }

    @Bean
    Queue queue() {
        return QueueBuilder.durable(this.ascii).build();
    }

    @Bean
    Binding binding() {
        return BindingBuilder.bind(this.queue())
                .to(this.exchange())
                .with(this.ascii)
                .noargs();
    }

    @Bean
    IntegrationFlow amqp(AmqpTemplate amqpTemplate) {
        return IntegrationFlows.from(this.asciiProcessors())
                .handle(
                        Amqp.outboundAdapter(amqpTemplate)
                                .exchangeName(this.ascii)
                                .routingKey(this.ascii)
                )
                .get();
    }

	@Bean
	IntegrationFlow files (@Value("${input-directory:${HOME}/Desktop/in}") File in,
						   Environment environment) {

		GenericTransformer<File, Message<String>> fileStringGenericTransformer = (File source) -> {

			try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 PrintStream printStream = new PrintStream(baos)) {
                ImageBanner imageBanner = new ImageBanner(new FileSystemResource(source));
                imageBanner.printBanner(environment, getClass(), printStream);
                return MessageBuilder.withPayload(new String(baos.toByteArray(), StandardCharsets.UTF_8))
                        .setHeader(FileHeaders.FILENAME, source.getAbsoluteFile().getName())
                        .build();
			} catch (IOException e) {
				ReflectionUtils.rethrowRuntimeException(e);
			}
			return null;
		};

		return IntegrationFlows
				.from(Files.inboundAdapter(in).autoCreateDirectory(true).preventDuplicates(true),
						poller -> poller.poller(pm -> pm.fixedRate(1000)))
				.transform(File.class, fileStringGenericTransformer)
                .channel(this.asciiProcessors())
				.get();
	}

	@Bean
    IntegrationFlow ftp(DefaultFtpSessionFactory ftpSessionFactory) {
	    return IntegrationFlows.from(this.asciiProcessors())
                .handle(
                    Ftp.outboundAdapter(ftpSessionFactory)
                        .remoteDirectory("uploads")
                        .fileNameGenerator(message -> {
                            Object o = message.getHeaders().get(FileHeaders.FILENAME);
                            String fileName = String.class.cast(o);
                            return fileName.split("\\.")[0] + ".txt";
                        })
                )
                .get();
    }

	@Bean
    MessageChannel asciiProcessors() {
	    return MessageChannels.publishSubscribe().get();
    }

	public static void main(String[] args) {
		SpringApplication.run(FilesIntegrationApplication.class, args);
	}
}
