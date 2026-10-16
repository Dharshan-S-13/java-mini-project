import { useState } from "react";

export default function SearchBar({ city, setCity, onSearch }) {
  const [value, setValue] = useState(city || "");
  function submit(e) { e.preventDefault(); onSearch(value); }
  return (
    <form onSubmit={submit} className="search-form">
      <input value={value} onChange={(e) => { setValue(e.target.value); setCity(e.target.value); }} placeholder="Search a city" />
      <button type="submit">Search</button>
    </form>
  );
}
