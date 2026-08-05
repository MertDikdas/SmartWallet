import { Link } from 'react-router'
import '../App.css'
import './LoginPage.css'

function LoginPage() {
    return (
        <main className="auth-page">
            <section className="auth-card">
                <Link to="/" className="auth-logo">
                    SmartWallet
                </Link>

                <h1>Welcome back</h1>
                <p>Sign in to manage your finances.</p>

                <form className="auth-form">
                    <label htmlFor="email">Email</label>
                    <input
                        id="email"
                        type="email"
                        placeholder="example@email.com"
                        required
                    />

                    <label htmlFor="password">Password</label>
                    <input
                        id="password"
                        type="password"
                        placeholder="Enter your password"
                        required
                    />

                    <button type="submit">Login</button>
                </form>

                <p className="auth-footer">
                    Don&apos;t have an account? <Link to="/register">Register</Link>
                </p>
            </section>
        </main>
    )
}

export default LoginPage