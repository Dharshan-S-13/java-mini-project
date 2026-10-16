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
    setMessages((current) => [...current, { role: "user", content: message }, {
      role: "assistant", content: "Thinking…", thinking: true,
    }]);
    setInput("");
    setBusy(true);
    try {
      const reply = await sendChat(message, city, history);
      setMessages((current) => current.map((item, index) => (
        index === current.length - 1 ? { role: "assistant", content: reply } : item
      )));
    } catch (error) {
      setMessages((current) => current.map((item, index) => (
        index === current.length - 1
          ? { role: "assistant", content: error.message, error: true }
          : item
      )));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="chat-section" aria-labelledby="chat-title">
      <div className="chat-heading">
        <div><p className="eyebrow">Your local AI assistant</p><h2 id="chat-title">Ask VaanilAI</h2></div>
        <span className="chat-model">Ollama · Qwen 3.5</span>
      </div>
      <div className="chat-messages" id="chat-messages" role="log" aria-live="polite" aria-relevant="additions text">
        {messages.map((item, index) => (
          <p
            className={[
              item.role === "welcome" ? "chat-welcome" : "chat-message",
              item.role !== "welcome" ? item.role : "",
              item.thinking ? "thinking" : "",
              item.error ? "error" : "",
            ].filter(Boolean).join(" ")}
            key={`${index}-${item.role}`}
          >
            {item.content}
          </p>
        ))}
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
        <button className="search-button" id="chat-send" type="submit" disabled={busy}>
          {busy ? "Thinking…" : "Send"}
        </button>
      </form>
      <p className="chat-note">Messages are sent to Ollama on this device. Start Ollama before chatting.</p>
    </section>
  );
}
