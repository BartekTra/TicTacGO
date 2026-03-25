import { useState, type ChangeEvent } from "react";
import { InputField } from "../../../../components/InputField";
import { Button } from "../../../../components/Button";
import { useMutation } from "@apollo/client/react";
import { REGISTER_USER } from "../../../../graphql/mutations/authorization/registerUser";
import { useNavigate } from "react-router-dom";
import type { User } from "../../../../types/User";

interface RegisterMutationResponseData {
  registerUser: {
    errors: string[];
    user: User;
  };
}

export const RegisterForm = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    nickname: "",
    name: "",
    email: "",
    password: "",
    confirmPassword: "",
    terms: false,
  });

  const [validationError, setValidationError] = useState("");

  const [registerUser, { data, loading, error: apolloError }] =
    useMutation<RegisterMutationResponseData>(REGISTER_USER);

  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    const { id, value, type, checked } = e.target;
    setFormData((prevData) => ({
      ...prevData,
      [id]: type === "checkbox" ? checked : value,
    }));
    if (validationError) setValidationError("");
  };

  const handleRegisterSubmit = async (e: { preventDefault: () => void }) => {
    e.preventDefault();

    if (formData.password !== formData.confirmPassword) {
      setValidationError("Podane hasła nie są identyczne.");
      return;
    }

    try {
      const response = await registerUser({
        variables: {
          nickname: formData.nickname,
          name: formData.name,
          email: formData.email,
          password: formData.password,
          passwordConfirmation: formData.confirmPassword,
        },
      });

      if (response.data?.registerUser.user) {
        alert("Pomyślnie zarejestrowano");
        navigate("/login");
      }

      console.log("Rejestracja zakonczona: ", response.data);
    } catch (err) {
      console.error("Blad podczas rejestracji:", err);
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen p-4 bg-gray-900">
      <form
        onSubmit={handleRegisterSubmit}
        className="w-full max-w-md p-10 bg-gray-800 rounded-lg shadow-md"
      >
        <h2 className="mb-8 text-2xl font-bold text-center text-gray-50">
          Załóż konto
        </h2>

        {(validationError || apolloError) && (
          <div className="mb-4 text-sm text-red-600 bg-red-100 p-3 rounded">
            {validationError || apolloError?.message}
          </div>
        )}

        <InputField
          label="Nickname"
          id="nickname"
          type="text"
          placeholder="Your displayed nickname"
          required
          value={formData.nickname}
          onChange={handleChange}
        />

        <InputField
          label="Name"
          id="name"
          type="text"
          placeholder="Your name"
          required
          value={formData.name}
          onChange={handleChange}
        />

        <InputField
          label="Adres e-mail"
          id="email"
          type="email"
          placeholder="your e-mail address"
          required
          value={formData.email}
          onChange={handleChange}
        />

        <InputField
          label="Hasło"
          id="password"
          type="password"
          placeholder="password"
          required
          value={formData.password}
          onChange={handleChange}
        />

        <InputField
          label="Powtórz hasło"
          id="confirmPassword"
          type="password"
          placeholder="confirm password"
          required
          value={formData.confirmPassword}
          onChange={handleChange}
        />

        <div className="flex items-start mb-6">
          <input
            type="checkbox"
            id="terms"
            checked={formData.terms}
            onChange={handleChange}
            className="w-4 h-4 mt-0.5 text-blue-600 transition duration-150 ease-in-out border-gray-300 rounded cursor-pointer focus:ring-blue-500"
            required
          />
          <label
            htmlFor="terms"
            className="ml-2 text-sm text-gray-600 cursor-pointer"
          >
            Akceptuję{" "}
            <a
              href="/terms"
              className="font-medium text-blue-600 hover:underline"
            >
              Regulamin
            </a>{" "}
            oraz{" "}
            <a
              href="/privacy"
              className="font-medium text-blue-600 hover:underline"
            >
              Politykę Prywatności
            </a>
          </label>
        </div>

        <Button type="submit" disabled={loading}>
          {loading ? "Rejestrowanie..." : "Zarejestruj się"}
        </Button>

        <p className="mt-6 text-sm text-center text-gray-500">
          Masz już konto?{" "}
          <a
            href="/login"
            className="font-medium text-blue-600 hover:underline"
          >
            Zaloguj się
          </a>
        </p>
      </form>
    </div>
  );
};
