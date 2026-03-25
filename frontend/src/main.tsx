import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "./index.css";
import client from "./apolloClient";
import { ApolloProvider } from "@apollo/client/react";
import { Provider } from "react-redux";
import { store } from "./app/store.ts";
import { RouterProvider } from "react-router-dom";
import { router } from "./router.tsx";
// test
createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <Provider store={store}>
      <ApolloProvider client={client}>
          <RouterProvider router={router} />
      </ApolloProvider>
    </Provider>
  </StrictMode>,
);
