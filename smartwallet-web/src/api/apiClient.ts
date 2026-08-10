import { getAccessToken } from '../auth/authStorage'
import { refreshAuthSession } from '../auth/authRefresh'

export type ApiErrorBody = {
    message?: string
    fieldErrors?: Record<string, string>
}

export class ApiError extends Error {
    fieldErrors?: Record<string, string>
    status?: number

    constructor(
        message: string,
        fieldErrors?: Record<string, string>,
        status?: number,
    ) {
        super(message)
        this.name = 'ApiError'
        this.fieldErrors = fieldErrors
        this.status = status
    }
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

export async function apiRequest<T>(
    url: string,
    options: RequestInit = {},
): Promise<T> {
    let accessToken = getAccessToken()
    let response = await sendRequest(url, options, accessToken)

    if (
        response.status === 401 &&
        accessToken &&
        !url.startsWith('/api/auth/')
    ) {
        accessToken = await refreshAuthSession()
        response = await sendRequest(url, options, accessToken)
    }

    const data = await parseResponse(response)

    if (!response.ok) {
        console.log("API ERROR STATUS:", response.status)
        console.log("API ERROR BODY:", data)

        const error = (data ?? {}) as ApiErrorBody
        const firstFieldError = error.fieldErrors
            ? Object.values(error.fieldErrors)[0]
            : undefined

        throw new ApiError(
            firstFieldError ?? error.message ?? 'Request failed',
            error.fieldErrors,
            response.status,
        )
    }

    return data as T
}

async function sendRequest(
    url: string,
    options: RequestInit,
    accessToken: string | null,
) {
    try {
        return await fetch(`${API_BASE_URL}${url}`, {
            ...options,
            headers: {
                Accept: 'application/json',
                ...(options.body ? { 'Content-Type': 'application/json' } : {}),
                ...(accessToken
                    ? { Authorization: `Bearer ${accessToken}` }
                    : {}),
                ...options.headers,
            },
        })
    } catch {
        throw new ApiError(
            'Could not connect to SmartWallet. Make sure the API Gateway is running.',
        )
    }
}

async function parseResponse(response: Response) {
    if (response.status === 204) {
        return undefined
    }

    const contentType = response.headers.get('content-type') ?? ''

    if (contentType.includes('application/json')) {
        return response.json()
    }

    const text = await response.text()
    return text ? { message: text } : undefined
}
