import { Navigate, Outlet } from "react-router-dom";
import { useAppSelector } from "../app/hooks";

/**
 * Wraps routes that require authentication.
 * Unauthenticated users are redirected to /login.
 */
const ProtectedRoute = () => {
  const isAuthenticated = useAppSelector((state) => state.auth.isAuthenticated);
  return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />;
};

export default ProtectedRoute;
