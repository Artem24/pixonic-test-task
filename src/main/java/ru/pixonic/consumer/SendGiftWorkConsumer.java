package ru.pixonic.consumer;

import ru.pixonic.dto.GiftDto;
import ru.pixonic.dto.ItemDto;
import ru.pixonic.repository.ItemRepository;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Класс реализует обработку задач из очереди.
 * Пытаемся последовательно захватить локи обоих игроков. В случае неудачи помещаем задачу обратно в очередь с
 * таймаутом и увеличиваем счетчик попыток. При достижении порогового значения задача завершается неудачей.
 * Если удалить весь вывод в консоль код методов не будет выглядить таким объемным.
 */
public class SendGiftWorkConsumer implements Runnable {

    private static final int MAX_TRIES = 5;
    private static final int DELAY_BEFORE_RETRY_MS = 10;

    private final Map<String, Lock> locks = new ConcurrentHashMap<>();
    private final ItemRepository itemRepository = new ItemRepository();
    private final AtomicInteger successfulCompletedFuturesCounter = new AtomicInteger();
    private final AtomicInteger unsuccessfulCompletedFuturesCounter = new AtomicInteger();
    private final AtomicInteger taskCounter = new AtomicInteger();
    private final AtomicInteger backToQueueCounter = new AtomicInteger();
    private final AtomicInteger numberOfConsumedElements = new AtomicInteger();
    private final BlockingQueue<WorkUnit> queue;

    public SendGiftWorkConsumer(BlockingQueue<WorkUnit> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        do {
            try {
                WorkUnit workUnit = queue.take();
                numberOfConsumedElements.incrementAndGet();
                System.out.printf("[%s] Consumer take: %s%n", Thread.currentThread().getName(), workUnit);
                if (workUnit.getTries() == MAX_TRIES) {
                    workUnit.getFuture().complete(false);
                    unsuccessfulCompletedFuturesCounter.incrementAndGet();
                } else {
                    consume(workUnit);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        } while (!Thread.currentThread().isInterrupted());
    }

    private void consume(WorkUnit workUnit) throws InterruptedException {
        int taskNumber = taskCounter.incrementAndGet();
        GiftDto giftDto = workUnit.getGiftDto();
        System.out.printf(
                "[%s] Try to send from %s to %s%n",
                Thread.currentThread().getName(),
                giftDto.getFromUserId(),
                giftDto.getToUserId()
        );

        if (!locks.containsKey(giftDto.getFromUserId())) {
            System.out.printf("[%s] Inserting lock for user %s%n", Thread.currentThread().getName(), giftDto.getFromUserId());
            locks.putIfAbsent(giftDto.getFromUserId(), new ReentrantLock(true));
        }
        Lock fromUserIdLock = locks.get(giftDto.getFromUserId());
        if (fromUserIdLock.tryLock()) {
            try {
                System.out.printf("[%s] got lock for user %s%n", Thread.currentThread().getName(), giftDto.getFromUserId());
                if (!locks.containsKey(giftDto.getToUserId())) {
                    System.out.printf("[%s] Inserting lock for user %s%n", Thread.currentThread().getName(), giftDto.getToUserId());
                    locks.putIfAbsent(giftDto.getToUserId(), new ReentrantLock(true));
                }
                Lock toUserIdLock = locks.get(giftDto.getToUserId());
                if (toUserIdLock.tryLock()) {
                    try {
                        System.out.printf("[%s] got lock for user %s%n", Thread.currentThread().getName(), giftDto.getToUserId());
                        ItemDto itemDto = itemRepository.getItemByUserIdAndItemId(giftDto.getFromUserId(), giftDto.getItemId());
                        if (itemDto == null) {
                            workUnit.getFuture().complete(false);
                            System.out.printf(
                                    "User %s doesn't have Item with id %s%n",
                                    giftDto.getFromUserId(),
                                    giftDto.getItemId()
                            );
                        }

                        itemRepository.removeItemFromUser(giftDto.getItemId(), giftDto.getFromUserId());
                        itemRepository.addItemToUser(giftDto.getItemId(), giftDto.getToUserId());
                        workUnit.getFuture().complete(true);
                        successfulCompletedFuturesCounter.incrementAndGet();

                        System.out.printf(
                                "[%s] **************************************** Operation %s finished ***%n",
                                Thread.currentThread().getName(),
                                taskNumber
                        );

                    } finally {
                        toUserIdLock.unlock();
                    }
                } else {
                    System.out.printf(
                            "[%s] Fail to get lock for user %s, send it back to Queue%n",
                            Thread.currentThread().getName(),
                            giftDto.getToUserId()
                    );
                    pushBackToQueue(workUnit);
                }
            } finally {
                fromUserIdLock.unlock();
            }
        } else {
            System.out.printf(
                    "[%s] Fail to get lock for user %s, send it back to Queue%n",
                    Thread.currentThread().getName(),
                    giftDto.getFromUserId()
            );
            pushBackToQueue(workUnit);
        }
    }

    private void pushBackToQueue(WorkUnit workUnit) {
        backToQueueCounter.incrementAndGet();
        int tries = workUnit.getTries();
        queue.add(
                new WorkUnit(
                        workUnit.getGiftDto(),
                        ++tries,
                        DELAY_BEFORE_RETRY_MS,
                        workUnit.getFuture()
                )
        );
    }

    public AtomicInteger getSuccessfulCompletedFuturesCounter() {
        return successfulCompletedFuturesCounter;
    }

    public AtomicInteger getUnsuccessfulCompletedFuturesCounter() {
        return unsuccessfulCompletedFuturesCounter;
    }

    public AtomicInteger getBackToQueueCounter() {
        return backToQueueCounter;
    }
}
