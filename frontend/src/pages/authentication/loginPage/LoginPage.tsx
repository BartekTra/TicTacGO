import { LoginForm } from "./loginComponents/LoginForm";
import { setCredentials } from "../../../features/auth/authSlice";
import { useAppDispatch } from "../../../app/hooks";
import { useNavigate } from "react-router-dom";
import api from "../../../api/axios";
import type { User } from "../../../types/user";

const LoginPage = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();

  const handleTestButton = async (index: number) => {
    let testIndex = index === 2 ? "2" : "";

    try {
      const response = await api.post<User>("/auth/login", {
        email: `testuje${testIndex}@wp.pl`,
        password: "12qwaszx",
      });

      const token = response.data.token;
      console.log("[Login] Wyodrębniony token JWT z Body: ", token);
      
      dispatch(setCredentials({ user: response.data, token }));
      console.log("Zalogowano:", response.data);
      navigate("/");
    } catch (e: any) {
      if (e.response && e.response.data) {
        const serverMessage = e.response.data.message;
        console.error("Błąd z serwera:", serverMessage);
        alert(serverMessage);
      } else {
        console.error("Błąd połączenia:", e.message);
      }
    }
  };

  return (
    <div>
      <LoginForm />
      {import.meta.env.DEV && (
        <div>
          <button
            onClick={() => handleTestButton(1)}
            className=" w-50 h-20 bg-gray-300 outline-1 hover:bg-gray-500"
          >
            1
          </button>
          <button
            onClick={() => handleTestButton(2)}
            className="bg-gray-300  w-50 h-20 outline-1 hover:bg-gray-500"
          >
            2
          </button>
        </div>
      )}
    </div>
  );
};

export default LoginPage;
