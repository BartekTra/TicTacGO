import { createBrowserRouter } from "react-router-dom";
import { AuthInitializer } from "./features/auth/AuthInitializer";
import LoginPage from "./pages/authentication/loginPage/LoginPage";
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
        path: "game/:gameId",
        element: <GamePage />,
      },
    ],
  },
]);
