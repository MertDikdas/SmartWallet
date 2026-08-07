import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { register } from '../api/authApi'
import Icon from '../components/Icon'
import '../styles/Auth.css'

function RegisterPage() {
    const navigate = useNavigate()
    const [firstName, setFirstName] = useState('')
    const [lastName, setLastName] = useState('')
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [showPassword, setShowPassword] = useState(false)
    const [error, setError] = useState('')
    const [isLoading, setIsLoading] = useState(false)

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setError('')
        setIsLoading(true)

        try {
            await register({ firstName: firstName.trim(), lastName: lastName.trim(), email: email.trim(), password })
            navigate('/login', { replace: true, state: { registered: true } })
        } catch (err) {
            setError(err instanceof Error ? err.message : 'An unexpected error occurred')
        } finally {
            setIsLoading(false)
        }
    }

    return (
        <main className="auth-page">
            <section className="auth-visual register-visual">
                <Link to="/" className="auth-brand">
                    <span><Icon name="wallet" size={22} /></span>SmartWallet
                </Link>
                <div className="auth-visual-copy">
                    <span className="auth-kicker"><Icon name="sparkle" size={15} />Start with a clearer plan</span>
                    <h1>Build a wallet<br /><em>you understand.</em></h1>
                    <p>Create your account and turn everyday transactions into useful financial insight.</p>
                </div>
                <div className="auth-benefits">
                    <span><Icon name="check" size={16} />Track multiple currencies</span>
                    <span><Icon name="check" size={16} />Create category budgets</span>
                    <span><Icon name="check" size={16} />Automate recurring activity</span>
                </div>
            </section>

            <section className="auth-form-side">
                <div className="auth-card register-card">
                    <div className="mobile-auth-brand"><Link to="/"><Icon name="wallet" size={20} />SmartWallet</Link></div>
                    <p className="auth-eyebrow">Create your account</p>
                    <h2>Start managing money smarter</h2>
                    <p className="auth-subtitle">It only takes a minute to set up your wallet.</p>

                    <form className="auth-form" onSubmit={handleSubmit}>
                        <div className="auth-name-grid">
                            <div>
                                <label htmlFor="firstName">First name</label>
                                <input id="firstName" type="text" placeholder="Mert" maxLength={100} autoComplete="given-name" value={firstName} onChange={(event) => setFirstName(event.target.value)} required />
                            </div>
                            <div>
                                <label htmlFor="lastName">Last name</label>
                                <input id="lastName" type="text" placeholder="Dikdaş" maxLength={100} autoComplete="family-name" value={lastName} onChange={(event) => setLastName(event.target.value)} required />
                            </div>
                        </div>

                        <label htmlFor="email">Email address</label>
                        <input id="email" type="email" placeholder="you@example.com" maxLength={255} autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} required />

                        <label htmlFor="password">Password</label>
                        <div className="password-field">
                            <input id="password" type={showPassword ? 'text' : 'password'} placeholder="At least 8 characters" minLength={8} maxLength={64} autoComplete="new-password" value={password} onChange={(event) => setPassword(event.target.value)} required />
                            <button type="button" aria-label={showPassword ? 'Hide password' : 'Show password'} onClick={() => setShowPassword((value) => !value)}>
                                <Icon name={showPassword ? 'eye-off' : 'eye'} size={18} />
                            </button>
                        </div>
                        <span className="password-hint"><Icon name="shield" size={14} />Use 8–64 characters.</span>

                        {error && <p className="auth-error">{error}</p>}
                        <button className="auth-submit" type="submit" disabled={isLoading}>
                            {isLoading ? <><span className="button-spinner" />Creating account...</> : <>Create account <Icon name="arrow-right" size={17} /></>}
                        </button>
                    </form>

                    <p className="auth-footer">Already have an account? <Link to="/login">Sign in</Link></p>
                </div>
            </section>
        </main>
    )
}

export default RegisterPage
