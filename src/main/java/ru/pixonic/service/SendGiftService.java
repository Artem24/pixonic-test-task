package ru.pixonic.service;

import ru.pixonic.consumer.WorkUnit;
import ru.pixonic.context.AppContext;
import ru.pixonic.dto.GiftDto;

import java.util.concurrent.CompletableFuture;

public class SendGiftService {

    public CompletableFuture<Boolean> sendGift(GiftDto giftDto) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        WorkUnit workUnit = new WorkUnit(
                giftDto,
                0,
                0,
                future
        );

        try {
            AppContext.getInstance().getSendGiftQueue().put(workUnit);
            return future;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(false);
        }
    }

}
