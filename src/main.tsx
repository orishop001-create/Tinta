import ReactDOM from "react-dom/client";
import "@fontsource-variable/newsreader/index.css";
import "@fontsource-variable/instrument-sans/index.css";
import "./styles/tokens.css";
import "./styles/app.css";
import App from "./App";
import { StoreProvider } from "./lib/store";

const root = document.getElementById("root");
if (!root) throw new Error("Elemen #root tidak ditemukan");

ReactDOM.createRoot(root).render(
  <StoreProvider>
    <App />
  </StoreProvider>,
);
