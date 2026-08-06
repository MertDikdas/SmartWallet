import type { LoginResponse } from '../api/authApi'
import {
    clearAuthSession,
    getRefreshToken,
    saveAuthSession,
} from './authStorage'

function redirectToLogin(): never {
    clearAuthSession()
    window.location.replace('/')

    throw new Error('Your session has expired')
}

export async function refreshAuthSession(): Promise<string> {
    const refreshToken = getRefreshToken()

    if (!refreshToken) {
        return redirectToLogin()
    }

    let response: Response

    try {
        response = await fetch('/api/auth/refresh', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
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