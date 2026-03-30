import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_ADDRESS,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("jwtToken");
  console.log("[Axios] Token loading from storage:", token ? "FOUND" : "MISSING");
  if (token) {
    config.headers.set("Authorization", `Bearer ${token}`);
  }
  return config;
});

export default api;
