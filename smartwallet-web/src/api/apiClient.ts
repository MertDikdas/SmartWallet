import { getAccessToken } from '../auth/authStorage'
import { refreshAuthSession } from '../auth/authRefresh'

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
    options: RequestInit = {},
): Promise<T> {
    let accessToken = getAccessToken()

    let response = await sendRequest(url, options, accessToken)

    if (response.status === 401 && accessToken) {
        accessToken = await refreshAuthSession()

        response = await sendRequest(url, options, accessToken)
    }

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

function sendRequest(
    url: string,
    options: RequestInit,
    accessToken: string | null,
) {
    return fetch(url, {
        ...options,
        headers: {
            'Content-Type': 'application/json',
            ...(accessToken
                ? { Authorization: `Bearer ${accessToken}` }
                : {}),
            ...options.headers,
        },
    })
}