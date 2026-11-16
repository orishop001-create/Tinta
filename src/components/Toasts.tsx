import { useEffect, useState } from "react";
import { TOAST_EVENT, type ToastPayload, type ToastTone } from "../lib/toast";

interface Item extends ToastPayload {
  id: number;
}

export function Toasts() {
  const [items, setItems] = useState<Item[]>([]);

  useEffect(() => {
    let sequence = 0;
    const onToast = (event: Event) => {
      const detail = (event as CustomEvent<ToastPayload>).detail;
      sequence += 1;
      const id = sequence;
      setItems((prev) => [...prev.slice(-2), { id, ...detail }]);
      window.setTimeout(() => {
        setItems((prev) => prev.filter((item) => item.id !== id));
      }, 3600);
    };
    window.addEventListener(TOAST_EVENT, onToast);
    return () => window.removeEventListener(TOAST_EVENT, onToast);
  }, []);

  return (
    <div className="toasts" role="status" aria-live="polite">
      {items.map((item) => (
        <div key={item.id} className={`toast toast--${item.tone as ToastTone}`}>
          {item.message}
        </div>
      ))}
    </div>
  );
}
