import { Link } from 'react-router'
import '../styles/Auth.css'

function RegisterPage() {
    return (
        <main className="auth-page">
            <section className="auth-card">
                <Link to="/" className="auth-logo">
                    SmartWallet
                </Link>

                <h1>Create an account</h1>
                <p>Start managing your finances smarter.</p>

                <form className="auth-form">
                    <label htmlFor="fullName">Full name</label>
                    <input
                        id="fullName"
                        type="text"
                        placeholder="John Doe"
                        required
                    />

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
                        placeholder="Create a password"
                        minLength={8}
                        required
                    />

                    <button type="submit">Create account</button>
                </form>

                <p className="auth-footer">
                    Already have an account? <Link to="/login">Login</Link>
                </p>
            </section>
        </main>
    )
}

export default RegisterPage