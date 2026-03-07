package com.payflow.dto;

import java.util.UUID;

public record TransferResponse(UUID correlationId, TransactionResponse debit, TransactionResponse credit) {}
