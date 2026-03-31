import { useEffect, useState } from "react";
import { Outlet, useNavigate, useLocation } from "react-router-dom";
import { useAppDispatch } from "../../app/hooks";
import { fetchCurrentUser } from "./authSlice";

export const AuthInitializer = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const initAuth = async () => {
      try {
        await dispatch(fetchCurrentUser()).unwrap();
        if (location.pathname === "/login") {
          navigate("/");
        }
      } catch {
        if (location.pathname !== "/login") {
          navigate("/login");
        }
      } finally {
        setLoading(false);
      }
    };
    initAuth();
  }, [dispatch]);

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center text-gray-500 bg-gray-900">
        Ładowanie sesji...
      </div>
    );
  }

  return <Outlet />;
};
