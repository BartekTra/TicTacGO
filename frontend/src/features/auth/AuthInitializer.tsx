import { useEffect, useState } from "react";
import { Outlet, useNavigate, useLocation } from "react-router-dom";
import { useAppDispatch } from "../../app/hooks";
import { fetchCurrentUser } from "./authSlice";

const PUBLIC_PATHS = ["/login", "/register"];

export const AuthInitializer = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const initAuth = async () => {
      const isPublicPath = PUBLIC_PATHS.includes(location.pathname);
      try {
        await dispatch(fetchCurrentUser()).unwrap();
        console.log("XD?")
        if (isPublicPath) {
          navigate("/");
        }
      } catch {
        if (!isPublicPath) {
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
      <div className="flex h-screen items-center justify-center bg-gray-900">
        <div className="flex flex-col items-center gap-4">
          <div className="h-10 w-10 animate-spin rounded-full border-4 border-indigo-500 border-t-transparent" />
          <p className="text-gray-500 text-sm">Ładowanie sesji…</p>
        </div>
      </div>
    );
  }

  return <Outlet />;
};
