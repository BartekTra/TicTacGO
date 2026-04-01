import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useAppDispatch } from "../../app/hooks";
import { setCredentials } from "../../features/auth/authSlice";
import api from "../../api/axios";
import type { User } from "../../types/user";

/**
 * Landing page for the OAuth redirect flow.
 *
 * The backend redirects here after a successful OAuth2 login:
 *   /oauth/success?token=<jwt>
 *
 * Steps:
 *  1. Read the JWT from the query string.
 *  2. Store it in localStorage via setCredentials.
 *  3. Fetch the full user profile from /players/me (token is now in localStorage).
 *  4. Redirect to the home page.
 *
 * If anything fails, redirects to /login?error=oauth_failed.
 */
const OAuthCallbackPage = () => {
  const [searchParams] = useSearchParams();
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    const token = searchParams.get("token");
    const error = searchParams.get("error");

    if (error || !token) {
      setErrorMessage("Logowanie przez zewnętrznego dostawcę nie powiodło się. Spróbuj ponownie.");
      setTimeout(() => navigate("/login", { replace: true }), 3000);
      return;
    }

    const finalise = async () => {
      try {
        // Store token first so the Axios interceptor can attach it
        localStorage.setItem("jwtToken", token);

        const response = await api.get<User>("/players/me");
        dispatch(setCredentials({ user: response.data, token }));

        navigate("/", { replace: true });
      } catch {
        localStorage.removeItem("jwtToken");
        setErrorMessage("Nie udało się pobrać danych użytkownika. Spróbuj ponownie.");
        setTimeout(() => navigate("/login", { replace: true }), 3000);
      }
    };

    finalise();
  }, []);

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-900">
      {errorMessage ? (
        <div className="text-center">
          <p className="mb-2 text-red-400 text-lg font-medium">{errorMessage}</p>
          <p className="text-gray-500 text-sm">Przekierowywanie do strony logowania…</p>
        </div>
      ) : (
        <div className="flex flex-col items-center gap-4">
          <div className="h-12 w-12 animate-spin rounded-full border-4 border-indigo-500 border-t-transparent" />
          <p className="text-gray-400 text-sm">Finalizowanie logowania…</p>
        </div>
      )}
    </div>
  );
};

export default OAuthCallbackPage;
