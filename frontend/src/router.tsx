import { createBrowserRouter, Navigate } from "react-router-dom";
import { AuthInitializer } from "./features/auth/AuthInitializer";
import ProtectedRoute from "./components/ProtectedRoute";
import LoginPage from "./pages/authentication/loginPage/LoginPage";
import RegisterPage from "./pages/authentication/registerPage/RegisterPage";
import LandingPage from "./pages/landingPage/LandingPage";
import GamePage from "./pages/gamePage/GamePage";
import OAuthCallbackPage from "./pages/oauth/OAuthCallbackPage";

export const router = createBrowserRouter([
  {
    // Public routes — OAuth callback and auth pages live outside AuthInitializer
    // so they are never caught in the "redirect unauthenticated → /login" loop.
    path: "/oauth/success",
    element: <OAuthCallbackPage />,
  },
  {
    element: <AuthInitializer />,
    children: [
      {
        path: "/",
        element: <LandingPage />,
      },
      {
        path: "/login",
        element: <LoginPage />,
      },
      {
        path: "/register",
        element: <RegisterPage />,
      },
      // Protected routes — require authentication
      {
        element: <ProtectedRoute />,
        children: [
          {
            path: "game/:gameId",
            element: <GamePage />,
          },
        ],
      },
    ],
  },
  {
    path: "*",
    element: <Navigate to="/" replace />,
  },
]);
