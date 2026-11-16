import { useEffect, useRef, type ReactNode } from "react";
import { MoreVertical } from "lucide-react";

interface MenuProps {
  label: string;
  children: (close: () => void) => ReactNode;
  trigger?: ReactNode;
  className?: string;
}

export function Menu({ label, children, trigger, className = "" }: MenuProps) {
  const ref = useRef<HTMLDetailsElement>(null);

  useEffect(() => {
    const onPointerDown = (event: PointerEvent) => {
      if (ref.current && !ref.current.contains(event.target as Node)) {
        ref.current.open = false;
      }
    };
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape" && ref.current?.open) {
        ref.current.open = false;
        ref.current.querySelector<HTMLElement>("summary")?.focus();
      }
    };
    document.addEventListener("pointerdown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("pointerdown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, []);

  return (
    <details className={`menu ${className}`.trim()} ref={ref}>
      <summary className="icon-btn menu-trigger" aria-label={label} title={label}>
        {trigger ?? <MoreVertical size={17} />}
      </summary>
      <div className="menu-panel" role="menu" aria-label={label}>
        {children(() => {
          if (ref.current) ref.current.open = false;
        })}
      </div>
    </details>
  );
}

interface MenuItemProps {
  children: ReactNode;
  icon?: ReactNode;
  onClick: () => void;
  danger?: boolean;
  disabled?: boolean;
}

export function MenuItem({ children, icon, onClick, danger, disabled }: MenuItemProps) {
  return (
    <button
      type="button"
      role="menuitem"
      className={`menu-item${danger ? " menu-item--danger" : ""}`}
      onClick={onClick}
      disabled={disabled}
    >
      {icon ? <span className="menu-item-icon">{icon}</span> : null}
      <span>{children}</span>
    </button>
  );
}

export function MenuSeparator() {
  return <div className="menu-sep" role="separator" />;
}

export function MenuLabel({ children }: { children: ReactNode }) {
  return <div className="menu-label">{children}</div>;
}
