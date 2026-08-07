import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { login } from '../api/authApi'
import { saveAuthSession } from '../auth/authStorage'
import Icon from '../components/Icon'
import '../styles/Auth.css'

function LoginPage() {
    const navigate = useNavigate()
    const location = useLocation()
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [showPassword, setShowPassword] = useState(false)
    const [error, setError] = useState('')
    const [isLoading, setIsLoading] = useState(false)
    const registered = Boolean((location.state as { registered?: boolean } | null)?.registered)

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setError('')
        setIsLoading(true)

        try {
            const data = await login({ email: email.trim(), password })
            saveAuthSession(data)
            navigate('/dashboard', { replace: true })
        } catch (err) {
            setError(err instanceof Error ? err.message : 'An unexpected error occurred')
        } finally {
            setIsLoading(false)
        }
    }

    return (
        <main className="auth-page">
            <section className="auth-visual">
                <Link to="/" className="auth-brand">
                    <span><Icon name="wallet" size={22} /></span>SmartWallet
                </Link>
                <div className="auth-visual-copy">
                    <span className="auth-kicker"><Icon name="shield" size={15} />Secure personal finance</span>
                    <h1>Your money,<br /><em>finally clear.</em></h1>
                    <p>Bring every account, budget, and recurring payment into one calm dashboard.</p>
                </div>
                <div className="auth-quote">
                    <Icon name="sparkle" size={18} />
                    <p>Small daily decisions become better long-term habits when the numbers are easy to understand.</p>
                </div>
            </section>

            <section className="auth-form-side">
                <div className="auth-card">
                    <div className="mobile-auth-brand"><Link to="/"><Icon name="wallet" size={20} />SmartWallet</Link></div>
                    <p className="auth-eyebrow">Welcome back</p>
                    <h2>Sign in to your wallet</h2>
                    <p className="auth-subtitle">Enter your account details to continue.</p>

                    {registered && <p className="auth-success"><Icon name="check" size={16} />Your account is ready. You can sign in now.</p>}

                    <form className="auth-form" onSubmit={handleSubmit}>
                        <label htmlFor="email">Email address</label>
                        <input id="email" type="email" placeholder="you@example.com" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} required />

                        <label htmlFor="password">Password</label>
                        <div className="password-field">
                            <input id="password" type={showPassword ? 'text' : 'password'} placeholder="Enter your password" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} required />
                            <button type="button" aria-label={showPassword ? 'Hide password' : 'Show password'} onClick={() => setShowPassword((value) => !value)}>
                                <Icon name={showPassword ? 'eye-off' : 'eye'} size={18} />
                            </button>
                        </div>

                        {error && <p className="auth-error">{error}</p>}
                        <button className="auth-submit" type="submit" disabled={isLoading}>
                            {isLoading ? <><span className="button-spinner" />Signing in...</> : <>Sign in <Icon name="arrow-right" size={17} /></>}
                        </button>
                    </form>

                    <p className="auth-footer">New to SmartWallet? <Link to="/register">Create an account</Link></p>
                </div>
            </section>
        </main>
    )
}

export default LoginPage
