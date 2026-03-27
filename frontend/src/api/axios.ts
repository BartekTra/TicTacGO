import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_ADDRESS, 
  withCredentials: true, 
});

export default api;
