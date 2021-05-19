package ru.pixonic.repository;


import ru.pixonic.dto.ItemDto;

import java.util.concurrent.ThreadLocalRandom;

public class ItemRepository {

    public ItemDto getItemByUserIdAndItemId(String fromUserId, String itemId) {
        try {
            Thread.sleep(10 + ThreadLocalRandom.current().nextInt(10));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.printf("[%s] Item %s returned from DB%n", Thread.currentThread().getName(), itemId);
        return new ItemDto();
    }

    public void removeItemFromUser(String itemId, String userId) {
        try {
            Thread.sleep(10 + ThreadLocalRandom.current().nextInt(10));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.printf("[%s] Item %s removed from user %s%n", Thread.currentThread().getName(), itemId, userId);
    }

    public void addItemToUser(String itemId, String userId) {
        try {
            Thread.sleep(10 + ThreadLocalRandom.current().nextInt(10));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.printf("[%s] Item %s added to user %s%n", Thread.currentThread().getName(), itemId, userId);
    }
}
