export type ApiErrorBody = {
    message?: string
    fieldErrors?: Record<string, string>
}

export class ApiError extends Error {
    fieldErrors?: Record<string, string>

    constructor(message: string, fieldErrors?: Record<string, string>) {
        super(message)
        this.name = 'ApiError'
        this.fieldErrors = fieldErrors
    }
}

export async function apiRequest<T>(
    url: string,
    options: RequestInit,
): Promise<T> {
    const response = await fetch(url, {
        ...options,
        headers: {
            'Content-Type': 'application/json',
            ...options.headers,
        },
    })

    const data = await response.json()

    if (!response.ok) {
        const error = data as ApiErrorBody

        const firstFieldError = error.fieldErrors
            ? Object.values(error.fieldErrors)[0]
            : undefined

        throw new ApiError(
            firstFieldError ?? error.message ?? 'Request failed',
            error.fieldErrors,
        )
    }

    return data as T
}