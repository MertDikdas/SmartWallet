export function formatMoney(
    value: number | string,
    currency = 'TRY',
) {
    const amount = Number(value)

    return new Intl.NumberFormat('tr-TR', {
        style: 'currency',
        currency,
        maximumFractionDigits: 2,
    }).format(Number.isFinite(amount) ? amount : 0)
}

export function formatCompactMoney(
    value: number | string,
    currency = 'TRY',
) {
    const amount = Number(value)

    return new Intl.NumberFormat('tr-TR', {
        style: 'currency',
        currency,
        notation: 'compact',
        maximumFractionDigits: 1,
    }).format(Number.isFinite(amount) ? amount : 0)
}

export function formatDate(value: string) {
    return new Intl.DateTimeFormat('en-US', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
    }).format(new Date(value))
}

export function formatDateTime(value: string) {
    return new Intl.DateTimeFormat('en-US', {
        day: '2-digit',
        month: 'short',
        hour: '2-digit',
        minute: '2-digit',
    }).format(new Date(value))
}

export function toLocalDateTimeInput(date = new Date()) {
    const localDate = new Date(
        date.getTime() - date.getTimezoneOffset() * 60_000,
    )

    return localDate.toISOString().slice(0, 16)
}

export function toIsoInstant(localDateTime: string) {
    return new Date(localDateTime).toISOString()
}

export function monthName(month: number) {
    return new Intl.DateTimeFormat('en-US', { month: 'short' }).format(
        new Date(2024, month - 1, 1),
    )
}

export function percentage(value: number, maximum: number) {
    if (maximum <= 0) {
        return 0
    }

    return Math.min(Math.max((value / maximum) * 100, 0), 100)
}
