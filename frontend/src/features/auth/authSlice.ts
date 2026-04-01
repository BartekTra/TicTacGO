import { createSlice, createAsyncThunk, type PayloadAction } from "@reduxjs/toolkit";
import { type User } from "../../types/user";
import { type AuthState } from "../../types/authState";
import api from "../../api/axios";

export const fetchCurrentUser = createAsyncThunk<User, void, { rejectValue: string }>(
  "auth/fetchCurrentUser",
  async (_, { rejectWithValue }) => {
    try {
      const response = await api.get<User>("/players/me");
      console.log(response.data);
      return response.data;
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || "Failed to fetch user");
    }
  }
);

const initialState: AuthState = {
  user: null,
  isAuthenticated: false,
};

const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {
    setCredentials: (state, action: PayloadAction<{ user: User; token?: string }>) => {
      state.user = action.payload.user;
      state.isAuthenticated = true;
      if (action.payload.token) {
        localStorage.setItem("jwtToken", action.payload.token);
      }
    },
    logout: (state) => {
      state.user = null;
      state.isAuthenticated = false;
      localStorage.removeItem("jwtToken");
    },
    updateUser: (state, action: PayloadAction<User>) => {
      state.user = action.payload;
      state.isAuthenticated = true;
    }
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchCurrentUser.fulfilled, (state, action) => {
        state.user = action.payload;
        state.isAuthenticated = true;
      })
      .addCase(fetchCurrentUser.rejected, (state) => {
        state.user = null;
        state.isAuthenticated = false;
        localStorage.removeItem("jwtToken");
      });
  },
});

export const { setCredentials, logout, updateUser } = authSlice.actions;
export default authSlice.reducer;
