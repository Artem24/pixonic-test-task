package ru.pixonic.consumer;

import com.google.common.base.MoreObjects;
import ru.pixonic.dto.GiftDto;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class WorkUnit implements Delayed {

    private final GiftDto giftDto;
    private final int tries;
    private final long startTime;
    private final CompletableFuture<Boolean> future;

    public WorkUnit(
            GiftDto giftDto,
            int tries,
            long delay,
            CompletableFuture<Boolean> future
    ) {
        this.giftDto = giftDto;
        this.tries = tries;
        this.startTime = System.currentTimeMillis() + delay;
        this.future = future;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long diff = startTime - System.currentTimeMillis();
        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        return Long.compare(startTime, ((WorkUnit) o).startTime);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("giftDto", giftDto)
                .add("tries", tries)
                .add("startTime",  startTime)
                .toString();
    }

    public CompletableFuture<Boolean> getFuture() {
        return future;
    }

    public int getTries() {
        return tries;
    }

    public GiftDto getGiftDto() {
        return giftDto;
    }
}
