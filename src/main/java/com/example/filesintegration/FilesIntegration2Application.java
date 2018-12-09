//package com.example.filesintegration;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.context.annotation.Bean;
//import org.springframework.integration.dsl.IntegrationFlow;
//import org.springframework.integration.dsl.IntegrationFlows;
//import org.springframework.integration.dsl.Pollers;
//import org.springframework.integration.file.dsl.Files;
//import org.springframework.integration.file.transformer.FileToStringTransformer;
//
//import java.io.File;
//
//@SpringBootApplication
//public class FilesIntegration2Application {
//
//	@Bean
//	public FileToStringTransformer fileToStringTransformer() {
//		return new FileToStringTransformer();
//	}
//
//	@Bean
//	public IntegrationFlow fileToFile(@Value("${input-directory:${HOME}/Desktop/in}") File in,
//									  @Value("$(input-directory:${HOME}/Desktop/out}") String out) {
//		return IntegrationFlows.from(Files.inboundAdapter(in)
//						.autoCreateDirectory(true)
//						.patternFilter("*.txt"),
//				e -> e.poller(Pollers.fixedDelay(1000)))
//				.transform(fileToStringTransformer())
//				.transform("payload.replaceAll('\r\n', '\n')")
//				.transform("payload.toUpperCase()")
//				.handle(Files.outboundAdapter("'/Users/mikhailmaklashov/Desktop/out'")
//						.autoCreateDirectory(true))
//				.get();
//	}
//
//    public static void main(String[] args) {
//        SpringApplication.run(FilesIntegrationApplication.class, args);
//    }
//}
