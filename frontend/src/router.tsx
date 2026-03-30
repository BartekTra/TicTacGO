import { createBrowserRouter, Outlet } from "react-router-dom";
import { UserProvider } from "./context/UserContext";
import LoginPage from "./pages/authentication/loginPage/LoginPage";
import LandingPage from "./pages/landingPage/LandingPage";
import TicTacToeGame from "./pages/gamePage/TicTacToe";
import GamePage from "./pages/gamePage/GamePage";
export const router = createBrowserRouter([
  {
    element: (
      <UserProvider>
        <Outlet />
      </UserProvider>
    ),
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
