import { useState } from "react";
import api from "../../../../api/axios";
import { InputField } from "../../../../components/InputField";
import { Button } from "../../../../components/Button";
import { useAppDispatch } from "../../../../app/hooks";
import { setCredentials } from "../../../../features/auth/authSlice";
import { type User } from "../../../../types/user";
import { useNavigate, useSearchParams } from "react-router-dom";

const API_URL = import.meta.env.VITE_API_ADDRESS as string;

// ── OAuth provider button ────────────────────────────────────────────────────

interface OAuthButtonProps {
  provider: "google" | "github";
  label: string;
  icon: React.ReactNode;
}

const OAuthButton = ({ provider, label, icon }: OAuthButtonProps) => {
  const handleClick = () => {
    // Redirect the browser to Spring's OAuth2 authorization endpoint.
    // Spring Security handles the rest of the OAuth flow server-side.
    window.location.href = `${API_URL}/oauth2/authorization/${provider}`;
  };

  return (
    <button
      type="button"
      onClick={handleClick}
      className="flex w-full items-center justify-center gap-3 rounded-md border border-gray-600 bg-gray-800 px-4 py-2.5 text-sm font-medium text-gray-200 transition-colors duration-200 hover:bg-gray-700 hover:border-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-1 focus:ring-offset-gray-900 cursor-pointer"
    >
      {icon}
      {label}
    </button>
  );
};

// ── Icon SVGs (inline, no extra deps) ───────────────────────────────────────

const GoogleIcon = () => (
  <svg viewBox="0 0 24 24" className="h-5 w-5" aria-hidden="true">
    <path
      d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
      fill="#4285F4"
    />
    <path
      d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
      fill="#34A853"
    />
    <path
      d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z"
      fill="#FBBC05"
    />
    <path
      d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
      fill="#EA4335"
    />
  </svg>
);

const GitHubIcon = () => (
  <svg viewBox="0 0 24 24" className="h-5 w-5 fill-current text-gray-200" aria-hidden="true">
    <path d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z" />
  </svg>
);

// ── Divider ──────────────────────────────────────────────────────────────────

const OrDivider = () => (
  <div className="relative my-6">
    <div className="absolute inset-0 flex items-center" aria-hidden="true">
      <div className="w-full border-t border-gray-700" />
    </div>
    <div className="relative flex justify-center text-xs uppercase">
      <span className="bg-gray-900 px-3 text-gray-500 tracking-wider">lub</span>
    </div>
  </div>
);

// ── Main form ────────────────────────────────────────────────────────────────

export const LoginForm = () => {
  const [email, setEmail] = useState<string>("");
  const [password, setPassword] = useState<string>("");
  const [rememberMe, setRememberMe] = useState<boolean>(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  // Show a user-friendly message if redirected back from a failed OAuth attempt
  const oauthError = searchParams.get("error");

  const handleLogin = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setErrorMsg(null);
    try {
      const response = await api.post<User>("/auth/login", { email, password });
      const token = response.data.token;
      dispatch(setCredentials({ user: response.data, token }));
      navigate("/");
    } catch (error: any) {
      const message = error.response?.data?.message ?? "Błąd połączenia z serwerem.";
      setErrorMsg(message);
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen p-4 bg-gray-900">
      <div className="w-full max-w-sm">
        <form
          onSubmit={handleLogin}
          className="p-10 bg-gray-900 rounded-2xl shadow-2xl border border-gray-800"
        >
          <h2 className="mb-2 text-2xl font-bold text-center text-gray-100">
            Witaj ponownie
          </h2>
          <p className="mb-8 text-sm text-center text-gray-500">
            Zaloguj się, aby kontynuować grę
          </p>

          {/* OAuth error banner */}
          {oauthError && (
            <div className="mb-4 rounded-md bg-red-900/40 border border-red-700 px-4 py-2.5 text-sm text-red-300">
              Logowanie przez zewnętrznego dostawcę nie powiodło się. Spróbuj ponownie.
            </div>
          )}

          {/* ── OAuth buttons ── */}
          <div className="flex flex-col gap-3">
            <OAuthButton
              provider="google"
              label="Kontynuuj z Google"
              icon={<GoogleIcon />}
            />
            <OAuthButton
              provider="github"
              label="Kontynuuj z GitHub"
              icon={<GitHubIcon />}
            />
          </div>

          <OrDivider />

          {/* ── Email / password form ── */}
          <InputField
            label="Adres e-mail"
            id="email"
            type="email"
            placeholder="jan.kowalski@example.com"
            onChange={(e) => setEmail(e.target.value)}
            required
          />

          <InputField
            label="Hasło"
            id="password"
            type="password"
            placeholder="••••••••"
            onChange={(e) => setPassword(e.target.value)}
            required
          />

          <div className="flex items-center justify-between mb-6">
            <div className="flex items-center">
              <input
                type="checkbox"
                id="remember"
                className="w-4 h-4 text-indigo-500 border-gray-600 rounded cursor-pointer focus:ring-indigo-500"
                onChange={(e) => setRememberMe(e.target.checked)}
                checked={rememberMe}
              />
              <label htmlFor="remember" className="ml-2 text-sm text-gray-400 cursor-pointer">
                Zapamiętaj mnie
              </label>
            </div>
            <a href="/reset-password" className="text-sm font-medium text-indigo-400 hover:text-indigo-300 hover:underline">
              Zapomniałeś hasła?
            </a>
          </div>

          {/* Inline error message */}
          {errorMsg && (
            <p className="mb-4 text-sm text-center text-red-400">{errorMsg}</p>
          )}

          <Button type="submit">Zaloguj się</Button>

          <p className="mt-6 text-sm text-center text-gray-500">
            Nie masz jeszcze konta?{" "}
            <a href="/register" className="font-medium text-indigo-400 hover:text-indigo-300 hover:underline">
              Zarejestruj się
            </a>
          </p>
        </form>
      </div>
    </div>
  );
};
