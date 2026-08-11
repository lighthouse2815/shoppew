import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import "@shoppew/ui/tokens.css";
import "./styles.css";
import { Providers } from "./providers";
import { App } from "./app";

createRoot(document.getElementById("root")!).render(<StrictMode><BrowserRouter><Providers><App /></Providers></BrowserRouter></StrictMode>);
