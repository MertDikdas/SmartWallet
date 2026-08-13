import { Navigate, Outlet } from 'react-router'
import { isAuthenticated } from './authStorage'

function ProtectedRoute() {
    if (!isAuthenticated()) {
        return <Navigate to="/login" replace />
    }

    return <Outlet />
}

export default ProtectedRoute