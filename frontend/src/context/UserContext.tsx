import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import api from "../api/axios";
import { useNavigate } from "react-router-dom";
import { type User } from "../types/user";

interface UserContextType {
  user: User | null;
  loading: boolean;
  refetchUser: () => Promise<void>;
  setUser: React.Dispatch<React.SetStateAction<User | null>>;
  logout: () => Promise<void>;
}

const UserContext = createContext<UserContextType | null>(null);

interface UserProviderProps {
  children: ReactNode;
}

export const UserProvider = ({ children }: UserProviderProps) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const navigate = useNavigate();

  const fetchCurrentUser = async () => {
    setLoading(true);
    try {
      const response = await api.get<User>("/players/me");
      setUser(response.data);
      console.log(response.data);
    } catch (error) {
      console.error("Error fetching user:", error);
      setUser(null);
      navigate("/login");
    } finally {
      setLoading(false);
    }
  };

  const logout = async () => {
    try {
      await api.delete("/auth/sign_out");
    } catch (error) {
      console.error("Error during logout:", error);
    } finally {
      setUser(null);
      navigate("/login");
    }
  };

  useEffect(() => {
    fetchCurrentUser();
  }, []);

  return (
    <UserContext.Provider
      value={{ user, loading, refetchUser: fetchCurrentUser, setUser, logout }}
    >
      {children}
    </UserContext.Provider>
  );
};

export const useUser = (): UserContextType => {
  const context = useContext(UserContext);
  if (!context) {
    throw new Error("useUser must be used within a UserProvider");
  }
  return context;
};
