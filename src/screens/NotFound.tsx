import { Compass } from "lucide-react";
import { EmptyState } from "../components/EmptyState";

export function NotFoundScreen() {
  return (
    <EmptyState
      icon={<Compass size={26} />}
      title="Halaman tidak ditemukan"
      description="Alamat ini tidak ada di Tinta. Kembali ke pustaka dan pilih tulisan yang ingin dibuka."
      action={
        <a className="btn btn-primary" href="#/pustaka">
          Ke pustaka
        </a>
      }
    />
  );
}
