package com.demo.vietqr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TransactionSyncResponse {
    private boolean error;
    private String errorReason;
    private String toastMessage;
    private Object object;

    public static TransactionSyncResponse success(String reftransactionid) {
        return new TransactionSyncResponse(false, null, "Success", new RefTransaction(reftransactionid));
    }

    public static TransactionSyncResponse failed(String errorReason, String toastMessage) {
        return new TransactionSyncResponse(true, errorReason, toastMessage, null);
    }

    @Data
    @AllArgsConstructor
    public static class RefTransaction {
        private String reftransactionid;
    }
}
