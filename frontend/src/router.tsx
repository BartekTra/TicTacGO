import { createBrowserRouter } from "react-router-dom";
import { AuthInitializer } from "./features/auth/AuthInitializer";
import LoginPage from "./pages/authentication/loginPage/LoginPage";
import RegisterPage from "./pages/authentication/registerPage/RegisterPage";
import LandingPage from "./pages/landingPage/LandingPage";
import GamePage from "./pages/gamePage/GamePage";

export const router = createBrowserRouter([
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
      {
        path: "game/:gameId",
        element: <GamePage />,
      },
    ],
  },
]);
