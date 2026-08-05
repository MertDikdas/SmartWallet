import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import '../styles/Auth.css'

type ApiError = {
    message?: string
    fieldErrors?: Record<string, string>
}

function RegisterPage() {
    const navigate = useNavigate()

    const [firstName, setFirstName] = useState('')
    const [lastName, setLastName] = useState('')
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [error, setError] = useState('')
    const [isLoading, setIsLoading] = useState(false)

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setError('')
        setIsLoading(true)

        try {
            const response = await fetch('/api/auth/register', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    firstName,
                    lastName,
                    email,
                    password,
                }),
            })

            if (!response.ok) {
                const apiError: ApiError = await response.json()

                const firstFieldError = apiError.fieldErrors
                    ? Object.values(apiError.fieldErrors)[0]
                    : undefined

                throw new Error(
                    firstFieldError ?? apiError.message ?? 'Registration failed',
                )
            }

            navigate('/login')
        } catch (err) {
            setError(
                err instanceof Error
                    ? err.message
                    : 'An unexpected error occurred',
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

                <h1>Create an account</h1>
                <p>Start managing your finances smarter.</p>

                <form className="auth-form" onSubmit={handleSubmit}>
                    <label htmlFor="firstName">First name</label>
                    <input
                        id="firstName"
                        type="text"
                        placeholder="John"
                        maxLength={100}
                        value={firstName}
                        onChange={(event) => setFirstName(event.target.value)}
                        required
                    />

                    <label htmlFor="lastName">Last name</label>
                    <input
                        id="lastName"
                        type="text"
                        placeholder="Doe"
                        maxLength={100}
                        value={lastName}
                        onChange={(event) => setLastName(event.target.value)}
                        required
                    />

                    <label htmlFor="email">Email</label>
                    <input
                        id="email"
                        type="email"
                        placeholder="example@email.com"
                        maxLength={255}
                        value={email}
                        onChange={(event) => setEmail(event.target.value)}
                        required
                    />

                    <label htmlFor="password">Password</label>
                    <input
                        id="password"
                        type="password"
                        placeholder="Create a password"
                        minLength={8}
                        maxLength={64}
                        value={password}
                        onChange={(event) => setPassword(event.target.value)}
                        required
                    />

                    {error && <p className="auth-error">{error}</p>}

                    <button type="submit" disabled={isLoading}>
                        {isLoading ? 'Creating account...' : 'Create account'}
                    </button>
                </form>

                <p className="auth-footer">
                    Already have an account? <Link to="/login">Login</Link>
                </p>
            </section>
        </main>
    )
}

export default RegisterPage