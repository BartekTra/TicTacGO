import { type User } from "../../../../types/User";

export interface UserLoginResponse {
  loginUser: UserLoginResponseData;
}

interface UserLoginResponseData {
  errors: string[];
  success: boolean;
  user: User;
}