import { useState, type ChangeEvent } from "react";
import { useNavigate } from "react-router-dom";
import api from "../../../../api/axios";
import { InputField } from "../../../../components/InputField";
import { Button } from "../../../../components/Button";
import { useAppDispatch } from "../../../../app/hooks";
import { setCredentials } from "../../../../features/auth/authSlice";
import { type User } from "../../../../types/user";
export const LoginForm = () => {
  const [email, setEmail] = useState<string>("");
  const [password, setPassword] = useState<string>("");
  const [rememberMe, setRememberMe] = useState<boolean>(false);
  const navigate = useNavigate();
  const dispatch = useAppDispatch();

  const handleLogin = async (e: ChangeEvent<HTMLFormElement>) => {
    e.preventDefault();
    try {
      const { data } = await api.post<User>("/auth/login", {
        variables: { nickname: email, password: password },
      });

      dispatch(setCredentials(data));
    } catch (error) {
      console.error("Błąd logowania:", error);
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen p-4 bg-gray-700">
      <form
        onSubmit={handleLogin}
        className="w-full max-w-sm p-10 bg-gray-900 rounded-lg shadow-md"
      >
        <h2 className="mb-8 text-2xl font-bold text-center text-gray-100">
          Zaloguj się
        </h2>

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
              className="w-4 h-4 text-gray-100 transition duration-150 ease-in-out border-gray-300 rounded cursor-pointer focus:ring-blue-500"
              onChange={(e) => setRememberMe(e.target.checked)}
              checked={rememberMe}
            />
            <label
              htmlFor="remember"
              className="ml-2 text-sm text-gray-100 cursor-pointer"
            >
              Zapamiętaj mnie
            </label>
          </div>
          <a
            href="/reset-password"
            className="text-sm font-medium text-gray-100 hover:underline"
          >
            Zapomniałeś hasła?
          </a>
        </div>

        <Button type="submit">Zaloguj</Button>

        <p className="mt-6 text-sm text-center text-gray-100">
          Nie masz jeszcze konta?{" "}
          <a
            href="/register"
            className="font-medium text-gray-100 hover:underline"
          >
            Zarejestruj się
          </a>
        </p>
      </form>
    </div>
  );
};
