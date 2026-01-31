package com.payflow.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class TransferResponse {
    private UUID correlationId;
    private TransactionResponse debit;
    private TransactionResponse credit;
}
