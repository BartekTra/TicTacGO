import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "./index.css";
import { router } from "./router.tsx";
import { Provider } from "react-redux";

import { RouterProvider } from "react-router-dom";
import { UserProvider } from "./context/UserContext.tsx";
import { store } from "./app/store.ts";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <Provider store={store}>
      <RouterProvider router={router} />
    </Provider>
  </StrictMode>,
);
