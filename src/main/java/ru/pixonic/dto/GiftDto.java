package ru.pixonic.dto;

import com.google.common.base.MoreObjects;

public class GiftDto {
    private final String fromUserId;
    private final String toUserId;
    private final String itemId;

    public GiftDto(String fromUserId, String toUserId, String itemId) {
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.itemId = itemId;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public String getToUserId() {
        return toUserId;
    }

    public String getItemId() {
        return itemId;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("fromUserId", fromUserId)
                .add("toUserId", toUserId)
                .add("itemId", itemId)
                .toString();
    }
}
