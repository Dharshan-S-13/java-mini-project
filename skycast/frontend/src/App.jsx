import { useEffect, useState } from "react";
import SearchBar from "./components/SearchBar.jsx";

export default function App() {
  const [city, setCity] = useState("Chennai");
  return (
    <div className="page-shell">
      <header className="site-header">
        <h1>SkyCast</h1>
      </header>
      <main>
        <SearchBar city={city} setCity={setCity} onSearch={() => {}} />
        <section id="app-placeholder">New SkyCast frontend (Vite + React)</section>
      </main>
    </div>
  );
}
