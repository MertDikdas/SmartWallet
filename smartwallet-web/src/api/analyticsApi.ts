import { apiRequest } from './apiClient'

export type Currency = 'TRY' | 'USD' | 'EUR'

export type MonthlyAnalytics = {
    year: number
    month: number
    totalIncome: number
    totalExpense: number
    netAmount: number
    transactionCount: number
    currency: Currency
}

export type CategoryExpense = {
    categoryId: number
    categoryName: string
    totalExpense: number
    percentage: number
    transactionCount: number
}

export type MonthlyCategoryAnalytics = {
    year: number
    month: number
    totalExpense: number
    categories: CategoryExpense[]
    currency : Currency
}

export type MonthlyTrendItem = {
    year: number
    month: number
    totalIncome: number
    totalExpense: number
    netAmount: number
    transactionCount: number
    currency : Currency
}

export type MonthlyTrend = {
    months: MonthlyTrendItem[]
}

export type DailyExpenseItem = {
    day: number
    totalIncome: number
    totalExpense: number
    currency: Currency
}

export type DailyCashFlow = {
    year: number
    month: number
    totalIncome: number
    totalExpense: number
    days: DailyExpenseItem[]
    currency: Currency
}

export function getDailyCashFlow(year: number, month: number, currency: Currency) {
    return apiRequest<DailyCashFlow>(
        `/api/analytics/daily-expense?year=${year}&month=${month}&currency=${currency}`,
    )
}

export function getMonthlyAnalytics(year: number, month: number, currency: Currency) {
    return apiRequest<MonthlyAnalytics>(
        `/api/analytics/monthly?year=${year}&month=${month}&currency=${currency}`,
    )
}

export function getMonthlyCategoryAnalytics(year: number, month: number, currency: Currency) {
    return apiRequest<MonthlyCategoryAnalytics>(
        `/api/analytics/monthly/categories?year=${year}&month=${month}&currency=${currency}`,
    )
}

export function getMonthlyTrend(currency: Currency, months = 6) {
    return apiRequest<MonthlyTrend>(
        `/api/analytics/monthly-trend?months=${months}&currency=${currency}`,
    )
}
