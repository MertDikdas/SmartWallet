import { apiRequest } from './apiClient'

export type BudgetStatus = 'ACTIVE' | 'EXCEEDED'

export type Budget = {
    id: number
    categoryId: number
    limitAmount: number
    spentAmount: number
    remainingAmount: number
    year: number
    month: number
    status: BudgetStatus
    createdAt: string
    updatedAt: string
}

export type CreateBudgetRequest = {
    categoryId: number
    limitAmount: number
    year: number
    month: number
}

export type UpdateBudgetRequest = {
    limitAmount: number
}

export function getBudgets() {
    return apiRequest<Budget[]>('/api/budgets')
}

export function createBudget(request: CreateBudgetRequest) {
    return apiRequest<Budget>('/api/budgets', {
        method: 'POST',
        body: JSON.stringify(request),
    })
}

export function updateBudget(budgetId: number, request: UpdateBudgetRequest) {
    return apiRequest<Budget>(`/api/budgets/${budgetId}`, {
        method: 'PATCH',
        body: JSON.stringify(request),
    })
}

export function deleteBudget(budgetId: number) {
    return apiRequest<void>(`/api/budgets/${budgetId}`, {
        method: 'DELETE',
    })
}
