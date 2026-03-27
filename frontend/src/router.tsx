import { createBrowserRouter, Outlet } from "react-router-dom";
import { UserProvider } from "./context/UserContext";
import LoginPage from "./pages/authentication/loginPage/LoginPage";
export const router = createBrowserRouter([
  {
    element: (
      <UserProvider>
        <Outlet />
      </UserProvider>
    ),
    children: [
      {
        path: "/login",
        element: <LoginPage />,
      }
    ],
  },
]);
