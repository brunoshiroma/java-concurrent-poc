package com.brunoshiroma.java.concurrent.poc;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private final static HttpClient HTTP_CLIENT;

    private final static Random RANDOM = new SecureRandom();

    private final static Logger LOGGER = Logger.getAnonymousLogger();

    static {
        HTTP_CLIENT = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(1000))
                .build();
    }

    private static CompletableFuture<String> waitAndReturn(Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
                    try {
                        final var startDate = LocalDateTime.now();

                        final var pokemonNumber = RANDOM.nextInt(1026);

                        final var request = HttpRequest
                                .newBuilder(URI.create(String.format("https://pokeapi.co/api/v2/pokemon/%d", pokemonNumber)))
                                .build();

                        final var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                        final var endDate = LocalDateTime.now();

                        LOGGER.log(Level.INFO, "duration {0}ms {1}",
                                new Object[]{
                                        Duration.between(startDate, endDate).toMillis(),
                                        Thread.currentThread().getName()});

                        return response.body();
                    } catch (InterruptedException | IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                executor);
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {

        final var sr = SecureRandom.getInstanceStrong();

        try (final var executor = Executors.newScheduledThreadPool(3, Thread.ofVirtual().name("virtual-", 1).factory())) {

            List<CompletableFuture<String>> tasks = new ArrayList<>();


            for ( int i = 0; i < 100; i++) {
                tasks.add(waitAndReturn(executor));
            }

            LOGGER.info(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
                    .thenAccept(_ -> {
                        LOGGER.info(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

                        tasks.forEach( t -> LOGGER.info( "" + t.join().length()));

                        LOGGER.info(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    });
        }

    }
}