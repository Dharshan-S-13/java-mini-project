import { useState } from "react";
import { sendChat } from "../services/chatApi.js";

export default function AIChat({ city }) {
  const [messages, setMessages] = useState([
    { role: "welcome", content: "Ask a question and chat with Qwen running on your device." },
  ]);
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(event) {
    event.preventDefault();
    const message = input.trim();
    if (!message || busy) return;

    const history = messages
      .filter((item) => item.role === "user" || item.role === "assistant")
      .slice(-12)
      .map(({ role, content }) => ({ role, content }));
    setMessages((current) => [...current, { role: "user", content: message }]);
    setInput("");
    setBusy(true);
    try {
      const reply = await sendChat(message, city, history);
      setMessages((current) => [...current, { role: "assistant", content: reply }]);
    } catch (error) {
      setMessages((current) => [...current, {
        role: "assistant", content: error.message, error: true,
      }]);
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className={`chat-section${busy ? " is-thinking" : ""}`} id="assistant" aria-labelledby="chat-title">
      <div className="chat-heading">
        <img
          className="climate-robot"
          src="/images/vaanilai-weather-robot.png"
          alt=""
          width="120"
          height="120"
          aria-hidden="true"
        />
        <div className="chat-title-copy">
          <h2 id="chat-title">Ask VaanilAI</h2>
          <p>Weather help, powered by Qwen.</p>
        </div>
      </div>
      <div className="chat-messages" id="chat-messages" role="log" aria-live="polite" aria-relevant="additions text" aria-busy={busy}>
        {messages.map((item, index) => (
          <p
            className={[
              item.role === "welcome" ? "chat-welcome" : "chat-message",
              item.role !== "welcome" ? item.role : "",
              item.error ? "error" : "",
            ].filter(Boolean).join(" ")}
            key={`${index}-${item.role}`}
          >
            {item.content}
          </p>
        ))}
        {busy && (
          <div className="thinking-indicator" role="status" aria-label="VaanilAI is thinking">
            <span aria-hidden="true" />
            <span aria-hidden="true" />
            <span aria-hidden="true" />
          </div>
        )}
      </div>
      <form className="chat-form" onSubmit={submit}>
        <label className="visually-hidden" htmlFor="chat-input">Message the VaanilAI assistant</label>
        <input
          id="chat-input"
          name="message"
          type="text"
          maxLength="4000"
          placeholder="Ask me anything…"
          autoComplete="off"
          required
          value={input}
          disabled={busy}
          onChange={(event) => setInput(event.target.value)}
        />
        <button
          className="search-button"
          id="chat-send"
          type="submit"
          disabled={busy}
          aria-label={busy ? "VaanilAI is preparing a reply" : "Send message"}
        >
          {busy ? "•••" : "Send"}
        </button>
      </form>
    </section>
  );
}
