import { apiRequest } from './apiClient'
import type { Currency } from './accountApi'
import type { PageResponse } from './transactionApi'

export type Transfer = {
    id: number
    fromAccountId: number
    fromAccountName: string
    toAccountId: number
    toAccountName: string
    amount: number
    currency: Currency
    description: string
    transferredAt: string
    createdAt: string
}

export type CreateTransferRequest = {
    fromAccountId: number
    toAccountId: number
    amount: number
    description?: string
    transferredAt?: string
}

export function getTransfers(page = 0, size = 20) {
    return apiRequest<PageResponse<Transfer>>(
        `/api/transfers?page=${page}&size=${size}`,
    )
}

export function createTransfer(request: CreateTransferRequest) {
    return apiRequest<Transfer>('/api/transfers', {
        method: 'POST',
        headers: {
            'Idempotency-Key': crypto.randomUUID(),
        },
        body: JSON.stringify(request),
    })
}
