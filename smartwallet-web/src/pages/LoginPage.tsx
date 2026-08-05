import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import '../styles/Auth.css'

function LoginPage() {
    const navigate = useNavigate()

    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [error, setError] = useState('')
    const [isLoading, setIsLoading] = useState(false)

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setError('')
        setIsLoading(true)

        try {
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ email, password }),
            })

            const data = await response.json()

            if (!response.ok) {
                throw new Error(data.message ?? 'Email or password is incorrect')
            }

            sessionStorage.setItem('accessToken', data.accessToken)
            sessionStorage.setItem('refreshToken', data.refreshToken)
            sessionStorage.setItem('user', JSON.stringify(data.user))

            navigate('/dashboard')
        } catch (err) {
            setError(
                err instanceof Error ? err.message : 'An unexpected error occurred',
            )
        } finally {
            setIsLoading(false)
        }
    }
    return (
        <main className="auth-page">
            <section className="auth-card">
                <Link to="/" className="auth-logo">
                    SmartWallet
                </Link>

                <h1>Welcome back</h1>
                <p>Sign in to manage your finances.</p>

                <form className="auth-form" onSubmit={handleSubmit}>
                    <label htmlFor="email">Email</label>
                    <input
                        id="email"
                        type="email"
                        placeholder="example@email.com"
                        value={email}
                        onChange={(event) => setEmail(event.target.value)}
                        required
                    />

                    <label htmlFor="password">Password</label>
                    <input
                        id="password"
                        type="password"
                        placeholder="Enter your password"
                        value={password}
                        onChange={(event) => setPassword(event.target.value)}
                        required
                    />
                    {error && <p className="auth-error">{error}</p>}
                    <button type="submit" disabled={isLoading}>
                        {isLoading ? 'Logging in...' : 'Login'}
                    </button>
                </form>

                <p className="auth-footer">
                    Don&apos;t have an account? <Link to="/register">Register</Link>
                </p>
            </section>
        </main>
    )
}

export default LoginPage