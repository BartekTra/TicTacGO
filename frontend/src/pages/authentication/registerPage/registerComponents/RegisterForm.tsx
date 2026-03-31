import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAppDispatch } from "../../../../app/hooks";
import { setCredentials } from "../../../../features/auth/authSlice";
import { type User } from "../../../../types/user";
import api from "../../../../api/axios";
import { InputField } from "../../../../components/InputField";
import { Button } from "../../../../components/Button";

export const RegisterForm = () => {
  const [username, setUsername] = useState<string>("");
  const [email, setEmail] = useState<string>("");
  const [password, setPassword] = useState<string>("");
  const [confirmPassword, setConfirmPassword] = useState<string>("");
  const [error, setError] = useState<string | null>(null);
  const dispatch = useAppDispatch();
  const navigate = useNavigate();

  const handleRegister = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError(null);

    if (password !== confirmPassword) {
      setError("Hasła nie są identyczne.");
      return;
    }

    try {
      const response = await api.post<User>("/auth/register", {
        username,
        email,
        password,
      });

      const token = response.data.token;
      dispatch(setCredentials({ user: response.data, token }));
      navigate("/");
    } catch (err: any) {
      const serverMessage = err.response?.data?.message;
      setError(serverMessage ?? "Wystąpił błąd podczas rejestracji.");
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen p-4 bg-gray-700">
      <form
        onSubmit={handleRegister}
        className="w-full max-w-sm p-10 bg-gray-900 rounded-lg shadow-md"
      >
        <h2 className="mb-8 text-2xl font-bold text-center text-gray-100">
          Zarejestruj się
        </h2>

        <InputField
          label="Nazwa użytkownika"
          id="username"
          type="text"
          placeholder="jan_kowalski"
          onChange={(e) => setUsername(e.target.value)}
          required
        />

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

        <InputField
          label="Powtórz hasło"
          id="confirmPassword"
          type="password"
          placeholder="••••••••"
          onChange={(e) => setConfirmPassword(e.target.value)}
          required
        />

        {error && (
          <p role="alert" className="mb-4 text-sm text-red-400 text-center">
            {error}
          </p>
        )}

        <Button type="submit">Zarejestruj</Button>

        <p className="mt-6 text-sm text-center text-gray-100">
          Masz już konto?{" "}
          <a href="/login" className="font-medium text-gray-100 hover:underline">
            Zaloguj się
          </a>
        </p>
      </form>
    </div>
  );
};
