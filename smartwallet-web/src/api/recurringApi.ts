import { apiRequest } from './apiClient'
import type { TransactionType } from './transactionApi'

export type RecurrenceFrequency = 'WEEKLY' | 'MONTHLY'
export type RecurringStatus = 'ACTIVE' | 'PAUSED' | 'CANCELLED'

export type RecurringTransaction = {
    id: number
    accountId: number
    accountName: string
    categoryId: number
    categoryName: string
    type: TransactionType
    amount: number
    description: string
    frequency: RecurrenceFrequency
    status: RecurringStatus
    startDate: string
    endDate: string | null
    nextExecutionDate: string | null
    lastExecutionDate: string | null
    createdAt: string
    updatedAt: string
}

export type CreateRecurringTransactionRequest = {
    accountId: number
    categoryId: number
    type: TransactionType
    amount: number
    description?: string
    frequency: RecurrenceFrequency
    startDate: string
    endDate?: string
}

export function getRecurringTransactions() {
    return apiRequest<RecurringTransaction[]>('/api/recurring-transactions')
}

export function createRecurringTransaction(
    request: CreateRecurringTransactionRequest,
) {
    return apiRequest<RecurringTransaction>('/api/recurring-transactions', {
        method: 'POST',
        body: JSON.stringify(request),
    })
}

export function pauseRecurringTransaction(id: number) {
    return apiRequest<RecurringTransaction>(
        `/api/recurring-transactions/${id}/pause`,
        { method: 'PATCH' },
    )
}

export function resumeRecurringTransaction(id: number) {
    return apiRequest<RecurringTransaction>(
        `/api/recurring-transactions/${id}/resume`,
        { method: 'PATCH' },
    )
}
