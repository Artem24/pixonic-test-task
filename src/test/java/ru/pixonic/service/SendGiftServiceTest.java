package ru.pixonic.service;

import org.junit.jupiter.api.Test;
import ru.pixonic.consumer.SendGiftWorkConsumer;
import ru.pixonic.context.AppContext;
import ru.pixonic.dto.GiftDto;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SendGiftServiceTest {
    private static final int USERS_ARRAY_LENGTH = 12;

    private final ExecutorService giftServiceInvoker = Executors.newFixedThreadPool(8);

    private final String[][] users = {
            {"Ji", "We"},
            {"Ji", "Miko"},
            {"Ji", "Poli"},
            {"We", "Ji"},
            {"We", "Miko"},
            {"We", "Poli"},
            {"Poli", "Miko"},
            {"Poli", "We"},
            {"Poli", "Ji"},
            {"Miko", "Poli"},
            {"Miko", "We"},
            {"Miko", "Ji"}};


    /**
     * В выводе видно, что очередь выполнеямых задач сортируется в порядке возрастания времени ожидания.
     * При большом кол-ве тредов консьюмера {@link AppContext#GIFT_WORK_CONSUMER_TREADS} производительность падает из-за
     * высокой конкуренции за локи между потоками.
     *
     * @throws InterruptedException
     */
    @Test
    public void simpleSendGiftServiceTest() throws InterruptedException {

        SendGiftService giftService = new SendGiftService();

        List<Future<CompletableFuture<Boolean>>> futures =
                Stream
                        .generate(() ->
                                giftServiceInvoker.submit(() -> {
                                    int usersId = ThreadLocalRandom.current().nextInt(USERS_ARRAY_LENGTH);
                                    GiftDto giftDto = new GiftDto(users[usersId][0], users[usersId][1], "arm431");

                                    return giftService.sendGift(giftDto);
                                })
                        )
                        .limit(100)
                        .collect(Collectors.toList());

        AtomicInteger trues = new AtomicInteger();
        AtomicInteger falses = new AtomicInteger();

        futures.forEach(completableFutureFuture -> {
            try {
                completableFutureFuture.get().whenComplete(
                        (aBoolean, throwable) -> {
                            if (Boolean.TRUE.equals(aBoolean)) {
                                trues.incrementAndGet();
                            } else {
                                falses.incrementAndGet();
                            }
                        });
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });

        Thread.sleep(3000);
        /*
        Приходится завершать выполнение таким образом т.к. после опустошения очереди треды блокируются на методе
        queue.take(), что нормально при целевом использовании с постоянно пополняемой очередью заданий.
         */
        AppContext.getInstance().getGiftWorkExecutor().shutdownNow();

        SendGiftWorkConsumer giftWorkConsumer = AppContext.getInstance().getGiftWorkConsumer();
        AtomicInteger successfulCompletedFuturesCounter = giftWorkConsumer.getSuccessfulCompletedFuturesCounter();
        AtomicInteger unsuccessfulCompletedFuturesCounter = giftWorkConsumer.getUnsuccessfulCompletedFuturesCounter();
        AtomicInteger backToQueueCounter = giftWorkConsumer.getBackToQueueCounter();

        System.out.printf(
                "*** successful sent %s unsuccessful sent %s sent back to Queue %s times ***%n",
                successfulCompletedFuturesCounter.get(),
                unsuccessfulCompletedFuturesCounter.get(),
                backToQueueCounter.get()
        );

        assertEquals(successfulCompletedFuturesCounter.get(), trues.get());
        assertEquals(unsuccessfulCompletedFuturesCounter.get(), falses.get());
    }

}

