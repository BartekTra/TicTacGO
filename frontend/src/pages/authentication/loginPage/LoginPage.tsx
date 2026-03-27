import { LoginForm } from "./loginComponents/LoginForm";
import { type UserLoginResponse } from "./loginComponents/UserLoginResponse";
import { setCredentials } from "../../../features/auth/authSlice";
import { useAppDispatch } from "../../../app/hooks";
import { useNavigate } from "react-router-dom";

const LoginPage = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();

  const handleTestButton = async (index: number) => {
    let testIndex = "";
    switch (index) {
      case 1:
        testIndex = "";
        break;
      case 2:
        testIndex = "2";
        break;
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
