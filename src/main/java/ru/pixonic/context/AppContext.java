package ru.pixonic.context;

import ru.pixonic.consumer.SendGiftWorkConsumer;
import ru.pixonic.consumer.WorkUnit;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppContext {

    private static final AppContext INSTANCE = new AppContext();
    private static final int GIFT_WORK_CONSUMER_TREADS = 2;

    private final BlockingQueue<WorkUnit> sendGiftQueue;
    private final ExecutorService giftWorkExecutor;
    private final SendGiftWorkConsumer giftWorkConsumer;

    public static AppContext getInstance() {
        return INSTANCE;
    }

    private AppContext() {
        sendGiftQueue = new DelayQueue<>();
        giftWorkExecutor = Executors.newFixedThreadPool(GIFT_WORK_CONSUMER_TREADS);
        giftWorkConsumer = new SendGiftWorkConsumer(sendGiftQueue);

        for (int i = 0; i < GIFT_WORK_CONSUMER_TREADS; i++) {
            giftWorkExecutor.submit(giftWorkConsumer);
        }
    }

    public BlockingQueue<WorkUnit> getSendGiftQueue() {
        return sendGiftQueue;
    }

    public ExecutorService getGiftWorkExecutor() {
        return giftWorkExecutor;
    }

    public SendGiftWorkConsumer getGiftWorkConsumer() {
        return giftWorkConsumer;
    }
}
