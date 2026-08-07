import type { LoginResponse } from '../api/authApi'
import {
    clearAuthSession,
    getRefreshToken,
    saveAuthSession,
} from './authStorage'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''
let refreshPromise: Promise<string> | null = null

function redirectToLogin(): never {
    clearAuthSession()
    window.location.replace('/login')
    throw new Error('Your session has expired')
}

export function refreshAuthSession(): Promise<string> {
    if (refreshPromise) {
        return refreshPromise
    }

    refreshPromise = performRefresh().finally(() => {
        refreshPromise = null
    })

    return refreshPromise
}

async function performRefresh(): Promise<string> {
    const refreshToken = getRefreshToken()

    if (!refreshToken) {
        return redirectToLogin()
    }

    let response: Response

    try {
        response = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Accept: 'application/json',
            },
            body: JSON.stringify({ refreshToken }),
        })
    } catch {
        throw new Error('Could not connect to the server')
    }

    if (!response.ok) {
        return redirectToLogin()
    }

    const data = (await response.json()) as LoginResponse
    saveAuthSession(data)
    return data.accessToken
}
