export type ToastTone = "info" | "success" | "danger";

export interface ToastPayload {
  message: string;
  tone: ToastTone;
}

export const TOAST_EVENT = "tinta:toast";

export function toast(message: string, tone: ToastTone = "info"): void {
  window.dispatchEvent(
    new CustomEvent<ToastPayload>(TOAST_EVENT, { detail: { message, tone } }),
  );
}
